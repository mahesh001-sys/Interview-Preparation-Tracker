package com.mahesh.tracker.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mahesh.tracker.dao.CompanyDAO;
import com.mahesh.tracker.dao.InterviewRoundDAO;
import com.mahesh.tracker.model.InterviewRound;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class InterviewRoundController implements HttpHandler {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final Set<String> ALLOWED_STATUSES = Set.of(
            "PENDING", "SCHEDULED", "COMPLETED", "CANCELLED"
    );

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "TECHNICAL", "HR", "CODING", "MANAGERIAL",
            "BEHAVIORAL", "APTITUDE", "OTHER"
    );

    private final InterviewRoundDAO roundDAO = new InterviewRoundDAO();
    private final CompanyDAO companyDAO = new CompanyDAO();

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
            String basePath = "/api/interview-rounds";
            String remaining = path.substring(basePath.length());

            if (remaining.isEmpty() || remaining.equals("/")) {
                handleCollection(exchange);
                return;
            }

            if (!remaining.matches("/\\d+")) {
                sendError(exchange, 404, "Interview round endpoint not found");
                return;
            }

            long id = Long.parseLong(remaining.substring(1));
            handleSingleRound(exchange, id);

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

        String method = exchange.getRequestMethod();

        switch (method) {
            case "GET" -> {
                String companyIdParam =
                        exchange.getRequestURI().getQuery();

                if (companyIdParam == null || companyIdParam.isBlank()) {
                    sendJson(exchange, 200, roundDAO.findAll());
                    return;
                }

                if (!companyIdParam.matches("companyId=\\d+")) {
                    throw new IllegalArgumentException(
                            "Use a valid companyId query parameter"
                    );
                }

                long companyId = Long.parseLong(
                        companyIdParam.substring("companyId=".length())
                );

                if (companyId <= 0) {
                    throw new IllegalArgumentException(
                            "Company ID must be positive"
                    );
                }

                if (companyDAO.findById(companyId).isEmpty()) {
                    sendError(exchange, 404, "Company not found");
                    return;
                }

                sendJson(exchange, 200,
                        roundDAO.findByCompanyId(companyId));
            }

            case "POST" -> {
                InterviewRound round = readRound(exchange);
                validate(round);

                if (companyDAO.findById(round.companyId()).isEmpty()) {
                    sendError(exchange, 404, "Company not found");
                    return;
                }

                InterviewRound created = roundDAO.create(round);
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

    private void handleSingleRound(HttpExchange exchange, long id)
            throws IOException, SQLException {

        if (id <= 0) {
            sendError(exchange, 400, "Interview round ID must be positive");
            return;
        }

        switch (exchange.getRequestMethod()) {

            case "GET" -> {
                Optional<InterviewRound> round = roundDAO.findById(id);

                if (round.isPresent()) {
                    sendJson(exchange, 200, round.get());
                } else {
                    sendError(exchange, 404, "Interview round not found");
                }
            }

            case "PUT" -> {
                InterviewRound round = readRound(exchange);
                validate(round);

                if (companyDAO.findById(round.companyId()).isEmpty()) {
                    sendError(exchange, 404, "Company not found");
                    return;
                }

                Optional<InterviewRound> updated =
                        roundDAO.update(id, round);

                if (updated.isPresent()) {
                    sendJson(exchange, 200, updated.get());
                } else {
                    sendError(exchange, 404, "Interview round not found");
                }
            }

            case "DELETE" -> {
                if (roundDAO.delete(id)) {
                    sendJson(exchange, 200,
                            Map.of("message",
                                    "Interview round deleted successfully"));
                } else {
                    sendError(exchange, 404, "Interview round not found");
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

    private InterviewRound readRound(HttpExchange exchange)
            throws IOException {

        return MAPPER.readValue(
                exchange.getRequestBody(),
                InterviewRound.class
        );
    }

    private void validate(InterviewRound round) {

        if (round == null) {
            throw new IllegalArgumentException(
                    "Request body is required"
            );
        }

        if (round.companyId() == null || round.companyId() <= 0) {
            throw new IllegalArgumentException(
                    "A valid company ID is required"
            );
        }

        if (round.roundName() == null ||
                round.roundName().isBlank()) {
            throw new IllegalArgumentException(
                    "Round name is required"
            );
        }

        if (round.roundName().length() > 100) {
            throw new IllegalArgumentException(
                    "Round name must not exceed 100 characters"
            );
        }

        if (round.roundType() != null &&
                !round.roundType().isBlank() &&
                !ALLOWED_TYPES.contains(
                        round.roundType().toUpperCase()
                )) {
            throw new IllegalArgumentException(
                    "Invalid round type"
            );
        }

        if (round.status() != null &&
                !round.status().isBlank() &&
                !ALLOWED_STATUSES.contains(
                        round.status().toUpperCase()
                )) {
            throw new IllegalArgumentException(
                    "Invalid round status"
            );
        }

        if (round.interviewDate() != null &&
                !round.interviewDate().isBlank()) {
            try {
                LocalDateTime.parse(round.interviewDate());
            } catch (DateTimeParseException exception) {
                throw new IllegalArgumentException(
                        "Interview date must use YYYY-MM-DDTHH:mm:ss format"
                );
            }
        }
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
