#!/bin/sh
set -eu
BUILD_DIR="${TMPDIR:-/tmp}/shipment-agent-example"
mkdir -p "$BUILD_DIR"
javac -d "$BUILD_DIR" $(find src/main/java -name '*.java' | sort)
java -cp "$BUILD_DIR" dev.infrai.logistics.ShipmentFailureApplication

