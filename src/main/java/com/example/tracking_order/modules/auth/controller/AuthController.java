package com.example.tracking_order.modules.auth.controller;

import com.example.tracking_order.common.response.ApiResponse;
import com.example.tracking_order.modules.auth.dto.ForgotPasswordRequest;
import com.example.tracking_order.modules.auth.dto.LoginRequest;
import com.example.tracking_order.modules.auth.dto.LoginResponse;
import com.example.tracking_order.modules.auth.dto.TokenRefreshRequest;
import com.example.tracking_order.modules.auth.dto.TokenRefreshResponse;
import com.example.tracking_order.modules.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        authService.logout();
        return ApiResponse.success(null, "Đăng xuất thành công");
    }

    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ApiResponse.success(null, "OTP đã được gửi đến email của bạn");
    }

    @PostMapping("/refresh")
    public ApiResponse<TokenRefreshResponse> refreshToken(@Valid @RequestBody TokenRefreshRequest request) {
        return ApiResponse.success(authService.refreshToken(request));
    }
}
