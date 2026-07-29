# WMS Authentication Migration

WMS authentication now uses the unified gateway-service and auth-service path.

## Current Request Path

```text
Client
-> gateway-service validates the auth-service access token
-> Gateway forwards X-User-Id / X-Username / X-Token-Id / X-Gateway-Token
-> WMS validates X-Gateway-Token
-> WMS calls auth-service /auth/context?applicationCode=WMS
-> WMS uses @RequiresPermission to enforce business permissions
```

## Current Rules

- Frontend login must call `/api/auth/login`.
- WMS business APIs must be accessed through `/api/wms/**`.
- Direct calls to WMS must include `X-Gateway-Token`; otherwise WMS returns 401.
- WMS does not parse client `Authorization` JWTs for protected requests.
- WMS does not query the auth database directly.
- Permission decisions use auth-service data from `auth_user`, `auth_role`, and `auth_permission`.
- Existing WMS `sys_user`, `sys_role`, and `sys_permission` tables are legacy data only, kept for rollback and historical reference.

## Deprecated WMS Entries

- WMS local login `/auth/login` is deprecated and disabled by default.
- WMS local JWT validation is disabled by default with `wms.auth.local-jwt-enabled=false`.
- WMS local user/role/permission management APIs are deprecated and blocked by default with `wms.auth.legacy-admin-enabled=false`.
- To rollback temporarily, explicitly enable the relevant `wms.auth.*` switch in environment-specific configuration.

## Required Configuration

```yaml
gateway:
  internal-token: ${GATEWAY_INTERNAL_TOKEN}

auth-service:
  base-url: ${AUTH_SERVICE_BASE_URL:http://127.0.0.1:8081/auth}
  application-code: WMS

wms:
  auth:
    local-login-enabled: false
    local-jwt-enabled: false
    legacy-admin-enabled: false
```

Do not hardcode `GATEWAY_INTERNAL_TOKEN` in source code. Configure the same value in gateway-service forwarding and WMS runtime configuration.
