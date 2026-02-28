# Technical Documentation Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Create comprehensive English technical documentation for EtendoRX platform and OBConnector module.

**Architecture:** Two-level docs — platform docs in `etendo_rx/docs/` (6 files), connector docs in `modules/com.etendorx.integration.obconnector/docs/` (10 files). Each location has an INDEX.md linking all topics. Files are self-contained markdown with cross-references.

**Tech Stack:** Markdown, ASCII diagrams, curl examples, Java code snippets

---

## Wave 1: Platform Docs (6 files, parallelizable)

All files in `/Users/sebastianbarrozo/Documents/work/epic/obconnector/etendo_rx/docs/`

### Task 1: Platform INDEX.md

**Files:**
- Create: `docs/INDEX.md`

**Step 1: Write the index file**

Master table of contents for the entire project. Must include:
- Project overview (EtendoRX = reactive microservices platform for Etendo ERP)
- Links to all 5 platform doc files
- Links to OBConnector docs at `../modules/com.etendorx.integration.obconnector/docs/INDEX.md`
- Quick reference: services table (name, port, purpose)

**Step 2: Verify links are correct**

Check that all referenced file paths exist or will exist after plan completion.

---

### Task 2: Platform Architecture (`docs/architecture.md`)

**Files:**
- Create: `docs/architecture.md`
- Read: `settings.gradle`, `build.gradle`, all module dirs

**Step 1: Write architecture doc**

Must cover:
- **Platform overview** — EtendoRX as Spring Boot microservices platform
- **Service catalog** — table with: service name, module path, port, purpose, Spring Boot app name
  - Config Server (8888) — Spring Cloud Config, central configuration
  - Auth (8094) — JWT authentication/authorization
  - DAS (8092) — Data Access Service, REST API to Etendo DB
  - Edge (8096) — API Gateway
  - AsyncProcess (8099) — Async task processing, Kafka consumer
- **ASCII architecture diagram** — show services, Kafka, DB, external systems
- **Dependency graph** — libs/, modules_core/, modules/, modules_gen/ relationships
- **Module categories** — explain each dir (libs, modules_core, modules, modules_gen, modules_test)
- **Technology stack** — Spring Boot 3.1.4, Spring Cloud 2022.0.4, Java 17, Gradle 8.3, Kafka/Redpanda, PostgreSQL

---

### Task 3: Getting Started (`docs/getting-started.md`)

**Files:**
- Create: `docs/getting-started.md`
- Read: `Makefile`, `gradle.properties`

**Step 1: Write getting started guide**

Must cover:
- **Prerequisites** — Java 17 (Corretto recommended), Docker, Docker Compose, Git, PostgreSQL with Etendo installed
- **Clone** — git clone + submodule init
- **Configure** — `gradle.properties` setup (DB credentials, GitHub tokens), `make config` to generate YAML from templates
- **Infrastructure** — `make infra` (Redpanda) or `make infra-kafka` (full Kafka)
- **Build** — `make build` (compiles all modules)
- **Run** — `make up` (with Config Server) or `make up-local` (without, fastest)
- **Verify** — `make status`, access Dev Portal at :8199, check each service health endpoint
- **First sync** — brief overview of how to trigger a Send or Receive workflow
- **Troubleshooting** — common issues (JAVA_HOME, DB connection, Kafka not ready)

---

### Task 4: Makefile Reference (`docs/makefile-reference.md`)

**Files:**
- Create: `docs/makefile-reference.md`
- Read: `Makefile` (complete)

**Step 1: Write Makefile reference**

Must cover every target organized by category:

- **Preflight Checks** — `check-db`, `check-java` with what they verify
- **Infrastructure** — `infra`, `infra-kafka`, `infra-down`, `infra-logs`, `infra-ps` with Docker Compose details
- **Configuration** — `config` with template system explanation, variable injection from gradle.properties
- **Build** — `build`, `build-lib`, `build-server`, `build-worker`, `test`, `test-lib` with Gradle commands underneath
- **Individual Services** — `run-config`, `run-auth`, `run-das`, `run-edge`, `run-server`, `run-worker`, `run-async` with ports and startup order
- **Orchestrated Startup** — `up`, `up-local`, `up-kafka`, `down`, `status`, `logs` with PID management, background processes
- **Utilities** — `portal`, `loadtest`, `loadtest.send`, `loadtest.receive`, `mock`, `purge`, `help`

For each target: description, usage example, dependencies, key environment variables.

Include **dependency order diagram**:
```
make config → make infra → make up
                              ├── run-config (8888, if not up-local)
                              ├── run-auth (8094)
                              ├── run-das (8092)
                              ├── run-edge (8096)
                              ├── run-async (8099)
                              ├── run-server (8101)
                              └── run-worker (8102)
```

---

### Task 5: Configuration Reference (`docs/configuration.md`)

**Files:**
- Create: `docs/configuration.md`
- Read: `rxconfig/*.yaml`, `rxconfig/*.yaml.template`, `gradle.properties`

**Step 1: Write configuration reference**

Must cover:
- **Configuration hierarchy** — Spring Cloud Config Server → application YAML → application-local.properties
- **Template system** — `.yaml.template` files, `make config` variable injection, `__VARIABLE__` placeholders
- **File-by-file reference:**
  - `application.yaml` — global Spring properties
  - `das.yaml` — DAS database connection (injected from gradle.properties)
  - `worker.yaml` — Kafka bootstrap, dashboard toggle
  - `obconnector.yaml` — connector instance, tokens, async-api-url
  - `obconnsrv.yaml` — server config
  - `auth.yaml` — JWT keys, token config
  - `edge.yaml` — gateway routes
  - `asyncprocess.yaml` — async processor config
- **Key properties table** — property name, default, description, which service uses it
- **Local overrides** — `application-local.properties` in each module, `spring.profiles.active=local`
- **Environment variables** — JAVA_HOME, SPRING_PROFILES_ACTIVE, etc.

---

### Task 6: Infrastructure (`docs/infrastructure.md`)

**Files:**
- Create: `docs/infrastructure.md`
- Read: `modules/com.etendorx.integration.obconnector/infraestructure/docker-compose*.yml`

**Step 1: Write infrastructure doc**

Must cover:
- **Two infrastructure modes:**
  - Redpanda (default, lightweight) — `docker-compose.redpanda.yml`
  - Full Kafka — `docker-compose.yml`
- **Services table** per compose file — container, image, port, purpose
- **Redpanda stack:**
  - Redpanda broker (:29092) — Kafka-compatible, single node
  - Redpanda Console (:9093) — Topic browser, consumer groups
  - Kafka Connect (:8083) — Debezium CDC connectors
  - Jaeger (:16686) — Distributed tracing
- **Full Kafka stack:**
  - Zookeeper (:22181), Kafka (:29092), Kafka UI (:9093)
  - Debezium Postgres (:5465), Kafka Connect (:8083), Connect UI (:8002)
  - Jaeger (:16686)
- **Kafka topics** — `obconnector.send`, `obconnector.receive`, DLT topics, async process topics
- **Debezium CDC** — how Change Data Capture works with PostgreSQL WAL
- **Topic management** — `make purge` to reset topics
- **Ports reference table** — all ports used by infra + services

---

## Wave 2: Connector Docs — Core (5 files, parallelizable)

All files in `modules/com.etendorx.integration.obconnector/docs/`

### Task 7: Connector INDEX.md

**Files:**
- Create: `modules/com.etendorx.integration.obconnector/docs/INDEX.md`

**Step 1: Write connector index**

- OBConnector overview — bidirectional sync between Etendo ERP and external systems
- Links to all 9 connector doc files
- Link back to platform docs at `../../../docs/INDEX.md`
- Quick reference: sub-project table (name, purpose, key classes)

---

### Task 8: Connector Architecture (`docs/architecture.md`)

**Files:**
- Create: `modules/com.etendorx.integration.obconnector/docs/architecture.md`
- Read: All `build.gradle` files in sub-projects

**Step 1: Write connector architecture doc**

Must cover:
- **Module structure** — 5 sub-projects with purpose:
  - `common` — Interfaces and models (SyncWorkflow, SyncActivities, SynchronizationEntity)
  - `lib` — Core engine (workflow runners, Kafka integration, dashboard, resilience)
  - `server` — REST API entry point (SyncController, port 8101)
  - `worker` — Sync execution (converters, operations, filters, DAS/external HTTP)
  - `loadtest` — Performance testing (Send/Receive modes)
- **Dependency diagram:**
  ```
  common ← lib ← worker
              ← server (common only)
              ← loadtest
  ```
- **Class hierarchy** — key interfaces and their implementations
- **Pipeline pattern** — MAP → PRE_LOGIC → SYNC → POST_LOGIC → PROCESS_DATA → POST_ACTION
- **Two workflow directions:**
  - Send: Etendo DB change → Debezium CDC → Kafka → Worker → External System
  - Receive: External System → HTTP POST → Server → Kafka → Worker → DAS → Etendo DB
- **ASCII data flow diagram** for each direction

---

### Task 9: Workflows (`docs/workflows.md`)

**Files:**
- Create: `modules/com.etendorx.integration.obconnector/docs/workflows.md`
- Read: `SyncWorkflowBase.java`, `ReceiveWorkflowRunner.java`, `SendWorkflowRunner.java`, all step implementations

**Step 1: Write workflows doc**

Must cover for **each workflow** (Send and Receive):

**Receive Workflow (External → Etendo):**
- Entry: `POST /api/sync/{modelName}` → SyncController → SyncServices → AsyncApi (Kafka)
- Worker: KafkaListener on `obconnector.receive` → ReceiveWorkflowRunner
- Filters: ReceiveLogicFilter → ReceiveOrganizationFilter → ReceiveClientFilter
- Pipeline steps:
  - **MAP** (DasConverter): Lookup external→internal ID via `GET /connector/ETRX_instance_externalid/...`, determine POST/PUT, resolve FK fields via `subEntityMap()`
  - **PRE_LOGIC** (DasPreLogic): No-op (extensible)
  - **SYNC** (DasOperation): `POST/PUT {dasUrl}/{projection}/{entity}` to DAS, returns created/updated entity
  - **POST_LOGIC**: No-op (extensible)
  - **PROCESS_DATA** (DasProcessData): Store ID mapping via `POST /connector/ETRX_instance_externalid`
  - **POST_ACTION**: Execute registered WorkerActions
- Step messages: What `DashboardEventRecorder.setStepDetail()` reports at each step
- Error handling: catch(Exception) → dashboard event → compensate → DLT

**Send Workflow (Etendo → External):**
- Entry: Debezium CDC → Kafka topic → SendWorkflowRunner
- Filters: SendLogicFilter → SendOrganizationFilter → SendClientFilter
- Pipeline steps:
  - **MAP** (SendDasConverter): Extract internal ID, lookup external ID, fetch entity from DAS, resolve FK fields
  - **PRE_LOGIC** (SendDasPreLogic): No-op (extensible)
  - **SYNC** (SendDasOperation): `POST/PUT` to external system via ExternalRequestService
  - **POST_LOGIC**: No-op (extensible)
  - **PROCESS_DATA** (SendDasProcessData): Store ID mapping
  - **POST_ACTION**: Execute registered WorkerActions
- Step messages at each step

---

### Task 10: API Reference (`docs/api-reference.md`)

**Files:**
- Create: `modules/com.etendorx.integration.obconnector/docs/api-reference.md`
- Read: `SyncController.java`, `SyncServices.java`

**Step 1: Write API reference**

Must cover:
- **Base URL:** `http://localhost:8101/api/sync`
- **Authentication:** `X-TOKEN` header (JWT)

**Endpoints:**

1. `POST /api/sync/{modelName}` — Create entity
   - Path params: modelName (must match tableName in connector config, e.g., "BusinessPartner")
   - Headers: X-TOKEN, Content-Type: application/json
   - Body: `{ "data": { "id": "external-id", "name": "...", ... } }`
   - Response: `{ "workflowId": "uuid", "status": "ACCEPTED" }`
   - curl example

2. `PUT /api/sync/{modelName}/{entityId}` — Update entity
   - Path params: modelName, entityId (external ID)
   - Same headers/body structure
   - curl example

3. `GET /api/sync/status/{workflowId}` — Check workflow status
   - Response: `{ "status": "DONE|STARTED|ERROR|RETRY", "steps": [...] }`
   - curl example

- **Error responses** — 400, 401, 404, 500 with examples
- **Model name resolution** — how modelName maps to connector instance tableName in ExternalSystemConfiguration

---

### Task 11: Dashboard (`docs/dashboard.md`)

**Files:**
- Create: `modules/com.etendorx.integration.obconnector/docs/dashboard.md`
- Read: `DashboardController.java`, `DashboardEventRecorder.java`, `SyncEventStore.java`, all templates

**Step 1: Write dashboard doc**

Must cover:
- **Enable:** `dashboard.enabled=true` in worker application properties
- **Access:** `http://localhost:8102/dashboard`
- **Technology:** Thymeleaf + HTMX (polling every 2-5s)
- **Sections:**
  - **Metrics panel** — Total messages, processed, errors, filtered, in-progress counts
  - **Resources panel** — CPU/memory usage of the worker JVM
  - **Kafka Lag panel** — Consumer group lag per topic/partition, total lag
  - **Events feed** — Live workflow events, expandable rows showing step-by-step progress
  - **DLT panel** — Dead letter messages with replay button
- **REST endpoints:**
  - `GET /dashboard` — Main page
  - `GET /dashboard/metrics` — Metrics fragment
  - `GET /dashboard/resources` — Resources fragment
  - `GET /dashboard/lag` — Kafka lag fragment
  - `GET /dashboard/events` — Events fragment
  - `GET /dashboard/dlt` — DLT fragment
- **Step detail messages** — What each step reports:
  - MAP: entity, extId, verb, FK resolution
  - SYNC: HTTP method, URL, status code, internalId
  - PROCESS_DATA: ID map stored/skipped, IDs
- **Status badges:** STARTED, PROCESSING, DONE, ERROR, RETRY, FILTERED, DLT
- **DashboardEventRecorder API:**
  - `setStepDetail(String)` — ThreadLocal, set from step implementations
  - `getStepDetail()` / `clearStepDetail()` — Lifecycle
  - `recordStepComplete(runId, workflow, entity, step, syncEntity)` — Called by SyncWorkflowBase
  - `recordWorkflowError(runId, workflow, entity, errorMessage)` — Called on exception

---

## Wave 3: Connector Docs — Advanced (5 files, parallelizable)

### Task 12: Worker Internals (`docs/worker.md`)

**Files:**
- Create: `modules/com.etendorx.integration.obconnector/docs/worker.md`
- Read: Worker classes (filters, converters, operations, DasRequestService, ExternalRequestService)

**Step 1: Write worker doc**

Must cover:
- **Worker overview** — Spring Boot app on :8102, Kafka consumer, executes sync workflows
- **Filters** — Chain of filters before workflow execution:
  - `SendLogicFilter` / `ReceiveLogicFilter` — Check model name matches connector config
  - `SendOrganizationFilter` / `ReceiveOrganizationFilter` — Organization-based filtering
  - `SendClientFilter` / `ReceiveClientFilter` — Client-based filtering
- **Converters** — MAP step implementations:
  - `DasConverter` (Receive) — External→Etendo format, FK resolution
  - `SendDasConverter` (Send) — Etendo→External format, FK resolution
- **Operations** — SYNC step implementations:
  - `DasOperation` (Receive) — POST/PUT to DAS API
  - `SendDasOperation` (Send) — POST/PUT to external system
- **DasRequestService** — All HTTP calls to DAS:
  - `getEtendoEntity()` — Lookup external→internal ID mapping
  - `getExternalEntity()` — Lookup internal→external ID mapping
  - `getExternalEntityMap()` — Get full entity representation
  - `insertEntity()` — POST to DAS
  - `updateEntity()` — PUT to DAS
  - `addEtendoIdMap()` — Store ID mapping
  - `getProjectionName()` — Resolve projection from connector config
- **ExternalRequestService** — HTTP calls to external system
- **SubEntitySynchronizationUtils** — FK field resolution:
  - How `subEntityMap()` works
  - JSONPath expressions for FK fields
  - External ID → Internal ID lookup for each FK
  - Fallback behavior when mapping not found
- **WorkerConfigService** — Configuration caching, role/org extraction from JWT

---

### Task 13: Load Testing (`docs/loadtest.md`)

**Files:**
- Create: `modules/com.etendorx.integration.obconnector/docs/loadtest.md`
- Read: `LoadTestRunner.java`, `ReceiveLoadTestRunner.java`, `MessageGenerator.java`, `MockReceiverServer.java`

**Step 1: Write loadtest doc**

Must cover:
- **Two modes:**
  - `loadtest.send` — Simulates Debezium CDC messages to Kafka
  - `loadtest.receive` — Sends HTTP POST to Server endpoint
- **Send load test** (LoadTestRunner):
  - Publishes Debezium-format messages to Kafka topic
  - Configurable: threads, messages-per-thread, topic
  - MessageGenerator: creates realistic CDC payloads
- **Receive load test** (ReceiveLoadTestRunner):
  - HTTP POST to `http://localhost:8101/api/sync/BusinessPartner`
  - Generates BusinessPartner payloads with all required fields
  - Optional status polling after completion
- **Configuration properties:**
  - `loadtest.mode` — send/receive
  - `loadtest.enabled` — enable/disable (default true for send)
  - `loadtest.threads` — concurrent thread count
  - `loadtest.messages-per-thread` — messages per thread
  - `loadtest.delay-ms` — delay between messages
  - `loadtest.poll-status` — poll workflow status (receive only)
  - `loadtest.server.url` — server URL (receive only)
  - `loadtest.server.token` — auth token (receive only)
  - `loadtest.model` — entity model name (receive only)
- **Makefile targets:**
  - `make loadtest` — Run both sequentially
  - `make loadtest.send` — Send only
  - `make loadtest.receive` — Receive only
- **Mock receiver:**
  - `make mock` — Start MockReceiverServer on :8090
  - Logs all incoming requests
  - Useful for testing Send workflow without real external system

---

### Task 14: Resilience (`docs/resilience.md`)

**Files:**
- Create: `modules/com.etendorx.integration.obconnector/docs/resilience.md`
- Read: Resilience classes in lib

**Step 1: Write resilience doc**

Must cover:
- **Message deduplication** (MessageDeduplicationService):
  - In-memory ConcurrentHashMap with TTL (300s default)
  - Dedup key: message ID or composite key
  - Prevents re-processing of same message on Kafka rebalance
- **Dead Letter Topic (DLT):**
  - Failed messages after retry exhaustion go to DLT
  - `DltReplayService` — replay individual or all DLT messages
  - Dashboard DLT panel for manual replay
- **HTTP retry** (HttpRetryHelper):
  - 3 attempts, 1s initial delay, 2x exponential backoff
  - Wraps OkHttp and java.net.http calls
  - `withRetry(operationName, callable)` — static method
- **Saga compensation** (SagaManager):
  - Register compensation actions per runId
  - On failure: execute compensations in reverse order
  - Tracked in-memory per workflow execution
- **Idempotent step execution:**
  - `lastCompletedStep` on SynchronizationEntity
  - On retry, steps up to lastCompletedStep are skipped
  - Prevents re-executing successful steps
- **Health indicator** (SyncHealthIndicator):
  - DOWN after 10+ consecutive errors
  - DOWN after 5min without successful processing
  - Exposed via `/actuator/health`
- **Metrics** (SyncMetricsService):
  - Micrometer counters: messages received, processed, errors, filtered
  - Exposed via `/actuator/metrics`
- **Distributed tracing:**
  - SLF4J MDC: runId, workflow, entity propagated across threads
  - CorrelationHeaders for cross-service tracing

---

### Task 15: Extending the Connector (`docs/extending.md`)

**Files:**
- Create: `modules/com.etendorx.integration.obconnector/docs/extending.md`

**Step 1: Write extending guide**

Must cover:
- **Adding a new entity type:**
  1. Configure in Etendo AD: ETRX_Instance_Connector mappings
  2. Set tableName, integrationDirection, projectionName
  3. Worker auto-discovers via WorkerConfigService
- **Custom converters:**
  - Implement `SyncConverters` interface
  - Use `@Qualifier("send.converter")` or `@Qualifier("receive.converter")`
  - Use `@Order(N)` for priority
  - `appliesTo(entityName)` — return true for your entity
  - `convert(data, entityName)` — transform data
- **Custom operations:**
  - Implement `SyncOperation` interface
  - `appliesTo(entityName)` — entity routing
  - `operation(data)` — execute sync
- **Custom pre/post logic:**
  - Implement `SyncPreLogic` or `SyncProcessData`
  - Register with appropriate qualifier
- **Custom post-actions:**
  - Implement `WorkerAction` interface
  - `@Qualifier("send.post.action")` or `@Qualifier("receive.post.action")`
  - Example: `BookOrder` — books an order after sync
- **Custom filters:**
  - Implement `KafkaChangeFilter`
  - `isValid(kafkaChange)` — return false to skip message

---

### Task 16: Connector Configuration (`docs/configuration.md`)

**Files:**
- Create: `modules/com.etendorx.integration.obconnector/docs/configuration.md`
- Read: All `application*.properties` in server and worker

**Step 1: Write connector configuration doc**

Must cover:
- **Server configuration** (`application-local.properties` in server):
  - `server.port` (default 8101)
  - `auth.disabled` — bypass auth for development
  - `async-api-url` — AsyncProcess service URL
- **Worker configuration** (`application-local.properties` in worker):
  - `server.port` (default 8102)
  - `das.url` — DAS service URL
  - `classic.url` / `openbravo.url` — Etendo Classic URL
  - `token` — JWT token for DAS authentication
  - `public-key` — EC public key for JWT validation
  - `classic.token` — Token for Classic API
  - `connector.instance` — UUID of the ETRX_Instance_Connector
  - `connector.user` — User ID for operations
  - `spring.kafka.bootstrap-servers` — Kafka broker
  - `dashboard.enabled` — Enable dashboard UI
  - `auth.disabled` — Bypass incoming HTTP auth
- **Connector instance setup:**
  - How to find your connector instance UUID
  - ETRX_Instance_Connector table structure
  - Table mappings: tableName, integrationDirection, projectionName
- **Token setup:**
  - Generate JWT from auth.yaml private key
  - Derive public key: `openssl ec -pubout`
  - Set in worker properties
- **External system configuration:**
  - ExternalSystemConfiguration loaded from DAS
  - Cached in WorkerConfigService
  - Mappings define which entities sync in which direction

---

## Wave 4: Commit

### Task 17: Final commit and push

**Step 1: Review all files exist**

Verify all 16 markdown files are created.

**Step 2: Commit platform docs**

```bash
cd /Users/sebastianbarrozo/Documents/work/epic/obconnector/etendo_rx
git add docs/INDEX.md docs/architecture.md docs/getting-started.md docs/makefile-reference.md docs/configuration.md docs/infrastructure.md
git commit -m "Add EtendoRX platform technical documentation"
```

**Step 3: Commit connector docs**

```bash
cd modules/com.etendorx.integration.obconnector
git add docs/
git commit -m "Add OBConnector technical documentation"
git push origin feature/ETP-3459
```

**Step 4: Push parent**

```bash
cd /Users/sebastianbarrozo/Documents/work/epic/obconnector/etendo_rx
git add .
git commit -m "Update submodule reference for documentation"
git push origin feature/ETP-3459
```
