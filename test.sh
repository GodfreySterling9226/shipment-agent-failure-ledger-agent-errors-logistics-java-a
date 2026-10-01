#!/bin/sh
set -eu
BUILD_DIR="${TMPDIR:-/tmp}/shipment-agent-test"
mkdir -p "$BUILD_DIR"
javac -d "$BUILD_DIR" $(find src/main/java src/test/java -name '*.java' | sort)
java -cp "$BUILD_DIR" dev.infrai.logistics.ShipmentAgentLoopTest

