package com.example.wms.admin;

import com.example.wms.admin.client.AuthContextResponse;
import com.example.wms.admin.client.AuthServiceClient;
import org.springframework.web.client.RestClient;

import java.util.List;

class TestAuthServiceClient extends AuthServiceClient {

    private AuthContextResponse context = new AuthContextResponse(1L, "admin", "WMS", List.of("ADMIN"), List.of());

    TestAuthServiceClient() {
        super(RestClient.builder(), "http://127.0.0.1:1/auth", "WMS");
    }

    @Override
    public AuthContextResponse getCurrentUserContext(String authorizationHeader) {
        return context;
    }

    void setContext(AuthContextResponse context) {
        this.context = context;
    }
}
