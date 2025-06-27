FROM amazoncorretto:17.0.14

ARG GRADLE_VERSION=8.12.1
ARG OPEN_TELEMETRY_VERSION=v2.17.0

RUN yum install -y wget unzip

# Install gradle
RUN wget https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip -P /tmp \
    && unzip -d /opt/gradle /tmp/gradle-${GRADLE_VERSION}-bin.zip \
    && rm /tmp/gradle-${GRADLE_VERSION}-bin.zip

# Create directory for OpenTelemetry Java agent
RUN wget https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/${OPEN_TELEMETRY_VERSION}/opentelemetry-javaagent.jar -P /opt/open-telemetry

# Delete cache and tmp files
RUN rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

# Configurar variables de entorno para Gradle
ENV GRADLE_HOME=/opt/gradle/gradle-${GRADLE_VERSION}
ENV PATH=${PATH}:${GRADLE_HOME}/bin
ENV TASK="run"
ENV CONFIG_SERVER_URL=http://localhost:8888
ENV SPRING_CLOUD_CONFIG_SERVER_NATIVE_SEARCHLOCATIONS=
ENV SPRING_PROFILES_ACTIVE=
ENV DEBUG_PORT=8000
ENV OTEL_SERVICE_NAME=dinamic-gradle
ENV OTEL_EXPORTER_OTLP_ENDPOINT=http://jaeger:4318
ENV OTEL_EXPORTER_OTLP_PROTOCOL=http/protobuf

WORKDIR /app

COPY resources/dynamic-gradle/build.gradle /app
COPY resources/dynamic-gradle/run.sh /app
RUN chmod +x /app/run.sh

CMD [ "/app/run.sh" ]
