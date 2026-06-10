package com.example.auth.exception;

import com.example.auth.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorResponse> handleApp(AppException e) {
        ErrorCode code = e.getCode();
        if (code.getStatus().is5xxServerError())
            log.error("[{}] {} details={}", code.getCode(), code.getMessage(), e.getDetails(), e);
        else
            log.warn("[{}] {} details={}", code.getCode(), code.getMessage(), e.getDetails());
        return ResponseEntity.status(code.getStatus()).body(new ErrorResponse(code, e.getDetails()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        String details = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .findFirst().orElse(null);
        return ResponseEntity.status(ErrorCode.VALIDATION_FAILED.getStatus())
                .body(new ErrorResponse(ErrorCode.VALIDATION_FAILED, details));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxSize(MaxUploadSizeExceededException e) {
        return ResponseEntity.status(ErrorCode.PROFILE_IMAGE_INVALID.getStatus())
                .body(new ErrorResponse(ErrorCode.PROFILE_IMAGE_INVALID, "Файл слишком большой (макс. 5 МБ)"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccess(AccessDeniedException e) {
        return ResponseEntity.status(ErrorCode.ACCESS_DENIED.getStatus())
                .body(new ErrorResponse(ErrorCode.ACCESS_DENIED, e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception e) {
        log.error("[SRV_001] unhandled", e);
        // Exception class/message can leak internals (SQL, file paths) — keep the response generic.
        return ResponseEntity.status(ErrorCode.SERVER_INTERNAL.getStatus())
                .body(new ErrorResponse(ErrorCode.SERVER_INTERNAL));
    }
}
