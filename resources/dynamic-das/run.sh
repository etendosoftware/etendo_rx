#!/bin/bash
# Copyright Etendo Software.
# shellcheck disable=SC1091

set -euo pipefail
# set -x  # Uncomment for debugging

# Defaults
: "${ENABLE_OPEN_TELEMETRY:=false}"
: "${DISABLE_DEBUG:=false}"
: "${JAVA_OPTS:=}"
: "${DEPENDENCIES:=com.etendorx:generate-entities:${ETENDORX_VERSION}}"

DOWNLOAD_DEP="$DEPENDENCIES"

echo "==> Copying template to /app"
cp -r /app-template/* /app/
cd /app

echo "==> Setting DEPENDENCIES for entity generation"
DEPENDENCIES="com.etendorx:generate-entities:${ETENDORX_VERSION}"
echo "DEPENDENCIES=${DEPENDENCIES}"
gradle "${TASK}" --no-daemon --info
gradle --stop

echo "==> Cleaning old generated sources"
rm -rf /app/modules_gen/com.etendorx.entities/src

echo "==> Generating gradle.properties for DB"
cat > /app/gradle.properties <<EOF
bbdd.rdbms=POSTGRE
bbdd.driver=org.postgresql.Driver
bbdd.url=jdbc:postgresql://${DB_HOST}:${DB_PORT}
bbdd.sid=${DB_SID}
bbdd.systemUser=postgres
bbdd.systemPassword=syspass
bbdd.user=tad
bbdd.password=tad
bbdd.sessionConfig=select
update_dateFormat('DD-MM-YYYY')
rx.generateCode=true
EOF

echo "==> Running base app JAR (first pass)"
java -jar libs/app.jar

echo "==> Compiling generated entities"
cd /app/modules_gen/com.etendorx.entities
echo "ETENDORX_VERSION=${ETENDORX_VERSION}" > gradle.properties
make compile

cd /app
echo "==> Setting DEPENDENCIES for DAS"
DEPENDENCIES="com.etendorx:das:${ETENDORX_VERSION}"
echo "DEPENDENCIES=${DEPENDENCIES}"
gradle "${TASK}" --no-daemon --info --refresh-dependencies

echo "==> Restoring original DEPENDENCIES"
DEPENDENCIES="$DOWNLOAD_DEP"
echo "DEPENDENCIES=${DEPENDENCIES}"
gradle "${TASK}" --no-daemon --info --refresh-dependencies
gradle --stop

echo "------ CHECKING FOR CONFIG SERVER ------"
gradle waitForConfigServer

echo "------- STARTING APP -------"
JAVA_AGENT_OPTS=""
if [ "$ENABLE_OPEN_TELEMETRY" = "true" ]; then
  JAVA_AGENT_OPTS="-javaagent:/opt/open-telemetry/opentelemetry-javaagent.jar"
fi

JAVA_LOADER_OPTS="-Dloader.path=./modules_gen/com.etendorx.entities/build/libs/com.etendorx.entities-1.0.0-plain.jar,./libs/"

JAVA_DEBUG_OPTS=""
if [ ! "$DISABLE_DEBUG" = "true" ]; then
    JAVA_DEBUG_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:${DEBUG_PORT}"
fi

ENTRYPOINT_JAVA_OPTS="${JAVA_AGENT_OPTS} ${JAVA_LOADER_OPTS} ${JAVA_DEBUG_OPTS} ${JAVA_OPTS}"

exec java ${ENTRYPOINT_JAVA_OPTS} -jar libs/app.jar
