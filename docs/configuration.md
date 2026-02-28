# EtendoRX Configuration Reference

This document is the authoritative reference for all configuration files, properties, and mechanisms in EtendoRX. It covers the full configuration hierarchy, the template system, every YAML file in `rxconfig/`, key properties, local development overrides, and environment variables.

---

## Table of Contents

1. [Configuration Hierarchy](#1-configuration-hierarchy)
2. [Template System](#2-template-system)
3. [File-by-File Reference](#3-file-by-file-reference)
   - [application.yaml](#applicationyaml)
   - [das.yaml](#dasyaml)
   - [worker.yaml](#workeryaml)
   - [obconnector.yaml](#obconnectoryaml)
   - [obconnsrv.yaml](#obconnsrvyaml)
   - [auth.yaml](#authyaml)
   - [edge.yaml](#edgeyaml)
   - [asyncprocess.yaml](#asyncprocessyaml)
4. [Key Properties Table](#4-key-properties-table)
5. [Local Development Overrides](#5-local-development-overrides)
6. [Environment Variables](#6-environment-variables)

---

## 1. Configuration Hierarchy

EtendoRX uses a layered Spring configuration model. Properties are resolved in the following order (highest precedence first):

```
application-local.properties  (active when spring.profiles.active=local)
        |
        v
rxconfig/<service>.yaml       (served by Spring Cloud Config Server on :8888)
        |
        v
rxconfig/application.yaml     (global defaults, served to all services)
        |
        v
application.properties        (embedded in each service JAR — bootstrap only)
```

### 1.1 Spring Cloud Config Server (port 8888)

The Config Server (`com.etendorx.configserver`) serves YAML files from the `rxconfig/` directory at the project root. All client services bootstrap with the following in their embedded `application.properties`:

```properties
config.server.url=http://localhost:8888
spring.config.import=optional:configserver:${config.server.url}
spring.application.name=<service-name>
```

The `spring.application.name` value determines which YAML file the service fetches:

| Service module | `spring.application.name` | Config file fetched |
|---|---|---|
| `com.etendorx.das` | `das` | `rxconfig/das.yaml` |
| `com.etendorx.auth` | `auth` | `rxconfig/auth.yaml` |
| `com.etendorx.edge` | `edge` | `rxconfig/edge.yaml` |
| `com.etendorx.asyncprocess` | `asyncprocess` | `rxconfig/asyncprocess.yaml` |
| `com.etendorx.integration.obconn.worker` | `worker` | `rxconfig/worker.yaml` |
| `com.etendorx.integration.obconn.server` | `obconnsrv` | `rxconfig/obconnsrv.yaml` |

All services additionally receive `rxconfig/application.yaml` as shared global defaults.

The `optional:` prefix means startup does not fail if the Config Server is unreachable. This is what enables local-profile mode (see section 1.3).

### 1.2 Service Startup Order

When running with the Config Server, services must start in the following order:

1. Config Server (`:8888`) — must be healthy before any other service starts
2. Auth (`:8094`), DAS (`:8092`), Edge (`:8096`) — in parallel
3. OBConnector Server (`:8101`), OBConnector Worker (`:8102`)

`make up` enforces this order and waits on `/actuator/health` at each step.

### 1.3 Local Profile Mode

When `SPRING_PROFILES_ACTIVE=local` is set (passed via `-Dspring.profiles.active=local` in `BOOTRUN_ARGS` in the Makefile), each service loads its `application-local.properties` file from within its own module resources. These files contain self-contained configuration that does not require a running Config Server.

`make up-local` uses this mode. It starts infrastructure (Redpanda), generates entities, builds and starts DAS, OBConnector Server, OBConnector Worker, and Async Process — all with local profiles, bypassing the Config Server, Auth, and Edge services entirely.

---

## 2. Template System

### 2.1 Overview

All files in `rxconfig/` ending in `.yaml.template` are the version-controlled source of truth. The corresponding `.yaml` files (without the `.template` suffix) are **gitignored** (see `.gitignore` line: `/rxconfig/*.yaml`) and must be generated locally before running services.

### 2.2 The `make config` Command

Running `make config` performs the following steps:

1. For each `rxconfig/*.yaml.template`, if the corresponding `.yaml` does not already exist, it copies the template to create the `.yaml`. Existing files are skipped to preserve manual edits.
2. After copying, it injects database connection values from `gradle.properties` into `rxconfig/das.yaml` using `sed`:
   - `url:` — set to `bbdd.url/bbdd.sid` (e.g., `jdbc:postgresql://localhost:5432/etendo`)
   - `username:` — set to `bbdd.user`
   - `password:` — set to `bbdd.password`

```bash
make config
```

This is automatically called as a dependency of `make up`, `make up-local`, and `make up-kafka`.

### 2.3 Gradle Properties as the Source of Truth for Database Config

The following properties in `gradle.properties` are the canonical source for database configuration injected into `das.yaml`:

```properties
bbdd.rdbms=POSTGRE
bbdd.driver=org.postgresql.Driver
bbdd.url=jdbc:postgresql://localhost\:5432
bbdd.sid=etendo
bbdd.systemUser=postgres
bbdd.systemPassword=syspass
bbdd.user=tad
bbdd.password=tad
bbdd.sessionConfig=select update_dateFormat('DD-MM-YYYY')
```

Note that `bbdd.url` uses a backslash-escaped colon (`\:`) in `gradle.properties` to prevent Gradle from misinterpreting it; the Makefile strips the backslash when injecting the value into YAML.

### 2.4 What Templates Do Not Replace

Most `.yaml.template` files are identical to the target `.yaml` — they act as safe defaults. The only template with active substitution logic is `das.yaml` (DB credentials via `make config`). All other service-specific secrets (JWT tokens, private keys, connector instance UUIDs) must be edited directly in the generated `.yaml` files after `make config` creates them.

---

## 3. File-by-File Reference

### `application.yaml`

**Path:** `rxconfig/application.yaml`
**Served to:** All services (global defaults)

This file provides shared configuration inherited by every service that connects to the Config Server.

```yaml
classic:
  url: http://localhost:8080/etendo

das:
  url: http://localhost:8092
  grpc:
    ip: localhost
    port: 9090

management:
  endpoints:
    web:
      exposure:
        include: '*'

spring:
  output:
    ansi:
      enabled: ALWAYS

public-key: >
  -----BEGIN PUBLIC KEY-----
  MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEo0SYGXhAXy35V802Hkhbj0pcARpV
  Slw2Nfm2liTNi9BPqNzS8i1hWNao37nUVGPB9wJEqDWNypn0+u4e1nuehQ==
  -----END PUBLIC KEY-----
```

**Key properties:**

| Property | Default | Description |
|---|---|---|
| `classic.url` | `http://localhost:8080/etendo` | Base URL of the Etendo Classic (Openbravo) backend |
| `das.url` | `http://localhost:8092` | Base URL of the Data Access Service |
| `das.grpc.ip` | `localhost` | gRPC host for DAS |
| `das.grpc.port` | `9090` | gRPC port for DAS |
| `management.endpoints.web.exposure.include` | `'*'` | Actuator endpoints exposed. Restrict to `health,metrics` in production |
| `spring.output.ansi.enabled` | `ALWAYS` | ANSI color codes in console output |
| `public-key` | (EC public key, PEM) | ES256 public key used by services to verify JWT tokens issued by Auth |

**Notes:**

- `management.endpoints.web.exposure.include: '*'` exposes all Spring Boot Actuator endpoints (health, metrics, env, beans, etc.). For production deployments, restrict this to only the required endpoints.
- `public-key` is the EC (P-256) public key corresponding to the private key in `auth.yaml`. All services use this to verify JWT tokens without calling Auth on every request.

---

### `das.yaml`

**Path:** `rxconfig/das.yaml`
**Application name:** `das`
**Port:** `8092`

The Data Access Service (DAS) configuration. DAS is the central data layer that exposes Etendo database entities as REST endpoints. Its database credentials are injected by `make config` from `gradle.properties`.

```yaml
server:
  port: 8092

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/etendo
    username: tad
    password: tad
  jackson:
    serialization:
      FAIL_ON_EMPTY_BEANS: false

scan:
  basePackage:

post-upsert: true
```

**Key properties:**

| Property | Default | Description |
|---|---|---|
| `server.port` | `8092` | HTTP port DAS listens on |
| `spring.datasource.url` | `jdbc:postgresql://localhost:5432/etendo` | JDBC URL for the Etendo PostgreSQL database. Injected from `bbdd.url/bbdd.sid` |
| `spring.datasource.username` | `tad` | Database user. Injected from `bbdd.user` |
| `spring.datasource.password` | `tad` | Database password. Injected from `bbdd.password` |
| `spring.jackson.serialization.FAIL_ON_EMPTY_BEANS` | `false` | Prevents Jackson serialization errors on entities with no serializable fields |
| `scan.basePackage` | (empty) | Base package for entity scanning. Set by code generation to `com.etendorx.integration.to_openbravo.mapping` when entities are generated |
| `post-upsert` | `true` | When `true`, REST API `POST` requests operate as upsert (insert or update). When `false`, `POST` only inserts |

**Notes:**

- The datasource is a direct JDBC connection to the Etendo PostgreSQL database. DAS is the only service with direct DB access; all other services access data through DAS REST or gRPC APIs.
- DAS also starts a Hibernate `CustomInterceptor` (`spring.jpa.properties.hibernate.session_factory.statement_inspector`) configured in `application.properties`, not in `das.yaml`.

---

### `worker.yaml`

**Path:** `rxconfig/worker.yaml`
**Application name:** `worker`
**Port:** `0` (random, assigned at startup) in the generated yaml; `8102` in the template

The OBConnector Worker configuration. The worker is the Kafka consumer that executes the sync pipeline (receive and send workflows).

```yaml
server:
  port: 0

openbravo:
  url:

classic:
  token:

dashboard:
  enabled: true
```

Note: The `.yaml.template` version sets `server.port: 8102` and comments out `dashboard`. The generated `worker.yaml` (after `make config`) sets `server.port: 0` by default. In practice, the local profile (`:8102`) takes precedence when running via `make up-local`.

**Key properties:**

| Property | Default | Description |
|---|---|---|
| `server.port` | `0` (template: `8102`) | HTTP port. `0` means a random port is assigned; use local profile to fix it at `8102` |
| `openbravo.url` | (empty) | URL of the Etendo Classic instance. Must be set to the same value as `classic.url` in `application.yaml` |
| `classic.token` | (empty) | JWT token used by the worker for REST calls to Etendo Classic. Must be a valid ES256 token |
| `dashboard.enabled` | `true` | When `true`, enables the worker dashboard UI at `/dashboard`. Set to `false` to disable |

**Notes:**

- Kafka bootstrap servers are not explicitly set in `worker.yaml`; they are set via `spring.kafka.bootstrap-servers` in `application-local.properties` for local development, or via the Kafka cluster configuration in production.
- The `classic.token` must correspond to a valid user/role in the Etendo database and is used to authenticate calls to the `/sws/` endpoints via Bearer authorization.

---

### `obconnector.yaml`

**Path:** `rxconfig/obconnector.yaml`
**Application name:** not directly used as a Spring app name; properties loaded by the worker

This file carries OBConnector-specific configuration that the worker loads in addition to `worker.yaml`. It defines the connector identity, the token for the async API, and the Kafka consumer group.

```yaml
token:
connector:
  instance:
  user:

async-api-url: http://localhost:8099

spring:
  kafka:
    consumer:

openbravo:
  token:
  url:
```

**Key properties:**

| Property | Default | Description |
|---|---|---|
| `token` | (empty) | JWT token used by the connector for internal EtendoRX service calls |
| `connector.instance` | (empty) | UUID identifying this connector instance in the Etendo database (`etrx_rx_services_access` table). Must match the registered connector record |
| `connector.user` | (empty) | User ID associated with the connector for reactivity operations |
| `async-api-url` | `http://localhost:8099` | Base URL of the Async Process service |
| `spring.kafka.consumer` | (empty block) | Kafka consumer configuration. Consumer group ID and other Kafka consumer settings go here |
| `openbravo.token` | (empty) | Alternative token field for Etendo Classic calls (may overlap with `classic.token` in worker.yaml depending on usage path) |
| `openbravo.url` | (empty) | URL of the Etendo Classic instance used for obconnector-specific calls |

**Notes:**

- `connector.instance` is the most critical deployment-specific value. It must be the UUID of the `ETRX_RX_SERVICES_ACCESS` record that grants this connector access to the Etendo instance. An incorrect value will cause silent authorization failures.
- In local development, `connector.instance` is set in `application-local.properties` of the worker module (see Section 5).

---

### `obconnsrv.yaml`

**Path:** `rxconfig/obconnsrv.yaml`
**Application name:** `obconnsrv`
**Port:** `8101`

The OBConnector Server configuration. The server is a lightweight REST API that receives sync requests and enqueues them for the worker.

```yaml
server:
  port: 8101
token:
```

**Key properties:**

| Property | Default | Description |
|---|---|---|
| `server.port` | `8101` | HTTP port the OBConnector Server listens on |
| `token` | (empty) | JWT token used by the server for internal service-to-service authentication |

**Notes:**

- The server is intentionally minimal. It delegates all heavy processing to the worker via Kafka.
- REST endpoints are exposed at `/api/sync/` and require an `X-TOKEN` header for authentication.
- Auth validation can be disabled for local development via `auth.disabled=true` in `application-local.properties`.

---

### `auth.yaml`

**Path:** `rxconfig/auth.yaml`
**Application name:** `auth`
**Port:** `8094`

The Auth service configuration. Auth is responsible for issuing and validating JWT tokens using EC (Elliptic Curve, P-256) key pairs. It supports OAuth2 client configuration and exposes a `/api/authenticate` login endpoint.

```yaml
server:
  port: 8094

token: <JWT token for internal service calls>

admin.token: <JWT token with admin privileges>

management:
  endpoint:
    restart:
      enabled: true

spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: placeholder
            client-secret: placeholder

private-key: >
  -----BEGIN PRIVATE KEY-----
  MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgM4dc5BquzL28t/+9
  BEZfjsQdFzdePiCwcNxYQbdi4BGhRANCAATDE+yXNQM6OCJP3ENNckQc2YOyW2FM
  zmFsXfNSMMOppqYczKzri9q9QuU/k+9WzMAlSNQXj4AdX5k8F8bjp9We
  -----END PRIVATE KEY-----

logging:
  level:
    root: INFO
```

**Key properties:**

| Property | Default | Description |
|---|---|---|
| `server.port` | `8094` | HTTP port Auth listens on |
| `token` | (ES256 JWT) | JWT token used by Auth for calls to Etendo Classic (`/sws/` endpoints). The token encodes user, client, role, organization, and warehouse |
| `admin.token` | (ES256 JWT) | JWT token with elevated privileges (different organization/warehouse context) |
| `management.endpoint.restart.enabled` | `true` | Enables the `/actuator/restart` endpoint for hot-reloading Auth without a process restart |
| `spring.security.oauth2.client.registration.google.client-id` | `placeholder` | Google OAuth2 client ID for social login. Replace with a real credential if Google login is needed |
| `spring.security.oauth2.client.registration.google.client-secret` | `placeholder` | Google OAuth2 client secret |
| `private-key` | (EC private key, PKCS8 PEM) | ES256 private key used to sign JWT tokens. Must correspond to `public-key` in `application.yaml` |
| `logging.level.root` | `INFO` | Root log level for the Auth service |

**Notes:**

- The `private-key` / `public-key` pair is an EC P-256 (secp256r1) key pair. The private key is PKCS8-encoded PEM stored in `auth.yaml`; the public key is a raw SubjectPublicKeyInfo PEM stored in `application.yaml`.
- Tokens (`token`, `admin.token`) are standard ES256 JWTs. Their payload claims include `iss`, `aud`, `user`, `client`, `role`, `organization`, `warehouse`, and `iat`. Do not use tokens with an expired-by-convention `iat` in production; regenerate as needed.
- The `token` value in `auth.yaml` is used by Auth itself to call back into Etendo Classic, not for incoming client authentication.

---

### `edge.yaml`

**Path:** `rxconfig/edge.yaml`
**Application name:** `edge`
**Port:** `8096`

The Edge service is a Spring Cloud Gateway that provides a single entry point to the EtendoRX service mesh. It routes incoming HTTP requests to the appropriate backend service.

```yaml
server:
  port: 8096

logging:
  level:
    org:
      springframework:
        web: DEBUG
      hibernate: ERROR

etendorx:
  auth:
    url: http://localhost:8094
  zapier:
    url: http://localhost:8091

spring:
  cloud:
    gateway:
      routes:
        - id: login_auth_route
          uri: ${etendorx.auth.url}
          predicates:
            - Method=GET,POST
            - Path=/login
          filters:
            - RewritePath=/login, /api/authenticate

        - id: root-route
          uri: no://op
          predicates:
            - Method=GET,POST
            - Path=/
          filters:
            - SetStatus=200
```

**Key properties:**

| Property | Default | Description |
|---|---|---|
| `server.port` | `8096` | HTTP port the Edge gateway listens on |
| `logging.level.org.springframework.web` | `DEBUG` | Log level for Spring web layer. Set to `INFO` or `WARN` in production to reduce verbosity |
| `logging.level.org.hibernate` | `ERROR` | Hibernate log level (suppress in Edge since it has no DB) |
| `etendorx.auth.url` | `http://localhost:8094` | Base URL of the Auth service. Used as the upstream URI for login route |
| `etendorx.zapier.url` | `http://localhost:8091` | Base URL of the Zapier integration service (if deployed) |
| `spring.cloud.gateway.routes[login_auth_route].uri` | `${etendorx.auth.url}` | Upstream target for `GET/POST /login` — proxied to Auth's `/api/authenticate` |
| `spring.cloud.gateway.routes[root-route].uri` | `no://op` | No-op route that returns HTTP 200 for requests to `/` (health probe compatibility) |

**Notes:**

- The Edge service is skipped in `make up-local` (local mode). Services are accessed directly on their individual ports.
- Additional routes for DAS, Auth API, and custom microservices can be added under `spring.cloud.gateway.routes` in this file.
- The `DEBUG` log level for the web layer generates significant output; lower it in any environment with meaningful traffic.

---

### `asyncprocess.yaml`

**Path:** `rxconfig/asyncprocess.yaml`
**Application name:** `asyncprocess`
**Port:** `8099`

The Async Process service configuration. This service manages asynchronous processing using Kafka Streams.

```yaml
bootstrap_servers_config: localhost:9092
application_id_config: async-process-queries
```

**Key properties:**

| Property | Default | Description |
|---|---|---|
| `bootstrap_servers_config` | `localhost:9092` | Kafka broker address for Kafka Streams. Note: in local development this is overridden to `localhost:29092` (Redpanda) via `application-local.properties` |
| `application_id_config` | `async-process-queries` | Kafka Streams application ID. Used as the consumer group prefix and state store directory name |

**Notes:**

- The `asyncprocess.yaml` is minimal; most runtime configuration for this service comes from `application.yaml` (global defaults) and `application-local.properties` (local overrides for Kafka broker, state dir, etc.).
- `application_id_config` (not the standard `spring.kafka.streams.application-id`) is a custom property read directly by the service's Kafka Streams configuration bean.

---

## 4. Key Properties Table

The following table consolidates all significant properties across the configuration files.

| Property | Default Value | Description | Used By |
|---|---|---|---|
| `classic.url` | `http://localhost:8080/etendo` | Etendo Classic base URL | All services (via `application.yaml`) |
| `das.url` | `http://localhost:8092` | DAS REST base URL | All services (via `application.yaml`) |
| `das.grpc.ip` | `localhost` | DAS gRPC host | Services using gRPC |
| `das.grpc.port` | `9090` | DAS gRPC port | Services using gRPC |
| `public-key` | EC PEM | ES256 public key for JWT verification | All services |
| `management.endpoints.web.exposure.include` | `'*'` | Actuator endpoint exposure | All services |
| `server.port` (das) | `8092` | DAS HTTP port | DAS |
| `spring.datasource.url` | `jdbc:postgresql://localhost:5432/etendo` | DAS database JDBC URL | DAS |
| `spring.datasource.username` | `tad` | DAS database username | DAS |
| `spring.datasource.password` | `tad` | DAS database password | DAS |
| `post-upsert` | `true` | Enable POST-as-upsert in DAS REST API | DAS |
| `scan.basePackage` | (empty) | Entity scan package for DAS | DAS |
| `server.port` (worker) | `0` / `8102` | Worker HTTP port | Worker |
| `openbravo.url` | (empty) | Etendo Classic URL for worker calls | Worker |
| `classic.token` | (empty) | JWT token for Etendo Classic API | Worker |
| `dashboard.enabled` | `true` | Enable worker dashboard at `/dashboard` | Worker |
| `spring.kafka.bootstrap-servers` | `localhost:29092` | Kafka broker address | Worker, Async Process |
| `connector.instance` | (empty) | UUID of the connector's `ETRX_RX_SERVICES_ACCESS` record | Worker |
| `connector.user` | (empty) | User ID for reactivity operations | Worker |
| `async-api-url` | `http://localhost:8099` | Async Process service URL | Worker, OBConn Server |
| `openbravo.token` | (empty) | JWT token for Etendo Classic API (obconnector path) | Worker |
| `server.port` (obconnsrv) | `8101` | OBConnector Server HTTP port | OBConn Server |
| `token` (obconnsrv) | (empty) | JWT for internal auth | OBConn Server |
| `server.port` (auth) | `8094` | Auth service HTTP port | Auth |
| `token` (auth) | ES256 JWT | Token used by Auth to call Etendo Classic | Auth |
| `admin.token` | ES256 JWT | Elevated privilege token | Auth |
| `private-key` | EC PKCS8 PEM | ES256 private key for signing JWTs | Auth |
| `management.endpoint.restart.enabled` | `true` | Enable actuator restart endpoint | Auth |
| `spring.security.oauth2.client.registration.google.client-id` | `placeholder` | Google OAuth2 client ID | Auth |
| `spring.security.oauth2.client.registration.google.client-secret` | `placeholder` | Google OAuth2 client secret | Auth |
| `logging.level.root` (auth) | `INFO` | Auth root log level | Auth |
| `server.port` (edge) | `8096` | Edge gateway HTTP port | Edge |
| `etendorx.auth.url` | `http://localhost:8094` | Auth service URL for gateway routing | Edge |
| `etendorx.zapier.url` | `http://localhost:8091` | Zapier integration service URL | Edge |
| `logging.level.org.springframework.web` | `DEBUG` | Spring web log level in Edge | Edge |
| `bootstrap_servers_config` | `localhost:9092` | Kafka Streams broker (asyncprocess) | Async Process |
| `application_id_config` | `async-process-queries` | Kafka Streams application ID | Async Process |
| `auth.disabled` | (not set) | When `true`, bypasses JWT validation. Development only | Worker, OBConn Server, DAS |
| `dedup.ttl.seconds` | `300` | Message deduplication TTL in seconds | Worker (lib) |
| `dlt.max.queue.size` | `1000` | Max DLT replay queue size | Worker (lib) |

---

## 5. Local Development Overrides

When `spring.profiles.active=local` is active, each service loads its module-local `application-local.properties`. These files take precedence over Config Server-supplied YAML and allow running without a Config Server.

### 5.1 Worker: `application-local.properties`

**Path:** `modules/com.etendorx.integration.obconnector/com.etendorx.integration.obconn.worker/src/main/resources/application-local.properties`

```properties
server.port=8102

# Shared config (normally from application.yaml via Config Server)
classic.url=http://localhost:8080/etendo
das.url=http://localhost:8092
public-key=MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEwxPslzUDOjgiT9xDTXJEHNmDslthTM5hbF3zUjDDqaamHMys64vavULlP5PvVszAJUjUF4+AHV+ZPBfG46fVng==

# Worker-specific (normally from worker.yaml via Config Server)
openbravo.url=http://localhost:8080/etendo
classic.token=
token=<ES256 JWT>
connector.instance=AC5535A2F4284094AA72B875769D0B0E
connector.user=

# Kafka
spring.kafka.bootstrap-servers=localhost:29092

# Dashboard
dashboard.enabled=true

# Disable auth for local development
auth.disabled=true

# Expose actuator metrics
management.endpoints.web.exposure.include=health,metrics
```

**Properties explained:**

| Property | Value | Notes |
|---|---|---|
| `server.port` | `8102` | Fixes the worker port (overrides `0` in `worker.yaml`) |
| `classic.url` | `http://localhost:8080/etendo` | Replaces `application.yaml` global default |
| `das.url` | `http://localhost:8092` | DAS URL, same as global default |
| `public-key` | (base64 EC key) | Inline DER-encoded public key (not PEM-wrapped). Different encoding from the PEM form in `application.yaml` |
| `openbravo.url` | `http://localhost:8080/etendo` | Etendo Classic URL for worker REST calls |
| `classic.token` | (empty — must be set) | JWT token for Etendo Classic. Must be filled in for sync operations to work |
| `token` | (ES256 JWT) | Token for internal EtendoRX service calls |
| `connector.instance` | `AC5535A2F4284094AA72B875769D0B0E` | Local dev connector instance UUID |
| `connector.user` | (empty) | Optional; user ID for reactivity |
| `spring.kafka.bootstrap-servers` | `localhost:29092` | Redpanda broker (port `29092`, not the default Kafka `9092`) |
| `dashboard.enabled` | `true` | Enables the sync dashboard at `http://localhost:8102/dashboard` |
| `auth.disabled` | `true` | Bypasses JWT verification. **Never set in production** |
| `management.endpoints.web.exposure.include` | `health,metrics` | Exposes only health and metrics actuator endpoints (narrower than `'*'` in `application.yaml`) |

### 5.2 OBConnector Server: `application-local.properties`

**Path:** `modules/com.etendorx.integration.obconnector/com.etendorx.integration.obconn.server/src/main/resources/application-local.properties`

```properties
server.port=8101

# Disable auth for local development
auth.disabled=true

# Expose actuator metrics
management.endpoints.web.exposure.include=health,metrics
```

The server local profile is minimal because the server does not directly interact with Kafka or the database. It only disables auth and fixes the port.

### 5.3 DAS: `application-local.properties`

**Path:** `modules_core/com.etendorx.das/src/main/resources/application-local.properties`

```properties
server.port=8092
spring.datasource.url=jdbc:postgresql://localhost:5432/etendo
spring.datasource.username=tad
spring.datasource.password=tad
spring.jackson.serialization.FAIL_ON_EMPTY_BEANS=false
scan.basePackage=com.etendorx.integration.to_openbravo.mapping
post-upsert=true

# Disable auth for local development
auth.disabled=true

# Expose actuator metrics
management.endpoints.web.exposure.include=health,metrics
```

Note that `scan.basePackage` is explicitly set here to `com.etendorx.integration.to_openbravo.mapping`, which is the package generated by `./gradlew generate.entities`. This must match the actual generated code package.

### 5.4 Async Process: `application-local.properties`

**Path:** `modules_core/com.etendorx.asyncprocess/src/main/resources/application-local.properties`

```properties
server.port=8099
bootstrap_server=localhost:29092
spring.cloud.stream.kafka.binder.brokers=localhost:29092
kafka.streams.host.info=localhost:8099
kafka.streams.state.dir=/tmp/kafka-streams/async-process-local
spring.config.import=optional:configserver:http://localhost:8888
management.endpoints.web.exposure.include=health,metrics
auth.disabled=true
```

Note that unlike other services, the async process local profile still declares `spring.config.import` pointing at the Config Server — the `optional:` prefix means it will proceed if the server is unavailable. This is a belt-and-suspenders approach in case the Config Server is running.

---

## 6. Environment Variables

### `JAVA_HOME`

Must point to a Java 17 JDK. The Makefile auto-detects it using `/usr/libexec/java_home -v 17` (macOS) or falls back to `~/Library/Java/JavaVirtualMachines/corretto-17.0.18/Contents/Home`.

```bash
# Set explicitly if auto-detection fails:
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
# or for Amazon Corretto:
export JAVA_HOME=~/Library/Java/JavaVirtualMachines/corretto-17.0.18/Contents/Home
```

**Why Java 17 is required:** Lombok 1.18.22 (used across all modules) is incompatible with JDK 21+ due to changes in the compiler API for annotation processing. All Gradle builds and `bootRun` tasks must use Java 17.

You can verify the configured Java version with:

```bash
make check-java
```

### `SPRING_PROFILES_ACTIVE`

When set to `local`, activates the `application-local.properties` profile in each service module, bypassing the Config Server.

```bash
export SPRING_PROFILES_ACTIVE=local
```

The Makefile passes this via the Gradle JVM argument flag:

```makefile
BOOTRUN_ARGS := -Dspring.profiles.active=local
```

This is appended to all `bootRun` invocations (e.g., `$(GRADLE) :com.etendorx.das:bootRun $(BOOTRUN_ARGS)`).

### Summary of Environment Variables

| Variable | Required | Default | Description |
|---|---|---|---|
| `JAVA_HOME` | Yes | Auto-detected (macOS `/usr/libexec/java_home -v 17`) | Path to Java 17 JDK home directory |
| `SPRING_PROFILES_ACTIVE` | No | (unset) | Set to `local` to use local profile overrides instead of Config Server |

---

## Appendix: Service Port Reference

| Service | Port | Config file | Application name |
|---|---|---|---|
| Spring Cloud Config Server | `8888` | (embedded) | `configserver` |
| Auth | `8094` | `rxconfig/auth.yaml` | `auth` |
| DAS | `8092` | `rxconfig/das.yaml` | `das` |
| Edge Gateway | `8096` | `rxconfig/edge.yaml` | `edge` |
| Async Process | `8099` | `rxconfig/asyncprocess.yaml` | `asyncprocess` |
| OBConnector Server | `8101` | `rxconfig/obconnsrv.yaml` | `obconnsrv` |
| OBConnector Worker | `8102` | `rxconfig/worker.yaml` | `worker` |
| Mock Receiver (loadtest) | `8090` | (in-module profile) | — |
| Redpanda Broker | `29092` | (docker-compose) | — |
| Redpanda Console | `9093` | (docker-compose) | — |
| Kafka Connect API | `8083` | (docker-compose) | — |
| Jaeger UI | `16686` | (docker-compose) | — |
