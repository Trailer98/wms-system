# API Implementation Standard

> 本文档基于 `wms-system` 当前真实代码提炼。
> 用途：新增、修改、重构任何 WMS 业务接口时，先读本文档，再决定控制器、DTO、权限码和服务边界。

## 1. Read Order

1. `PROJECT_CONTEXT.md`
2. `API_IMPLEMENTATION_STANDARD.md`（本文）
3. 目标模块相邻的 Controller / Service / DTO / Entity
4. 如果涉及权限码或 auth-service 契约，再看 `auth-service/PROJECT_CONTEXT.md`

## 2. Ownership First

先判断需求是不是 WMS 自己拥有：

- 这里拥有：仓库、库区、库位、SKU、客户、供应商、库存、入库、出库、盘点、调整、异常事件、数据字典、操作日志、AI/RAG
- 这里不拥有：登录、用户、角色、权限、RBAC 真相源

不要因为仓库里还留着 `controller/auth/` 那套历史代码，就把新接口继续加进去。默认它们是禁用/遗留路径，不是扩展点。

## 3. Package Placement

按现有真实分层落位：

- 基础资料/字典/系统型控制器：`wms-admin/src/main/java/com/example/wms/admin/controller/base/`
- 订单/库存/盘点/调整等主业务控制器：`.../controller/`
- AI：`.../controller/ai/`
- 服务：`.../service/`
- DTO：`.../view/dto/`
- Entity / Mapper：`.../model/entity/`、`.../model/mapper/`

如果新增的是“仓库业务接口”，不要新建一套 `api/`、`facade/`、`resource/` 平行层。

## 4. Response and Pagination Shape

遵守当前仓库现有响应契约：

- 所有接口统一返回 `ApiResponse<T>`
- 成功消息固定走 `ApiResponse.ok(...)`
- 分页结果统一使用 `PageResponse<T>`
- `PageResponse` 形状固定是 `{pageNum,pageSize,total,records}`

不要引入第二套 `{items,total,page,pageSize}` 分页形状；那是 `auth-service` 的约定，不是这里的。

## 5. Controller Patterns

当前仓库控制器的稳定模式是：

- `@RestController`
- `@RequestMapping(...)` 做模块根路径
- 查询接口常用 GET + Query DTO 绑定
- 写接口用 `@Valid @RequestBody`
- 业务逻辑不放在 Controller，Controller 只做收参、调 Service、包 `ApiResponse`

但这个仓库**并不全局强制一种 URL 风格**。例如：

- `CustomerController` / `SkuController` / `WarehouseController` 的 `PUT` / `PATCH` 走 body 里的 `id`
- `WarehouseAreaController` / `WarehouseLocationController` 的 `PUT` / `PATCH` 走路径变量 `/{id}`

因此新增接口时的规则不是“全仓统一”，而是：

- 优先跟**同模块相邻控制器**保持一致
- 不要为了追求抽象上的整齐，反向把既有模块的局部约定打碎

## 6. Permission Rules

当前活跃鉴权链是：

- Gateway 注入可信身份头
- `GatewayUserContextInterceptor` 建 `CurrentUserContext`
- `@RequiresPermission("x:y")` + `PermissionAspect` 调 auth-service `/auth/context`

因此新增业务接口时默认要考虑：

- 是否需要 `@RequiresPermission`
- 权限码是否已经在 auth-service live DB 中存在
- 若不存在，是否需要 auth-service 增加 migration / live grant

不要假设本仓库本地就知道权限码全集。

## 7. Operation Log Rules

对“有业务副作用”的写接口，默认跟现有模式一样加：

- `@SysOperationLog(...)`

一般查询接口不加。是否要加，以相邻模块现有写接口为准。

## 8. Error Handling

业务错误沿用当前异常体系：

- 一般业务校验失败：`BusinessException`
- 未登录/身份不可信：`UnauthorizedException`
- 已登录但没权限：`ForbiddenException`
- 依赖 auth-service 不可用：`ServiceUnavailableException`

让 `wms-common/.../GlobalExceptionHandler.java` 统一出 `{code,message,data}`，不要在 Controller 里堆 `try/catch` 组装错误响应。

## 9. Service and Domain Boundary

Controller 只做薄封装；核心状态机、事务和业务约束放在 Service / Entity：

- 状态流转校验
- 库存增减/锁定
- 子级存在性检查
- 字典项/系统项保护
- 盘点/调整等复杂流程

如果一个新接口要改业务状态，先看相邻 Service 里有没有现成状态机，不要在 Controller 里临时拼规则。

## 10. Cross-Service Rules

WMS 与 auth-service 的真实关系要保持：

- WMS 直连 auth-service `GET /auth/context`
- 不经 Gateway 走这条后台到后台的权限上下文查询

如果新增接口涉及权限决策、CurrentUser、auth 上下文，不要“顺手改成走 Gateway”。

## 11. Historical Auth Warning

以下目录默认不是新功能扩展点：

- `controller/auth/`
- `security/AuthInterceptor.java`
- `security/JwtTokenService.java`
- `SysUser` / `SysRole` / `SysPermission` 相关本地 RBAC

除非任务明确要求做回滚/遗留链路维护，否则新增接口不要加到这套历史认证体系里。

## 12. Checklist

- [ ] 这个接口真的属于 WMS 业务域，不属于 auth-service
- [ ] 控制器放在正确模块（`controller/base` / `controller` / `controller/ai`）
- [ ] 返回值使用 `ApiResponse` / `PageResponse`
- [ ] 分页形状保持 `{pageNum,pageSize,total,records}`
- [ ] URL 风格与相邻模块一致，而不是强行套别的模块
- [ ] 是否正确加了 `@RequiresPermission`
- [ ] 如需新权限码，是否先确认 auth-service live RBAC / migration
- [ ] 写接口是否需要 `@SysOperationLog`
- [ ] 业务校验放在 Service / Domain，不塞进 Controller
- [ ] 没有把新功能加进历史 `controller/auth/` 遗留体系

