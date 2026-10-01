package dev.infrai.logistics.client;

public final class InfraiApiException extends RuntimeException {
    private final String code;
    private final int httpStatus;

    public InfraiApiException(String code, String message, int httpStatus) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public String code() {
        return code;
    }

    public int httpStatus() {
        return httpStatus;
    }
}

