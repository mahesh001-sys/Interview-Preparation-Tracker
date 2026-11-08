package com.mahesh.tracker.controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class StaticFileHandler implements HttpHandler {

    private final Path frontendDirectory =
            Path.of("..", "frontend").toAbsolutePath().normalize();

    private static final Map<String, String> CONTENT_TYPES = Map.of(
            ".html", "text/html; charset=UTF-8",
            ".css", "text/css; charset=UTF-8",
            ".js", "application/javascript; charset=UTF-8",
            ".json", "application/json; charset=UTF-8",
            ".png", "image/png",
            ".jpg", "image/jpeg",
            ".svg", "image/svg+xml"
    );

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        try {
            String method = exchange.getRequestMethod();

            if (!method.equals("GET") && !method.equals("HEAD")) {
                exchange.getResponseHeaders().set("Allow", "GET, HEAD");
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            String requestPath = exchange.getRequestURI().getPath();

            if (requestPath.equals("/")) {
                requestPath = "/index.html";
            }

            Path file = frontendDirectory
                    .resolve(requestPath.substring(1))
                    .normalize();

            if (!file.startsWith(frontendDirectory) ||
                    !Files.isRegularFile(file)) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }

            String filename = file.getFileName().toString();
            String extension = filename.contains(".")
                    ? filename.substring(filename.lastIndexOf("."))
                    : "";

            String contentType = CONTENT_TYPES.getOrDefault(
                    extension, "application/octet-stream"
            );

            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.getResponseHeaders().set(
                    "X-Content-Type-Options", "nosniff"
            );

            long fileSize = Files.size(file);

            if (method.equals("HEAD")) {
                exchange.sendResponseHeaders(200, -1);
                return;
            }

            exchange.sendResponseHeaders(200, fileSize);

            try (var output = exchange.getResponseBody()) {
                Files.copy(file, output);
            }

        } finally {
            exchange.close();
        }
    }
}
