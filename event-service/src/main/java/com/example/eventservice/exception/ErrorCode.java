package com.example.eventservice.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    EVENT_NOT_FOUND("EVENT_001", "Мероприятие не найдено", HttpStatus.NOT_FOUND),
    EVENT_CLOSED("EVENT_002", "Мероприятие закрыто для заявок", HttpStatus.BAD_REQUEST),
    EVENT_FULL("EVENT_003", "Мероприятие заполнено", HttpStatus.BAD_REQUEST),
    APPLICATION_NOT_FOUND("APP_001", "Заявка не найдена", HttpStatus.NOT_FOUND),
    APPLICATION_ALREADY_EXISTS("APP_002", "Вы уже подали заявку на это мероприятие", HttpStatus.CONFLICT),
    APPLICATION_NOT_PENDING("APP_003", "Заявка уже рассмотрена", HttpStatus.BAD_REQUEST),
    ACCESS_DENIED("SEC_001", "Доступ запрещён", HttpStatus.FORBIDDEN),
    UNAUTHORIZED("SEC_002", "Необходима авторизация", HttpStatus.UNAUTHORIZED),
    VALIDATION_ERROR("VAL_001", "Ошибка валидации", HttpStatus.BAD_REQUEST),
    INTERNAL_ERROR("SRV_001", "Внутренняя ошибка сервера", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String message;
    private final HttpStatus status;

    ErrorCode(String code, String message, HttpStatus status) {
        this.code = code;
        this.message = message;
        this.status = status;
    }
}
