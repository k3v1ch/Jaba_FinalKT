package com.example.e2e;

import org.junit.jupiter.api.*;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Авторизация и регистрация. */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("AUTH — регистрация и авторизация")
class AuthTests {

    static final ApiClient api = new ApiClient();
    static final String EMAIL = ApiClient.uniqueEmail("qa_auth");
    static final String PW = "Passw0rd!";
    static String access, refresh;

    @Test @Order(1) @DisplayName("Регистрация нового пользователя → 200")
    void register() {
        var r = api.post("/api/auth/register",
                Map.of("email", EMAIL, "password", PW, "name", "QA Auth"), null);
        assertEquals(200, r.status(), r.raw());
    }

    @Test @Order(2) @DisplayName("Повторная регистрация того же email отклоняется")
    void duplicateRegister() {
        var r = api.post("/api/auth/register",
                Map.of("email", EMAIL, "password", PW, "name", "QA Auth"), null);
        assertTrue(r.status() >= 400, "ожидалась ошибка, получен HTTP " + r.status());
    }

    @Test @Order(3) @DisplayName("Невалидные данные регистрации отклоняются")
    void invalidRegister() {
        var r = api.post("/api/auth/register",
                Map.of("email", "bad", "password", "x", "name", ""), null);
        assertTrue(r.status() >= 400, "ожидалась 400, получен HTTP " + r.status());
    }

    @Test @Order(4) @DisplayName("Подтверждение email по токену из письма → 200")
    void verifyEmail() {
        assumeTrue(Db.available(), "нет доступа к authdb — пропуск верификации email");
        String token = Db.verificationToken(EMAIL);
        assertNotNull(token, "после регистрации должен быть создан токен верификации");
        var r = api.get("/api/auth/verify-email?token=" + token, null);
        assertEquals(200, r.status(), r.raw());
    }

    @Test @Order(5) @DisplayName("Логин с верным паролем → accessToken")
    void loginValid() {
        assumeTrue(Db.available(), "логин требует подтверждённого email (нужна БД)");
        var r = api.post("/api/auth/login", Map.of("email", EMAIL, "password", PW), null);
        assertEquals(200, r.status(), r.raw());
        access = r.str("accessToken");
        refresh = r.str("refreshToken");
        assertNotNull(access, "должен вернуться accessToken");
    }

    @Test @Order(6) @DisplayName("Логин с неверным паролем → 401")
    void loginWrong() {
        var r = api.post("/api/auth/login", Map.of("email", EMAIL, "password", "WRONGpass1"), null);
        assertEquals(401, r.status(), r.raw());
    }

    @Test @Order(7) @DisplayName("Профиль /api/users/me с токеном → 200")
    void profileWithToken() {
        assumeTrue(access != null, "нет токена (предыдущий шаг)");
        var r = api.get("/api/users/me", access);
        assertEquals(200, r.status(), r.raw());
        assertEquals(EMAIL, r.str("email"));
    }

    @Test @Order(8) @DisplayName("Профиль без токена → отказ (401/403)")
    void profileNoToken() {
        var r = api.get("/api/users/me", null);
        assertTrue(r.status() == 401 || r.status() == 403, "ожидался отказ, получен HTTP " + r.status());
    }

    @Test @Order(9) @DisplayName("Обновление токена (refresh) → 200")
    void refreshToken() {
        assumeTrue(refresh != null, "нет refresh-токена");
        var r = api.post("/api/auth/refresh", Map.of("refreshToken", refresh), null);
        assertEquals(200, r.status(), r.raw());
        assertNotNull(r.str("accessToken"));
    }

    @Test @Order(10) @DisplayName("Логин seed-админа → токен")
    void adminLogin() {
        assertNotNull(api.adminToken(), "админ admin@event.com должен логиниться");
    }
}
