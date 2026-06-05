package com.example.auth.dto;

import com.example.auth.exception.ErrorCode;
import lombok.Getter;

@Getter
public class ErrorResponse {
    private final String code;
    private final String message;
    private final String details;

    public ErrorResponse(ErrorCode errorCode, String details) {
        this.code    = errorCode.getCode();
        this.message = errorCode.getMessage();
        this.details = details;
    }

    public ErrorResponse(ErrorCode errorCode) { this(errorCode, null); }
}
