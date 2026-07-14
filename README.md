# WMS 后端启动说明

这是 WMS（仓储管理系统）后端服务，Spring Boot 3 单体应用。默认 context-path 为 `/api`，默认端口 `8081`（见 [启动前置条件](#2-启动前置条件) 中的配置来源说明）。

> 技术栈、模块划分、业务功能等背景说明见文末 [附录：技术栈说明](#10-附录技术栈说明)。本文重点是"如何在本地把它跑起来"。

## 1. 后端定位

- 运行入口模块：`wms-admin`（唯一包含可运行 `@SpringBootApplication` 的模块）
- Maven 多模块 reactor 根：`wms-system/pom.xml`（`wms-common` 为公共依赖，`wms-framework`、`wms-modules` 目前是空壳模块，无实际代码）
- 所有命令均从 `wms-system/` 目录（reactor 根目录）执行

## 2. 启动前置条件

| 依赖 | 版本要求 | 来源 |
|---|---|---|
| JDK | 17 | `pom.xml` `<java.version>17</java.version>` |
| Maven | 建议 3.9+（未见 `mvnw` 挂在 reactor 根目录，请使用本机已安装的 `mvn`） | 环境要求，无 wrapper 锁定 |
| MySQL | 建议 8.x（驱动为 `mysql-connector-j`，未见版本强约束） | `wms-admin/pom.xml` |
| PostgreSQL + pgvector | 建议使用项目自带镜像 `pgvector/pgvector:0.8.5-pg17` | `docker-compose-ai.yml` |
| Ollama（可选，AI 向量化需要） | 需能提供 `bge-m3` 模型 | `application-ai.yml` |
| DeepSeek API Key（**必须设置环境变量，但可选是否使用真实 Key**，见下方说明） | - | `application-ai.yml` |

**重要：不使用 AI 功能也必须设置 `DEEPSEEK_API_KEY` 环境变量。**
`application.yml` 中 `spring.profiles.group.dev: [ai]` 表示默认的 `dev` profile 会自动一并激活 `ai` profile；`application-ai.yml` 里 `spring.ai.deepseek.api-key: ${DEEPSEEK_API_KEY}` 没有默认值。`wms-admin/src/test/resources/application-test.yml` 的注释明确写到：如果 `ai` profile 激活但没有关闭 chat/embedding 模型，"the DeepSeek model fails the whole context with 'API key must be set'"。也就是说：**用默认方式启动（不额外指定 profile）时，只要 `DEEPSEEK_API_KEY` 未设置，应用会在启动阶段直接失败**，即使你完全不打算用 AI/RAG 功能。

- 不想用 AI 功能：随便设置一个占位值即可让应用正常启动，例如 `export DEEPSEEK_API_KEY=placeholder`；只有真正调用 RAG 问答接口时才会因为 Key 无效而报错。
- 想用 AI 功能：需要一个真实有效的 DeepSeek Key，并额外准备 Ollama + PostgreSQL/pgvector（见第 4 节）。

## 3. 本地数据库准备

### MySQL（业务主库，必需）

- 用途：全部业务数据的唯一真源
- 数据库名：`wms_system`（来自 `application-dev.yml` 的 `spring.datasource.url`，这是默认 `dev` profile 使用的库）
- 用户名 / 密码：`root` / `root123456`（`application-dev.yml` 中的本地开发默认值；如果你修改过这个文件，请以你本地实际配置为准）
- 初始化方式：**Flyway 自动执行**，无需手工建表。首次连接一个全新空库时，Flyway 会按顺序执行 `wms-admin/src/main/resources/db/migration/V1__init_schema.sql` 到 `V9__add_developer_role.sql`（建表 + RBAC 基础数据 + 字典数据 + AI 知识库表 + 默认账号，见第 8 节）

创建空库：

```sql
CREATE DATABASE IF NOT EXISTS wms_system DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

> 如果你本地已有一个由旧版 `schema.sql` 建好的非空 `wms_system` 库（历史遗留，见 `docs/legacy-sql/schema.sql`），`dev` profile 已经配置了 `flyway.baseline-on-migrate=true`（版本号 1），Flyway 会自动把该库基线到 V1、只执行 V2 之后的脚本。更完整的迁移/基线策略说明见 [`docs/database-migration.md`](docs/database-migration.md)。

### PostgreSQL + pgvector（AI 知识库向量索引，仅 AI/RAG 功能需要）

- 用途：AI 知识库的向量检索索引，MySQL 中的 `knowledge_document` / `knowledge_chunk` 才是主数据，pgvector 只是可重建的索引副本
- 是否基础业务必需：**不是**。仓库/入库/出库/库存/盘点等核心 WMS 功能不依赖这个库
- 项目已提供现成的 Docker Compose 定义（`wms-system/docker-compose-ai.yml`），可以直接使用，无需自己新建：

```bash
cd wms-system
docker compose -f docker-compose-ai.yml up -d
```

该文件会启动一个 `wms-pgvector` 容器（宿主机 `5433` 映射容器 `5432`，库名/用户/密码均为 `wms_ai` / `wms_ai_password`），并在**数据卷首次初始化时**自动执行 `docker/pgvector/init/01-init.sql`（启用 `vector`/`hstore`/`uuid-ossp` 扩展、建 `vector_store` 表）。

如果不使用 Docker，手动准备等价的库：

```sql
CREATE DATABASE wms_ai;
CREATE EXTENSION IF NOT EXISTS vector;
```

> **需要本地确认的差异**：`docker/pgvector/init/01-init.sql` 里手工建表时向量列是 `vector(1536)`，而后端 `AiVectorStoreConfig` 中配置的向量维度是 `1024`（对应 `bge-m3` 模型的输出维度），且该配置 `initializeSchema(false)` 表示 Spring AI 不会自动建表或改列。这两者维度不一致，实际以哪个为准、是否需要手工调整表结构，请在启用 AI 功能前本地核实，不要假设开箱即用。

## 4. AI 相关前置条件（仅 AI/RAG 功能需要）

### Ollama（本地 embedding）

- 模型：`bge-m3`（`application-ai.yml` 中 `spring.ai.ollama.embedding.model`）
- 默认地址：`http://localhost:11434`（`application-ai.yml` 中 `spring.ai.ollama.base-url`）

```bash
ollama serve
ollama pull bge-m3
ollama list
```

### DeepSeek（RAG 问答生成）

- 通过环境变量 `DEEPSEEK_API_KEY` 注入（`application-ai.yml` 中 `spring.ai.deepseek.api-key: ${DEEPSEEK_API_KEY}`，无默认值）
- 模型固定为 `deepseek-v4-flash`（`application-ai.yml` 中硬编码，不通过环境变量配置）

```bash
export DEEPSEEK_API_KEY=your_api_key_here
```

请勿把真实 Key 提交到仓库或写进配置文件；本地开发建议只在 shell 环境变量或 IDE 的 Run Configuration 里设置。

## 5. 配置文件说明

实际存在的 profile 配置文件（均在 `wms-admin/src/main/resources/`，另有仅测试可见的一份）：

| 文件 | profile | 说明 |
|---|---|---|
| `application.yml` | 无（基础配置，所有 profile 共用） | `server.servlet.context-path=/api`；`spring.profiles.default=dev`；`spring.profiles.group.dev=[ai]`；Flyway 通用配置（`enabled=true`、`locations=classpath:db/migration`） |
| `application-dev.yml` | `dev`（**默认激活**） | `wms_system` 库连接信息、`server.port=8081`、`flyway.baseline-on-migrate=true` |
| `application-ai.yml` | `ai`（随 `dev` 自动激活） | pgvector 数据源、DeepSeek/Ollama 模型配置、RAG 参数（`top-k=3`、`similarity-threshold=0.65`） |
| `application-verify.yml` | `verify` | 指向独立的 `wms_flyway_verify` 库，`baseline-on-migrate=false`，用于验证全新库上 Flyway 从 V1 开始的完整迁移 |
| `wms-admin/src/test/resources/application-test.yml` | `test`（仅测试 classpath，不打包进正式 jar） | 指向 `wms_system_test`，并显式关闭 `spring.ai.model.chat/embedding`，避免测试因缺少 DeepSeek Key 而失败 |

- **不指定任何参数直接启动 = `dev` profile**（因为是 `spring.profiles.default`），同时自动附带 `ai` profile。
- 本地日常开发推荐直接使用默认（`dev`），只需保证第 2 节提到的 `DEEPSEEK_API_KEY` 占位值已设置。
- 切换 profile 用 `--spring.profiles.active=xxx`（命令行）或 `-Dspring-boot.run.profiles=xxx`（`spring-boot:run` 插件参数），例如切到 `verify` 验证全新库迁移；注意 `verify` 不属于 `dev` 组，不会自动带上 `ai`，也不会有 `server.port=8081`（会退回 Spring Boot 默认端口 8080）。

## 6. 编译与启动

以下命令均在 `wms-system/`（reactor 根目录）下执行：

```bash
cd wms-system
mvn clean package -DskipTests
```

启动（开发模式，默认 `dev` + `ai` profile）：

```bash
mvn -pl wms-admin -am spring-boot:run
```

指定 profile 启动：

```bash
mvn -pl wms-admin -am spring-boot:run -Dspring-boot.run.profiles=dev
```

或使用打包好的 jar（`wms-admin` 的 `artifactId` + `version`，见 `wms-admin/pom.xml` / 根 `pom.xml`）：

```bash
java -jar wms-admin/target/wms-admin-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

## 7. 启动成功验证

- 服务地址：`http://localhost:8081/api`（端口来自 `application-dev.yml`，context-path 来自 `application.yml`）
- Knife4j 接口文档：`http://localhost:8081/api/doc.html`（`WebMvcConfig` 中该路径被排除在登录校验之外，说明它挂在 context-path 下）
- Actuator 健康检查：`http://localhost:8081/api/actuator/health`（`management.endpoints.web.exposure.include: health,info,metrics`，且 `AuthInterceptor` 排除了 `/actuator/**`）
- 登录接口验证：

```bash
curl -X POST http://localhost:8081/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}'
```

返回 JWT 说明后端与数据库均已就绪。

## 8. 默认账号

已确认（`db/migration/V2__system_base_data.sql`）：

- 用户名：`admin`
- 初始密码：`admin123`（迁移脚本注释：`password_hash` 是该密码的 Hutool BCrypt 哈希）
- 角色：`ADMIN`（拥有全部权限）

该账号只在库为空（首次执行 V2）时插入，且用 `ON DUPLICATE KEY UPDATE username=username`（no-op）保证不会覆盖已修改过的密码。

## 9. 常见问题

**1. MySQL 连接失败（`Communications link failure` / `Access denied`）**
- 现象：启动时抛出数据源连接异常
- 可能原因：MySQL 未启动、库不存在、账号密码与 `application-dev.yml` 不一致
- 排查：确认 MySQL 进程存活、`wms_system` 库已创建、账号密码与 `application-dev.yml` 一致
- 处理：先按第 3 节创建空库，或修正本地 MySQL 账号密码

**2. Flyway migration 失败（`FlywayValidateException` / checksum 不一致）**
- 现象：启动时报 Flyway 校验或迁移失败
- 可能原因：手工改动过已执行的迁移脚本；或已有非空库缺少 baseline
- 排查：`SELECT * FROM flyway_schema_history ORDER BY installed_rank;` 查看已应用版本
- 处理：参考 [`docs/database-migration.md`](docs/database-migration.md)；已执行脚本禁止修改，新变更一律加 `V{n+1}` 脚本

**3. PostgreSQL / pgvector 连接失败**
- 现象：调用 AI 知识库 / RAG 相关接口时报数据源或连接异常（基础 WMS 功能不受影响）
- 可能原因：`docker-compose-ai.yml` 未启动，或本地未安装 pgvector 扩展
- 排查：`docker ps` 确认 `wms-pgvector` 容器状态；`docker compose -f docker-compose-ai.yml logs` 查看日志
- 处理：`cd wms-system && docker compose -f docker-compose-ai.yml up -d`

**4. pgvector 扩展不存在（`extension "vector" does not exist`）**
- 现象：手动建库（未用项目自带镜像）时报错
- 可能原因：使用的是普通 PostgreSQL 镜像/实例，没有 pgvector 扩展
- 处理：改用项目自带的 `pgvector/pgvector:0.8.5-pg17` 镜像（`docker-compose-ai.yml`），或在已安装 pgvector 扩展的实例上执行 `CREATE EXTENSION IF NOT EXISTS vector;`

**5. Ollama 未启动 / embedding 报错**
- 现象：知识库向量化、RAG 检索接口报连接被拒绝
- 排查：`curl http://localhost:11434/api/tags` 确认 Ollama 是否在监听
- 处理：`ollama serve` 启动服务

**6. bge-m3 模型未拉取**
- 现象：Ollama 已启动，但 embedding 调用报模型不存在
- 排查：`ollama list` 查看已拉取模型
- 处理：`ollama pull bge-m3`

**7. 应用启动阶段直接失败，报 DeepSeek / API key 相关错误**
- 现象：还没调用任何接口，`mvn spring-boot:run` 本身就失败
- 可能原因：默认 `dev` profile 自动带上 `ai` profile（见第 2、5 节），但 `DEEPSEEK_API_KEY` 环境变量未设置
- 处理：`export DEEPSEEK_API_KEY=placeholder`（不使用 AI 功能时占位即可）后重新启动

**8. 8081 端口被占用**
- 排查：`lsof -i:8081` 或 `netstat -ano | grep 8081`
- 处理：结束占用进程，或用 `--server.port=xxxx` 临时改用其他端口启动（注意前端 `vite.config.js` 的代理目标也要同步改）

**9. 前端请求 404**
- 可能原因：前端把接口路径拼成了不带 `/api` 前缀的地址
- 处理：确认后端 `context-path=/api`，所有接口实际路径都在 `/api` 之下（如 `/api/auth/login`）

## 10. 附录：技术栈说明

- **语言 / 构建**：Java 17，Maven 多模块（`wms-common` / `wms-framework` / `wms-modules` / `wms-admin`，实际业务代码全部在 `wms-admin`，其余为预留空模块）
- **Web 框架**：Spring Boot 3.3.6，Spring MVC，Spring AOP（权限校验、操作日志埋点），Bean Validation，Spring Boot Actuator
- **持久层**：MyBatis-Plus 3.5.16（分页、雪花 ID、`@TableLogic` 软删除）
- **数据库**：MySQL（主库）+ PostgreSQL/pgvector（AI 知识库向量索引，非业务主库）
- **数据库版本管理**：Flyway（`db/migration/V1~V9`）
- **API 文档**：springdoc-openapi + Knife4j 4.5.0
- **鉴权**：自研 JWT + RBAC（Hutool JWT/BCrypt），自定义 `@RequiresPermission` 注解 + AOP 切面，未使用 Spring Security
- **AI / RAG**：Spring AI 1.0.3，DeepSeek Chat（问答生成）+ Ollama（本地 embedding）+ PgVectorStore（向量检索），支持 SSE 流式问答
- **其他**：Hutool、Lombok、Jackson（东八区时区）、HikariCP（双数据源）
- 未引入消息队列、多租户；属于单体、单数据库、以同步调用为主的架构（仅操作日志异步落库、AI 问答走 SSE 流式）

已实现业务模块（仓库/库区/库位、SKU/客户/供应商、入库、出库、库存/调整/盘点、异常事件、RBAC、AI 知识库与 RAG 问答等）与具体功能清单见根目录 [`WMS-README.md`](../WMS-README.md)。
