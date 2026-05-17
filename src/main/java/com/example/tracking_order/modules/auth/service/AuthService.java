package com.example.tracking_order.modules.auth.service;

import com.example.tracking_order.modules.auth.dto.ForgotPasswordRequest;
import com.example.tracking_order.modules.auth.dto.LoginRequest;
import com.example.tracking_order.modules.auth.dto.LoginResponse;
import com.example.tracking_order.modules.auth.dto.TokenRefreshRequest;
import com.example.tracking_order.modules.auth.dto.TokenRefreshResponse;

public interface AuthService {
    LoginResponse login(LoginRequest request);
    void logout();
    void forgotPassword(ForgotPasswordRequest request);
    TokenRefreshResponse refreshToken(TokenRefreshRequest request);
}
