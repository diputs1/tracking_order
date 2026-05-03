package com.example.tracking_order.modules.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;
    
@Data
@Builder
public class LoginResponse {
    @JsonProperty("access_token")
    private String accessToken;
    @JsonProperty("refresh_token")
    private String refreshToken;
    @JsonProperty("token_type")
    private String tokenType;
    private UserDto user;

    @Data
    @Builder
    public static class UserDto {
        private Long id;
        @JsonProperty("full_name")
        private String fullName;
        private String email;
        private List<String> roles;
        @JsonProperty("avatar_url")
        private String avatarUrl;
    }
}
