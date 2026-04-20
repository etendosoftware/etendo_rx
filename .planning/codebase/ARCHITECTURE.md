# Architecture

**Analysis Date:** 2026-02-05

## Pattern Overview

**Overall:** Microservices distributed architecture with cloud-native design (Spring Boot/Spring Cloud)

**Key Characteristics:**
- Spring Boot 3.1.4 with Java 17 as runtime
- Modular multi-project Gradle build system
- Spring Cloud Config Server for centralized configuration
- Spring Cloud Gateway (Edge) for API routing and JWT authentication
- Event-driven async processing with Kafka integration
- Domain-driven separation between core services and integration modules
- Generated entities and REST clients from configuration
- Plugin-based architecture with custom Gradle plugin (`com.etendorx.gradlepluginrx`)

## Layers

**Configuration & Discovery:**
- Purpose: Centralized configuration and service discovery
- Location: `modules_core/com.etendorx.configserver`
- Contains: Spring Cloud Config Server bootstrap application
- Depends on: Spring Cloud Config Server
- Used by: All core services for configuration management

**Authentication & Authorization:**
- Purpose: JWT token generation, validation, and security service
- Location: `modules_core/com.etendorx.auth`
- Contains: JWT generation/validation, OAuth2 client, user credential validation
- Depends on: Spring Security, JWT libraries, Feign client
- Used by: Edge gateway, all services requiring authentication

**API Gateway & Routing:**
- Purpose: Entry point for all external requests, JWT validation, request routing
- Location: `modules_core/com.etendorx.edge`
- Contains: Spring Cloud Gateway configuration, JWT authentication filter, route definitions
- Depends on: Spring Cloud Gateway, Spring WebFlux, JWT utilities
- Used by: All external clients, proxies requests to backend services

**Data Access & Synchronization:**
- Purpose: Entity management, ORM mapping, external ID tracking, data synchronization
- Location: `modules_core/com.etendorx.das`
- Contains: Hibernate integration, event handlers, entity repositories, connector field mapping
- Depends on: JPA/Hibernate, Spring Data, custom entity metadata
- Used by: Integration modules, async processors, REST endpoints

**Async Processing & Event Streaming:**
- Purpose: Asynchronous job processing, Kafka-based event streaming
- Location: `modules_core/com.etendorx.asyncprocess`
- Contains: Kafka consumer/producer, async process execution, priority queue management
- Depends on: Kafka Streams, Spring Cloud Stream, serialization libraries
- Used by: DAS service, integration modules

**Reactive Web Service:**
- Purpose: Non-blocking request handling infrastructure
- Location: `modules_core/com.etendorx.webflux`
- Contains: Spring WebFlux configuration, reactive endpoints
- Depends on: Spring WebFlux, Project Reactor
- Used by: Edge gateway and service implementations

**Generated Entities & Models:**
- Purpose: Auto-generated entity classes, REST client stubs, ORM mappings
- Location: `modules_gen/com.etendorx.entities`, `modules_gen/com.etendorx.clientrest`, `modules_gen/com.etendorx.grpc.common`
- Contains: Entity classes (from code generation), REST client interfaces, gRPC protocol buffers
- Depends on: JPA annotations, OpenAPI client generators
- Used by: DAS service, integration modules

**Shared Libraries & Utilities:**
- Purpose: Common utilities, authentication helpers, async utilities
- Location: `libs/com.etendorx.utils.auth`, `libs/com.etendorx.utils.common`, `libs/com.etendorx.lib.asyncprocess`, `libs/com.etendorx.lib.kafka`
- Contains: JWT key utilities, authentication context, Kafka configuration, common utilities
- Depends on: Spring libraries, JWT libraries, Kafka libraries
- Used by: All services and modules

**Integration Modules:**
- Purpose: Domain-specific data synchronization and business logic
- Location: `modules/com.etendorx.integration.*` (obconnector, to_openbravo, mobilesync, petclinic)
- Contains: Connector-specific workers, server implementations, common interfaces, configuration
- Depends on: DAS, generated entities, Spring Boot
- Used by: DAS service as event handlers

**Test Support Modules:**
- Purpose: Testing utilities and test containers
- Location: `modules_test/`
- Contains: Event handler test utilities, gRPC test containers, Spring test containers
- Depends on: TestContainers, JUnit, Spring Test
- Used by: All test suites

## Data Flow

**Authentication Flow:**

1. Client sends credentials (username/password) to Auth service
2. Auth service validates credentials against DAS/Classic system via Feign client
3. Auth service generates JWT token with user claims (scopes, service access)
4. JWT token returned to client
5. Client includes token in `X-TOKEN` header for subsequent requests

**Request Processing Flow:**

1. Client sends HTTP request with `X-TOKEN` header to Edge gateway
2. Edge's `JwtAuthenticationFilter` validates JWT signature using public key
3. Valid request routed to backend service (DAS, Async, etc.) via Spring Cloud Gateway
4. Service processes request and returns response
5. Response returned to client

**Data Synchronization Flow:**

1. Integration module (obconnector, etc.) receives sync request via REST endpoint
2. Module creates AsyncProcess job with entity data
3. Job enqueued to Kafka topic via async process producer
4. AsyncProcess service consumes job from Kafka
5. AsyncProcess executes job, calls DAS endpoints for entity creation/updates
6. DAS service creates/updates entities in database via Hibernate
7. Hibernate triggers event handlers on entity changes
8. Event handlers trigger integration module workers for post-sync operations
9. ExternalId service tracks external system IDs for synced entities
10. Completion status returned via AsyncProcess API

**Event-Driven Async Processing:**

1. DAS service generates change events for entity operations
2. Events published to Kafka topic via AsyncProcessDbProducer
3. AsyncProcess service consumes events and builds processing queue
4. Queue stored with priority and state in priority queue structure
5. AsyncProcess controller monitors job status and execution progress
6. Integration module workers execute custom processing logic per job

## Key Abstractions

**AsyncProcess:**
- Purpose: Represents a single asynchronous job with metadata and processing state
- Examples: `modules_core/com.etendorx.asyncprocess/src/main/java/com/etendorx/asyncprocess/`, serialization in `serdes/`
- Pattern: Data model with serialization/deserialization, controller API for monitoring

**EventHandler:**
- Purpose: Plugin interface for domain-specific event processing during data sync
- Examples: Integration module workers in `modules/com.etendorx.integration.obconnector/`
- Pattern: Event-driven, auto-wired Spring beans, called after DAS entity operations

**ExternalId:**
- Purpose: Bidirectional mapping between Etendo internal IDs and external system IDs
- Examples: `modules_core/com.etendorx.das/src/main/java/com/etendorx/das/externalid/`
- Pattern: Service layer managing tracking and lookup, post-sync service handling

**Connector Configuration:**
- Purpose: YAML-based configuration for synchronization behavior and field mappings
- Examples: `rxconfig/obconnector.yaml`, `rxconfig/worker.yaml`, `rxconfig/server.yaml`
- Pattern: Static configuration files loaded at startup, defines entity workers and behavior

**JwtService:**
- Purpose: JWT token generation and validation
- Examples: `modules_core/com.etendorx.auth/src/main/java/com/etendorx/auth/auth/jwt/`
- Pattern: Cryptographic operations using public/private keys, claims-based token structure

## Entry Points

**Auth Service:**
- Location: `modules_core/com.etendorx.auth/src/main/java/com/etendorx/auth/JwtauthApplication.java`
- Triggers: Application startup via Spring Boot
- Responsibilities: Exposes `/api/authenticate` endpoint, validates user credentials, generates JWT tokens

**Edge Gateway:**
- Location: `modules_core/com.etendorx.edge/src/main/java/com/etendorx/edge/EdgeApplication.java`
- Triggers: Application startup via Spring Boot
- Responsibilities: Routes all incoming requests, validates JWT tokens, forwards to backend services

**DAS Service:**
- Location: `modules_core/com.etendorx.das/src/main/java/com/etendorx/das/EtendorxDasApplication.java`
- Triggers: Application startup via Spring Boot
- Responsibilities: Manages entity CRUD operations, triggers event handlers, communicates with database

**AsyncProcess Service:**
- Location: `modules_core/com.etendorx.asyncprocess/src/main/java/com/etendorx/asyncprocess/AsyncProcessDbApp.java`
- Triggers: Application startup via Spring Boot, Kafka topic subscriptions
- Responsibilities: Processes async jobs, manages queue state, executes integration module workers

**ConfigServer:**
- Location: `modules_core/com.etendorx.configserver/src/main/java/com/etendorx/configserver/ConfigServerApplication.java`
- Triggers: Application startup via Spring Boot
- Responsibilities: Serves configuration files to all services, supports actuator refresh

## Error Handling

**Strategy:** Exception propagation with HTTP status mapping

**Patterns:**
- Service layer throws custom exceptions (e.g., `ExternalIdException`)
- Spring REST controllers map exceptions to HTTP response codes (401 UNAUTHORIZED, 400 BAD_REQUEST, 500 INTERNAL_SERVER_ERROR)
- Async process failures stored in execution history with error details
- Gateway filter returns 400/401 for authentication failures before routing

## Cross-Cutting Concerns

**Logging:** Spring Boot default logging (SLF4J), debug mode available with `--debug-jvm -PdebugPort=<port>`

**Validation:** Request validation at controller level (e.g., `AuthController` calls `validateJwtRequest()`)

**Authentication:** JWT token-based, validated by Edge gateway filter before request reaches backend services

**Configuration:** Centralized via ConfigServer, local YAML overrides in `rxconfig/`, environment variable substitution support

**Security:** Spring Security framework, JWT with RSA public key verification, OAuth2 client for external auth

---

*Architecture analysis: 2026-02-05*
