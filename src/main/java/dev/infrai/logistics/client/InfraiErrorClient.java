package dev.infrai.logistics.client;

import dev.infrai.logistics.config.AgentErrorConfig;
import dev.infrai.logistics.service.ErrorCapturePort;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class InfraiErrorClient implements ErrorCapturePort {
    public static final String CANONICAL_CALL = "infrai.errors.capture";
    private static final Pattern OK = Pattern.compile("\\\"ok\\\"\\s*:\\s*(true|false)");
    private static final Pattern ERROR = Pattern.compile("\\\"error\\\"\\s*:\\s*\\{(.*?)\\}", Pattern.DOTALL);
    private static final Pattern CODE = Pattern.compile("\\\"code\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"])*)\\\"");
    private static final Pattern MESSAGE = Pattern.compile("\\\"(?:message|hint)\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"])*)\\\"");

    private final AgentErrorConfig config;
    private final HttpClient http;

    public InfraiErrorClient(AgentErrorConfig config) {
        this(config, HttpClient.newBuilder().connectTimeout(config.requestTimeout()).build());
    }

    InfraiErrorClient(AgentErrorConfig config, HttpClient http) {
        this.config = config;
        this.http = http;
    }

    @Override
    public void capture(String exceptionPayload, String idempotencyKey) {
        String body = "{\"exception\":\"" + escapeJson(exceptionPayload) + "\"}";
        for (int attempt = 1; attempt <= config.maxAttempts(); attempt++) {
            HttpResponse<String> response = send(body, idempotencyKey);
            Envelope envelope = decodeEnvelope(response.body(), response.statusCode());
            if (response.statusCode() == 429 && attempt < config.maxAttempts()) {
                pause(retryDelay(response, attempt));
                continue;
            }
            if (!envelope.ok()) {
                throw new InfraiApiException(envelope.code(), envelope.message(), response.statusCode());
            }
            if (response.statusCode() >= 500) {
                throw new InfraiApiException("TRANSPORT_FAILURE", "Remote service rejected the request", response.statusCode());
            }
            return;
        }
    }

    private HttpResponse<String> send(String body, String idempotencyKey) {
        URI endpoint = config.baseUri().resolve("/v1/errors/capture");
        HttpRequest request = HttpRequest.newBuilder(endpoint)
                .timeout(config.requestTimeout())
                .header("Authorization", "Bearer " + config.apiKey())
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", idempotencyKey)
                .method("POST", HttpRequest.BodyPublishers.ofString(body))
                .build();
        try {
            return http.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException failure) {
            throw new IllegalStateException("Error capture transport failed", failure);
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Error capture interrupted", failure);
        }
    }

    private static Envelope decodeEnvelope(String body, int status) {
        Matcher ok = OK.matcher(body);
        if (!ok.find()) {
            throw new InfraiApiException("INVALID_ENVELOPE", "Response did not contain an ok field", status);
        }
        if (Boolean.parseBoolean(ok.group(1))) {
            return new Envelope(true, "", "");
        }
        Matcher error = ERROR.matcher(body);
        String errorObject = error.find() ? error.group(1) : "";
        return new Envelope(false, value(CODE, errorObject, "REQUEST_REJECTED"),
                value(MESSAGE, errorObject, "Request rejected"));
    }

    private static String value(Pattern pattern, String source, String fallback) {
        Matcher matcher = pattern.matcher(source);
        return matcher.find() ? matcher.group(1).replace("\\\"", "\"").replace("\\\\", "\\") : fallback;
    }

    private static Duration retryDelay(HttpResponse<?> response, int attempt) {
        return response.headers().firstValue("Retry-After")
                .flatMap(InfraiErrorClient::seconds)
                .map(Duration::ofSeconds)
                .orElse(Duration.ofMillis(250L * (1L << (attempt - 1))));
    }

    private static java.util.Optional<Long> seconds(String value) {
        try {
            return java.util.Optional.of(Long.parseLong(value));
        } catch (NumberFormatException ignored) {
            return java.util.Optional.empty();
        }
    }

    private static void pause(Duration delay) {
        try {
            Thread.sleep(delay.toMillis());
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Retry interrupted", failure);
        }
    }

    private static String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    private record Envelope(boolean ok, String code, String message) {}
}
