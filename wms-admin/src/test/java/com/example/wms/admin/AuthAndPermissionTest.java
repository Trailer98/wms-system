package com.example.wms.admin;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.wms.admin.client.AuthContextResponse;
import com.example.wms.admin.model.entity.SysRole;
import com.example.wms.admin.model.entity.SysUser;
import com.example.wms.admin.model.entity.SysUserRole;
import com.example.wms.admin.model.mapper.SysRoleMapper;
import com.example.wms.admin.model.mapper.SysUserMapper;
import com.example.wms.admin.model.mapper.SysUserRoleMapper;
import com.example.wms.admin.security.GatewayUserContextInterceptor;
import com.example.wms.admin.security.PasswordEncoder;
import com.example.wms.admin.view.dto.LoginRequest;
import com.example.wms.common.common.ApiResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Covers acceptance cases 1 (admin sees everything / can call protected endpoints), 2 (a
// view-only role is rejected by a create/lock-type endpoint) and 4 (unauthenticated calls are
// rejected) from the RBAC requirement. Flyway migration V2__system_base_data.sql seeds admin/admin123
// and the four default roles against this same wms_system_test database, so no manual seeding is
// needed here beyond creating one extra INVENTORY_VIEWER test user.
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.datasource.url=jdbc:mysql://localhost:3306/wms_system_test?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Tokyo&useSSL=false&allowPublicKeyRetrieval=true"
)
class AuthAndPermissionTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @TestConfiguration
    static class AuthClientTestConfig {
        @Bean
        @Primary
        TestAuthServiceClient testAuthServiceClient() {
            return new TestAuthServiceClient();
        }
    }

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private SysUserMapper sysUserMapper;
    @Autowired
    private SysRoleMapper sysRoleMapper;
    @Autowired
    private SysUserRoleMapper sysUserRoleMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private TestAuthServiceClient authServiceClient;

    private String viewerUsername;
    private Long viewerUserId;

    @BeforeEach
    void setUp() {
        String suffix = String.valueOf(System.nanoTime());
        viewerUsername = "viewer-" + suffix;

        SysUser viewer = new SysUser(viewerUsername, passwordEncoder.encode("viewer123"), "测试查看员", null, null);
        sysUserMapper.insert(viewer);
        viewerUserId = viewer.getId();

        SysRole viewerRole = sysRoleMapper.selectOne(Wrappers.lambdaQuery(SysRole.class)
                .eq(SysRole::getRoleCode, "INVENTORY_VIEWER"));
        assertNotNull(viewerRole, "DataInitializer must have seeded the INVENTORY_VIEWER role");
        sysUserRoleMapper.insert(new SysUserRole(viewer.getId(), viewerRole.getId()));
    }

    @Test
    void adminCanLoginAndAccessProtectedEndpoint() {
        String adminToken = login("admin", "admin123");
        assertNotNull(adminToken);
        Long adminUserId = adminUserId();
        mockAuthContext(adminUserId, "admin", List.of("ADMIN"), List.of("warehouse:view"));

        ResponseEntity<ApiResponse> response = getWithGatewayHeaders("/warehouses", adminToken, adminUserId, "admin");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(200, response.getBody().code());
    }

    @Test
    void inventoryViewerIsForbiddenFromOutboundLock() {
        String viewerToken = login(viewerUsername, "viewer123");
        assertNotNull(viewerToken);
        mockAuthContext(viewerUserId, viewerUsername, List.of("INVENTORY_VIEWER"), List.of("inventory:view"));

        // Permission check runs before the target method body, so a non-existent order id still
        // proves enforcement: a viewer must never reach outbound:lock regardless of what exists.
        ResponseEntity<ApiResponse> response = postWithGatewayHeaders(
                "/outbound-orders/999999999/lock", viewerToken, viewerUserId, viewerUsername);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals(403, response.getBody().code());
    }

    @Test
    void inventoryViewerIsForbiddenFromConfirmingStockAdjustOrder() {
        String viewerToken = login(viewerUsername, "viewer123");
        assertNotNull(viewerToken);
        mockAuthContext(viewerUserId, viewerUsername, List.of("INVENTORY_VIEWER"), List.of("inventory:view"));

        // Same reasoning as the outbound:lock case: the permission check happens before the
        // controller method body runs, so a non-existent order id still proves enforcement.
        ResponseEntity<ApiResponse> response = postWithGatewayHeaders(
                "/stock-adjust-orders/999999999/confirm", viewerToken, viewerUserId, viewerUsername);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals(403, response.getBody().code());
    }

    @Test
    void inventoryViewerIsForbiddenFromViewingOperationLogs() {
        String viewerToken = login(viewerUsername, "viewer123");
        assertNotNull(viewerToken);
        mockAuthContext(viewerUserId, viewerUsername, List.of("INVENTORY_VIEWER"), List.of("inventory:view"));

        ResponseEntity<ApiResponse> response = getWithGatewayHeaders("/operation-logs", viewerToken, viewerUserId, viewerUsername);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals(403, response.getBody().code());
    }

    @Test
    void unauthenticatedRequestIsRejected() {
        HttpHeaders headers = new HttpHeaders();
        ResponseEntity<ApiResponse> response = restTemplate.exchange(
                url("/warehouses"), HttpMethod.GET, new HttpEntity<>(headers), ApiResponse.class);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(401, response.getBody().code());
    }

    // Parsed as raw JSON rather than the typed LoginResponse: the project's JacksonConfig
    // registers a custom LocalDateTime *serializer* ("yyyy-MM-dd HH:mm:ss") but no matching
    // deserializer, so TestRestTemplate's default converter can't round-trip UserResponse.lastLoginTime.
    private String login(String username, String password) {
        ResponseEntity<String> response = restTemplate.postForEntity(
                url("/auth/login"), new LoginRequest(username, password), String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonNode root = readTree(response.getBody());
        assertEquals(200, root.get("code").asInt());
        JsonNode data = root.get("data");
        String token = data.get("token").asText();
        assertFalse(token.isBlank());
        assertTrue(data.get("permissions").size() > 0);
        return token;
    }

    private JsonNode readTree(String json) {
        try {
            return OBJECT_MAPPER.readTree(json);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void mockAuthContext(Long userId, String username, List<String> roles, List<String> permissions) {
        authServiceClient.setContext(new AuthContextResponse(userId, username, "WMS", roles, permissions));
    }

    private Long adminUserId() {
        SysUser admin = sysUserMapper.selectOne(Wrappers.lambdaQuery(SysUser.class).eq(SysUser::getUsername, "admin"));
        assertNotNull(admin);
        return admin.getId();
    }

    private ResponseEntity<ApiResponse> getWithGatewayHeaders(String path, String token, Long userId, String username) {
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        addGatewayHeaders(headers, token, userId, username);
        return restTemplate.exchange(url(path), HttpMethod.GET, new HttpEntity<>(new HttpHeaders(headers)), ApiResponse.class);
    }

    private ResponseEntity<ApiResponse> postWithGatewayHeaders(String path, String token, Long userId, String username) {
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        addGatewayHeaders(headers, token, userId, username);
        return restTemplate.exchange(url(path), HttpMethod.POST, new HttpEntity<>(null, new HttpHeaders(headers)), ApiResponse.class);
    }

    private void addGatewayHeaders(MultiValueMap<String, String> headers, String token, Long userId, String username) {
        headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        headers.add(GatewayUserContextInterceptor.HEADER_GATEWAY_TOKEN, "test-gateway-token");
        headers.add(GatewayUserContextInterceptor.HEADER_USER_ID, String.valueOf(userId));
        headers.add(GatewayUserContextInterceptor.HEADER_USERNAME, username);
        headers.add(GatewayUserContextInterceptor.HEADER_TOKEN_ID, "test-token-id");
    }

    private String url(String path) {
        return "http://localhost:" + port + "/wms" + path;
    }
}
