package com.example.tracking_order.common.exception;

import lombok.Getter;

@Getter
public class AppException extends RuntimeException {
    private final ErrorCode errorCode;
    private final String message;
    private final Object errors;

    public AppException(ErrorCode errorCode) {
        super(errorCode.getCode());
        this.errorCode = errorCode;
        this.message = errorCode.name();
        this.errors = null;
    }

    public AppException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.message = message;
        this.errors = null;
    }

    public AppException(ErrorCode errorCode, String message, Object errors) {
        super(message);
        this.errorCode = errorCode;
        this.message = message;
        this.errors = errors;
    }
}
