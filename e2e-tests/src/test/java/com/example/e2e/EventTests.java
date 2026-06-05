package com.example.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Мероприятия: создание/чтение/обновление + проверки доступа и валидации. */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("EVENTS — мероприятия")
class EventTests {

    static final ApiClient api = new ApiClient();
    static String admin;
    static long eventId;
    static final String FUTURE = LocalDateTime.now().plusDays(30).withNano(0).toString();

    @BeforeAll
    static void setup() {
        admin = api.adminToken();
        assertNotNull(admin, "нужен токен админа (admin@event.com)");
    }

    @Test @Order(1) @DisplayName("Создание мероприятия админом → 201")
    void create() {
        var body = Map.of(
                "title", "QA Event " + System.currentTimeMillis(),
                "description", "автотест",
                "eventDate", FUTURE,
                "maxParticipants", 50,
                "type", "CONFERENCE");
        var r = api.post("/api/events", body, admin);
        assertEquals(201, r.status(), r.raw());
        eventId = r.asLong("id");
        assertTrue(eventId > 0, "должен вернуться id мероприятия");
    }

    @Test @Order(2) @DisplayName("Создание без токена → отказ (401/403)")
    void createNoToken() {
        var body = Map.of("title", "x", "eventDate", FUTURE, "maxParticipants", 5, "type", "CONFERENCE");
        var r = api.post("/api/events", body, null);
        assertTrue(r.status() == 401 || r.status() == 403, "ожидался отказ, получен HTTP " + r.status());
    }

    @Test @Order(3) @DisplayName("Список открытых мероприятий содержит созданное")
    void listContainsEvent() {
        var r = api.get("/api/events", null);
        assertEquals(200, r.status(), r.raw());
        boolean found = false;
        for (JsonNode n : r.body()) if (n.get("id").asLong() == eventId) found = true;
        assertTrue(found, "созданное мероприятие должно присутствовать в списке");
    }

    @Test @Order(4) @DisplayName("Получение мероприятия по id → 200")
    void getById() {
        var r = api.get("/api/events/" + eventId, null);
        assertEquals(200, r.status(), r.raw());
        assertEquals(eventId, r.asLong("id"));
    }

    @Test @Order(5) @DisplayName("Обновление мероприятия админом → 200")
    void update() {
        var r = api.put("/api/events/" + eventId, Map.of("description", "обновлено автотестом"), admin);
        assertEquals(200, r.status(), r.raw());
    }

    @Test @Order(6) @DisplayName("Дата в прошлом отклоняется валидацией")
    void pastDateRejected() {
        var body = Map.of("title", "past", "eventDate", "2000-01-01T10:00:00",
                "maxParticipants", 5, "type", "CONFERENCE");
        var r = api.post("/api/events", body, admin);
        assertTrue(r.status() >= 400, "дата в прошлом должна падать, получен HTTP " + r.status());
    }
}
