#!/bin/bash
# Copyright Etendo Software.
# shellcheck disable=SC1091

set -euo pipefail
# set -x  # Uncomment for debugging

# Defaults
: "${ENABLE_OPEN_TELEMETRY:=false}"
: "${DISABLE_DEBUG:=false}"
: "${JAVA_OPTS:=}"

gradle "${TASK}" --no-daemon --info
gradle --stop

echo "------ CHECKING FOR CONFIG SERVER ------"
gradle waitForConfigServer

echo "------- STARTING APP -------"
JAVA_AGENT_OPTS=""
if [ "$ENABLE_OPEN_TELEMETRY" = "true" ]; then
  JAVA_AGENT_OPTS="-javaagent:/opt/open-telemetry/opentelemetry-javaagent.jar"
fi

JAVA_DEBUG_OPTS=""
if [ ! "$DISABLE_DEBUG" = "true" ]; then
    JAVA_DEBUG_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:${DEBUG_PORT}"
fi

ENTRYPOINT_JAVA_OPTS="${JAVA_AGENT_OPTS} ${JAVA_DEBUG_OPTS} ${JAVA_OPTS}"

exec java ${ENTRYPOINT_JAVA_OPTS} -jar libs/app.jar
