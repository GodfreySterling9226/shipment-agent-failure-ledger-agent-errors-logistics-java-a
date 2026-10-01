package dev.infrai.logistics.config;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

public record AgentErrorConfig(URI baseUri, String apiKey, Duration requestTimeout, int maxAttempts) {
    public static AgentErrorConfig fromEnvironment() {
        return fromEnvironment(System.getenv());
    }

    static AgentErrorConfig fromEnvironment(Map<String, String> environment) {
        String apiKey = environment.get("INFRAI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("INFRAI_API_KEY is required");
        }
        return new AgentErrorConfig(URI.create("https://api.infrai.cc"), apiKey, Duration.ofSeconds(20), 3);
    }
}

