package com.mahesh.tracker.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mahesh.tracker.dao.QuestionDAO;
import com.mahesh.tracker.model.Question;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class QuestionController implements HttpHandler {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final Set<String> ALLOWED_DIFFICULTIES = Set.of(
            "EASY", "MEDIUM", "HARD"
    );

    private final QuestionDAO questionDAO = new QuestionDAO();

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        exchange.getResponseHeaders().set(
                "Access-Control-Allow-Origin", "*"
        );
        exchange.getResponseHeaders().set(
                "Access-Control-Allow-Methods",
                "GET, POST, PUT, DELETE, OPTIONS"
        );
        exchange.getResponseHeaders().set(
                "Access-Control-Allow-Headers", "Content-Type"
        );
        exchange.getResponseHeaders().set(
                "Content-Type", "application/json; charset=UTF-8"
        );

        try {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            String path = exchange.getRequestURI().getPath();
            String basePath = "/api/questions";
            String remaining = path.substring(basePath.length());

            if (remaining.isEmpty() || remaining.equals("/")) {
                handleCollection(exchange);
                return;
            }

            if (!remaining.matches("/\\d+")) {
                sendError(exchange, 404, "Question endpoint not found");
                return;
            }

            long id = Long.parseLong(remaining.substring(1));
            handleSingleQuestion(exchange, id);

        } catch (IllegalArgumentException exception) {
            sendError(exchange, 400, exception.getMessage());
        } catch (SQLException exception) {
            exception.printStackTrace();
            sendError(exchange, 500, "A database error occurred");
        } catch (Exception exception) {
            exception.printStackTrace();
            sendError(exchange, 500, "An unexpected server error occurred");
        } finally {
            exchange.close();
        }
    }

    private void handleCollection(HttpExchange exchange)
            throws IOException, SQLException {

        switch (exchange.getRequestMethod()) {

            case "GET" ->
                    sendJson(exchange, 200, questionDAO.findAll());

            case "POST" -> {
                Question question = validate(readQuestion(exchange));
                Question created = questionDAO.create(question);
                sendJson(exchange, 201, created);
            }

            default -> {
                exchange.getResponseHeaders().set(
                        "Allow", "GET, POST, OPTIONS"
                );
                sendError(exchange, 405, "Method not allowed");
            }
        }
    }

    private void handleSingleQuestion(HttpExchange exchange, long id)
            throws IOException, SQLException {

        if (id <= 0) {
            sendError(exchange, 400, "Question ID must be positive");
            return;
        }

        switch (exchange.getRequestMethod()) {

            case "GET" -> {
                Optional<Question> question = questionDAO.findById(id);

                if (question.isPresent()) {
                    sendJson(exchange, 200, question.get());
                } else {
                    sendError(exchange, 404, "Question not found");
                }
            }

            case "PUT" -> {
                Question question = validate(readQuestion(exchange));

                Optional<Question> updated =
                        questionDAO.update(id, question);

                if (updated.isPresent()) {
                    sendJson(exchange, 200, updated.get());
                } else {
                    sendError(exchange, 404, "Question not found");
                }
            }

            case "DELETE" -> {
                if (questionDAO.delete(id)) {
                    sendJson(exchange, 200,
                            Map.of("message", "Question deleted successfully"));
                } else {
                    sendError(exchange, 404, "Question not found");
                }
            }

            default -> {
                exchange.getResponseHeaders().set(
                        "Allow", "GET, PUT, DELETE, OPTIONS"
                );
                sendError(exchange, 405, "Method not allowed");
            }
        }
    }

    private Question readQuestion(HttpExchange exchange)
            throws IOException {

        return MAPPER.readValue(
                exchange.getRequestBody(),
                Question.class
        );
    }

    private Question validate(Question question) {

        if (question == null) {
            throw new IllegalArgumentException(
                    "Request body is required"
            );
        }

        if (question.category() == null ||
                question.category().isBlank()) {
            throw new IllegalArgumentException(
                    "Question category is required"
            );
        }

        String category = question.category().trim();

        if (category.length() > 100) {
            throw new IllegalArgumentException(
                    "Category must not exceed 100 characters"
            );
        }

        if (question.questionText() == null ||
                question.questionText().isBlank()) {
            throw new IllegalArgumentException(
                    "Question text is required"
            );
        }

        if (question.questionText().length() > 10000) {
            throw new IllegalArgumentException(
                    "Question text must not exceed 10000 characters"
            );
        }

        String difficulty = question.difficulty();

        if (difficulty == null || difficulty.isBlank()) {
            difficulty = "MEDIUM";
        } else {
            difficulty = difficulty.trim().toUpperCase();
        }

        if (!ALLOWED_DIFFICULTIES.contains(difficulty)) {
            throw new IllegalArgumentException(
                    "Difficulty must be EASY, MEDIUM, or HARD"
            );
        }

        String answerNotes = question.answerNotes();

        if (answerNotes != null && answerNotes.isBlank()) {
            answerNotes = null;
        }

        return new Question(
                null,
                category,
                question.questionText().trim(),
                answerNotes,
                difficulty,
                Boolean.TRUE.equals(question.isCompleted())
        );
    }

    private void sendJson(
            HttpExchange exchange,
            int status,
            Object data
    ) throws IOException {

        byte[] response = MAPPER.writeValueAsBytes(data);
        exchange.sendResponseHeaders(status, response.length);

        try (var output = exchange.getResponseBody()) {
            output.write(response);
        }
    }

    private void sendError(
            HttpExchange exchange,
            int status,
            String message
    ) throws IOException {

        sendJson(exchange, status, Map.of("error", message));
    }
}
