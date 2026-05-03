package com.example.tracking_order.modules.auth.service.impl;

import com.example.tracking_order.common.exception.AppException;
import com.example.tracking_order.common.exception.ErrorCode;
import com.example.tracking_order.modules.auth.dto.ForgotPasswordRequest;
import com.example.tracking_order.modules.auth.dto.LoginRequest;
import com.example.tracking_order.modules.auth.dto.LoginResponse;
import com.example.tracking_order.modules.auth.service.AuthService;
import com.example.tracking_order.modules.user.repository.UserRepository;
import com.example.tracking_order.security.JwtUtils;
import com.example.tracking_order.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final UserRepository userRepository;

    @Override
    public LoginResponse login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            String jwt = jwtUtils.generateJwtToken(authentication);

            String refreshToken = UUID.randomUUID().toString();

            List<String> roles = userDetails.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList());

            return LoginResponse.builder()
                    .accessToken(jwt)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .user(LoginResponse.UserDto.builder()
                            .id(userDetails.getId())
                            .email(userDetails.getUsername())
                            .fullName(userDetails.getFullName())
                            .roles(roles)
                            .avatarUrl(userDetails.getAvatarUrl())
                            .build())
                    .build();
        } catch (AuthenticationException e) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS, "Email hoặc mật khẩu không đúng");
        }
    }

    @Override
    public void logout() {
    }

    @Override
    public void forgotPassword(ForgotPasswordRequest request) {
        if (!userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.NOT_FOUND, "Email không tồn tại trong hệ thống");
        }
    }
}
