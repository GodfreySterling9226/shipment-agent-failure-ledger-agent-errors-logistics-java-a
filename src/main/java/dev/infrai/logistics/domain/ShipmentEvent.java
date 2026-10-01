package dev.infrai.logistics.domain;

import java.util.Objects;

public record ShipmentEvent(String eventId, String shipmentId, String eventType, ProofOfDelivery proof) {
    public ShipmentEvent {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(shipmentId, "shipmentId");
        Objects.requireNonNull(eventType, "eventType");
        Objects.requireNonNull(proof, "proof");
    }

    public record ProofOfDelivery(String documentName, String sha256, boolean signed) {
        public ProofOfDelivery {
            Objects.requireNonNull(documentName, "documentName");
            Objects.requireNonNull(sha256, "sha256");
        }
    }
}

