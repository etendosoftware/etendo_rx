FROM mcr.microsoft.com/openjdk/jdk:17-ubuntu AS das
COPY modules_gen/com.etendorx.entities/build/libs/com.etendorx.entities-2.1.0-plain.jar /opt/app/lib/entities.jar
COPY modules_core/com.etendorx.das/build/libs/com.etendorx.das-2.1.0.jar /opt/app/das.jar
COPY modules_core/com.etendorx.edge/build/libs/com.etendorx.edge-2.1.0.jar /opt/app/edge.jar
ENTRYPOINT ["java", "-Dfile.encoding=UTF-8" , "-Dloader.path=/opt/app/lib/" , "-jar", "/opt/app/das.jar"]