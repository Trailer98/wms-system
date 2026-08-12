# Testing Standard

> 本文档基于 `wms-system` 当前真实代码和现有测试提炼。
> 这个仓库当前最有价值的测试方式不是“把 Service 全 mock 掉”，而是**在 `wms-admin` 里跑带真实 Spring/MyBatis/Flyway 的业务流测试**。

## 1. Read Order

1. `PROJECT_CONTEXT.md`
2. `API_IMPLEMENTATION_STANDARD.md`
3. `TESTING_STANDARD.md`（本文）
4. 目标模块相邻的现有测试类

## 2. Current Tooling Reality

- 这是一个 Maven 多模块工程；当前测试主要集中在 `wms-admin/src/test`
- `wms-admin/src/test/resources/application-test.yml` 指向专用 MySQL 测试库 `wms_system_test`
- 测试环境里 Flyway **开启**
- AI 模型在测试 profile 下被显式关闭
- `TestAuthServiceClient` 已存在，可替代真实 auth-service
- 现有测试既有：
  - HTTP 级 `@SpringBootTest(webEnvironment = RANDOM_PORT)` 全链路测试
  - 直接测拦截器/小型组件的窄范围单测

## 3. Default Test Style

新增或重构代码时默认遵守：

1. **Controller / service / inventory-flow / order-flow 改动**：优先补集成测试，走真实 Spring + MyBatis + Flyway
2. **HTTP 契约 / 权限 / 操作日志链路改动**：优先补 `TestRestTemplate` 驱动的 HTTP 级测试
3. **拦截器 / 小型纯逻辑组件**：可以写不启动全上下文的窄范围单测

不要默认把 WMS 的核心业务规则退化成全 mock 的 service test；这个仓库很多风险都在事务、Mapper 查询、状态流转和 AOP/拦截器联动里。

## 4. Minimum Coverage Required

任何新增或重构的 WMS 业务代码，至少覆盖：

- 一个 happy path
- 一个失败路径：如校验失败、非法状态、无权限、依赖数据不存在
- 如果改动会改库存 / 单据 / 状态流转，断言**最终数据库状态**，不要只断言 HTTP 200
- 如果改动涉及 `@RequiresPermission` / 当前用户上下文，用 `TestAuthServiceClient` + Gateway 头模拟，不要调用真实 auth-service
- 如果改动涉及 AI/RAG，测试必须保持离线，不依赖真实模型 Key 或外部接口

## 5. Commands

优先跑定向或模块级测试：

- `mvn -pl wms-admin -am -Dtest=GatewayUserContextInterceptorTest test`
- `mvn -pl wms-admin -am -Dtest=AiRagAskControllerTest test`

需要更大范围时再跑：

- `mvn -pl wms-admin -am test`
- 如果改动跨模块且你想连编译一起兜住：`mvn -pl wms-admin -am compile test`

## 6. Baseline Caveats

- 这个仓库的全量测试在当前环境里曾出现过与本次改动无关的基线失败；如果它们在 clean tree 上也原样存在，要如实标注为 baseline，而不是混成“本次改动失败”。
- 这些测试默认假设本地 `wms_system_test` MySQL 已经可用并允许 Flyway 迁移；若该前提不成立，要在结论里明确说未能执行，不要假装通过。

## 7. Done Means

一个 wms-system 任务只有在下面三点都满足时才算“测试完成”：

- 根据改动类型补了集成测试或窄范围单测
- 至少跑了定向或模块级测试；若声称跑了更大范围命令则写明
- 最终说明里写清楚命令、测试类名，以及覆盖了哪些状态流转 / 权限 / DB 断言
