package com.example.wms.admin;

import com.example.wms.admin.security.LegacyAdminAccessInterceptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyAdminAccessInterceptorTest {

    @Test
    void disabledLegacyAdminEndpointIsRejected() throws Exception {
        LegacyAdminAccessInterceptor interceptor = new LegacyAdminAccessInterceptor(new ObjectMapper(), false);
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(new MockHttpServletRequest(), response, new Object()));
        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("auth-service"));
    }

    @Test
    void enabledLegacyAdminEndpointIsAllowedForRollback() throws Exception {
        LegacyAdminAccessInterceptor interceptor = new LegacyAdminAccessInterceptor(new ObjectMapper(), true);

        assertTrue(interceptor.preHandle(new MockHttpServletRequest(), new MockHttpServletResponse(), new Object()));
    }
}
