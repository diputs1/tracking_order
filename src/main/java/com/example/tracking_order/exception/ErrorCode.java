package com.example.tracking_order.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {
    USER_NOT_FOUND(404, "User not found"),
    UNAUTHORIZED(401, "Unauthorized access"),
    BAD_REQUEST(400, "Bad request"),
    INTERNAL_SERVER_ERROR(500, "Internal server error"),
    EMAIL_ALREADY_EXISTS(400, "Email already exists"),
    PHONE_ALREADY_EXISTS(400, "Phone number already exists");

    private final int status;
    private final String message;

    ErrorCode(int status, String message) {
        this.status = status;
        this.message = message;
    }
}
