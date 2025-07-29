#!/bin/sh
# Copyright Etendo Software.

# Exit on any error
# shellcheck disable=SC1091,SC2154
set -e

# Defaults
: "${ENABLE_OPEN_TELEMETRY:=false}"
: "${DISABLE_DEBUG:=false}"
: "${JAVA_OPTS:=}"
: "${GRADLE_FLAGS:=--no-daemon --info --refresh-dependencies}"
: "${DEBUG_MODE:=false}"

echo "🚀 [ENTRYPOINT] Starting container configuration..."
echo "--------------------------------------------------------"

# --- 1. Environment Preparation ---
echo "⚙️ [STEP 1/2] Preparing the working environment..."
echo "  -> Run the tool using Gradle..."
gradle "${TASK}" ${GRADLE_FLAGS}
gradle --stop

# --- 2. Application Startup ---
echo "⏳ [STEP 2/2] Starting the application..."
if [ "$DEBUG_MODE" = "true" ]; then
  echo "⚙️ DEBUG MODE: Waiting for the Config Server at ${CONFIG_SERVER_URL}"
fi
gradle waitForConfigServer

echo "✅ All set! Starting the main application."
JAVA_AGENT_OPTS=""
if [ "$ENABLE_OPEN_TELEMETRY" = "true" ]; then
  JAVA_AGENT_OPTS="-javaagent:/opt/open-telemetry/opentelemetry-javaagent.jar"
  if [ "$DEBUG_MODE" = "true" ]; then
    echo "⚙️ DEBUG MODE: Opentelemetry enabled - Configurations: ${ENTRYPOINT_JAVA_OPTS}"
    echo "⚙️ DEBUG MODE: OTEL_SERVICE_NAME: ${OTEL_SERVICE_NAME}"
    echo "⚙️ DEBUG MODE: OTEL_METRICS_EXPORTER: ${OTEL_METRICS_EXPORTER}"
    echo "⚙️ DEBUG MODE: OTEL_LOGS_EXPORTER: ${OTEL_LOGS_EXPORTER}"
    echo "⚙️ DEBUG MODE: OTEL_TRACES_EXPORTER: ${OTEL_TRACES_EXPORTER}"
    echo "⚙️ DEBUG MODE: OTEL_EXPORTER_OTLP_ENDPOINT: ${OTEL_EXPORTER_OTLP_ENDPOINT}"
    echo "⚙️ DEBUG MODE: OTEL_EXPORTER_OTLP_PROTOCOL: ${OTEL_EXPORTER_OTLP_PROTOCOL}"
    echo "⚙️ DEBUG MODE: OTEL_EXPORTER_OTLP_TIMEOUT: ${OTEL_EXPORTER_OTLP_TIMEOUT}"
  fi
fi

JAVA_DEBUG_OPTS=""
if [ ! "$DISABLE_DEBUG" = "true" ]; then
    JAVA_DEBUG_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:${DEBUG_PORT}"
fi

if [ "$DEBUG_MODE" = "true" ] && [ -n "${JAVA_OPTS}" ]; then
   echo "⚙️ DEBUG MODE: Additional JAVA_OPTS: ${JAVA_OPTS}"
fi

ENTRYPOINT_JAVA_OPTS="${JAVA_AGENT_OPTS} ${JAVA_DEBUG_OPTS} ${JAVA_OPTS}"
if [ "$DEBUG_MODE" = "true" ]; then
  echo "⚙️ DEBUG MODE: EntryPoint JAVA_OPTS: ${ENTRYPOINT_JAVA_OPTS}"
fi
echo "🚀 ------- RUNNING APP ------- 🚀"
java ${ENTRYPOINT_JAVA_OPTS} -jar libs/app.jar
