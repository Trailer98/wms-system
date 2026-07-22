package com.example.wms.admin.client;

import com.example.wms.common.common.ApiResponse;
import com.example.wms.common.common.ForbiddenException;
import com.example.wms.common.common.ServiceUnavailableException;
import com.example.wms.common.common.UnauthorizedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class AuthServiceClient {

    private static final ParameterizedTypeReference<ApiResponse<AuthContextResponse>> AUTH_CONTEXT_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient restClient;
    private final String applicationCode;

    public AuthServiceClient(
            RestClient.Builder restClientBuilder,
            @Value("${auth-service.base-url:http://127.0.0.1:8081/auth}") String baseUrl,
            @Value("${auth-service.application-code:WMS}") String applicationCode) {
        this.restClient = restClientBuilder.baseUrl(stripTrailingSlash(baseUrl)).build();
        this.applicationCode = applicationCode;
    }

    public AuthContextResponse getCurrentUserContext(String authorizationHeader) {
        if (!StringUtils.hasText(authorizationHeader)) {
            throw new UnauthorizedException("missing Authorization header");
        }

        try {
            ApiResponse<AuthContextResponse> response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/context")
                            .queryParam("applicationCode", applicationCode)
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                    .retrieve()
                    .body(AUTH_CONTEXT_TYPE);
            if (response == null || response.code() != 200 || response.data() == null) {
                throw new ForbiddenException("empty permission context");
            }
            return response.data();
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 401) {
                throw new UnauthorizedException("auth-service rejected token");
            }
            if (ex.getStatusCode().value() == 403) {
                throw new ForbiddenException("auth-service rejected permission context");
            }
            throw new ServiceUnavailableException("auth-service permission context unavailable", ex);
        } catch (RestClientException ex) {
            throw new ServiceUnavailableException("auth-service permission context unavailable", ex);
        }
    }

    private String stripTrailingSlash(String value) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException("auth-service.base-url must not be blank");
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
