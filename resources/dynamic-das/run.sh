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

export DOWNLOAD_DEP="$DEPENDENCIES"

echo "🚀 [ENTRYPOINT] Starting container configuration..."
echo "--------------------------------------------------------"

# --- 1. Environment Preparation ---
echo "⚙️ [STEP 1/5] Preparing the working environment..."
cp -r /app-template/* /app/
cd /app

# --- 2. Dynamic Entity Generation ---
echo "🧬 [STEP 2/5] Generating entities from the database..."
echo "  -> Configuring dependency for the 'generate-entities' tool."
export DEPENDENCIES="com.etendorx:generate-entities:${ETENDORX_VERSION}"
if [ "$DEBUG_MODE" = "true" ]; then
    echo "⚙️ DEBUG MODE: generate-entities dependency: ${DEPENDENCIES}"
fi
echo "  -> Downloading the tool using Gradle..."
gradle "${TASK}" ${GRADLE_FLAGS}
gradle --stop

echo "  -> Cleaning old source code (if it exists)."
rm -rf /app/modules_gen/com.etendorx.entities/src

echo "  -> Creating gradle.properties file."
echo -e "bbdd.rdbms=POSTGRE
bbdd.driver=org.postgresql.Driver
bbdd.url=jdbc:postgresql://${DB_HOST}:${DB_PORT}
bbdd.sid=${DB_SID}
bbdd.systemUser=postgres
bbdd.systemPassword=syspass
bbdd.user=tad
bbdd.password=tad
bbdd.sessionConfig=select
update_dateFormat('DD-MM-YYYY')
rx.generateCode=true" > /app/gradle.properties
if [ "$DEBUG_MODE" = "true" ]; then
  echo "⚙️ DEBUG MODE: gradle.properties file:"
  cat /app/gradle.properties
fi

echo "  -> Running the tool to generate the entity source code..."
java -jar libs/app.jar

# --- 3. Compilation of Generated Entities ---
echo "🛠️ [STEP 3/5] Compiling the generated entities..."
cd /app/modules_gen/com.etendorx.entities
echo -e "ETENDORX_VERSION=${ETENDORX_VERSION}" > gradle.properties
echo "  -> Compiling the source code..."
gradle build ${GRADLE_FLAGS}

# --- 4. Downloading Application Dependencies ---
echo "📦 [STEP 4/5] Downloading main application dependencies..."
cd /app

echo "  -> Downloading the main artifact (DAS)..."
export DEPENDENCIES="com.etendorx:das:${ETENDORX_VERSION}"
echo "DEPENDENCIES=${DEPENDENCIES}"
if [ "$DEBUG_MODE" = "true" ]; then
  echo "⚙️ DEBUG MODE: Main artifact (DAS): ${DEPENDENCIES}"
fi
gradle "${TASK}" ${GRADLE_FLAGS}

echo "  -> Downloading the user's additional dependencies..."
export DEPENDENCIES="$DOWNLOAD_DEP"
echo "DEPENDENCIES=${DEPENDENCIES}"
if [ "$DEBUG_MODE" = "true" ]; then
  echo "⚙️ DEBUG MODE: Additional dependencies: ${DEPENDENCIES}"
fi
gradle "${TASK}" ${GRADLE_FLAGS}
gradle --stop

# --- 5. Application Startup ---
echo "⏳ [STEP 5/5] Starting the application..."
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

JAVA_LOADER_OPTS="-Dloader.path=/app/modules_gen/com.etendorx.entities/build/libs/com.etendorx.entities-1.0.0-plain.jar,/app/libs/"

JAVA_DEBUG_OPTS=""
if [ ! "$DISABLE_DEBUG" = "true" ]; then
    JAVA_DEBUG_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:${DEBUG_PORT}"
fi

if [ "$DEBUG_MODE" = "true" ] && [ -n "${JAVA_OPTS}" ]; then
   echo "⚙️ DEBUG MODE: Additional JAVA_OPTS: ${JAVA_OPTS}"
fi

ENTRYPOINT_JAVA_OPTS="${JAVA_AGENT_OPTS} ${JAVA_LOADER_OPTS} ${JAVA_DEBUG_OPTS} ${JAVA_OPTS}"
if [ "$DEBUG_MODE" = "true" ]; then
  echo "⚙️ DEBUG MODE: EntryPoint JAVA_OPTS: ${ENTRYPOINT_JAVA_OPTS}"
fi
echo "🚀 ------- RUNNING APP ------- 🚀"
java ${ENTRYPOINT_JAVA_OPTS} -jar libs/app.jar
