package com.example.wms.admin.controller.auth;

import com.example.wms.admin.annotation.SysOperationLog;
import com.example.wms.admin.service.AuthService;
import com.example.wms.admin.view.dto.LoginRequest;
import com.example.wms.admin.view.dto.LoginResponse;
import com.example.wms.common.common.ApiResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@Deprecated(forRemoval = false)
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private static final String LOCAL_LOGIN_DEPRECATED_MESSAGE =
            "WMS local login is deprecated. Please use auth-service login via gateway.";

    private final AuthService authService;
    private final boolean localLoginEnabled;

    public AuthController(
            AuthService authService,
            @Value("${wms.auth.local-login-enabled:false}") boolean localLoginEnabled) {
        this.authService = authService;
        this.localLoginEnabled = localLoginEnabled;
    }

    @PostMapping("/login")
    @SysOperationLog(
            operationType = "登录",
            content = "用户登录",
            module = "认证",
            bizNo = "#request.username()"
    )
    @Deprecated(forRemoval = false)
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        log.warn(LOCAL_LOGIN_DEPRECATED_MESSAGE);
        if (!localLoginEnabled) {
            return ResponseEntity.status(HttpStatus.GONE)
                    .body(new ApiResponse<>(410, LOCAL_LOGIN_DEPRECATED_MESSAGE, null));
        }
        return ResponseEntity.ok(ApiResponse.ok(authService.login(request)));
    }

    @PostMapping("/logout")
    @SysOperationLog(operationType = "登出", content = "用户登出", module = "认证")
    public ApiResponse<Void> logout() {
        // stateless JWT: nothing to invalidate server-side, the client just drops the token.
        return ApiResponse.ok(null);
    }

    @GetMapping("/me")
    public ApiResponse<LoginResponse> me() {
        return ApiResponse.ok(authService.me());
    }
}
