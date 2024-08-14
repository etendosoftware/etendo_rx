FROM mcr.microsoft.com/openjdk/jdk:17-ubuntu

ARG GRADLE_VERSION=7.4.2
RUN apt update && apt install -y wget unzip make

RUN wget https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip -P /tmp \
    && unzip -d /opt/gradle /tmp/gradle-${GRADLE_VERSION}-bin.zip \
    && rm /tmp/gradle-${GRADLE_VERSION}-bin.zip

RUN rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

# Set environment variables for Gradle
ENV GRADLE_HOME=/opt/gradle/gradle-${GRADLE_VERSION}
ENV PATH=${PATH}:${GRADLE_HOME}/bin
ENV TASK="downloadJar"
ENV CONFIG_SERVER_URL=http://localhost:8888
ENV SPRING_CLOUD_CONFIG_SERVER_NATIVE_SEARCHLOCATIONS=
ENV SPRING_PROFILES_ACTIVE=
ENV ETENDORX_VERSION=${ETENDORX_VERSION}
ENV DEBUG_PORT=5005
ENV DB_HOST=db
ENV DB_PORT=5432
ENV DB_SID=etendo

# Set the working directory for any subsequent ADD, COPY, CMD, ENTRYPOINT, or RUN instructions
WORKDIR /app

# Copy only the necessary build files
COPY build.gradle /app-template/
# Copy specific module gradle files and Makefile
COPY modules_gen/com.etendorx.entities/build.gradle /app-template/modules_gen/com.etendorx.entities/
COPY modules_gen/com.etendorx.entities/Makefile /app-template/modules_gen/com.etendorx.entities/

# Prefer non-interactive gradle commands to reduce logs and potential hangups
CMD cp -r /app-template/* /app/ && \
    cd /app && \
    export DEPENDENCIES="com.etendorx:generate-entities:${ETENDORX_VERSION}" && \
    gradle ${TASK} --no-daemon --info && gradle --stop && \
    echo "bbdd.rdbms=POSTGRE\nbbdd.driver=org.postgresql.Driver\nbbdd.url=jdbc:postgresql://${DB_HOST}:${DB_PORT}\nbbdd.sid=${DB_SID}\nbbdd.systemUser=postgres\nbbdd.systemPassword=syspass\nbbdd.user=tad\nbbdd.password=tad\nbbdd.sessionConfig=select\nupdate_dateFormat('DD-MM-YYYY')\nrx.generateCode=true" > /app/gradle.properties && \
    java -jar libs/app.jar && \
    cd /app/modules_gen/com.etendorx.entities && \
    echo "ETENDORX_VERSION=${ETENDORX_VERSION}" > gradle.properties && \
    make compile && \
    cd /app && \
    export DEPENDENCIES="com.etendorx:das:${ETENDORX_VERSION}" && \
    gradle ${TASK} --no-daemon --info --refresh-dependencies && \
    gradle --stop && \
    echo "------ CHECKING FOR CONFIG SERVER ------" && \
    gradle waitForConfigServer && \
    echo "------- STARTING APP -------\n" && \
    java -Dloader.path=./modules_gen/com.etendorx.entities/build/libs/com.etendorx.entities-1.0.0-plain.jar -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:${DEBUG_PORT} -jar libs/app.jar
