package com.example.auth.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    AUTH_EMAIL_EXISTS       ("AUTH_001", HttpStatus.CONFLICT,     "Email уже зарегистрирован"),
    AUTH_USER_NOT_FOUND     ("AUTH_002", HttpStatus.NOT_FOUND,    "Пользователь не найден"),
    AUTH_EMAIL_NOT_VERIFIED ("AUTH_003", HttpStatus.FORBIDDEN,    "Email не подтверждён"),
    AUTH_BAD_CREDENTIALS    ("AUTH_004", HttpStatus.UNAUTHORIZED, "Неверный email или пароль"),
    AUTH_TOKEN_INVALID      ("AUTH_005", HttpStatus.UNAUTHORIZED, "Неверный или истёкший токен"),
    AUTH_TOKEN_USED         ("AUTH_006", HttpStatus.CONFLICT,     "Токен уже использован"),
    AUTH_REFRESH_INVALID    ("AUTH_007", HttpStatus.UNAUTHORIZED, "Неверный refresh token"),
    AUTH_REFRESH_REVOKED    ("AUTH_008", HttpStatus.UNAUTHORIZED, "Refresh token отозван"),
    AUTH_2FA_INVALID        ("AUTH_009", HttpStatus.UNAUTHORIZED, "Неверный 2FA-код"),
    AUTH_2FA_NOT_SETUP      ("AUTH_010", HttpStatus.BAD_REQUEST,  "2FA не настроена"),
    AUTH_SECURITY_CODE_INVALID("AUTH_011", HttpStatus.FORBIDDEN,  "Неверный security-code"),

    PROFILE_IMAGE_LIMIT     ("PROFILE_001", HttpStatus.CONFLICT,         "Достигнут лимит изображений (макс. 5)"),
    PROFILE_IMAGE_NOT_FOUND ("PROFILE_002", HttpStatus.NOT_FOUND,        "Изображение не найдено"),
    PROFILE_IMAGE_INVALID   ("PROFILE_003", HttpStatus.BAD_REQUEST,      "Недопустимый формат изображения"),
    PROFILE_UPLOAD_FAILED   ("PROFILE_004", HttpStatus.INTERNAL_SERVER_ERROR, "Ошибка загрузки изображения"),

    VALIDATION_FAILED ("VAL_001", HttpStatus.BAD_REQUEST,           "Ошибка валидации"),
    ACCESS_DENIED     ("SRV_002", HttpStatus.FORBIDDEN,             "Доступ запрещён"),
    SERVER_INTERNAL   ("SRV_001", HttpStatus.INTERNAL_SERVER_ERROR, "Внутренняя ошибка сервера");

    private final String code;
    private final HttpStatus status;
    private final String message;

    ErrorCode(String code, HttpStatus status, String message) {
        this.code = code; this.status = status; this.message = message;
    }

    public String getCode()       { return code; }
    public HttpStatus getStatus() { return status; }
    public String getMessage()    { return message; }
}
