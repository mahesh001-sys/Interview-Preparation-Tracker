package com.mahesh.tracker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mahesh.tracker.config.DatabaseConfig;
import com.mahesh.tracker.controller.CompanyController;
import com.mahesh.tracker.controller.InterviewRoundController;
import com.mahesh.tracker.controller.QuestionController;
import com.mahesh.tracker.controller.StaticFileHandler;
import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Map;

public class Main {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static void main(String[] args) throws Exception {

        HttpServer server = HttpServer.create(
                new InetSocketAddress(8080), 0
        );

        server.createContext("/api/health", exchange -> {

            int statusCode = 200;
            Map<String, Object> response;

            try (Connection connection = DatabaseConfig.getConnection();
                 Statement statement = connection.createStatement();
                 ResultSet result = statement.executeQuery("SELECT 1")) {

                result.next();

                response = Map.of(
                        "status", "UP",
                        "application", "Interview Preparation Tracker",
                        "database", "CONNECTED"
                );

            } catch (Exception exception) {

                statusCode = 503;

                response = Map.of(
                        "status", "DOWN",
                        "database", "DISCONNECTED",
                        "error", "Database connection failed"
                );

                exception.printStackTrace();
            }

            byte[] responseBytes = mapper.writeValueAsBytes(response);

            exchange.getResponseHeaders().set(
                    "Content-Type", "application/json; charset=UTF-8"
            );
            exchange.getResponseHeaders().set(
                    "Access-Control-Allow-Origin", "*"
            );

            exchange.sendResponseHeaders(statusCode, responseBytes.length);

            try (var output = exchange.getResponseBody()) {
                output.write(responseBytes);
            } finally {
                exchange.close();
            }
        });

        server.createContext(
                "/api/companies",
                new CompanyController()
        );

        server.createContext(
                "/api/interview-rounds",
                new InterviewRoundController()
        );

        server.createContext(
                "/api/questions",
                new QuestionController()
        );

        // Keep static files last so API routes take priority.
        server.createContext("/", new StaticFileHandler());

        server.setExecutor(null);
        server.start();

        System.out.println("==========================================");
        System.out.println("Interview Preparation Tracker Started");
        System.out.println("Dashboard:       http://localhost:8080");
        System.out.println("Health:          http://localhost:8080/api/health");
        System.out.println("Companies:       http://localhost:8080/api/companies");
        System.out.println("Interview Rounds: http://localhost:8080/api/interview-rounds");
        System.out.println("Questions:        http://localhost:8080/api/questions");
        System.out.println("==========================================");
    }
}
