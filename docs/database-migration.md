# 数据库迁移（Flyway）

本项目的数据库结构与系统基础数据由 **Flyway** 版本化管理，迁移脚本位于：

```
wms-admin/src/main/resources/db/migration/
├── V1__init_schema.sql                       # 全部表结构（DDL），从旧 schema.sql 原样迁移
├── V2__system_base_data.sql                  # RBAC 基础数据（权限/角色/授权/admin），原 DataInitializer.java
├── V3__dict_base_data.sql                    # 库存流水字典（operationType/bizType），原 DictDataInitializer.java
├── V4__add_konwledge_base_tables.sql         # AI 知识库表 knowledge_document / knowledge_chunk
├── V5__alter_knowledge_document_add_content.sql  # knowledge_document 补 content + 继承元数据列
├── V6__add_ai_knowledge_permissions.sql      # ai-knowledge:* 权限点 + 角色授权
├── V7__ai_knowledge_dicts.sql                # ai_knowledge_* 展示字典
└── V8__add_ai_rag_permissions.sql            # ai-rag:ask 权限点 + 角色授权（RAG 问答接口）
```

> **V4 文件名拼写说明**：`V4__add_konwledge_base_tables.sql` 文件名把 knowledge 误拼为 `konwledge`。
> 但**表名本身正确**（`knowledge_document` / `knowledge_chunk`），只是文件名有笔误。该迁移**已在各库执行**
> （`flyway_schema_history` 中 rank 4、success=1，description = `add konwledge base tables`）。Flyway 的
> `validate` 会校验 checksum 与 description，重命名文件会改变 description 导致校验失败，故**按红线不再修改**——
> 仅在此说明。后续新表/新列一律用正确拼写的 `V8+` 增量脚本。

旧的 `schema.sql` 已归档至 `docs/legacy-sql/schema.sql`（仅作历史参考，不再被加载）。
`spring.sql.init.mode=never`，Spring Boot 不再自动执行任何 init SQL。

## 每次表结构变更必须新增 migration

1. **所有表结构变更必须新增一个 Flyway migration**，不要改动已有已执行的脚本。
2. **已经执行过的 migration（V1/V2/V3…）禁止修改**——Flyway 用 checksum 校验，改动会导致 `validate` 失败。
3. 新脚本命名遵循 Flyway 规范，版本号递增：
   - `V4__add_xxx_table.sql`
   - `V5__alter_xxx_add_column.sql`
4. 系统基础字典/权限的新增，也通过新的 migration（例如新增一个权限点 → `V6__add_permission_xxx.sql`，用 `INSERT ... ON DUPLICATE KEY UPDATE`）。
5. **业务 Demo 数据不通过 migration。** 小规模演示数据、以及企业年度级批量数据，都不放进 Flyway。
6. **大规模企业年度数据使用 `--enterprise-demo.insert=true`**（`EnterpriseYearDemoDataBulkInserter`），与 Flyway 完全独立。
7. **禁止使用 Flyway clean**（`spring.flyway.clean-disabled=true` 已永久关闭）。
8. **上线前必须备份数据库。**

## 迁移脚本红线

Flyway 迁移脚本中**禁止**出现：`DROP DATABASE`、`DROP TABLE`、`TRUNCATE`、`DELETE` 业务数据、reset/seed demo 数据。
V1 只包含 DDL + 幂等的 `information_schema` 守卫式 `ALTER` + 历史数据规范化 `UPDATE`（空库上为 no-op）。

## 新环境初始化（空库）

```sql
CREATE DATABASE wms_flyway_verify CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

直接启动应用（指向空库）：

```bash
java -jar wms-admin/target/wms-admin-0.0.1-SNAPSHOT.jar \
  --spring.datasource.url='jdbc:mysql://localhost:3306/wms_flyway_verify?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Tokyo&useSSL=false&allowPublicKeyRetrieval=true' \
  --spring.datasource.username=root \
  --spring.datasource.password=<你的密码>
```

预期：Flyway 依次执行 V1 → V2 → V3，创建 `flyway_schema_history`，应用启动成功，表/权限/角色/字典/admin 全部就绪。
（空库不会被 baseline，V1 会正常执行。）

## 配置按 profile 隔离（重要）

`baseline-on-migrate` **不在全局 `application.yml`** 配置，避免生产误 baseline。按 profile 分离：

| profile | 文件 | baseline-on-migrate | 数据源 | 用途 |
|---|---|---|---|---|
| （无 / 通用） | `application.yml`（main） | 不设置（Flyway 默认 false） | `wms_system` | 通用 Flyway 配置：enabled / locations / validate / clean-disabled / out-of-order |
| `dev` | `application-dev.yml`（main） | **true**（version=1） | 继承 `wms_system` | 允许已有开发库 baseline 后接入 |
| `test` | `application-test.yml`（**test classpath**，不打包进 jar） | **false** | `wms_system_test` | 测试专用库，绝不污染 `wms_system` |

- 测试通过 `src/test/resources/application.properties` 的 `spring.profiles.active=test` 自动激活 `test` profile，因此任何 `@SpringBootTest`（即使没有显式覆盖数据源）都连 `wms_system_test`，不会再 baseline / 写入默认开发库 `wms_system`。
- `application-dev.yml`、`application-test.yml` 分别只在对应 profile 激活时生效。

## 已有数据库接入（baseline，仅 dev）

已有的开发库（历史上由 schema.sql 建好、非空）第一次接入 Flyway 时，用 **dev** profile 启动：

```bash
java -jar wms-admin/target/wms-admin-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

- dev 下 `baseline-on-migrate=true`、`baseline-version=1`。
- Flyway 检测到「非空库 + 无 `flyway_schema_history`」→ 打 baseline 到版本 1（**V1 视为已应用，不重复建表**）→ 只执行 V2、V3（幂等，`ON DUPLICATE KEY UPDATE`，不会重复插入、不覆盖已改密码）。

若已有库**缺少某些新表**（schema 落后于 V1）：不要让 Flyway 在不确定状态强行执行。两种处理：

1. 先手工把该库的表结构对齐到 V1 所描述的结构，再启动（走上面的 baseline 流程）；或
2. 为「从旧结构补齐到最新」单独编写增量脚本 `V2+__xxx.sql`（在把该库 baseline 到它当时对应的版本之后再执行）。

## 测试前置：创建 `wms_system_test`

测试连 `wms_system_test`（不是 `wms_system`）。首次跑测试前创建该库（建议空库，Flyway 会自动跑 V1/V2/V3）：

```sql
CREATE DATABASE IF NOT EXISTS wms_system_test CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

test profile 下 `baseline-on-migrate=false`：该库要么是空库（Flyway 从 V1 建全表），要么已是 Flyway 管理（已有 history 表）；不应是「非空且未 baseline」的状态。

## 生产环境注意（必须人工确认）

1. **生产环境默认不开启 `baseline-on-migrate`**（只有 dev profile 开）；生产不要激活 dev profile。
2. 生产已有库接入 Flyway **前必须先备份数据库**。
3. baseline **只能人工确认结构与 V1 一致后手动执行**（例如 `flyway baseline` CLI 或临时显式配置），不要盲目依赖任何自动 baseline。
4. **禁止 Flyway clean**（`clean-disabled=true` 已永久关闭）。
5. **已执行过的 migration（V1/V2/V3…）禁止修改**——checksum 校验会失败；变更一律新增 `V4+`。

## 校验命令

```sql
-- 迁移历史（应看到 V1/V2/V3 或 baseline 记录，success=1）
SELECT installed_rank, version, description, type, success FROM flyway_schema_history ORDER BY installed_rank;

-- 基础数据是否就绪
SELECT COUNT(*) FROM sys_permission;   -- 期望 69
SELECT COUNT(*) FROM sys_role;         -- 期望 4
SELECT COUNT(*) FROM sys_user WHERE username='admin';  -- 期望 1
SELECT COUNT(*) FROM sys_dict_type;    -- 期望 2
SELECT COUNT(*) FROM sys_dict_item;    -- 期望 23
```
