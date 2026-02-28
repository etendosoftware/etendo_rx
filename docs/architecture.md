# EtendoRX Platform Architecture

## Table of Contents

1. [Platform Overview](#1-platform-overview)
2. [Service Catalog](#2-service-catalog)
3. [Architecture Diagram](#3-architecture-diagram)
4. [Module Categories](#4-module-categories)
5. [Dependency Graph](#5-dependency-graph)
6. [Technology Stack](#6-technology-stack)
7. [Key Architectural Patterns](#7-key-architectural-patterns)

---

## 1. Platform Overview

EtendoRX is a reactive microservices platform built on top of the Etendo ERP system. It exposes Etendo's business data via REST APIs, enables event-driven processing through Apache Kafka, and provides bidirectional synchronization with external systems (e.g., Openbravo POS, third-party ERPs).

### Core capabilities

- **REST API layer**: The Data Access Service (DAS) exposes JPA-backed REST endpoints over the Etendo PostgreSQL database, with fine-grained read/write mapping per entity.
- **Event-driven sync**: Change Data Capture (CDC) via Debezium streams PostgreSQL WAL events into Kafka. Workers consume these events and push data to external systems.
- **Bidirectional integration**: The OBConnector module handles both directions — receiving from external systems into Etendo (via DAS), and sending from Etendo to external systems (via Debezium CDC).
- **Centralized configuration**: All services fetch their configuration from a Spring Cloud Config Server at startup. No service carries its own YAML in production.
- **Unified authentication**: A dedicated Auth service issues EC-key-signed JWTs. All inter-service and client calls are validated by the Edge gateway or by individual services using the shared `utils.auth` library.

### Build system

| Attribute | Value |
|---|---|
| Build tool | Gradle 8.3 |
| Language | Java 17 |
| Spring Boot | 3.1.4 |
| Spring Cloud | 2022.0.4 |
| Platform version | 2.3.4 (defined in `settings.gradle`) |
| Custom Gradle plugin | `com.etendorx.gradlepluginrx:2.1.0` |

The root project is a Gradle multi-project build named `etendorx`. It dynamically discovers subprojects by scanning five top-level directories (`libs`, `modules_core`, `modules_gen`, `modules_test`, `modules`) for `build.gradle` files.

---

## 2. Service Catalog

### Deployable services (Spring Boot applications)

| Service Name | Spring App Name | Port | Module Path | Purpose |
|---|---|---|---|---|
| Config Server | `configserver` | 8888 | `modules_core/com.etendorx.configserver` | Spring Cloud Config Server. Central YAML distribution point for all services. All other services fetch their configuration from this service at startup. |
| Auth | `auth` | 8094 | `modules_core/com.etendorx.auth` | JWT authentication service. Issues and validates EC-signed tokens. Integrates with Spring Security OAuth2 and Spring Cloud OpenFeign. Exposes Swagger UI. |
| DAS | `das` | 8092 | `modules_core/com.etendorx.das` | Data Access Service. JPA-based REST API over the Etendo PostgreSQL database. Supports optional gRPC transport. Dynamically loads entity definitions from the code-generated `entities` module. |
| Edge | `edge` | 8096 | `modules_core/com.etendorx.edge` | Spring Cloud Gateway (reactive). Single ingress point for all external API traffic. Routes requests to DAS, Auth, and other services. Validates JWTs using `utils.auth`. |
| AsyncProcess | `asyncprocess` | 8099 | `modules_core/com.etendorx.asyncprocess` | Asynchronous task processing service. Kafka consumer for workflow status events. Exposes REST endpoints for polling async operation results. Uses Spring Cloud Stream + Reactor Kafka. |
| OBConnector Server | `obconnsrv` | 8101 | `modules/com.etendorx.integration.obconnector/com.etendorx.integration.obconn.server` | REST API entrypoint for triggering sync operations. Exposes `PUT /api/sync/{modelName}/{entityId}`, `POST /api/sync/{modelName}`, and `GET /api/sync/status/{workflowId}`. Authenticates via `X-TOKEN` header. |
| OBConnector Worker | `worker` | 8102 | `modules/com.etendorx.integration.obconnector/com.etendorx.integration.obconn.worker` | Kafka consumer that executes the full sync workflow pipeline (MAP → PRE_LOGIC → SYNC → POST_LOGIC → PROCESS_DATA → POST_ACTION). Handles both receive (external → Etendo) and send (Etendo → external) directions. |

### Non-deployable services (library modules, not standalone applications)

| Module | Path | Role |
|---|---|---|
| WebFlux | `modules_core/com.etendorx.webflux` | Reactive WebFlux base module with Thymeleaf and JPA. Not a standalone service. |
| Auth Client | `modules/com.etendorx.auth.client` | Client-side auth utilities. No standalone application. |
| To Openbravo (mapping) | `modules/com.etendorx.integration.to_openbravo/com.etendorx.integration.to_openbravo.mapping` | DTO mapper components for Etendo-Openbravo field transformations. Loaded by DAS at runtime (`includeInDasDependencies = true`). |
| To Openbravo (worker) | `modules/com.etendorx.integration.to_openbravo/com.etendorx.integration.to_openbravo.worker` | SPI adapters for HTTP method binding and response parsing specific to Openbravo POS. Loaded by OBConnector Worker at runtime. |

---

## 3. Architecture Diagram

### Full system view

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                              CONFIGURATION PLANE                                │
│                                                                                 │
│   ┌─────────────────────────────────────────────────────────────────────────┐  │
│   │              Config Server (port 8888)  [configserver]                  │  │
│   │              Spring Cloud Config — serves YAML from /rxconfig           │  │
│   └─────────┬──────────┬──────────┬──────────┬──────────┬───────────────────┘  │
│             │          │          │          │          │   (all services        │
│             │          │          │          │          │    fetch config        │
│             v          v          v          v          v    at startup)         │
└─────────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────────┐
│                               RECEIVE DIRECTION                                 │
│                          (External System → Etendo)                             │
│                                                                                 │
│  ┌──────────────┐     ┌─────────────────┐     ┌──────────────────────────────┐ │
│  │   External   │     │  OBConnector    │     │      Kafka / Redpanda        │ │
│  │   System     │────>│  Server         │────>│                              │ │
│  │              │ PUT │  (port 8101)    │     │  topic: sync.receive.*       │ │
│  │  e.g.        │ POST│  [obconnsrv]    │     │                              │ │
│  │  Openbravo   │     │                 │     └──────────────┬───────────────┘ │
│  │  POS         │     │  REST API       │                    │                 │
│  └──────────────┘     │  /api/sync/*    │                    │ consume          │
│                        └─────────────────┘                    v                 │
│                                                 ┌──────────────────────────────┐│
│                                                 │     OBConnector Worker       ││
│                                                 │     (port 8102) [worker]     ││
│                                                 │                              ││
│                                                 │  MAP → PRE_LOGIC → SYNC →   ││
│                                                 │  POST_LOGIC → PROCESS_DATA  ││
│                                                 │  → POST_ACTION              ││
│                                                 └──────────────┬───────────────┘│
│                                                                │ HTTP (OkHttp3) │
│                                                                v                │
│                                                 ┌──────────────────────────────┐│
│                                                 │         DAS (port 8092)      ││
│                                                 │         [das]                ││
│                                                 │                              ││
│                                                 │  JPA REST API               ││
│                                                 │  /api/<entity>              ││
│                                                 └──────────────┬───────────────┘│
│                                                                │ JDBC           │
│                                                                v                │
│                                                 ┌──────────────────────────────┐│
│                                                 │      PostgreSQL (Etendo DB)  ││
│                                                 │      port 5432               ││
│                                                 └──────────────────────────────┘│
└─────────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────────┐
│                                SEND DIRECTION                                   │
│                          (Etendo → External System)                             │
│                                                                                 │
│  ┌──────────────────────────────┐                                               │
│  │      PostgreSQL (Etendo DB)  │                                               │
│  │      WAL (Write-Ahead Log)   │                                               │
│  └──────────────┬───────────────┘                                               │
│                 │ CDC                                                            │
│                 v                                                                │
│  ┌──────────────────────────────┐     ┌──────────────────────────────────────┐  │
│  │   Debezium Kafka Connect     │────>│        Kafka / Redpanda              │  │
│  │   (port 8083)                │     │                                      │  │
│  │   Captures INSERT/UPDATE/    │     │  topic: dbz.<table>                  │  │
│  │   DELETE events              │     └──────────────┬───────────────────────┘  │
│  └──────────────────────────────┘                    │ DbzListener consumes      │
│                                                       v                          │
│                                       ┌──────────────────────────────────────┐  │
│                                       │      OBConnector Worker              │  │
│                                       │      (port 8102) [worker]            │  │
│                                       │                                      │  │
│                                       │  SendWorkflowImpl:                   │  │
│                                       │  MAP → PRE_LOGIC → SYNC →           │  │
│                                       │  POST_LOGIC → PROCESS_DATA          │  │
│                                       │  → POST_ACTION                      │  │
│                                       └──────────────┬───────────────────────┘  │
│                                                      │ HTTP (OkHttp3)           │
│                                                      v                           │
│                                       ┌──────────────────────────────────────┐  │
│                                       │         External System              │  │
│                                       │         e.g. Openbravo POS           │  │
│                                       └──────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────────┐
│                          AUTHENTICATION & ROUTING PLANE                         │
│                                                                                 │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │                      Edge Gateway (port 8096) [edge]                    │   │
│  │                      Spring Cloud Gateway — reactive                    │   │
│  │                      JWT validation via utils.auth                      │   │
│  └──────────────┬──────────────────────┬────────────────────────────────────┘  │
│                 │                      │                                        │
│                 v                      v                                        │
│  ┌──────────────────────┐   ┌──────────────────────────────────────────────┐   │
│  │   Auth (port 8094)   │   │         DAS / AsyncProcess / other           │   │
│  │   [auth]             │   │         internal services                    │   │
│  │   EC-signed JWTs     │   └──────────────────────────────────────────────┘   │
│  │   OAuth2 client      │                                                      │
│  │   Feign + OkHttp     │                                                      │
│  └──────────────────────┘                                                      │
└─────────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────────┐
│                         ASYNC WORKFLOW STATUS PLANE                             │
│                                                                                 │
│  ┌──────────────────────────────────────────────────────────────────────────┐  │
│  │                   AsyncProcess (port 8099) [asyncprocess]                │  │
│  │                   Spring Cloud Stream + Reactor Kafka                    │  │
│  │                   Consumes workflow status events from Kafka             │  │
│  │                   REST API for polling async operation results           │  │
│  └──────────────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

---

## 4. Module Categories

The Gradle multi-project build organizes all modules into five top-level directories. Each directory has a distinct responsibility scope.

### 4.1 libs/ — Shared Libraries (10 modules)

These are plain Java library JARs (no `bootJar`). They are consumed by both `modules_core` and `modules` services. They do not start standalone.

| Module | Artifact ID | Purpose |
|---|---|---|
| `com.etendorx.clientrest_core` | `clientrest_core` | Base classes for generated Feign REST clients. Provides Jackson and HATEOAS integration for REST client code. Used by `auth` and `modules_gen/clientrest`. |
| `com.etendorx.das_core` | `das_core` | Core DAS abstractions: base JPA repository wrappers, JSONPath-based field access, Swagger model annotations, and Bean Validation support. Used by DAS, entities, and mapping modules. |
| `com.etendorx.generate_entities` | `generate-entities` | Standalone Spring Boot CLI application (not a service). Reads the Etendo database schema and generates Java source code for JPA entities, JPA repositories, REST projections, and client stubs into `modules_gen/`. Depends on `generate_entities.core` and `generate_entities.extradomaintype`. |
| `com.etendorx.generate_entities.core` | `core` | Core schema-reading infrastructure for entity generation: Hibernate metadata introspection, JDBC-based DB schema access, Jettison JSON, and JSONPath. Pulled in by `generate_entities`. |
| `com.etendorx.generate_entities.extradomaintype` | `extra-domain-type` | Extension point for registering custom Hibernate domain types during entity generation. Depends on `generate_entities.core`. |
| `com.etendorx.lib.kafka` | _(no artifact, group: `com.etendorx.lib.kafka`)_ | Thin Kafka wrapper: message envelope POJOs, Kafka Streams configuration helpers. Used by `asyncprocess`, `lib.asyncprocess`, and OBConnector lib. |
| `com.etendorx.lib.asyncprocess` | _(no artifact, group: `com.etendorx.lib`)_ | Shared async processing abstractions and Kafka integration helpers. Depends on `lib.kafka`. Used by `asyncprocess` service. |
| `com.etendorx.utils.auth` | `utils.auth` | JWT utility library. Parsing and validation using `jjwt` 0.12.x and `nimbus-jose-jwt`. EC key support. Auth0 `java-jwt` for SWS compatibility. Used by almost every service. |
| `com.etendorx.utils.common` | `utils.common` | Minimal shared utilities: SLF4J logging helpers, JSONPath support. Zero Spring dependencies. |
| `com.etendorx.lib.asyncprocess` | _(group: `com.etendorx.lib`)_ | Async task processing base with Kafka Streams integration. Depends on `lib.kafka`. |

### 4.2 modules_core/ — Core Platform Services (6 modules, 5 deployable)

These are the foundational Spring Boot services that every EtendoRX deployment requires.

| Module | App Name | Deployable | Key Dependencies | Notes |
|---|---|---|---|---|
| `com.etendorx.configserver` | `configserver` | Yes | `spring-cloud-config-server` | Minimal service: serves YAML files. No custom code besides `@EnableConfigServer`. |
| `com.etendorx.auth` | `auth` | Yes | `spring-security`, `jjwt`, `openfeign`, `utils.auth`, `clientrest`, `clientrest_core` | Issues EC-signed JWTs. Feign + OkHttp for downstream calls. SpringDoc OpenAPI UI. |
| `com.etendorx.das` | `das` | Yes | `spring-data-jpa`, `das_core`, `entities` (codegen), `utils.auth`, PostgreSQL, Oracle JDBC, gRPC (optional) | Dynamically includes any module with `includeInDasDependencies = true`. JSqlParser for SQL query manipulation. |
| `com.etendorx.edge` | `edge` | Yes | `spring-cloud-starter-gateway`, `utils.auth` | Pure routing. WebFlux-based (reactive). No blocking I/O. |
| `com.etendorx.asyncprocess` | `asyncprocess` | Yes | `spring-cloud-stream-kafka`, `reactor-kafka`, `lib.kafka`, `lib.asyncprocess`, `utils.auth` | Handles async workflow tracking via Kafka topics. |
| `com.etendorx.webflux` | — | No | `spring-boot-starter-webflux`, `thymeleaf`, `spring-data-jpa`, PostgreSQL | Reactive base module. Not standalone. Experimental. |

### 4.3 modules/ — Custom Integration Modules (5 deployable modules across 3 integration packages)

These are business-logic modules specific to Etendo integrations. They depend on `libs/` and optionally on `modules_core/`.

#### com.etendorx.integration.obconnector (OBConnector — 5 submodules)

| Submodule | Artifact | Role |
|---|---|---|
| `com.etendorx.integration.obconn.common` | — (library) | Workflow contract interfaces only: `SyncWorkflow`, `SyncActivities`, `SyncOperation`, `SyncConverters`, `SyncPreLogic`, `SyncProcessData`. No implementations. Jackson annotations for model POJOs. |
| `com.etendorx.integration.obconn.lib` | — (library) | Core sync engine implementation. `SyncWorkflowBase` pipeline runner, Kafka integration (`DbzListener`, `KafkaChangeSend`, `KafkaProducerService`), configuration loading (`ExternalSystemConfiguration`), and all resilience patterns (dedup, DLT replay, HTTP retry, Saga, metrics, tracing). Depends on `obconn.common`, `lib.kafka`, `utils.auth`, `utils.common`. Uses OkHttp3, Micrometer, Thymeleaf, Commons JEXL3. |
| `com.etendorx.integration.obconn.server` | `obconn-srv` | Spring Boot REST server. `ConnectorApplication`. Exposes `/api/sync/*`. Auth via `X-TOKEN` header. Depends only on `obconn.common`. OkHttp3 and GSON for HTTP calls. |
| `com.etendorx.integration.obconn.worker` | `obconn-wrk` | Spring Boot Kafka consumer. `SyncWorkerMain`. Executes full sync pipeline. Depends on `obconn.common`, `obconn.lib`, `lib.kafka`, `utils.auth`. Runtime-loads `to_openbravo.worker` via `runtimeOnly`. GSON, JSONPath, OkHttp logging interceptor. |
| `com.etendorx.integration.obconn.loadtest` | — | Load testing module. Not deployed in production. |

#### com.etendorx.integration.to_openbravo (Openbravo POS adapter — 2 submodules)

| Submodule | Artifact | Role |
|---|---|---|
| `com.etendorx.integration.to_openbravo.mapping` | `mapping` | ~30 Spring `@Component` DTO mapper beans implementing `DTOReadMapping<T>` / `DTOWriteMapping<T, D>`. Loaded by DAS at runtime (`includeInDasDependencies = true`). Depends on `entities`, `das_core`, `utils.auth`. |
| `com.etendorx.integration.to_openbravo.worker` | `worker` | SPI adapters implementing `ExternalRequestMethodAdapter` and `ExternalRequestProcessResponseAdapter`. Openbravo-specific HTTP method binding (always POST) and response parsing (extracts entity ID from Openbravo JSON). Loaded by OBConnector Worker at runtime. |

#### com.etendorx.auth.client (Auth Client — 1 module)

| Module | Role |
|---|---|
| `com.etendorx.auth.client` | Client-side auth helper. No deployable application. Code-generated client stubs in `src-gen/`. |

### 4.4 modules_gen/ — Code-Generated Modules (4 modules)

These modules contain Java source code generated by `libs/com.etendorx.generate_entities` from the live Etendo PostgreSQL schema. They are regenerated when the schema changes. Do not edit manually.

| Module | Group | Purpose |
|---|---|---|
| `com.etendorx.entities` | `com.etendorx.entities` | Generated JPA entity classes, Spring Data JPA repositories, REST projections, and field mappings. Source directories: `src/main/entities`, `src/main/jparepo`, `src/main/projections`, `src/main/mappings`. Consumed by DAS as a `codegen` classpath dependency. |
| `com.etendorx.entitiesModel` | `com.etendorx.entitiesModel` | Generated HATEOAS model classes (DTOs) for REST responses. Depends on `clientrest_core`. |
| `com.etendorx.clientrest` | `com.etendorx.entitiesModel` | Generated Feign REST client interfaces for each entity. Depends on `clientrest_core`. Used by Auth and other services that need to call DAS programmatically. |
| `com.etendorx.grpc.common` | — | Generated gRPC Protobuf stubs. Built with `protobuf-gradle-plugin`. Used by DAS when `grpc.enabled=true`. |

### 4.5 modules_test/ — Test Modules (2 modules)

Standalone test applications for integration testing specific infrastructure components. Not deployed in production.

| Module | Purpose |
|---|---|
| `com.etendorx.test.eventhandler` | Integration tests for Kafka event handler flows. |
| `com.etendorx.test.grpc` | Integration tests for the gRPC transport layer in DAS. |

---

## 5. Dependency Graph

The dependency flow is strictly layered: generated modules and libs feed upward into core services and integration modules. No circular dependencies exist between layers.

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                            modules_gen/                                     │
│                                                                             │
│  ┌──────────────┐  ┌─────────────────┐  ┌──────────────┐  ┌─────────────┐ │
│  │   entities   │  │  entitiesModel  │  │  clientrest  │  │ grpc.common │ │
│  └──────┬───────┘  └────────┬────────┘  └──────┬───────┘  └──────┬──────┘ │
│         │                   │                   │                 │        │
└─────────┼───────────────────┼───────────────────┼─────────────────┼────────┘
          │                   │                   │                 │
          │  ┌────────────────┼───────────────────┼─────────────────┘
          │  │                │                   │
┌─────────┼──┼────────────────┼───────────────────┼───────────────────────────┐
│         │  │                │                   │    libs/                  │
│         │  │  ┌─────────────┘          ┌────────┘                          │
│         │  │  │                        │                                   │
│  ┌──────▼──▼──▼──┐  ┌───────────┐  ┌──▼─────────────┐  ┌───────────────┐  │
│  │   das_core    │  │  lib.kafka│  │ clientrest_core│  │  utils.auth   │  │
│  └──────┬────────┘  └─────┬─────┘  └────────────────┘  └───────┬───────┘  │
│         │                 │                                     │          │
│  ┌──────▼──────────────────▼─────────┐  ┌──────────────────────┘          │
│  │      generate_entities.*          │  │  utils.common                   │
│  │      (code gen tool only)         │  └─────────────────────────────────┘│
│  └───────────────────────────────────┘                                     │
│                                                                             │
│  ┌────────────────────────────────┐                                        │
│  │   lib.asyncprocess             │                                        │
│  │   (depends on: lib.kafka)      │                                        │
│  └────────────────────────────────┘                                        │
└─────────────────────────────────────────────────────────────────────────────┘
          │          │          │          │          │
          v          v          v          v          v
┌─────────────────────────────────────────────────────────────────────────────┐
│                          modules_core/                                      │
│                                                                             │
│  ┌─────────────┐  ┌──────────────┐  ┌─────────────────────────────────┐   │
│  │ configserver│  │    auth      │  │              das                │   │
│  │             │  │              │  │                                 │   │
│  │ (no lib     │  │ utils.auth   │  │ das_core + entities (codegen)  │   │
│  │  deps)      │  │ clientrest   │  │ utils.auth + grpc (optional)   │   │
│  │             │  │ clientrest_  │  │ to_openbravo.mapping (runtime) │   │
│  │             │  │ core         │  │                                 │   │
│  └─────────────┘  └──────────────┘  └─────────────────────────────────┘   │
│                                                                             │
│  ┌─────────────────────────────┐  ┌─────────────────────────────────────┐  │
│  │          edge               │  │           asyncprocess              │  │
│  │                             │  │                                     │  │
│  │ utils.auth                  │  │ lib.kafka + lib.asyncprocess        │  │
│  │ spring-cloud-gateway        │  │ utils.auth                          │  │
│  └─────────────────────────────┘  └─────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────┘
          │                    │
          v                    v
┌─────────────────────────────────────────────────────────────────────────────┐
│                             modules/                                        │
│                                                                             │
│  ┌──────────────────────────────────────────────────────────────────────┐  │
│  │                    com.etendorx.integration.obconnector              │  │
│  │                                                                      │  │
│  │  obconn.common (interfaces only)                                     │  │
│  │       │                                                              │  │
│  │       ├── obconn.lib (engine: depends on common, lib.kafka,          │  │
│  │       │              utils.auth, utils.common, Micrometer, JEXL3)   │  │
│  │       │                                                              │  │
│  │       ├── obconn.server (Spring Boot app: depends on common only)    │  │
│  │       │                                                              │  │
│  │       └── obconn.worker (Spring Boot app: depends on common, lib,    │  │
│  │                          lib.kafka, utils.auth;                      │  │
│  │                          runtimeOnly: to_openbravo.worker)           │  │
│  └──────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
│  ┌──────────────────────────────────────────────────────────────────────┐  │
│  │                  com.etendorx.integration.to_openbravo               │  │
│  │                                                                      │  │
│  │  mapping (depends on: entities, das_core, utils.auth)               │  │
│  │  worker  (depends on: obconn.common; SPI for obconn.worker)          │  │
│  └──────────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Runtime loading via `includeInDasDependencies`

DAS has a dynamic dependency mechanism: after project evaluation, it scans all `build.gradle` files across all module directories for the property `includeInDasDependencies = true`. Modules that set this property are automatically added as `implementation` dependencies of DAS. Currently this applies to:

- `com.etendorx.integration.to_openbravo.mapping`
- `com.etendorx.integration.to_openbravo.worker`

This allows custom integration mappers to be loaded by DAS without modifying the DAS `build.gradle`.

---

## 6. Technology Stack

### Core runtime

| Component | Version | Usage |
|---|---|---|
| Java | 17 (LTS) | Platform-wide language version. Lombok 1.18.22 is incompatible with JDK 24+. |
| Spring Boot | 3.1.4 | Application framework for all deployable services. |
| Spring Cloud | 2022.0.4 | Config Server, Gateway, OpenFeign, Stream (Kafka binder). |
| Gradle | 8.3 | Build tool. Multi-project with dynamic subproject discovery. |
| Spring Data JPA | (Spring Boot BOM) | ORM layer in DAS and entities module. |
| Spring Security / OAuth2 | (Spring Boot BOM) | Security in Auth service. |
| Spring Cloud Gateway | (Spring Cloud BOM) | Reactive API gateway in Edge. |
| Spring Cloud Stream | (Spring Cloud BOM) | Kafka consumer/producer abstraction in AsyncProcess and OBConnector lib. |

### Messaging and CDC

| Component | Version | Usage |
|---|---|---|
| Apache Kafka | 3.6.0 (Streams) | Message broker for all async workflows. |
| Redpanda | Compatible | Kafka-compatible broker used in local dev (see `infraestructure/docker-compose.yml`). |
| Reactor Kafka | (Spring Cloud BOM) | Reactive Kafka consumer in AsyncProcess. |
| Debezium Kafka Connect | (Docker Compose) | CDC connector capturing PostgreSQL WAL events. Port 8083. |
| Kafka UI | (Docker Compose) | Management UI for local dev. Port 9093 (UI), 8002 (Kafka Connect UI). |

### Database

| Component | Version | Usage |
|---|---|---|
| PostgreSQL | (runtime) | Primary Etendo ERP database. Port 5432 (prod), 5465 (local Docker). |
| PostgreSQL JDBC | 42.3.8 / 42.6.0 | JDBC driver in DAS and das_core. |
| Oracle JDBC (ojdbc8) | 21.6.0.0.1 | Optional Oracle DB driver available in DAS. |
| H2 | 1.4.200 | In-memory DB for DAS unit tests only. |

### HTTP and serialization

| Component | Version | Usage |
|---|---|---|
| OkHttp3 | 4.10.0 | HTTP client for all inter-service calls from OBConnector server and worker. Connection pool: 20 idle connections, 5-minute keepalive. 30s connect/read/write timeouts. |
| OkHttp Logging Interceptor | (Spring Cloud BOM) | HTTP request/response logging in OBConnector Worker. |
| Jackson | 2.13.x / 2.14.x / 2.17.x | JSON serialization across all modules. Singleton `ObjectMapper` pattern enforced. |
| GSON | 2.8.9 | JSON parsing in OBConnector server and worker (alongside Jackson). |
| Jettison | 1.5.4 | JSON/XML bridge used in Auth and generate_entities.core. |

### Authentication and JWT

| Component | Version | Usage |
|---|---|---|
| JJWT (io.jsonwebtoken) | 0.9.1 (legacy) + 0.11.2 / 0.12.2 | JWT creation and parsing. Multiple versions due to legacy usage in Auth service. |
| Nimbus JOSE JWT | 9.47 | EC key handling in `utils.auth`. |
| Auth0 java-jwt | 3.1.0 | SWS compatibility in `utils.auth`. |
| Spring Security OAuth2 Jose | (Spring Boot BOM) | OAuth2 JWT support in Auth service. |

### Observability and resilience

| Component | Version | Usage |
|---|---|---|
| Micrometer | 1.11.5 | Metrics collection in OBConnector lib (`SyncMetricsService`). Counters for messages received/processed/errors/deduplicated/DLT; timers for workflow and HTTP duration. |
| Spring Boot Actuator | (Spring Boot BOM) | Health endpoints (`/actuator/health`) in all services. `SyncHealthIndicator` in OBConnector lib reports DOWN after 10+ consecutive errors or 5 minutes without a successful sync. |
| SLF4J MDC | (transitive) | Distributed tracing via MDC fields: `runId`, `workflow`, `entity`, `entityId`. Set in `DbzListener` and workflow runners, cleared in `finally` blocks. |
| Jaeger | (Docker Compose) | Distributed tracing UI for local dev. Port 16686. |
| JaCoCo | 0.8.10 | Code coverage reports. Aggregated at root project via `jacocoRootReport` task. |

### Code generation and templating

| Component | Version | Usage |
|---|---|---|
| FreeMarker | 2.3.31 | Template engine for entity code generation in `generate_entities`. |
| Thymeleaf | (Spring Boot BOM) | Template engine used in OBConnector lib for workflow configuration rendering. |
| Apache Commons JEXL3 | 3.3 | Expression language for runtime field mapping evaluation in OBConnector lib (`ExternalSystemConfiguration`). |
| Hibernate Core | 5.4.2 | Schema introspection in `generate_entities`. Not used for runtime ORM (that is Spring Data JPA). |
| Google Protobuf | 3.19.4 | gRPC message serialization. Used in DAS gRPC transport and `grpc.common` module. |
| protobuf-gradle-plugin | 0.8.18 | Generates Java classes from `.proto` files for `grpc.common`. |

### Spring ecosystem extras

| Component | Version | Usage |
|---|---|---|
| SpringDoc OpenAPI (webmvc) | 2.2.0 | Swagger UI generation in Auth and DAS. |
| SpringDoc OpenAPI (UI, non-starter) | 1.7.0 | Swagger UI in AsyncProcess (older variant). |
| Spring HATEOAS | 1.4.0 / 2.1.2 | Hypermedia links in REST responses (clientrest_core, Auth). |
| Spring Cloud OpenFeign | 4.0.4 | Declarative HTTP clients in Auth and clientrest_core. |
| Feign OkHttp / Feign Jackson | 12.5 | OkHttp transport and Jackson encoder/decoder for Feign clients. |
| JSqlParser | 5.1 | SQL AST parsing used in DAS for dynamic query manipulation. |
| JSONPath (Jayway) | 2.8.0 | JSONPath expression evaluation in das_core, entities, OBConnector worker. |
| json-smart | 2.5.x | JSONPath dependency. Used alongside Jayway JSONPath. |
| Apache Commons Lang3 | 3.12.0 / 3.13.0 | String utilities across multiple modules. |
| Lombok | 1.18.22 / 1.18.30 | Boilerplate reduction (`@Slf4j`, `@Data`, `@Builder`, etc.). Java 17 required. |

---

## 7. Key Architectural Patterns

### 7.1 Sync workflow pipeline

The OBConnector uses a fixed six-step sequential pipeline defined in `SyncWorkflowBase`:

```
MAP → PRE_LOGIC → SYNC → POST_LOGIC → PROCESS_DATA → POST_ACTION
```

Each step is dispatched via `SyncActivities`. Two concrete workflow implementations exist, distinguished by Spring `@Qualifier`:

- `"receive.workflow"` / `"receive.activity"` — Handles external system → Etendo direction.
- `"send.workflow"` / `"send.activity"` — Handles Etendo → external system direction (triggered by Debezium CDC).

Entity-specific logic is registered via `SyncOperation.appliesTo(entityName)`. The framework routes by entity name, making it straightforward to add new entity handlers without modifying the pipeline.

### 7.2 Resilience patterns in OBConnector lib

| Pattern | Class | Behavior |
|---|---|---|
| Message deduplication | `MessageDeduplicationService` | In-memory `ConcurrentHashMap`. Composite key: `entity\|id\|verb\|SHA-256(data)`. Configurable TTL via `dedup.ttl.seconds` (default 300s). |
| Dead Letter Topic replay | `DltReplayService` | Failed messages stored in `ConcurrentLinkedQueue`. `replayAll()` re-publishes to original topics. Max queue via `dlt.max.queue.size` (default 1000). |
| HTTP retry with backoff | `HttpRetryHelper` | 3 retries, 1s initial delay, 2.0x multiplier. Retries on `IOException`. Wraps into `SyncException` after exhaustion. |
| Saga compensation | `SagaManager` | Tracks compensation actions per `runId`. On failure, executes compensations in reverse registration order. |
| Idempotent workflow | `SyncWorkflowBase` | Tracks `lastCompletedStep` on `SynchronizationEntity`. Skips already-completed steps on retry. |
| Kafka offset management | `StreamConfiguration` | `AckMode.RECORD` with auto-commit disabled. At-least-once delivery guarantee. |

### 7.3 Config server bootstrap

All services declare `spring.cloud.config.uri` pointing to the Config Server (default `http://localhost:8888`). Config is fetched before the application context starts. Configuration files are stored at `/rxconfig` on the Config Server host (e.g., `worker.yaml`, `obconnector.yaml`, `das.yaml`).

### 7.4 Dynamic DAS dependency loading

DAS scans all `build.gradle` files in the multi-project for the `includeInDasDependencies = true` property at configuration time. Matching modules are added to DAS's `implementation` classpath automatically. This enables shipping new entity mappers as separate Gradle subprojects without touching the DAS build file.

### 7.5 Entity code generation pipeline

```
Etendo PostgreSQL schema
        │
        │ (run generate_entities CLI tool)
        v
 FreeMarker templates
        │
        v
 modules_gen/
   ├── entities/        (JPA entities, repositories, projections, mappings)
   ├── entitiesModel/   (HATEOAS DTOs)
   └── clientrest/      (Feign client interfaces)
        │
        │ (consumed by)
        v
 modules_core/das/      (loads entities as codegen classpath dependency)
 modules_core/auth/     (uses clientrest for DAS calls)
```

Generated source is committed to version control. Regeneration is triggered manually when the Etendo DB schema changes by running `./gradlew :com.etendorx.generate_entities:bootRun`.
