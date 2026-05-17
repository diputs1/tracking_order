package com.example.tracking_order.modules.auth.service;

import com.example.tracking_order.modules.auth.entity.RefreshToken;

public interface RefreshTokenService {
    RefreshToken createRefreshToken(Long userId);
    RefreshToken verifyExpiration(RefreshToken token);
    int deleteByUserId(Long userId);
    int deleteByToken(String token);
}
