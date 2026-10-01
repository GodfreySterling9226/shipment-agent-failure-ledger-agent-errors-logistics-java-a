package dev.infrai.logistics;

import dev.infrai.logistics.domain.ShipmentEvent;
import dev.infrai.logistics.service.ErrorCapturePort;
import dev.infrai.logistics.service.ShipmentAgentLoop;

public final class ShipmentAgentLoopTest {
    public static void main(String[] args) {
        RecordingErrors errors = new RecordingErrors();
        ShipmentAgentLoop loop = new ShipmentAgentLoop(errors, event -> {
            throw new IllegalStateException("route plan missing carrier leg");
        });
        ShipmentEvent input = new ShipmentEvent("EVT-7781", "SHP-2048", "DELIVERED",
                new ShipmentEvent.ProofOfDelivery("pod-2048.pdf", "aa48d8f7", true));

        ShipmentAgentLoop.Outcome outcome = loop.process(input);

        check(outcome.status() == ShipmentAgentLoop.Status.REVIEW_REQUIRED, "shipment needs review");
        check(outcome.captured(), "failure is captured");
        check(errors.calls == 1, "one capture per event");
        check("shipment-event-EVT-7781".equals(errors.idempotencyKey), "stable event key");
        check(errors.payload.contains("pod-2048.pdf"), "proof reference is retained");
        System.out.println("PASS shipment failure moves to review and is captured once");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static final class RecordingErrors implements ErrorCapturePort {
        private int calls;
        private String payload;
        private String idempotencyKey;

        @Override
        public void capture(String exceptionPayload, String key) {
            calls++;
            payload = exceptionPayload;
            idempotencyKey = key;
        }
    }
}

