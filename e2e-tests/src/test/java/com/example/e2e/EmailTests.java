package com.example.e2e;

import org.junit.jupiter.api.*;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Отправка письма верификации.
 *
 * Факт реальной доставки на SMTP (Beget) виден в логах auth-service строкой
 * "EMAIL: sent to ...". Здесь проверяется функциональная цепочка письма:
 * регистрация создаёт токен верификации (та самая ссылка из письма),
 * а переход по ней активирует аккаунт.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("EMAIL — письмо верификации")
class EmailTests {

    static final ApiClient api = new ApiClient();
    static final String EMAIL = ApiClient.uniqueEmail("qa_mail");
    static final String PW = "Passw0rd!";

    @Test @Order(1) @DisplayName("Регистрация триггерит формирование письма (токен в БД)")
    void registrationCreatesVerificationToken() {
        var r = api.post("/api/auth/register",
                Map.of("email", EMAIL, "password", PW, "name", "QA Mail"), null);
        assertEquals(200, r.status(), r.raw());

        assumeTrue(Db.available(), "нет доступа к authdb — проверьте отправку письма в логах auth-service");
        String token = Db.verificationToken(EMAIL);
        assertNotNull(token,
                "после регистрации в БД должен появиться токен верификации — " +
                "именно эта ссылка уходит письмом на SMTP");
    }

    @Test @Order(2) @DisplayName("Переход по ссылке из письма активирует аккаунт (логин проходит)")
    void verificationLinkActivatesAccount() {
        assumeTrue(Db.available(), "нет доступа к authdb");
        // до верификации логин запрещён
        var before = api.post("/api/auth/login", Map.of("email", EMAIL, "password", PW), null);
        assertTrue(before.status() >= 400, "до подтверждения email логин не должен проходить");

        String token = Db.verificationToken(EMAIL);
        assertNotNull(token);
        var verify = api.get("/api/auth/verify-email?token=" + token, null);
        assertEquals(200, verify.status(), verify.raw());

        var after = api.post("/api/auth/login", Map.of("email", EMAIL, "password", PW), null);
        assertEquals(200, after.status(), "после подтверждения email логин должен проходить");
        assertNotNull(after.str("accessToken"));
    }
}
