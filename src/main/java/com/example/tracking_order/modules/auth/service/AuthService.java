package com.example.tracking_order.modules.auth.service;

import com.example.tracking_order.modules.auth.dto.ForgotPasswordRequest;
import com.example.tracking_order.modules.auth.dto.LoginRequest;
import com.example.tracking_order.modules.auth.dto.LoginResponse;

public interface AuthService {
    LoginResponse login(LoginRequest request);
    void logout();
    void forgotPassword(ForgotPasswordRequest request);
}
