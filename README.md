# WMS System

Spring Boot 3.x + Maven 的 WMS 后端项目骨架。

## 技术栈

- Java 17
- Spring Boot 3.3.6
- Spring Web
- MyBatis-Plus
- Bean Validation
- MySQL
- Actuator

## 已包含的基础模块

项目采用 MVC 分层：

- Controller：`com.example.wms.controller`
- Model：`com.example.wms.model.entity`、`com.example.wms.model.mapper`、`com.example.wms.service`
- View：`com.example.wms.view.dto`

- 仓库主数据：`/api/warehouses`
- SKU 主数据：`/api/skus`
- 库存查询：`/api/inventory`
- 入库单创建和收货：`/api/inbound-orders`
- 出库单创建和发货：`/api/outbound-orders`
- 库存流水：`stock_movements` 表

## 本地启动

先创建数据库：

```sql
create database wms_system character set utf8mb4 collate utf8mb4_unicode_ci;
```

```bash
mvn spring-boot:run
```

默认端口为 `8081`，接口前缀为 `/api`。默认数据库为 `wms_system`，账号 `root`，密码 `root123456`。

## 快速验证

```bash
curl -X POST http://localhost:8081/api/warehouses \
  -H 'Content-Type: application/json' \
  -d '{"code":"WH-SH-01","name":"上海一号仓","address":"上海"}'

curl -X POST http://localhost:8081/api/skus \
  -H 'Content-Type: application/json' \
  -d '{"code":"SKU-001","name":"标准纸箱","unit":"pcs","category":"PACKING"}'

curl -X POST http://localhost:8081/api/inbound-orders \
  -H 'Content-Type: application/json' \
  -d '{"orderNo":"IN-20260608-001","warehouseId":1,"supplierName":"默认供应商","items":[{"skuId":1,"quantity":100}]}'

curl -X POST http://localhost:8081/api/inbound-orders/1/receive

curl http://localhost:8081/api/inventory
```
