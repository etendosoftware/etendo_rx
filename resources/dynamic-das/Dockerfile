FROM amazoncorretto:17.0.14

ARG GRADLE_VERSION=8.12.1
ARG OPEN_TELEMETRY_VERSION=v2.17.0

RUN yum install -y wget unzip make

RUN wget https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip -P /tmp \
    && unzip -d /opt/gradle /tmp/gradle-${GRADLE_VERSION}-bin.zip \
    && rm /tmp/gradle-${GRADLE_VERSION}-bin.zip

RUN wget https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/${OPEN_TELEMETRY_VERSION}/opentelemetry-javaagent.jar -P /opt/open-telemetry

RUN rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

# Set environment variables for Gradle
ENV GRADLE_HOME=/opt/gradle/gradle-${GRADLE_VERSION}
ENV PATH=${PATH}:${GRADLE_HOME}/bin
ENV TASK="downloadJar"
ENV CONFIG_SERVER_URL=http://localhost:8888
ENV SPRING_CLOUD_CONFIG_SERVER_NATIVE_SEARCHLOCATIONS=
ENV SPRING_PROFILES_ACTIVE=
ENV DEBUG_PORT=5005
ENV DB_HOST=db
ENV DB_PORT=5432
ENV DB_SID=etendo

# Set the working directory for any subsequent ADD, COPY, CMD, ENTRYPOINT, or RUN instructions
WORKDIR /app

# Copy only the necessary build files
COPY resources/dynamic-das/build.gradle /app-template/
# Copy specific module gradle files and Makefile
COPY resources/dynamic-das/modules_gen/com.etendorx.entities/build.gradle /app-template/modules_gen/com.etendorx.entities/
COPY resources/dynamic-das/modules_gen/com.etendorx.entities/Makefile /app-template/modules_gen/com.etendorx.entities/
COPY resources/dynamic-das/run.sh /app-template/
RUN chmod +x /app-template/run.sh

CMD [ "/app-template/run.sh" ]
