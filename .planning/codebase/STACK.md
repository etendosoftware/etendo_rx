# Technology Stack

**Analysis Date:** 2026-02-05

## Languages

**Primary:**
- Java 17 - Core application language for all microservices
- Groovy - Build scripting with Gradle

**Secondary:**
- YAML - Configuration files and deployment manifests
- Properties - Application configuration files
- Bash/Shell - Deployment and utility scripts

## Runtime

**Environment:**
- JVM (Java 17) - `sourceCompatibility = JavaVersion.VERSION_17`
- Docker - Container runtime for deployment

**Package Manager:**
- Gradle 8+ - Build automation tool
- Lockfile: `gradle.properties` and `settings.gradle` present for version management
- Maven Central Repository - Primary artifact repository
- Custom Maven Repository - `https://repo.futit.cloud/repository/etendo-snapshot-jars`

## Frameworks

**Core:**
- Spring Boot 3.1.4 - Base framework for microservices
- Spring Cloud 2022.0.4 - Distributed systems support
- Spring Data JPA - ORM for relational databases
- Spring Security - Authentication and authorization
- Spring Cloud Config - Centralized configuration management
- Spring Cloud Gateway - API gateway and routing

**API & Web:**
- Spring Boot Web - REST API development
- Spring Boot WebFlux - Reactive web framework (async processing)
- Spring Cloud Starter OpenFeign - Declarative HTTP client
- OkHttp 4.10.0 - HTTP client library
- Spring HATEOAS 2.1.2 - REST hypermedia support
- SpringDoc OpenAPI 2.2.0 - OpenAPI/Swagger documentation

**Message Streaming:**
- Apache Kafka 3.6.0 - Event streaming platform
- Kafka Streams 3.6.0 - Stream processing
- Reactor Kafka - Reactive Kafka client
- Spring Cloud Stream Kafka - Kafka integration layer
- ZooKeeper 3.8 - Kafka coordination

**Authentication & Security:**
- Spring Security OAuth2 Client - OAuth2 authentication
- JJWT (Java JWT) 0.11.2 - JWT token generation and validation
- Nimbus JOSE JWT 9.47 - JWT library
- Auth0 Java JWT 3.1.0 - Legacy JWT support
- Spring Security Test - Test utilities

**Data Access:**
- Hibernate - JPA implementation
- PostgreSQL JDBC 42.3.8 - PostgreSQL driver
- Oracle JDBC 21.6.0.0.1 - Oracle database support
- H2 1.4.200 - In-memory test database

**Utility & Serialization:**
- Lombok 1.18.30 - Code generation (getters, setters, constructors)
- Jackson - JSON serialization/deserialization
- GSON 2.8.9 - Alternative JSON library
- Protocol Buffers 3.19.4 - Binary serialization format
- Apache Commons Lang 3.13.0 - Utility functions
- Jettison 1.5.4 - Alternative JSON processor

**Testing:**
- JUnit 5 (Jupiter) - Test framework
- Spring Boot Test - Spring integration testing
- WireMock 3.9.1 - HTTP mocking
- Kafka Streams Test Utils - Kafka stream testing utilities
- JUnit Vintage Engine - Legacy test compatibility

**Code Quality:**
- JaCoCo 0.8.10 - Code coverage analysis
- SonarQube - Code quality scanning

**Observability:**
- OpenTelemetry - Distributed tracing and metrics
- Spring Cloud Sleuth - Trace propagation
- Spring Boot Actuator - Application metrics and monitoring

## Key Dependencies

**Critical:**
- `org.springframework.boot:spring-boot` (3.1.4) - Core Spring Boot framework
- `org.springframework.cloud:spring-cloud-*` (2022.0.4) - Microservices patterns
- `org.apache.kafka:kafka-streams` (3.6.0) - Event-driven processing
- `io.jsonwebtoken:jjwt` (0.11.2, 0.9.1) - JWT token handling

**Infrastructure:**
- `net.devh:grpc-server-spring-boot-starter` (2.13.1.RELEASE) - gRPC support (optional, configurable)
- `org.springframework.boot:spring-boot-starter-data-rest` (2.5.10) - Optional REST data API
- `com.github.jsqlparser:jsqlparser` (5.1) - SQL parsing for DAS
- `io.github.openfeign:feign-okhttp` - HTTP client for Feign
- `io.github.openfeign:feign-jackson` - JSON serialization for Feign

## Configuration

**Environment:**
- Spring Cloud Config Server - Centralized configuration at `http://localhost:8888`
- Configuration files: `rxconfig/` directory with YAML per microservice
- Configuration sources:
  - `application.properties` - Default Spring Boot properties
  - `application.yaml` - YAML configuration
  - Environment-specific config files in `rxconfig/` (e.g., `das.yaml`, `auth.yaml`, `asyncprocess.yaml`)

**Build:**
- `build.gradle` - Root build configuration
- `settings.gradle` - Project structure and version management (version: 2.3.3)
- `gradle.properties` - Build variables and credentials
- Module-specific `build.gradle` files in each microservice directory

## Platform Requirements

**Development:**
- Java 17 JDK minimum
- Gradle 8+ (with daemon disabled in gradle.properties)
- PostgreSQL 12+ (for local development)
- Docker (for running Kafka, ZooKeeper, PostgreSQL)
- Maven Central Repository access

**Production:**
- Java 17 JRE runtime
- Spring Boot deployed as Docker containers
- Kubernetes or Docker Compose orchestration
- PostgreSQL or Oracle database backend
- Apache Kafka cluster for async processing

## Database

**Primary Database:**
- PostgreSQL (default, `bbdd.rdbms=POSTGRE`)
- Oracle database support (JDBC driver included)
- Connection pool via Spring Boot DataSource
- Default configuration in `gradle.properties`:
  - URL: `jdbc:postgresql://localhost:5432`
  - Database: `etendo`
  - User: `tad` / `postgres`

## Ports (Default Development Configuration)

**Microservices:**
- DAS (Data Access Service): 8092
- Auth Service: 8094
- Edge (API Gateway): 8096
- AsyncProcess: 8099
- OBConnector (Integration): 8101
- Config Server: 8888
- Kafka: 9092
- ZooKeeper: 2181

**Integration Points:**
- Etendo Classic: 8080 (`http://localhost:8080/etendo`)
- OpenTelemetry Collector: 4317 (OTLP endpoint)

## Version Management

- Current version: `2.3.3` (managed in `settings.gradle`)
- Version upgrade task: `upgradeRxVersion` with type parameter (major/minor/patch)
- Gradle version extension: `gradle.ext.version`

---

*Stack analysis: 2026-02-05*
