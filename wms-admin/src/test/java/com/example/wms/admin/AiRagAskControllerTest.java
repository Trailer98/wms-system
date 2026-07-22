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

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * HTTP-level checks for POST /ai/rag/ask: auth/permission enforcement (V8 grants ai-rag:ask to
 * ADMIN/WAREHOUSE_MANAGER/WAREHOUSE_OPERATOR but not INVENTORY_VIEWER) and that the endpoint is wired
 * end to end through PermissionAspect + SysOperationAspect without depending on a real DeepSeek key
 * (no VectorStore under test => the service's own degrade path answers instead of erroring).
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.datasource.url=jdbc:mysql://localhost:3306/wms_system_test?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Tokyo&useSSL=false&allowPublicKeyRetrieval=true"
)
class AiRagAskControllerTest {

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
        viewerUsername = "rag-viewer-" + suffix;

        SysUser viewer = new SysUser(viewerUsername, passwordEncoder.encode("viewer123"), "RAG权限测试查看员", null, null);
        sysUserMapper.insert(viewer);
        viewerUserId = viewer.getId();

        SysRole viewerRole = sysRoleMapper.selectOne(Wrappers.lambdaQuery(SysRole.class)
                .eq(SysRole::getRoleCode, "INVENTORY_VIEWER"));
        assertNotNull(viewerRole, "V2 must have seeded the INVENTORY_VIEWER role");
        sysUserRoleMapper.insert(new SysUserRole(viewer.getId(), viewerRole.getId()));
    }

    @Test
    void unauthenticatedAskIsRejected() throws Exception {
        // TestRestTemplate's default HttpURLConnection-based client throws HttpRetryException
        // ("cannot retry due to server authentication, in streaming mode") on a streamed POST body
        // whose response is a non-2xx (401) — a legacy JDK client-side quirk unrelated to
        // AuthInterceptor, which rejects on the missing header in preHandle before the body is ever
        // read. The modern java.net.http.HttpClient doesn't share this limitation, so use it directly
        // for this one assertion.
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url("/ai/rag/ask")))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"question\":\"test\"}"))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(401, response.statusCode());
        assertEquals(401, readTree(response.body()).get("code").asInt());
    }

    @Test
    void inventoryViewerIsForbiddenFromAsk() {
        // V8 grants ai-rag:ask to ADMIN/WAREHOUSE_MANAGER/WAREHOUSE_OPERATOR only — unlike the
        // read-only ai-knowledge:search, this invokes the chat model on every call.
        String token = login(viewerUsername, "viewer123");
        mockAuthContext(viewerUserId, viewerUsername, List.of("INVENTORY_VIEWER"), List.of("inventory:view"));
        ResponseEntity<ApiResponse> response = postWithGatewayHeaders(
                token, viewerUserId, viewerUsername, Map.of("question", "为什么出库锁库后库存没有减少？"));
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals(403, response.getBody().code());
    }

    @Test
    void adminBlankQuestionReturnsBadRequest() {
        String token = login("admin", "admin123");
        Long adminUserId = adminUserId();
        mockAuthContext(adminUserId, "admin", List.of("ADMIN"), List.of("ai-rag:ask"));
        ResponseEntity<ApiResponse> response = postWithGatewayHeaders(token, adminUserId, "admin", Map.of("question", ""));
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void adminAskSucceedsStructurallyThroughTheFullStack() {
        // No VectorStore under test profile: knowledge retrieval degrades gracefully (see
        // AiRagAskServiceTest), so this proves the HTTP + permission + operation-log wiring is sound
        // end to end without needing a real DeepSeek API key.
        String token = login("admin", "admin123");
        Long adminUserId = adminUserId();
        mockAuthContext(adminUserId, "admin", List.of("ADMIN"), List.of("ai-rag:ask"));
        ResponseEntity<ApiResponse> response = postWithGatewayHeaders(
                token, adminUserId, "admin", Map.of("question", "为什么出库锁库后库存没有减少？"));
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(200, response.getBody().code());
    }

    private void mockAuthContext(Long userId, String username, List<String> roles, List<String> permissions) {
        authServiceClient.setContext(new AuthContextResponse(userId, username, "WMS", roles, permissions));
    }

    private Long adminUserId() {
        SysUser admin = sysUserMapper.selectOne(Wrappers.lambdaQuery(SysUser.class).eq(SysUser::getUsername, "admin"));
        assertNotNull(admin);
        return admin.getId();
    }

    private String login(String username, String password) {
        ResponseEntity<String> response = restTemplate.postForEntity(
                url("/auth/login"), new LoginRequest(username, password), String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonNode root = readTree(response.getBody());
        return root.get("data").get("token").asText();
    }

    private JsonNode readTree(String json) {
        try {
            return OBJECT_MAPPER.readTree(json);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private ResponseEntity<ApiResponse> postWithGatewayHeaders(String token, Long userId, String username, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        headers.add(GatewayUserContextInterceptor.HEADER_GATEWAY_TOKEN, "test-gateway-token");
        headers.add(GatewayUserContextInterceptor.HEADER_USER_ID, String.valueOf(userId));
        headers.add(GatewayUserContextInterceptor.HEADER_USERNAME, username);
        headers.add(GatewayUserContextInterceptor.HEADER_TOKEN_ID, "test-token-id");
        return restTemplate.exchange(url("/ai/rag/ask"), HttpMethod.POST, new HttpEntity<>(body, headers), ApiResponse.class);
    }

    private String url(String path) {
        return "http://localhost:" + port + "/wms" + path;
    }
}
