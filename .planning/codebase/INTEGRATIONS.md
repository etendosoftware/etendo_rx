# External Integrations

**Analysis Date:** 2026-02-05

## APIs & External Services

**Etendo Classic (OpenBravo) ERP:**
- Service: Etendo Classic ERP system
- What it's used for: Legacy ERP backend integration, data synchronization with modern microservices
- SDK/Client: OpenFeign (`spring-cloud-starter-openfeign`), OkHttp, custom REST client
- Connection: HTTP REST API calls
- Configuration:
  - Default endpoint: `http://localhost:8080/etendo`
  - Production endpoint (rxconfig): `https://openbravo.obc.labs.etendo.cloud/openbravo`
  - Environment variable: `openbravo.url` (in `rxconfig/obconnector.yaml`)
  - Auth: Bearer token via `openbravo.token`

**OBConnector Integration Server:**
- Service: Custom integration connector for Etendo Classic
- What it's used for: Bridging Etendo Classic with Etendo RX services
- Connection: HTTP REST API
- Endpoint: `http://localhost:8101`
- Routes: `/api/sync/**` paths via API Gateway

**Zapier Integration:**
- Service: Zapier automation platform
- What it's used for: Workflow automation and third-party integrations
- Connection: HTTP REST API
- Endpoint: `http://localhost:8091`
- Route: `/secure/zapier/**` paths (JWT-protected)
- Client: OkHttp HTTP client
- Configuration: `etendorx.zapier.url` in `rxconfig/edge.yaml`

## Data Storage

**Databases:**

**PostgreSQL (Primary):**
- Provider: PostgreSQL 12+
- Connection: JDBC (`org.postgresql:postgresql:42.3.8`)
- Configuration (development):
  - URL: `jdbc:postgresql://localhost:5432`
  - Database: `etendo`
  - Username: `tad`
  - Password: `tad`
  - System user: `postgres` / `syspass`
- Session config: `select update_dateFormat('DD-MM-YYYY')`
- ORM: Hibernate with Spring Data JPA
- Configuration in: `gradle.properties` and `rxconfig/das.yaml`

**Oracle (Optional):**
- Provider: Oracle Database
- JDBC Driver: `com.oracle.database.jdbc:ojdbc8:21.6.0.0.1`
- Connection configuration available for multi-database support
- Used for organizations needing Oracle backend compatibility

**H2 Database:**
- Purpose: In-memory test database
- Package: `com.h2database:h2:1.4.200`
- Usage: Unit and integration testing only

**File Storage:**
- Local filesystem only - No S3 or cloud storage integrations detected
- Kafka state directories: `/tmp/kafka-streams/async-process-queries`

**Caching:**
- None detected - Relying on database query optimization and Hibernate caching

## Authentication & Identity

**Auth Provider:**
- Custom: Etendo RX Auth Service (`com.etendorx.auth` module)
- Implementation:
  - JWT-based token authentication
  - OAuth2 client support for third-party authentication
  - Spring Security integration
  - Token storage: In-memory via JWT claims

**JWT Token Configuration:**
- Algorithm: RS256 (RSA with SHA-256)
- Libraries: JJWT (0.11.2, 0.9.1), Nimbus JOSE JWT, Auth0 JWT
- Token composition: Claims include:
  - `iss` (issuer): "Etendo RX Auth"
  - `iat` (issued at): Token creation timestamp
  - `ad_user_id`: User identifier
  - `ad_client_id`: Client/organization identifier
  - `ad_org_id`: Organization identifier
  - `ad_role_id`: Role identifier
  - `search_key`: Search metadata
  - `service_id`: Service identifier

**Key Management:**
- Location: `rxconfig/das.yaml`, `rxconfig/auth.yaml`
- Storage: Configuration files (RSA private/public key pairs)
- Public Key: Distributed via `/public-key` endpoints
- Private Key: Kept in secure configuration, used for token signing

**OAuth2:**
- Spring Security OAuth2 Client (`spring-boot-starter-oauth2-client`)
- Support for third-party OAuth2 providers
- Session-based user attribute mapping

**Service Authentication:**
- Token type: Bearer tokens in Authorization header
- Validation: JWT signature verification using public key
- Token injection: Via `@RequestHeader HttpHeaders headers`

## Message Queue & Event Streaming

**Apache Kafka:**
- Broker: Kafka 3.3 with Zookeeper 3.8
- Purpose: Asynchronous processing, event-driven architecture
- Configuration:
  - Bootstrap servers: `localhost:29092` (development)
  - Application ID: `async-process-queries`
  - Serialization: Custom serializers for AsyncProcess objects
  - State store location: `/tmp/kafka-streams/async-process-queries`

**Kafka Topics & Streams:**
- Async Process Topic: Main event stream for async job processing
- Topology: `com.etendorx.lib.kafka.topology.AsyncProcessTopology`
- Stream builders: Stateful processing with state stores
- Consumer groups: Managed by application ID configuration

**Message Format:**
- Primary: Custom AsyncProcess serialization
- Queue Structures: Priority queues and deques for job prioritization
- Serializers:
  - `AsyncProcessSerializer` / `AsyncProcessDeserializer`
  - `DequeSerializer` / `DequeDeserializer`
  - `PriorityQueueSerde`

**Kafka Configuration:**
- Spring Cloud Stream Kafka (`spring-cloud-starter-stream-kafka`)
- Reactor Kafka for reactive consumption (`reactor-kafka`)
- Location: `modules_core/com.etendorx.asyncprocess/`
- Config file: `rxconfig/asyncprocess.yaml`

## Monitoring & Observability

**Distributed Tracing:**
- Framework: OpenTelemetry (OTLP protocol)
- Collector endpoint: `http://localhost:4317`
- Integration: Spring Cloud Sleuth with OpenTelemetry exporter
- Trace ID ratio: 1.0 (sample all traces in development)
- Service names: Per-service (das, auth, edge, asyncprocess, etc.)

**Metrics:**
- Spring Boot Actuator - Application metrics and health
- OpenTelemetry metrics export (configurable)
- Endpoints: `/actuator/**` (all exposed in development)

**Logging:**
- Framework: SLF4J 2.0.9 (abstraction) + Logback (implementation)
- Levels configured per module:
  - `com.etendorx.entities`: DEBUG
  - `org.springframework.cloud.sleuth`: DEBUG
  - `io.opentelemetry`: TRACE
- Correlation: Spring Cloud Sleuth trace propagation

**Code Quality:**
- JaCoCo 0.8.10 - Code coverage reports (XML + HTML)
- SonarQube: `https://sonar.etendo.cloud` (configured in gradle.properties)
- Root coverage report: `jacocoRootReport` task

## CI/CD & Deployment

**Hosting:**
- Docker containers (Spring Boot buildpack)
- Kubernetes-ready deployments (YAML manifests in `config/`)
- Docker registry push configured via gradle.properties

**Container Registry:**
- Push URL: Configured via `pushUrl`, `pushUsername`, `pushPassword` in gradle.properties
- Image names: Configurable per service:
  - DAS: `${dasPushImage}`
  - Auth: `${authPushImage}`
  - Edge: `${edgePushImage}`
  - AsyncProcess: `${asyncPushImage}`
  - ZapierIntegration: `${zapierIntegrationPushImage}`

**Build Artifacts:**
- Maven publication to: `https://repo.futit.cloud/repository/etendo-snapshot-jars`
- Artifact format: JAR (compiled) and bootJar (runnable)
- Artifact naming: `{group}:{artifactId}:{version}`

**CI/CD Pipeline:**
- GitHub Actions workflows in `.github/workflows/`
- Build pipeline: `build.yml`
- Snapshot publication: `publish-snapshots/Agent.yaml`
- Image build: `image-build.yml`
- Code scanning: `snyk-code-scan.yaml`
- Git policy enforcement: `git-police.yml`
- Auto-reviewer: `auto-reviewer.yml`

## Environment Configuration

**Required Environment Variables:**

Development:
- `config.server.url` - Config server location (default: http://localhost:8888)
- `das.url` - DAS service endpoint (default: http://localhost:8092)
- `bootstrap_server` - Kafka bootstrap servers (default: kafka:9092)
- `JAVA_TOOL_OPTIONS` - JVM settings (optional, for profiling/debugging)
- `RX_VERSION` - Runtime version injection for tests

Database:
- `spring.datasource.url` - JDBC connection string
- `spring.datasource.username` - Database user
- `spring.datasource.password` - Database password

**Secrets Location:**
- Configuration files in `rxconfig/` directory (local development)
- Private keys: `rxconfig/{service}.yaml`
- Public keys: Embedded in application configuration
- Tokens: Service-specific configuration files
- Maven credentials: `gradle.properties` (local)
- Git credentials: `gradle.properties` (GitHub token for plugin resolution)

**Gradle Properties Configuration:**
- Repository: Custom Etendo repository credentials
- GitHub access: Token for private plugin repository
- Database: RDBMS type, driver, connection details, credentials
- Code generation: `rx.generateCode`, `rx.computedColumns`, `rx.views` flags
- Feature flags: `grpc.enabled`, `data-rest.enabled`, `springdoc.show-actuator`

## Webhooks & Callbacks

**Incoming:**
- Async Process callbacks: REST endpoints in `com.etendorx.asyncprocess.controller`
- Event handlers: `EventHandlerEntities` in DAS module
- No webhook receiver framework detected

**Outgoing:**
- OBConnector synchronization: Push changes to OpenBravo via REST
- AsyncProcess notifications: Event-driven via Kafka topics
- No external webhook dispatching detected

## Cross-Service Communication

**Internal Service Discovery:**
- Spring Cloud Config Server: Centralized configuration
- Service-to-service HTTP: Feign clients with load balancing
- Service URL configuration: In `rxconfig/` YAML files
- Direct endpoint references for inter-service calls

**REST API Standards:**
- OpenAPI/Swagger documentation via SpringDoc
- HATEOAS links for hypermedia navigation
- JSON serialization with Jackson
- Custom error handling and validation

---

*Integration audit: 2026-02-05*
