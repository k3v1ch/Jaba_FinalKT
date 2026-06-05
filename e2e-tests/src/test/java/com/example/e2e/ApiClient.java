package com.example.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * Тонкая обёртка над java.net.http.HttpClient для обращения к API платформы.
 * Базовый URL и учётка админа настраиваются через -D свойства (см. README).
 */
public class ApiClient {

    public static final String BASE =
            System.getProperty("base.url", "https://event.vernovpn.com");

    private static final ObjectMapper M = new ObjectMapper();

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    /** Ответ API: статус, разобранный JSON (может быть null) и сырое тело. */
    public record Resp(int status, JsonNode body, String raw) {
        public String str(String field) {
            return (body != null && body.hasNonNull(field)) ? body.get(field).asText() : null;
        }
        public long asLong(String field) {
            return (body != null && body.hasNonNull(field)) ? body.get(field).asLong() : -1;
        }
    }

    public Resp send(String method, String path, Object body, String token) {
        try {
            HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(BASE + path))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json");
            if (token != null) b.header("Authorization", "Bearer " + token);

            BodyPublisher pub = (body == null)
                    ? BodyPublishers.noBody()
                    : BodyPublishers.ofString(body instanceof String s ? s : M.writeValueAsString(body));
            b.method(method, pub);

            HttpResponse<String> r = http.send(b.build(), HttpResponse.BodyHandlers.ofString());
            JsonNode node = (r.body() == null || r.body().isBlank()) ? null : tryParse(r.body());
            return new Resp(r.statusCode(), node, r.body());
        } catch (Exception e) {
            throw new RuntimeException("Запрос " + method + " " + path + " упал: " + e.getMessage(), e);
        }
    }

    private JsonNode tryParse(String s) {
        try { return M.readTree(s); } catch (Exception e) { return null; }
    }

    public Resp post(String p, Object b, String t)   { return send("POST", p, b, t); }
    public Resp get(String p, String t)              { return send("GET", p, null, t); }
    public Resp put(String p, Object b, String t)    { return send("PUT", p, b, t); }
    public Resp delete(String p, String t)           { return send("DELETE", p, null, t); }

    // ── высокоуровневые помощники ────────────────────────────────────────────

    /** Логин, возвращает accessToken либо null. */
    public String loginToken(String email, String password) {
        Resp r = post("/api/auth/login", Map.of("email", email, "password", password), null);
        return r.status() == 200 ? r.str("accessToken") : null;
    }

    /** Токен seed-админа (admin@event.com / Admin1234 по умолчанию). */
    public String adminToken() {
        return loginToken(
                System.getProperty("admin.email", "admin@event.com"),
                System.getProperty("admin.password", "Admin1234"));
    }

    /**
     * Регистрирует пользователя, подтверждает email по токену из БД (если БД доступна)
     * и логинит. Возвращает accessToken либо null.
     */
    public String registerVerifiedUser(String email, String password, String name) {
        post("/api/auth/register", Map.of("email", email, "password", password, "name", name), null);
        String token = Db.verificationToken(email);
        if (token != null) get("/api/auth/verify-email?token=" + token, null);
        return loginToken(email, password);
    }

    /** Уникальный email для изоляции прогонов. */
    public static String uniqueEmail(String prefix) {
        return prefix + "_" + System.currentTimeMillis() + "_" + (int) (Math.random() * 1000) + "@example.com";
    }
}
