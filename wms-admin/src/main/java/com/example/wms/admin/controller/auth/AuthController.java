package com.example.wms.admin.controller.auth;

import com.example.wms.admin.annotation.SysOperationLog;
import com.example.wms.admin.service.AuthService;
import com.example.wms.admin.view.dto.LoginRequest;
import com.example.wms.admin.view.dto.LoginResponse;
import com.example.wms.common.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @SysOperationLog(
            operationType = "登录",
            content = "用户登录",
            module = "认证",
            bizNo = "#request.username()"
    )
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
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
