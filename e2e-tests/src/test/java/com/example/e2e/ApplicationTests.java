package com.example.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Заявки на участие: подача участником, приём админом, счётчик, отмена. */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("APPLICATIONS — заявки на участие")
class ApplicationTests {

    static final ApiClient api = new ApiClient();
    static String admin, userTok, user2Tok;
    static long eventId, appId;

    @BeforeAll
    static void setup() {
        admin = api.adminToken();
        assertNotNull(admin, "нужен токен админа");
        assumeTrue(Db.available(), "нужна БД для регистрации подтверждённых участников");

        String future = LocalDateTime.now().plusDays(30).withNano(0).toString();
        var ev = api.post("/api/events", Map.of(
                "title", "QA AppEvent " + System.currentTimeMillis(),
                "description", "x", "eventDate", future,
                "maxParticipants", 50, "type", "MEETUP"), admin);
        eventId = ev.asLong("id");
        assertTrue(eventId > 0, "не удалось создать мероприятие: " + ev.raw());

        userTok  = api.registerVerifiedUser(ApiClient.uniqueEmail("qa_part"),  "Passw0rd!", "QA Participant");
        user2Tok = api.registerVerifiedUser(ApiClient.uniqueEmail("qa_part2"), "Passw0rd!", "QA Participant 2");
        assertNotNull(userTok,  "участник 1 должен залогиниться");
        assertNotNull(user2Tok, "участник 2 должен залогиниться");
    }

    @Test @Order(1) @DisplayName("Подача заявки участником → 201")
    void apply() {
        var r = api.post("/api/applications",
                Map.of("eventId", eventId, "comment", "Хочу участвовать"), userTok);
        assertEquals(201, r.status(), r.raw());
        appId = r.asLong("id");
        assertTrue(appId > 0);
    }

    @Test @Order(2) @DisplayName("Повторная заявка на то же мероприятие отклоняется")
    void duplicateApply() {
        var r = api.post("/api/applications",
                Map.of("eventId", eventId, "comment", "again"), userTok);
        assertTrue(r.status() >= 400, "повторная заявка должна падать, HTTP " + r.status());
    }

    @Test @Order(3) @DisplayName("Список моих заявок → 200")
    void myApplications() {
        var r = api.get("/api/applications/my", userTok);
        assertEquals(200, r.status(), r.raw());
    }

    @Test @Order(4) @DisplayName("Список заявок по мероприятию (админ) → 200")
    void listForEvent() {
        var r = api.get("/api/applications/event/" + eventId, admin);
        assertEquals(200, r.status(), r.raw());
    }

    @Test @Order(5) @DisplayName("Приём заявки админом (APPROVED) → 200")
    void review() {
        var r = api.put("/api/applications/" + appId + "/review",
                Map.of("status", "APPROVED", "reviewComment", "Добро пожаловать"), admin);
        assertEquals(200, r.status(), r.raw());
        assertEquals("APPROVED", r.str("status"));
    }

    @Test @Order(6) @DisplayName("Счётчик approvedCount мероприятия = 1")
    void approvedCount() {
        var r = api.get("/api/events/" + eventId, null);
        assertEquals(1, r.asLong("approvedCount"), "после приёма одной заявки approvedCount=1");
    }

    @Test @Order(7) @DisplayName("Участник видит свою заявку как APPROVED")
    void participantSeesApproved() {
        var r = api.get("/api/applications/my", userTok);
        boolean ok = false;
        for (JsonNode n : r.body()) if ("APPROVED".equals(n.get("status").asText())) ok = true;
        assertTrue(ok, "у участника должна быть APPROVED-заявка");
    }

    @Test @Order(8) @DisplayName("Отмена своей PENDING-заявки участником → 204")
    void cancelPending() {
        var a = api.post("/api/applications",
                Map.of("eventId", eventId, "comment", "временная"), user2Tok);
        assertEquals(201, a.status(), a.raw());
        long id = a.asLong("id");
        var d = api.delete("/api/applications/" + id, user2Tok);
        assertTrue(d.status() == 204 || d.status() == 200, "отмена pending-заявки, HTTP " + d.status());
    }
}
