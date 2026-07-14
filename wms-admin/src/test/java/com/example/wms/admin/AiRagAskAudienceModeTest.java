package com.example.wms.admin;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.wms.admin.model.entity.SysRole;
import com.example.wms.admin.model.entity.SysUser;
import com.example.wms.admin.model.entity.SysUserRole;
import com.example.wms.admin.model.mapper.SysRoleMapper;
import com.example.wms.admin.model.mapper.SysUserMapper;
import com.example.wms.admin.model.mapper.SysUserRoleMapper;
import com.example.wms.admin.security.PasswordEncoder;
import com.example.wms.admin.view.dto.AiRagAskRequest;
import com.example.wms.admin.view.dto.LoginRequest;
import com.example.wms.common.common.ApiResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Proves RAG's audience-mode selection is driven entirely by the caller's roles (via V9's DEVELOPER
 * role + CurrentUserContext), never by client input: DEVELOPER => TECHNICAL_USER, everyone else
 * (including ADMIN alone, and a request that tries to smuggle an "audience" field) => BUSINESS_USER.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.datasource.url=jdbc:mysql://localhost:3306/wms_system_test?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Tokyo&useSSL=false&allowPublicKeyRetrieval=true"
)
class AiRagAskAudienceModeTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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

    @Test
    void developerRoleGetsTechnicalUserMode() {
        String username = createUserWithRoles("dev-" + System.nanoTime(), "DEVELOPER");
        String token = login(username, "test123");

        JsonNode data = askJson(token, "为什么出库锁库后库存没有减少？");
        assertEquals("TECHNICAL_USER", data.get("audienceMode").asText());
        assertEquals("技术人员模式", data.get("audienceModeLabel").asText());
    }

    @Test
    void nonDeveloperRoleGetsBusinessUserMode() {
        // WAREHOUSE_OPERATOR has ai-rag:ask (V8) but is not DEVELOPER.
        String username = createUserWithRoles("operator-" + System.nanoTime(), "WAREHOUSE_OPERATOR");
        String token = login(username, "test123");

        JsonNode data = askJson(token, "为什么出库锁库后库存没有减少？");
        assertEquals("BUSINESS_USER", data.get("audienceMode").asText());
        assertEquals("业务用户模式", data.get("audienceModeLabel").asText());
    }

    @Test
    void adminAloneDoesNotImplyDeveloperMode() {
        // The seeded admin user only holds ADMIN (V2) — never DEVELOPER unless explicitly assigned.
        String token = login("admin", "admin123");

        JsonNode data = askJson(token, "为什么出库锁库后库存没有减少？");
        assertEquals("BUSINESS_USER", data.get("audienceMode").asText(),
                "ADMIN must not automatically imply DEVELOPER/technical mode");
    }

    @Test
    void havingDeveloperAmongMultipleRolesStillSelectsTechnicalMode() {
        String username = createUserWithRoles("multi-" + System.nanoTime(), "WAREHOUSE_OPERATOR", "DEVELOPER");
        String token = login(username, "test123");

        JsonNode data = askJson(token, "为什么出库锁库后库存没有减少？");
        assertEquals("TECHNICAL_USER", data.get("audienceMode").asText());
    }

    @Test
    void clientSuppliedAudienceFieldIsIgnored() throws Exception {
        String username = createUserWithRoles("spoof-" + System.nanoTime(), "WAREHOUSE_OPERATOR");
        String token = login(username, "test123");

        // Try to smuggle an "audience" field the DTO doesn't even declare — Jackson silently drops
        // unknown properties, so this must have zero effect on the server-computed mode.
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url("/ai/rag/ask")))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"question\":\"为什么出库锁库后库存没有减少？\",\"audience\":\"TECHNICAL_USER\",\"audienceMode\":\"TECHNICAL_USER\"}"))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());

        JsonNode root = readTree(response.body());
        JsonNode data = root.get("data");
        assertEquals("BUSINESS_USER", data.get("audienceMode").asText(),
                "a spoofed audience field in the request body must not change the server-decided mode");
    }

    @Test
    void streamSendsMetaEventFirstWithCorrectAudienceMode() throws Exception {
        String devUsername = createUserWithRoles("dev-stream-" + System.nanoTime(), "DEVELOPER");
        String token = login(devUsername, "test123");

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url("/ai/rag/ask/stream")))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(15))
                .POST(HttpRequest.BodyPublishers.ofString("{\"question\":\"为什么出库锁库后库存没有减少？\",\"topK\":3}"))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());

        String body = response.body();
        int metaIdx = firstIndexOfEvent(body, "meta");
        int referencesIdx = firstIndexOfEvent(body, "references");
        assertTrue(metaIdx >= 0, "meta event missing:\n" + body);
        assertTrue(referencesIdx > metaIdx, "meta must be sent before references:\n" + body);
        assertTrue(body.contains("\"audienceMode\":\"TECHNICAL_USER\""), "meta must carry the developer's technical mode:\n" + body);
        assertTrue(body.contains("\"audienceModeLabel\":\"技术人员模式\""));
    }

    private int firstIndexOfEvent(String body, String name) {
        int noSpace = body.indexOf("event:" + name);
        int withSpace = body.indexOf("event: " + name);
        if (noSpace < 0) return withSpace;
        if (withSpace < 0) return noSpace;
        return Math.min(noSpace, withSpace);
    }

    private String createUserWithRoles(String username, String... roleCodes) {
        SysUser user = new SysUser(username, passwordEncoder.encode("test123"), "审计测试用户", null, null);
        sysUserMapper.insert(user);
        for (String roleCode : roleCodes) {
            SysRole role = sysRoleMapper.selectOne(Wrappers.lambdaQuery(SysRole.class).eq(SysRole::getRoleCode, roleCode));
            assertNotNull(role, "role must be seeded: " + roleCode);
            sysUserRoleMapper.insert(new SysUserRole(user.getId(), role.getId()));
        }
        return username;
    }

    private JsonNode askJson(String token, String question) {
        ResponseEntity<ApiResponse> response = restTemplate.exchange(
                url("/ai/rag/ask"), HttpMethod.POST,
                new HttpEntity<>(new AiRagAskRequest(question, null, null, null, null), authHeaders(token)),
                ApiResponse.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        return OBJECT_MAPPER.valueToTree(response.getBody().data());
    }

    private HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        return headers;
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

    private String url(String path) {
        return "http://localhost:" + port + "/api" + path;
    }
}
