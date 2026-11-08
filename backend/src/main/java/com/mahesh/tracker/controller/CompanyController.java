package com.mahesh.tracker.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mahesh.tracker.dao.CompanyDAO;
import com.mahesh.tracker.model.Company;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class CompanyController implements HttpHandler {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Set<String> ALLOWED_STATUSES = Set.of(
            "APPLIED", "ASSESSMENT", "INTERVIEW",
            "OFFER", "REJECTED", "WITHDRAWN"
    );

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
            String remaining = path.substring("/api/companies".length());

            if (remaining.isEmpty() || "/".equals(remaining)) {
                handleCollection(exchange);
                return;
            }

            if (!remaining.matches("/\\d+")) {
                sendError(exchange, 404, "Company endpoint not found");
                return;
            }

            long id = Long.parseLong(remaining.substring(1));
            handleSingleCompany(exchange, id);

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
            case "GET" ->
                    sendJson(exchange, 200, companyDAO.findAll());

            case "POST" -> {
                Company company = readCompany(exchange);
                validate(company);

                Company created = companyDAO.create(company);
                sendJson(exchange, 201, created);
            }

            default -> {
                exchange.getResponseHeaders().set("Allow", "GET, POST, OPTIONS");
                sendError(exchange, 405, "Method not allowed");
            }
        }
    }

    private void handleSingleCompany(HttpExchange exchange, long id)
            throws IOException, SQLException {

        if (id <= 0) {
            sendError(exchange, 400, "Company ID must be positive");
            return;
        }

        String method = exchange.getRequestMethod();

        switch (method) {
            case "GET" -> {
                Optional<Company> company = companyDAO.findById(id);

                if (company.isPresent()) {
                    sendJson(exchange, 200, company.get());
                } else {
                    sendError(exchange, 404, "Company not found");
                }
            }

            case "PUT" -> {
                Company company = readCompany(exchange);
                validate(company);

                Optional<Company> updated = companyDAO.update(id, company);

                if (updated.isPresent()) {
                    sendJson(exchange, 200, updated.get());
                } else {
                    sendError(exchange, 404, "Company not found");
                }
            }

            case "DELETE" -> {
                if (companyDAO.delete(id)) {
                    sendJson(exchange, 200,
                            Map.of("message", "Company deleted successfully"));
                } else {
                    sendError(exchange, 404, "Company not found");
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

    private Company readCompany(HttpExchange exchange)
            throws IOException {

        return MAPPER.readValue(
                exchange.getRequestBody(),
                Company.class
        );
    }

    private void validate(Company company) {

        if (company == null) {
            throw new IllegalArgumentException("Request body is required");
        }

        if (company.companyName() == null ||
                company.companyName().isBlank()) {
            throw new IllegalArgumentException("Company name is required");
        }

        if (company.companyName().length() > 150) {
            throw new IllegalArgumentException(
                    "Company name must not exceed 150 characters"
            );
        }

        if (company.jobRole() == null || company.jobRole().isBlank()) {
            throw new IllegalArgumentException("Job role is required");
        }

        if (company.jobRole().length() > 150) {
            throw new IllegalArgumentException(
                    "Job role must not exceed 150 characters"
            );
        }

        if (company.applicationStatus() != null &&
                !company.applicationStatus().isBlank() &&
                !ALLOWED_STATUSES.contains(
                        company.applicationStatus().toUpperCase()
                )) {
            throw new IllegalArgumentException(
                    "Invalid application status"
            );
        }

        if (company.applicationDate() != null &&
                !company.applicationDate().isBlank()) {
            try {
                java.time.LocalDate.parse(company.applicationDate());
            } catch (java.time.format.DateTimeParseException exception) {
                throw new IllegalArgumentException(
                        "Application date must use YYYY-MM-DD format"
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
