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
mvn test -pl wms-admin -Dtest=SpringAiTest

# Run a specific test method
mvn test -pl wms-admin -Dtest=SpringAiTest#test
```

**Prerequisites:** MySQL must be running at `localhost:3306` with database `wms_system`, user `root`, password `root123456`. The schema is auto-applied on startup via `schema.sql` (`spring.sql.init.mode: always`).

The application starts on `http://localhost:8081/api`. API docs (Knife4j) are available at `http://localhost:8081/doc.html`.

**AI feature:** Set `DEEPSEEK_API_KEY` environment variable before starting if using the `/api/ai/chat` endpoint. `DEEPSEEK_MODEL` defaults to `deepseek-chat`.

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

`WmsAiConfig` creates a `wmsChatClient` bean by applying the system prompt from `WmsAiProperties` (`wms.ai.system-prompt`) to a `ChatClient.Builder`. `AiChatService` injects `@Qualifier("wmsChatClient")` to use this configured client for all chat calls.

The AI feature connects to DeepSeek's API via the Spring AI OpenAI adapter (`spring-ai-openai-spring-boot-starter`, version `1.0.0-M5` milestone). The Spring Milestones repository is declared in the root `pom.xml` because milestone artifacts are not on Maven Central.

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