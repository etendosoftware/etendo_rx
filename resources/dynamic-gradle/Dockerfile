FROM mcr.microsoft.com/openjdk/jdk:17-ubuntu

ARG GRADLE_VERSION=7.4.2
RUN apt update && apt install -y wget unzip

RUN wget https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip -P /tmp \
    && unzip -d /opt/gradle /tmp/gradle-${GRADLE_VERSION}-bin.zip \
    && rm /tmp/gradle-${GRADLE_VERSION}-bin.zip

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

WORKDIR /app
COPY build.gradle /app

CMD gradle ${TASK} --no-daemon --info && \
    gradle --stop && \
    echo "------ CHECKING FOR CONFIG SERVER ------" && \
    gradle waitForConfigServer && \
    echo "------- STARTING APP -------\n" && \
    java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:${DEBUG_PORT} -jar libs/app.jar
