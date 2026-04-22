package com.example.tracking_order.common.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {
    VALIDATION_ERROR("VALIDATION_ERROR", 400),
    UNAUTHORIZED("UNAUTHORIZED", 401),
    INVALID_CREDENTIALS("INVALID_CREDENTIALS", 401),
    FORBIDDEN("FORBIDDEN", 403),
    NOT_FOUND("NOT_FOUND", 404),
    DUPLICATE_SKU("DUPLICATE_SKU", 409),
    OUT_OF_STOCK("OUT_OF_STOCK", 422),
    INVALID_STATE_TRANSITION("INVALID_STATE_TRANSITION", 422),
    DISCOUNT_EXPIRED("DISCOUNT_EXPIRED", 422),
    INTERNAL_ERROR("INTERNAL_ERROR", 500);

    private final String code;
    private final int status;

    ErrorCode(String code, int status) {
        this.code = code;
        this.status = status;
    }
}
