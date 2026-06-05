package com.example.e2e;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Доступ к authdb для проверки факта формирования письма верификации
 * (токен в таблице verification_tokens) и подтверждения email в тестах.
 *
 * authdb опубликована на хосте (5432). Настраивается через -Ddb.url / -Ddb.user / -Ddb.pass.
 * Если БД недоступна — зависящие от неё проверки помечаются как пропущенные (assumeTrue).
 */
public final class Db {

    static final String URL  = System.getProperty("db.url", "jdbc:postgresql://localhost:5432/authdb");
    static final String USER = System.getProperty("db.user", "postgres");
    static final String PASS = System.getProperty("db.pass", "postgres");

    private Db() {}

    public static boolean available() {
        try (Connection c = DriverManager.getConnection(URL, USER, PASS)) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** Последний токен верификации для email, либо null. */
    public static String verificationToken(String email) {
        String sql = "SELECT vt.token FROM verification_tokens vt " +
                     "JOIN users u ON u.id = vt.user_id " +
                     "WHERE u.email = ? ORDER BY vt.id DESC LIMIT 1";
        try (Connection c = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        } catch (Exception e) {
            return null;
        }
    }
}
