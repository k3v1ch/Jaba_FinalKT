package com.example.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Админ-функции: доступ по ролям, статистика, смена роли с кодом безопасности. */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("ADMIN — права и управление пользователями")
class AdminTests {

    static final ApiClient api = new ApiClient();
    static final String USER_EMAIL = ApiClient.uniqueEmail("qa_role");
    static final String CODE = System.getProperty("admin.security.code", "admin-secure-2025");

    static String admin, userTok;
    static long userId;

    @BeforeAll
    static void setup() {
        admin = api.adminToken();
        assertNotNull(admin, "нужен токен админа");
        assumeTrue(Db.available(), "нужна БД для регистрации подтверждённого пользователя");
        userTok = api.registerVerifiedUser(USER_EMAIL, "Passw0rd!", "QA Role");
        assertNotNull(userTok, "обычный пользователь должен залогиниться");
    }

    @Test @Order(1) @DisplayName("Список пользователей доступен админу → 200")
    void listUsersAdmin() {
        var r = api.get("/api/admin/users", admin);
        assertEquals(200, r.status(), r.raw());
    }

    @Test @Order(2) @DisplayName("Обычному пользователю список пользователей запрещён → 403")
    void listUsersForbiddenForUser() {
        var r = api.get("/api/admin/users", userTok);
        assertEquals(403, r.status(), "обычному пользователю должно быть 403");
    }

    @Test @Order(3) @DisplayName("Статистика доступна админу → 200")
    void stats() {
        var r = api.get("/api/admin/stats", admin);
        assertEquals(200, r.status(), r.raw());
    }

    @Test @Order(4) @DisplayName("Поиск id созданного пользователя")
    void findUserId() {
        var r = api.get("/api/admin/users", admin);
        for (JsonNode n : r.body())
            if (USER_EMAIL.equals(n.get("email").asText())) userId = n.get("id").asLong();
        assertTrue(userId > 0, "id пользователя должен быть найден");
    }

    @Test @Order(5) @DisplayName("Смена роли на MODERATOR с кодом безопасности → 200")
    void changeRole() {
        assumeTrue(userId > 0, "не найден id пользователя");
        var r = api.put("/api/admin/users/" + userId + "/role",
                Map.of("role", "MODERATOR", "securityCode", CODE), admin);
        assertEquals(200, r.status(), r.raw());
        assertEquals("MODERATOR", r.str("role"), "роль должна стать MODERATOR");
    }

    @Test @Order(6) @DisplayName("Смена роли с неверным кодом отклоняется")
    void changeRoleWrongCode() {
        assumeTrue(userId > 0, "не найден id пользователя");
        var r = api.put("/api/admin/users/" + userId + "/role",
                Map.of("role", "MODERATOR", "securityCode", "WRONG"), admin);
        assertTrue(r.status() >= 400, "неверный код должен падать, HTTP " + r.status());
    }
}
