package dev.infrai.logistics.web;

import dev.infrai.logistics.client.InfraiApiException;
import dev.infrai.logistics.domain.ShipmentEvent;
import dev.infrai.logistics.service.ShipmentAgentLoop;

public final class ShipmentController {
    public record Response(int status, ShipmentAgentLoop.Outcome body, String errorCode) {}

    private final ShipmentAgentLoop loop;

    public ShipmentController(ShipmentAgentLoop loop) {
        this.loop = loop;
    }

    public Response accept(ShipmentEvent event) {
        try {
            return new Response(200, loop.process(event), null);
        } catch (InfraiApiException rejected) {
            int status = rejected.httpStatus() >= 400 && rejected.httpStatus() < 500
                    ? rejected.httpStatus() : 502;
            return new Response(status, null, rejected.code());
        }
    }
}

