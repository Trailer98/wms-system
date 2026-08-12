# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Run the application (requires MySQL running locally)
cd wms-admin && mvn spring-boot:run

# Build all modules
mvn clean package -DskipTests

# Run all tests
mvn test

# Run a single test class
mvn test -pl wms-admin -Dtest=AiRagAskServiceTest

# Run a specific test method
mvn test -pl wms-admin -Dtest=AiRagAskServiceTest#test
```

**Prerequisites:** MySQL must be running at `localhost:3306` with database `wms_system`. The connection string/credentials are **not** in this repo — they come from the Nacos config center (dataId `wms-service-dev.yaml`, group `WMS_GROUP`), imported unconditionally by `wms-admin/src/main/resources/application.yml` regardless of which Spring profile you run with. The schema is managed by **Flyway** (`db/migration/V1__init_schema.sql` … `V9__add_developer_role.sql`); the old `schema.sql`/`spring.sql.init.mode: always` mechanism this section used to describe is gone — `application.yml` now sets `spring.sql.init.mode: never` explicitly.

The application starts on `http://localhost:8083/wms` (not `8081`/`/api` — that was true before this service registered with Nacos and adopted the `/wms` context-path). API docs (Knife4j) are available at `http://localhost:8083/wms/doc.html`.

**AI feature:** RAG endpoints are `POST /wms/ai/rag/ask` and `POST /wms/ai/rag/ask/stream` (`AiRagAskController`), not `/api/ai/chat`. The chat model is DeepSeek `deepseek-v4-flash`, configured via Nacos (`wms-service.yaml`/`WMS_GROUP`, `spring.ai.deepseek.chat.model`) — it is **not** read from a `DEEPSEEK_MODEL` env var. `DEEPSEEK_API_KEY` is still required as an env var (`spring.ai.deepseek.api-key`).

> Full, kept-current runtime/config/API details (incl. what actually lives in Nacos) are in [PROJECT_CONTEXT.md](PROJECT_CONTEXT.md) — prefer that over hand-updating this section again.

## Architecture

### Module layout

This is a Maven multi-module project. Only `wms-admin` contains active business logic; `wms-framework` and `wms-modules` are empty scaffolding.

- **`wms-common`** — shared library: `ApiResponse`, `BusinessException`, `GlobalExceptionHandler`, `PageRequest`/`PageResponse`, business enums (`InboundOrderStatus`, `OutboundOrderStatus`, `MovementType`).
- **`wms-admin`** — the runnable Spring Boot application. Contains all domain logic: controllers, services, entities, mappers.

### Layering within `wms-admin`

```
controller/      HTTP endpoints — thin, delegate to service
service/         Business logic, transactions, domain operations
model/entity/    MyBatis-Plus entities (domain objects with behavior)
model/mapper/    MyBatis-Plus mapper interfaces
view/dto/        Java records for request/response DTOs
config/          Spring config beans
annotation/      Custom annotations (@SysOperationLog)
aspect/          AOP (SysOperationAspect)
```

### Key design patterns

**Entities carry domain behavior.** Entities are not anemic. `InboundOrder.markReceived()`, `InboundOrder.addItem()`, `Inventory.increase()`, `Inventory.decrease()` — business state transitions live on the entity, not in the service. Services orchestrate, entities enforce invariants.

**Assembly pattern for reads.** Because MyBatis-Plus does not auto-join, services have an `assemble()` method that manually attaches related entities (warehouse, SKU, items) after loading from separate mappers. See `InboundOrderService.assemble()` and `InventoryService.assemble()`.

**Soft delete via `enabled` flag.** All entities use `@TableLogic(value = "1", delval = "0")` on an `enabled` boolean. MyBatis-Plus applies the filter automatically.

**ID generation:** MyBatis-Plus Snowflake (`id-type: assign_id`).

**Audit logging.** Every mutating controller method must carry `@SysOperationLog(operationType="...", bizNo="...")`. The aspect runs the handler, then asynchronously saves a log record via `SysOperationLogAsyncService` on the `operationLogExecutor` thread pool. `bizNo` is a SpEL expression evaluated against `#request.*` (method args) or `#result.*` (return value).

**Pagination.** Query DTOs extend `PageRequest` from `wms-common`. Services call `inventoryMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()), ...)` and wrap the result with `PageResponse.from(page, converter)`.

**Error handling.** Throw `BusinessException(message)` for all domain errors — `GlobalExceptionHandler` maps these to HTTP 400. Do not throw `BusinessException` for upstream/infrastructure failures (those should map to 5xx).

**Response envelope.** All controller methods return `ApiResponse<T>`. Use `ApiResponse.ok(data)` for success and `ApiResponse.fail(code, message)` for failures (handled by `GlobalExceptionHandler`).

### Spring AI integration

**This section previously described a `WmsAiConfig`/`wmsChatClient`/`AiChatService` design that no longer exists in the codebase** (verified 2026-08-10: no such classes anywhere under `wms-admin`). The current AI/RAG entry points are `controller/ai/AiRagAskController.java` (`/ai/rag/ask`, `/ai/rag/ask/stream`) and `controller/ai/KnowledgeController.java` (`/ai/knowledge/**`), backed by `service/AiRagAskService.java`; RAG retrieval parameters (`top-k`, `similarity-threshold`) bind from `config/RagProperties.java` (`app.ai.rag.*`, sourced from Nacos `wms-service.yaml`). The actual Spring AI version is `1.0.3` (`spring-ai-bom`), using `spring-ai-starter-model-deepseek` + `spring-ai-starter-model-ollama` + `spring-ai-pgvector-store` — not the `1.0.0-M5` OpenAI-adapter milestone this section used to describe. Do not trust the rest of this paragraph's specifics without re-checking the source; see [PROJECT_CONTEXT.md](PROJECT_CONTEXT.md) §5/§8 for what's currently confirmed.

# Project Rules

这是一个 Spring Boot + Maven 多模块 WMS 项目。

## 模块结构

- wms-admin：启动模块，包含 Controller、配置、启动类
- wms-common：通用工具、通用返回对象、异常等
- wms-framework：框架层、配置、拦截器等
- wms-modules：业务模块

## 编码要求

- 不要随意改包名、模块名、groupId、artifactId、version
- 不要大范围重构，除非用户明确要求
- 修改 Maven 依赖时，先说明为什么需要改
- 每次修改后运行：
  ./mvnw -pl wms-admin -am clean test

## 输出要求

每次完成任务后输出：
1. 修改了哪些文件
2. 为什么这样改
3. 验证命令和结果
4. 仍然存在的风险

# Project Context Rules

> These rules supplement everything above; they don't override or remove any pre-existing content in this file.

Before performing project tasks:

1. Read PROJECT_CONTEXT.md first.
2. For cross-project tasks, read `/data/projects/SYSTEM_CONTEXT.md`.
3. Do not scan the whole repository by default.
4. Use PROJECT_CONTEXT.md to locate relevant files first.
5. Read source code when implementation details are required.
6. Code is the final source of truth.
7. After modifying code, determine whether PROJECT_CONTEXT.md needs to be updated.
8. Update PROJECT_CONTEXT.md only when project navigation or important architectural/business facts changed.
9. For cross-service architectural changes, also check:
   - `/data/projects/auth-service/PROJECT_CONTEXT.md`
   - `/data/projects/gateway-service/PROJECT_CONTEXT.md`
   - `/data/projects/SYSTEM_CONTEXT.md`
10. If documentation conflicts with code: CODE IS SOURCE OF TRUTH. If the conflict involves explicit system architecture conventions (e.g. WMS's port isn't 8083 anymore, or it starts owning user/role/permission again for real), report the conflict before changing architecture.

### Known legacy issues (confirmed when this doc was written — don't re-investigate from scratch)

- 本文件上方的"Commands"/"Spring AI integration"两处过期内容**已于 2026-08-10 更正**（端口/context-path、schema 机制、AI 端点/模型来源、`WmsAiConfig` 等已不存在的类名引用）。"Module layout"/"Layering"/"Key design patterns" 几节未发现反例，未改动，但也未逐条重新验证，遇到不一致以代码为准。
- 仓库内仍有一套完整的历史本地 RBAC 实现（`SysUser`/`SysRole`/`SysPermission` 相关代码），默认被拦截器/开关阻断，不要在其基础上继续开发，也不要把它当作"WMS 负责用户权限"的证据——详见 [PROJECT_CONTEXT.md](PROJECT_CONTEXT.md) 第 13 节。
- WMS 数据源配置外置于 Nacos 配置中心，真实值已于 2026-08-10 核实并记录在 [PROJECT_CONTEXT.md](PROJECT_CONTEXT.md) 第 2 节（会随 Nacos 内容变化而过期，需要时重新查询而不是死记）。
