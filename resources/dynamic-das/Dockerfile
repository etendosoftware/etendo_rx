FROM amazoncorretto:17.0.14-alpine3.21

ARG GRADLE_VERSION=8.12.1
ARG OPEN_TELEMETRY_VERSION=v2.18.1

# Install system tools
RUN apk add --no-cache wget unzip

# Install Gradle and remove archive
RUN GRADLE_ZIP=gradle-${GRADLE_VERSION}-bin.zip \
    && wget https://services.gradle.org/distributions/${GRADLE_ZIP} -P /tmp \
    && unzip /tmp/${GRADLE_ZIP} -d /opt/gradle \
    && rm /tmp/${GRADLE_ZIP}

# Install OpenTelemetry Java Agent
RUN wget https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/${OPEN_TELEMETRY_VERSION}/opentelemetry-javaagent.jar -P /opt/open-telemetry

# Set environment variables for Gradle
ENV GRADLE_HOME=/opt/gradle/gradle-${GRADLE_VERSION}
ENV PATH=${PATH}:${GRADLE_HOME}/bin
ENV TASK="downloadJar"
ENV CONFIG_SERVER_URL=http://localhost:8888
ENV SPRING_CLOUD_CONFIG_SERVER_NATIVE_SEARCHLOCATIONS=
ENV SPRING_PROFILES_ACTIVE=
ENV DEBUG_PORT=5021
ENV DB_HOST=db
ENV DB_PORT=5432
ENV DB_SID=etendo
# OpenTelemetry configuration
ENV OTEL_SERVICE_NAME=dynamic-das
ENV OTEL_EXPORTER_OTLP_ENDPOINT=http://jaeger:4318
ENV OTEL_EXPORTER_OTLP_PROTOCOL=http/protobuf
ENV OTEL_TRACES_EXPORTER=otlp
ENV OTEL_METRICS_EXPORTER=none
ENV OTEL_LOGS_EXPORTER=none
ENV OTEL_EXPORTER_OTLP_TIMEOUT=10000

# Set the working directory for any subsequent ADD, COPY, CMD, ENTRYPOINT, or RUN instructions
WORKDIR /app

# Copy only the necessary build files
COPY resources/dynamic-das/build.gradle /app-template/
# Copy specific module gradle files
COPY resources/dynamic-das/modules_gen/com.etendorx.entities/build.gradle /app-template/modules_gen/com.etendorx.entities/
COPY resources/dynamic-das/run.sh /app-template/
RUN chmod +x /app-template/run.sh

ENTRYPOINT [ "/app-template/run.sh" ]
