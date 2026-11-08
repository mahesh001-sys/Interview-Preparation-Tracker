package com.mahesh.tracker.config;

import io.github.cdimascio.dotenv.Dotenv;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DatabaseConfig {

    private static final Dotenv DOTENV = Dotenv.configure()
            .directory("..")
            .ignoreIfMissing()
            .load();

    private DatabaseConfig() {
    }

    private static String getConfig(String key, String defaultValue) {
        String value = System.getenv(key);

        if (value == null || value.isBlank()) {
            value = DOTENV.get(key);
        }

        return (value == null || value.isBlank()) ? defaultValue : value;
    }

    public static Connection getConnection() throws SQLException {
        String host = getConfig("DB_HOST", "localhost");
        String port = getConfig("DB_PORT", "3306");
        String database = getConfig("DB_NAME", "interview_tracker");
        String username = getConfig("DB_USER", "tracker_user");
        String password = getConfig("MYSQL_PASSWORD", "");

        String url = "jdbc:mysql://" + host + ":" + port + "/" + database
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

        return DriverManager.getConnection(url, username, password);
    }
}
