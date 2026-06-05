package com.example.auth.exception;

public class AppException extends RuntimeException {
    private final ErrorCode code;
    private final String details;

    public AppException(ErrorCode code) { super(code.getMessage()); this.code = code; this.details = null; }
    public AppException(ErrorCode code, String details) {
        super(code.getMessage() + (details != null ? " (" + details + ")" : ""));
        this.code = code; this.details = details;
    }
    public AppException(ErrorCode code, String details, Throwable cause) {
        super(code.getMessage() + (details != null ? " (" + details + ")" : ""), cause);
        this.code = code; this.details = details;
    }

    public ErrorCode getCode()  { return code; }
    public String getDetails()  { return details; }
}
