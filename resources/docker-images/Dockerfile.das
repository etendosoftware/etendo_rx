FROM mcr.microsoft.com/openjdk/jdk:17-ubuntu AS das
ARG RX_VERSION
ENV RX_VERSION=${RX_VERSION}
RUN useradd -m -s /bin/bash etendo && \
    mkdir -p /opt/app/lib && \
    chown -R etendo:etendo /opt/app
COPY modules_gen/com.etendorx.entities/build/libs/com.etendorx.entities-${RX_VERSION}-plain.jar /opt/app/lib/entities.jar
COPY modules_core/com.etendorx.das/build/libs/com.etendorx.das-${RX_VERSION}.jar /opt/app/das.jar
USER etendo
ENTRYPOINT ["java", "-Dfile.encoding=UTF-8", "-Dloader.path=/opt/app/lib/", "-jar", "/opt/app/das.jar"]

