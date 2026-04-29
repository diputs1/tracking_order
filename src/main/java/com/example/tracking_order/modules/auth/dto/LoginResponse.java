package com.example.tracking_order.modules.auth.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
    
@Data
@Builder
public class LoginResponse {
    private String access_token;
    private String refresh_token;
    private String token_type;
    private UserDto user;

    @Data
    @Builder
    public static class UserDto {
        private Long id;
        private String full_name;
        private String email;
        private List<String> roles;
        private String avatar_url;
    }
}
