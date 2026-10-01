package dev.infrai.logistics.service;

public interface ErrorCapturePort {
    void capture(String exceptionPayload, String idempotencyKey);
}

