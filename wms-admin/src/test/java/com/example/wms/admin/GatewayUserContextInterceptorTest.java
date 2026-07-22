package com.example.wms.admin;

import com.example.wms.admin.security.CurrentUser;
import com.example.wms.admin.security.CurrentUserContext;
import com.example.wms.admin.security.GatewayUserContextInterceptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatewayUserContextInterceptorTest {

    private final GatewayUserContextInterceptor interceptor =
            new GatewayUserContextInterceptor(new ObjectMapper(), "test-gateway-token");

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    void missingGatewayTokenIsRejected() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(GatewayUserContextInterceptor.HEADER_USER_ID, "1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(request, response, new Object()));
        assertEquals(401, response.getStatus());
        assertNull(CurrentUserContext.get());
    }

    @Test
    void validGatewayHeadersPopulateAndClearCurrentUser() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(GatewayUserContextInterceptor.HEADER_GATEWAY_TOKEN, "test-gateway-token");
        request.addHeader(GatewayUserContextInterceptor.HEADER_USER_ID, "42");
        request.addHeader(GatewayUserContextInterceptor.HEADER_USERNAME, "alice");
        request.addHeader(GatewayUserContextInterceptor.HEADER_TOKEN_ID, "token-1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertTrue(interceptor.preHandle(request, response, new Object()));
        CurrentUser currentUser = CurrentUserContext.get();
        assertEquals(42L, currentUser.userId());
        assertEquals("alice", currentUser.username());
        assertEquals("token-1", currentUser.tokenId());

        interceptor.afterCompletion(request, response, new Object(), null);
        assertNull(CurrentUserContext.get());
    }
}
