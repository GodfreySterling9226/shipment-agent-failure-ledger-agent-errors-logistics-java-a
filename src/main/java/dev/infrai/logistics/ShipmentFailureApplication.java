package dev.infrai.logistics;

import dev.infrai.logistics.client.InfraiErrorClient;
import dev.infrai.logistics.config.AgentErrorConfig;
import dev.infrai.logistics.domain.ShipmentEvent;
import dev.infrai.logistics.service.ShipmentAgentLoop;
import dev.infrai.logistics.web.ShipmentController;

public final class ShipmentFailureApplication {
    private ShipmentFailureApplication() {}

    public static void main(String[] args) {
        AgentErrorConfig config = AgentErrorConfig.fromEnvironment();
        ShipmentAgentLoop loop = new ShipmentAgentLoop(new InfraiErrorClient(config), event -> {
            throw new IllegalStateException("carrier route was absent from the planning response");
        });
        ShipmentController controller = new ShipmentController(loop);
        ShipmentEvent event = new ShipmentEvent("EVT-7781", "SHP-2048", "DELIVERED",
                new ShipmentEvent.ProofOfDelivery("pod-2048.pdf", "aa48d8f7", true));

        ShipmentController.Response response = controller.accept(event);
        if (response.body() == null) {
            throw new IllegalStateException("capture was not accepted");
        }
        System.out.printf("shipment=%s status=%s captured=%s%n", response.body().shipmentId(),
                response.body().status(), response.body().captured());
    }
}
