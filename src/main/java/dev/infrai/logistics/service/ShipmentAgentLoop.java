package dev.infrai.logistics.service;

import dev.infrai.logistics.domain.ShipmentEvent;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Objects;

public final class ShipmentAgentLoop {
    public enum Status { READY_FOR_DISPATCH, REVIEW_REQUIRED }

    public record Outcome(String shipmentId, Status status, boolean captured) {}

    @FunctionalInterface
    public interface RoutePlanner {
        void plan(ShipmentEvent event);
    }

    private final ErrorCapturePort errors;
    private final RoutePlanner planner;

    public ShipmentAgentLoop(ErrorCapturePort errors, RoutePlanner planner) {
        this.errors = Objects.requireNonNull(errors, "errors");
        this.planner = Objects.requireNonNull(planner, "planner");
    }

    public Outcome process(ShipmentEvent event) {
        Objects.requireNonNull(event, "event");
        try {
            if (!event.proof().signed()) {
                throw new IllegalArgumentException("proof of delivery must be signed");
            }
            planner.plan(event);
            return new Outcome(event.shipmentId(), Status.READY_FOR_DISPATCH, false);
        } catch (RuntimeException failure) {
            errors.capture(exceptionPayload(event, failure), "shipment-event-" + event.eventId());
            return new Outcome(event.shipmentId(), Status.REVIEW_REQUIRED, true);
        }
    }

    private static String exceptionPayload(ShipmentEvent event, RuntimeException failure) {
        StringWriter trace = new StringWriter();
        failure.printStackTrace(new PrintWriter(trace));
        return "shipment=" + event.shipmentId() + ", event=" + event.eventId()
                + ", proof=" + event.proof().documentName() + "\n" + trace;
    }
}

