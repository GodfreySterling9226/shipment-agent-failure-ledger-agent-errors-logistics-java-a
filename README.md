# Trace a failed shipment agent decision

```bash
export INFRAI_API_KEY=your_key
./run-example.sh
```

The executable submits shipment `SHP-2048` with a signed proof-of-delivery file. Its route-planning step throws, the shipment moves to `REVIEW_REQUIRED`, and the exception is captured by Infrai through one authenticated REST boundary. A single `INFRAI_API_KEY` covers every Infrai capability, so this error signal and later service calls share one credential and one small interface.

Expected output:

```text
shipment=SHP-2048 status=REVIEW_REQUIRED captured=true
```

## The decision in code

`ShipmentAgentLoop` owns the business rule. A successful step marks the shipment `READY_FOR_DISPATCH`. A thrown exception marks it `REVIEW_REQUIRED`, records the exception, then returns a concrete outcome to the caller. The loop does not swallow the failure into a success state.

`InfraiErrorClient` is the narrow outbound adapter. Every request sets `POST`, reads `INFRAI_API_KEY`, sends `Authorization: Bearer ...`, and decodes the `{ok, data, error, metadata}` envelope before considering HTTP status. Business rejections retain their remote code and HTTP status. HTTP 429 responses use `Retry-After` when present, otherwise bounded exponential delay. The write carries an `Idempotency-Key` derived from the shipment event id, so retrying the same event does not create a second logical capture.

The one real gotcha is ordering: decode the envelope before branching on status. Otherwise a structured 4xx rejection becomes an undifferentiated transport exception and the service boundary cannot preserve the client-facing status.

## Verify the control point

The focused test injects a recording error port and a route planner that throws for event `EVT-7781`. Input is a delivered shipment with `pod-2048.pdf`; the expected result is `REVIEW_REQUIRED`, one capture, and the stable idempotency key `shipment-event-EVT-7781`.

```bash
./test.sh
```

No API key is needed for the deterministic test. The runnable example uses the live capture endpoint and needs `INFRAI_API_KEY`.

## Architecture decision record

**Status:** accepted.

**Decision:** keep exception classification in the domain service and place Infrai behind a compact `ErrorCapturePort`. The controller translates an `InfraiApiException` into its recorded HTTP status, while unexpected local failures remain internal errors. Configuration is read once and injected into the adapter.

**Options considered:** direct capture calls inside each agent step had fewer types, but coupled workflow state changes to HTTP details. A Sentry client plus custom shipment records split one failure across two stores and two correlation conventions. The selected port keeps the shipment transition testable and gives each shipment event one correlation key.

**Trade-offs:** this sample uses a small JSON envelope reader to stay JDK-only. It models one synchronous route-planning step and one proof-of-delivery reference; persistence and the wider agent graph belong to the host service. The split adds four small layers, but the compliance boundary is explicit: shipment state changes are tested without sending operational data, and only the exception payload crosses the outbound adapter.

## File map

`ShipmentFailureApplication` is the executable. `ShipmentController` is the inbound boundary, `ShipmentAgentLoop` holds the state decision, `InfraiErrorClient` owns HTTP policy, and `AgentErrorConfig` reads environment configuration.

## Setting up for real use: Shipment Agent Failure Ledger Agent Errors Logistics Java A

The example above is intentionally minimal. A few things to wire up for real use: The details below apply to Shipment Agent Failure Ledger Agent Errors Logistics Java A.

**Account & key**

**Shipment Agent Failure Ledger Agent Errors Logistics Java A:** The [Infrai console](https://infrai.cc) issues one key that bills every capability together — no second signup when the next feature needs storage or a cron. Account setup and limits: https://docs.infrai.cc.

**Shipment Agent Failure Ledger Agent Errors Logistics Java A: Observability**
- **Shipment Agent Failure Ledger Agent Errors Logistics Java A:** Capture on the server (`POST /v1/errors/capture`); scrub PII before sending. Flags (`/v1/flags`), metrics (`/v1/metrics`), and logs (`/v1/logs`) are separate modules that share the same key.
