# Makefile Reference — EtendoRX OBConnector

## 1. Overview

The Makefile at the root of `etendo_rx/` orchestrates the full development lifecycle of the OBConnector module: preflight validation, infrastructure provisioning, configuration generation, compilation, and orchestrated startup/shutdown of all microservices.

**Shell:** GNU Make with `SHELL := /bin/bash`. All recipes run in bash, enabling features like `&&`, process substitution, ANSI color codes (`\033[…m`), and `printf`.

**Default goal:** `help` — running `make` with no arguments prints the target list.

**Key design decisions:**

- Services are started as background Gradle `bootRun` processes. PIDs are stored in `.run/*.pid` and logs in `.run/*.log`, enabling `make status`, `make logs`, and `make down` to manage them without an external process manager.
- The `wait_for` function polls `/actuator/health` via `curl` up to `MAX_WAIT` seconds before proceeding to the next service group, ensuring ordered startup.
- `up-local` is the fastest path: it skips Config Server, Auth, and Edge entirely and also skips already-running services (idempotent restarts).
- DB credentials are read from `gradle.properties` at Makefile parse time (shell expansion), so no manual secret management is required in shell sessions.

---

## 2. Variables Reference

| Variable | Default / Source | Description |
|---|---|---|
| `ROOT` | `$(shell pwd)` | Absolute path to the repo root. Used as the base for all other paths. |
| `JAVA_HOME` | `/usr/libexec/java_home -v 17` (macOS), fallback to `~/Library/Java/JavaVirtualMachines/corretto-17.0.18/Contents/Home` | Path to a Java 17 JDK. Overridable via environment: `JAVA_HOME=/path make up`. |
| `GRADLE` | `JAVA_HOME=$(JAVA_HOME) ./gradlew` | The Gradle wrapper invocation, with `JAVA_HOME` injected so all Gradle subprocesses use the correct JDK. |
| `INFRA` | `$(ROOT)/modules/com.etendorx.integration.obconnector/infraestructure` | Directory containing the docker-compose files for infrastructure services (Redpanda/Kafka, Kafka Connect, Jaeger, etc.). |
| `RXCONFIG` | `$(ROOT)/rxconfig` | Directory where service YAML configuration files live. Templates (`*.yaml.template`) are copied here and then patched with DB credentials by `make config`. |
| `PROPS` | `$(ROOT)/gradle.properties` | Source of truth for DB connection parameters. Read at parse time via `grep`/`cut`/`sed`. |
| `DB_URL` | Parsed from `bbdd.url` in `gradle.properties` | JDBC URL, with escaped colons (`\:`) unescaped to `:`. Example: `jdbc:postgresql://localhost:5432`. |
| `DB_SID` | Parsed from `bbdd.sid` in `gradle.properties` | Database name/SID. Example: `etendo`. |
| `DB_USER` | Parsed from `bbdd.user` in `gradle.properties` | PostgreSQL username. |
| `DB_PASS` | Parsed from `bbdd.password` in `gradle.properties` | PostgreSQL password. |
| `CTX_NAME` | Parsed from `context.name` in `gradle.properties`; defaults to `etendo` if absent | Docker Compose project name prefix. The actual project name is `$(CTX_NAME)-obconn`, which namespaces all containers. |
| `COMPOSE` | `docker-compose -p $(CTX_NAME)-obconn` | Docker Compose invocation with project name set, ensuring containers are grouped under one project. |
| `BOOTRUN_ARGS` | `-Dspring.profiles.active=local` | JVM system property passed to every `bootRun` invocation. Activates the `local` Spring profile, which reads YAML files from `rxconfig/`. |
| `PIDS_DIR` | `$(ROOT)/.run` | Directory where `.pid` and `.log` files are stored for background services. Created on demand by `make up` / `make up-local`. |
| `MAX_WAIT` | `120` (seconds) | Maximum time `wait_for` polls a service health endpoint before aborting with a timeout error. |
| `REDPANDA_CTR` | Detected at parse time via `docker ps` matching `redpanda-1$` | Name of the running Redpanda container. Used by `make purge` to exec `rpk` inside the container. Falls back to `"redpanda"` if not found. |
| `PURGE_TOPICS` | Hardcoded list (see Utilities section) | All OBConnector Kafka topic names that `make purge` deletes and allows to auto-recreate. |
| `CYAN`, `GREEN`, `YELLOW`, `DIM`, `RESET` | ANSI escape sequences | Terminal color codes used in `echo -e` calls for readable output. Not configurable. |

---

## 3. Target Categories

### 3.1 Preflight Checks

These targets validate the environment before attempting builds or service startups. They are automatically invoked as dependencies by `make up`, `make up-local`, and `make up-kafka`.

---

#### `check-db`

**Description:** Tests that the PostgreSQL database defined in `gradle.properties` is reachable and accepting connections.

**Usage:**
```bash
make check-db
```

**What it does under the hood:**

1. Reads `DB_URL`, `DB_SID`, `DB_USER`, `DB_PASS` from `gradle.properties` (already parsed at Makefile load time).
2. Extracts host and port from the JDBC URL by stripping the `jdbc:postgresql://` prefix with `sed`, then splitting on `:` with `cut`.
3. Runs `psql -h <host> -p <port> -U <user> -d <sid> -c "SELECT 1"` with `PGPASSWORD` set in the environment (avoids interactive password prompt).
4. Prints `OK` in green on success, `FAIL` in yellow on failure and exits with code 1.

**Dependencies:** None.

**Failure hint:** If this fails, verify that:
- `gradle.properties` has correct `bbdd.*` entries.
- Etendo Classic's PostgreSQL is running and accessible from localhost.
- The user has login privileges on the target database.

---

#### `check-java`

**Description:** Verifies that Java 17 is available at the resolved `JAVA_HOME`.

**Usage:**
```bash
make check-java
```

**What it does under the hood:**

Runs `$(JAVA_HOME)/bin/java -version 2>&1 | grep -q "17\."`. If the pattern matches, prints `OK` with the resolved `JAVA_HOME`. Otherwise prints `FAIL` and exits with code 1.

**Dependencies:** None.

**Failure hint:** If `JAVA_HOME` auto-detection fails (non-macOS system or missing `java_home` utility), set it explicitly:
```bash
JAVA_HOME=/path/to/jdk17 make up-local
```

---

### 3.2 Infrastructure

Infrastructure targets manage the Docker Compose stack that provides the message broker (Redpanda or Kafka), Kafka Connect (Debezium), and Jaeger tracing. All Docker operations use the project name `$(CTX_NAME)-obconn` to avoid conflicts with other Compose stacks.

---

#### `infra`

**Description:** Starts the lightweight Redpanda-based infrastructure stack. Redpanda is the default and recommended option for local development due to its lower resource consumption compared to a full Kafka deployment.

**Usage:**
```bash
make infra
```

**What it does under the hood:**

```bash
cd $(INFRA) && docker-compose -p $(CTX_NAME)-obconn -f docker-compose.redpanda.yml up -d
```

Starts containers defined in `docker-compose.redpanda.yml` in detached mode. After the command returns, it prints the endpoint summary:

| Service | Address |
|---|---|
| Redpanda Broker | `localhost:29092` |
| Redpanda Console UI | `http://localhost:9093` |
| Kafka Connect API | `http://localhost:8083` |
| Jaeger UI | `http://localhost:16686` |

**Dependencies:** None (but implicitly requires Docker daemon to be running).

---

#### `infra-kafka`

**Description:** Starts the heavier Kafka-based infrastructure stack. Use this when you need full Kafka semantics or are debugging Kafka-specific behavior. Includes an additional PostgreSQL instance used by Debezium.

**Usage:**
```bash
make infra-kafka
```

**What it does under the hood:**

```bash
cd $(INFRA) && docker-compose -p $(CTX_NAME)-obconn up -d
```

Uses the default `docker-compose.yml` (Kafka, not Redpanda). After startup prints:

| Service | Address |
|---|---|
| Kafka Broker | `localhost:29092` |
| Kafka UI | `http://localhost:9093` |
| Kafka Connect API | `http://localhost:8083` |
| Kafka Connect UI | `http://localhost:8002` |
| Jaeger UI | `http://localhost:16686` |
| PostgreSQL (Debezium) | `localhost:5465` |

**Dependencies:** None.

---

#### `infra-down`

**Description:** Stops and removes all infrastructure containers for both Compose files.

**Usage:**
```bash
make infra-down
```

**What it does under the hood:**

Runs `docker-compose down` for both `docker-compose.yml` and `docker-compose.redpanda.yml`, suppressing errors from whichever is not running. Both commands are attempted regardless of failures (`; true` at the end).

**Dependencies:** None. Also called as a dependency by `make down`.

---

#### `infra-logs`

**Description:** Tails the Docker Compose logs for whichever infrastructure stack is running.

**Usage:**
```bash
make infra-logs
```

**What it does under the hood:**

```bash
cd $(INFRA) && docker-compose -p $(CTX_NAME)-obconn logs -f --tail=50
```

Follows log output from all containers in the Compose project, starting from the last 50 lines. Uses the default `docker-compose.yml`; if using Redpanda, this may show no containers. Use `make infra-ps` first to confirm which stack is active.

**Dependencies:** None.

---

#### `infra-ps`

**Description:** Shows the running state of all infrastructure containers across both Compose files.

**Usage:**
```bash
make infra-ps
```

**What it does under the hood:**

Runs `docker-compose ps` for both `docker-compose.yml` and `docker-compose.redpanda.yml` sequentially, ignoring errors (`; true`). Useful for quickly verifying which containers are up without switching to the Docker CLI.

**Dependencies:** None.

---

### 3.3 Configuration

#### `config`

**Description:** Generates YAML configuration files for all EtendoRX services from their `.yaml.template` counterparts in `rxconfig/`, then injects database credentials into `das.yaml` directly from `gradle.properties`.

**Usage:**
```bash
make config
```

**What it does under the hood:**

1. Iterates over all `$(RXCONFIG)/*.yaml.template` files.
2. For each template, copies it to the same path without the `.template` suffix **only if the target does not already exist** (skips existing files to preserve local edits).
3. After the copy loop, patches `das.yaml` in-place using `sed -i.bak` with three substitutions:
   - Replaces the `url:` line containing a JDBC URL with `url: $(DB_URL)/$(DB_SID)`.
   - Replaces the `username:` line with `username: $(DB_USER)`.
   - Replaces the `password:` line with `password: $(DB_PASS)`.
4. Removes the `.bak` backup file created by `sed -i`.

**Important:** `config` is idempotent for file creation (skips existing files) but always re-patches `das.yaml` with the current `gradle.properties` values. If you have manually customized `das.yaml` beyond the DB credentials, re-running `make config` will overwrite those specific lines.

**Dependencies:** None. Called automatically by `make up`, `make up-local`, and `make up-kafka`.

---

### 3.4 Build

Build targets invoke Gradle to compile and package the OBConnector modules. They use the `build` Gradle task (which includes `compileJava`, resources processing, and JAR assembly, but respects `-x test` if needed). The modules are:

| Gradle Project ID | Role |
|---|---|
| `com.etendorx.integration.obconn.common` | Shared domain model and utilities |
| `com.etendorx.integration.obconn.lib` | Core business logic, tested independently |
| `com.etendorx.integration.obconn.server` | HTTP API service (port 8101) |
| `com.etendorx.integration.obconn.worker` | Kafka consumer/processor service (port 8102) |

---

#### `build`

**Description:** Builds all four OBConnector modules in a single Gradle invocation.

**Usage:**
```bash
make build
```

**What it does under the hood:**

```bash
JAVA_HOME=$(JAVA_HOME) ./gradlew \
  :com.etendorx.integration.obconn.common:build \
  :com.etendorx.integration.obconn.lib:build \
  :com.etendorx.integration.obconn.server:build \
  :com.etendorx.integration.obconn.worker:build
```

Gradle executes the tasks in dependency order. Since `server` and `worker` depend on `common` and `lib`, Gradle ensures correct compilation order.

**Dependencies:** None (Makefile-level). Gradle resolves inter-module dependencies internally.

---

#### `build-lib`

**Description:** Builds only the `lib` module.

**Usage:**
```bash
make build-lib
```

**What it does under the hood:**

```bash
JAVA_HOME=$(JAVA_HOME) ./gradlew :com.etendorx.integration.obconn.lib:build
```

**Dependencies:** None.

---

#### `build-server`

**Description:** Builds only the `server` module.

**Usage:**
```bash
make build-server
```

**What it does under the hood:**

```bash
JAVA_HOME=$(JAVA_HOME) ./gradlew :com.etendorx.integration.obconn.server:build
```

**Dependencies:** None (Makefile-level).

---

#### `build-worker`

**Description:** Builds only the `worker` module.

**Usage:**
```bash
make build-worker
```

**What it does under the hood:**

```bash
JAVA_HOME=$(JAVA_HOME) ./gradlew :com.etendorx.integration.obconn.worker:build
```

**Dependencies:** None (Makefile-level).

---

#### `test`

**Description:** Runs unit tests for `lib`, `server`, and `worker` modules.

**Usage:**
```bash
make test
```

**What it does under the hood:**

```bash
JAVA_HOME=$(JAVA_HOME) ./gradlew \
  :com.etendorx.integration.obconn.lib:test \
  :com.etendorx.integration.obconn.server:test \
  :com.etendorx.integration.obconn.worker:test
```

Test reports are generated by Gradle in each module's `build/reports/tests/` directory.

**Dependencies:** None.

---

#### `test-lib`

**Description:** Runs unit tests for the `lib` module only.

**Usage:**
```bash
make test-lib
```

**What it does under the hood:**

```bash
JAVA_HOME=$(JAVA_HOME) ./gradlew :com.etendorx.integration.obconn.lib:test
```

**Dependencies:** None.

---

### 3.5 Individual Services

These targets start a single service in the **foreground** of the current terminal. They are intended for development and debugging of individual services. For running the full stack, use `make up` or `make up-local` instead.

All individual service targets use `$(BOOTRUN_ARGS)` (`-Dspring.profiles.active=local`), which instructs Spring Boot to load configuration from `rxconfig/` via the `local` profile. The exception is `run-config`, which does not use `BOOTRUN_ARGS` because the Config Server reads its own bootstrap configuration directly.

---

#### `run-config`

**Description:** Starts the Spring Cloud Config Server on port **8888**. This service must be started before any other EtendoRX service when operating in full (non-local) mode, because other services fetch their configuration from it on startup.

**Usage:**
```bash
make run-config
```

**What it does under the hood:**

```bash
JAVA_HOME=$(JAVA_HOME) ./gradlew :com.etendorx.configserver:bootRun
```

Note: no `$(BOOTRUN_ARGS)` — the Config Server uses its own bootstrap configuration.

**Dependencies:** None.

---

#### `run-auth`

**Description:** Starts the Authentication Service on port **8094**. Handles JWT issuance and validation for the EtendoRX API gateway.

**Usage:**
```bash
make run-auth
```

**What it does under the hood:**

```bash
JAVA_HOME=$(JAVA_HOME) ./gradlew :com.etendorx.auth:bootRun -Dspring.profiles.active=local
```

**Dependencies:** None (Makefile-level). In practice, requires Config Server if not using `local` profile.

---

#### `run-das`

**Description:** Starts the Data Access Service (DAS) on port **8092**. DAS is the EtendoRX persistence layer that maps Etendo Classic entities to REST endpoints via generated JPA repositories.

**Usage:**
```bash
make run-das
```

**What it does under the hood:**

```bash
JAVA_HOME=$(JAVA_HOME) ./gradlew :com.etendorx.das:bootRun -Dspring.profiles.active=local
```

**Dependencies:** None (Makefile-level). Requires a running PostgreSQL with the Etendo Classic schema.

---

#### `run-edge`

**Description:** Starts the Edge Gateway on port **8096**. Acts as the API gateway, routing and authenticating requests to backend services.

**Usage:**
```bash
make run-edge
```

**What it does under the hood:**

```bash
JAVA_HOME=$(JAVA_HOME) ./gradlew :com.etendorx.edge:bootRun -Dspring.profiles.active=local
```

**Dependencies:** None (Makefile-level).

---

#### `run-server`

**Description:** Starts the OBConnector Server on port **8101**. Exposes the HTTP API at `/api/sync/` that receives integration payloads and publishes them to the `obconnector.receive` Kafka topic.

**Usage:**
```bash
make run-server
```

**What it does under the hood:**

```bash
JAVA_HOME=$(JAVA_HOME) ./gradlew :com.etendorx.integration.obconn.server:bootRun -Dspring.profiles.active=local
```

**Dependencies:** None (Makefile-level). Requires Kafka/Redpanda broker to be running.

---

#### `run-worker`

**Description:** Starts the OBConnector Worker on port **8102**. Consumes messages from `obconnector.receive` and `obconnector.send` Kafka topics, processes them (calls Etendo Classic or external system), and handles retry/DLT logic.

**Usage:**
```bash
make run-worker
```

**What it does under the hood:**

```bash
JAVA_HOME=$(JAVA_HOME) ./gradlew :com.etendorx.integration.obconn.worker:bootRun -Dspring.profiles.active=local
```

An optional dashboard is available at `http://localhost:8102/dashboard` when `dashboard.enabled=true` is set in the worker configuration.

**Dependencies:** None (Makefile-level). Requires Kafka/Redpanda and DAS to be running.

---

#### `run-async`

**Description:** Starts the Async Process service on port **8099**. Handles asynchronous background processing tasks within the EtendoRX platform.

**Usage:**
```bash
make run-async
```

**What it does under the hood:**

```bash
JAVA_HOME=$(JAVA_HOME) ./gradlew :com.etendorx.asyncprocess:bootRun -Dspring.profiles.active=local
```

**Dependencies:** None (Makefile-level).

---

### 3.6 Orchestrated Startup

These targets start the entire stack as background processes, managing PIDs and logs in `$(ROOT)/.run/`. They use the `wait_for` macro to enforce startup ordering.

**The `wait_for` macro** polls `http://localhost:<port>/actuator/health` every 2 seconds. If the service does not respond within `MAX_WAIT` (120) seconds, it prints `TIMEOUT`, shows the last 20 lines of the service's log file, and exits with code 1. This prevents dependent services from starting against an unhealthy dependency.

---

#### `up`

**Description:** Full orchestrated startup with Config Server. Runs `check-java`, `check-db`, `infra`, and `config` as prerequisites, then starts all services in the correct order with health-gate waits between groups.

**Usage:**
```bash
make up
```

**What it does under the hood (in order):**

1. Runs `check-java`, `check-db`, `infra`, `config` as Make prerequisites.
2. Creates `$(PIDS_DIR)` and clears any existing `.log` and `.pid` files.
3. Starts Config Server in background, writes PID to `configserver.pid`, logs to `configserver.log`.
4. Waits for Config Server on `:8888`.
5. Starts Auth, DAS, and Edge in background (in parallel — three `&` commands), writing individual PIDs and logs.
6. Waits for Auth on `:8094`, DAS on `:8092`, Edge on `:8096` sequentially.
7. Starts OBConnector Server and Worker in background.
8. Waits for OBConn Server on `:8101`, OBConn Worker on `:8102`.
9. Calls `_banner` (internal target) which prints the full endpoint summary.

**Dependencies:** `check-java`, `check-db`, `infra`, `config`.

---

#### `up-local`

**Description:** Fastest orchestrated startup. Skips Config Server, Auth, and Edge. Checks if each service is already running (by hitting its `/actuator/health`) and skips it if so, making this target idempotent. Also starts the Mock Receiver automatically.

**Usage:**
```bash
make up-local
```

**What it does under the hood (in order):**

1. Runs `check-java`, `check-db`, `infra`, `config` as Make prerequisites.
2. Creates `$(PIDS_DIR)`.
3. **DAS:** If already healthy on `:8092`, prints "already running". Otherwise:
   a. Runs `generate.entities -x test` to generate JPA entity sources from the Etendo Classic schema.
   b. Runs `:com.etendorx.das:build -x test` to compile DAS with the generated entities.
   c. Starts DAS in background.
4. Waits for DAS on `:8092`.
5. **OBConn Server:** If already healthy on `:8101`, skips. Otherwise starts in background.
6. **OBConn Worker:** If already healthy on `:8102`, skips. Otherwise starts in background.
7. **Async Process:** If already healthy on `:8099`, skips. Otherwise starts in background.
8. **Mock Receiver:** If already responding on `:8090`, skips. Otherwise starts `loadtest:bootRun --spring.profiles.active=mock` in background.
9. Waits for OBConn Server, OBConn Worker, and Async Process.
10. Waits 2 seconds then prints Mock Receiver status.
11. Calls `_banner` with local-mode labels (Config Server / Auth / Edge shown as "skipped").

The `generate.entities` step is only performed when DAS is not already running, preventing redundant entity regeneration on subsequent `make up-local` calls.

**Dependencies:** `check-java`, `check-db`, `infra`, `config`.

---

#### `up-kafka`

**Description:** Full orchestrated startup using Kafka instead of Redpanda. Identical flow to `make up` except it depends on `infra-kafka` instead of `infra`.

**Usage:**
```bash
make up-kafka
```

**What it does under the hood:**

Identical to `make up` with the following differences:
- Depends on `infra-kafka` (starts `docker-compose.yml` instead of `docker-compose.redpanda.yml`).
- Resets `.pid` and `.log` files before starting.
- Starts Config Server, waits, then Auth/DAS/Edge, waits, then OBConn Server/Worker, waits, then prints banner.

**Dependencies:** `check-java`, `check-db`, `infra-kafka`, `config`.

---

#### `down`

**Description:** Stops all background services started by `make up` / `make up-local` / `make up-kafka`, then stops the infrastructure containers.

**Usage:**
```bash
make down
```

**What it does under the hood:**

1. Iterates over all `$(PIDS_DIR)/*.pid` files.
2. For each PID file, reads the PID, checks if the process is alive with `kill -0`, and sends `SIGTERM` (`kill <pid>`) if so. Removes the `.pid` file.
3. Calls `make infra-down` to stop Docker Compose containers.
4. Prints "All stopped."

**Note:** This sends `SIGTERM` to the Gradle daemon wrapper process. The JVM hosting the Spring Boot application may take several seconds to shut down gracefully. If processes do not stop, use `kill -9 <pid>` manually or restart your terminal.

**Dependencies:** `infra-down` (called as a Make dependency internally).

---

#### `status`

**Description:** Shows the current state of all infrastructure containers and all background services tracked by the Makefile.

**Usage:**
```bash
make status
```

**What it does under the hood:**

1. Runs `docker-compose ps` (tries both Compose files, ignores errors).
2. Iterates over all `$(PIDS_DIR)/*.pid` files and checks each PID with `kill -0`. Prints `RUNNING` (green) or `STOPPED` (yellow) with the service name and PID.

**Dependencies:** None.

---

#### `logs`

**Description:** Tails all service log files in `$(PIDS_DIR)` simultaneously.

**Usage:**
```bash
make logs
```

**What it does under the hood:**

```bash
exec tail -f $(PIDS_DIR)/*.log
```

Uses `exec` to replace the Make subprocess with `tail`, so the process exits cleanly when interrupted with Ctrl+C. All log files from all services are interleaved in a single stream.

**Dependencies:** None. Requires services to have been started via `make up` or `make up-local`.

---

### 3.7 Utilities

#### `portal`

**Description:** Starts a Python HTTP server on port **8199** serving the `portal/` directory. The portal is a local developer UI for browsing services and documentation.

**Usage:**
```bash
make portal
```

**What it does under the hood:**

```bash
cd $(ROOT)/portal && exec python3 -m http.server 8199
```

Uses `exec` so the Python process replaces the Make subprocess. Access at `http://localhost:8199`.

**Dependencies:** None. Requires Python 3 in `PATH`.

---

#### `loadtest`

**Description:** Runs both the Send and Receive load tests sequentially.

**Usage:**
```bash
make loadtest
```

**What it does under the hood:**

Calls `loadtest.send` followed by `loadtest.receive` as Make dependencies (in that order).

**Dependencies:** `loadtest.send`, `loadtest.receive`.

---

#### `loadtest.send`

**Description:** Runs the Send load test, which simulates Debezium CDC events being published to the `obconnector.send` Kafka topic (the "Etendo Classic to external system" direction).

**Usage:**
```bash
make loadtest.send
```

**What it does under the hood:**

```bash
JAVA_HOME=$(JAVA_HOME) ./gradlew :com.etendorx.integration.obconn.loadtest:bootRun -Dspring.profiles.active=local
```

Starts the `loadtest` module in its default mode (send). The test produces synthetic change events that the Worker consumes.

**Dependencies:** None (Makefile-level). Requires Worker and Kafka/Redpanda to be running.

---

#### `loadtest.receive`

**Description:** Runs the Receive load test, which sends HTTP POST payloads to the OBConnector Server (`/api/sync/`) to simulate an external system pushing data into Etendo Classic.

**Usage:**
```bash
make loadtest.receive
```

**What it does under the hood:**

```bash
JAVA_HOME=$(JAVA_HOME) ./gradlew :com.etendorx.integration.obconn.loadtest:bootRun \
  --args='--loadtest.mode=receive --loadtest.enabled=false --loadtest.threads=1 \
          --loadtest.messages-per-thread=5 --loadtest.poll-status=true'
```

Key parameters:
- `loadtest.mode=receive` — activates the receive (HTTP ingest) test path.
- `loadtest.threads=1` — single-threaded execution.
- `loadtest.messages-per-thread=5` — sends 5 messages.
- `loadtest.poll-status=true` — polls the async status endpoint after each send to verify end-to-end processing.

**Dependencies:** None (Makefile-level). Requires OBConnector Server and Worker to be running.

---

#### `mock`

**Description:** Starts a mock HTTP receiver on port **8090** that simulates an external system accepting payloads from the OBConnector Worker (Send workflow). Used for testing the outbound integration path without a real external endpoint.

**Usage:**
```bash
make mock
```

**What it does under the hood:**

```bash
JAVA_HOME=$(JAVA_HOME) ./gradlew :com.etendorx.integration.obconn.loadtest:bootRun \
  --args='--spring.profiles.active=mock'
```

The `loadtest` module, when started with `spring.profiles.active=mock`, activates a minimal Spring Boot HTTP server that logs all incoming requests and returns `200 OK`. This mock is also started automatically by `make up-local`.

**Dependencies:** None.

---

#### `purge`

**Description:** Deletes all OBConnector-related Kafka topics from Redpanda, allowing them to auto-recreate when producers and consumers reconnect. Use this to reset the message queue state without restarting the broker.

**Usage:**
```bash
make purge
```

**What it does under the hood:**

1. Resolves the Redpanda container name at Makefile parse time via `docker ps | grep redpanda-1$`. Falls back to `"redpanda"`.
2. Iterates over the full list of topics defined in `PURGE_TOPICS`:
   - `obconnector.send`
   - `obconnector.send-dlt`
   - `obconnector.send-retry-10000`
   - `obconnector.send-retry-20000`
   - `obconnector.send-retry-40000`
   - `obconnector.send-retry-60000`
   - `obconnector.receive`
   - `obconnector.receive-dlt`
   - `obconnector.receive-retry-10000`
   - `obconnector.receive-retry-20000`
   - `obconnector.receive-retry-40000`
   - `obconnector.receive-retry-60000`
   - `default.public.c_bpartner` (Debezium CDC source topic)
3. For each topic, runs `docker exec <container> rpk topic describe <topic>` to check existence. If found, runs `rpk topic delete <topic>`. Prints `Deleted`, `Error`, or `Skip (not found)` per topic.

**Note:** Topics auto-recreate the next time a producer publishes or a consumer subscribes to them. The retry and DLT topics are created by Spring Kafka's retry configuration on first use.

**Dependencies:** None. Requires Redpanda container to be running.

---

#### `help`

**Description:** Prints a formatted list of all documented targets (those with `## comment` annotations) and a quick-start reference.

**Usage:**
```bash
make help
# or simply:
make
```

**What it does under the hood:**

```bash
grep -E '^[a-zA-Z_-]+:.*?## .*$' $(MAKEFILE_LIST) | \
  awk 'BEGIN {FS = ":.*?## "}; {printf "  %-18s %s\n", $1, $2}'
```

Extracts all targets annotated with `## <description>` using a regex, then formats them as a two-column table with `awk`. Internal targets prefixed with `_` (such as `_banner`) are excluded by design since they do not match `^[a-zA-Z_-]+:`.

**Dependencies:** None. This is the `.DEFAULT_GOAL`.

---

## 4. Startup Order Diagram

```
make config
    |
    v
make infra  (or infra-kafka)
    |
    v
make up  ─────────────────────────────────────────────────────┐
    |                                                          |
    ├── run-config  :8888   ← wait_for (120s)                 │ (make up / make up-kafka only)
    |                                                          │
    ├── run-auth    :8094 ─┐                                  |
    ├── run-das     :8092 ─┤ (parallel) ← wait_for each       │
    ├── run-edge    :8096 ─┘                                  |
    |                                                          |
    ├── run-server  :8101 ─┐                                  |
    └── run-worker  :8102 ─┘ (parallel) ← wait_for each      |
                                                               │
make up-local ────────────────────────────────────────────────┘
    |           (skips Config Server, Auth, Edge)
    |
    ├── generate.entities  (only if DAS not running)
    ├── das:build          (only if DAS not running)
    ├── run-das     :8092   ← wait_for
    ├── run-server  :8101   ← wait_for
    ├── run-worker  :8102   ← wait_for
    ├── run-async   :8099   ← wait_for
    └── mock        :8090   (loadtest module in mock profile)
```

**Port summary:**

| Port | Service | Started by |
|---|---|---|
| 8888 | Config Server | `up`, `up-kafka` |
| 8094 | Auth Service | `up`, `up-kafka` |
| 8092 | DAS | all `up*` targets |
| 8096 | Edge Gateway | `up`, `up-kafka` |
| 8099 | Async Process | `up-local` |
| 8090 | Mock Receiver | `up-local`, `mock` |
| 8101 | OBConn Server | all `up*` targets |
| 8102 | OBConn Worker | all `up*` targets |
| 29092 | Redpanda/Kafka Broker | `infra`, `infra-kafka` |
| 9093 | Redpanda Console / Kafka UI | `infra`, `infra-kafka` |
| 8083 | Kafka Connect API | `infra`, `infra-kafka` |
| 8002 | Kafka Connect UI | `infra-kafka` only |
| 16686 | Jaeger UI | `infra`, `infra-kafka` |
| 5465 | PostgreSQL (Debezium) | `infra-kafka` only |
| 8199 | Dev Portal | `portal` |

---

## 5. Common Workflows

### Fresh start from scratch

```bash
make config && make infra && make build && make up-local
```

Steps:
1. `make config` — generates `rxconfig/*.yaml` from templates and injects DB credentials.
2. `make infra` — starts Redpanda and Kafka Connect via Docker Compose.
3. `make build` — compiles all four OBConnector modules.
4. `make up-local` — starts DAS (with entity generation), OBConn Server, OBConn Worker, Async Process, and Mock Receiver.

### Rebuild and restart a single service

```bash
make build-worker && make run-worker
```

Rebuilds the worker JAR and starts it in the foreground. Use this during active development of the worker to get fast feedback. Run in a dedicated terminal.

```bash
make build-server && make run-server
```

Same pattern for the server module.

### Run the receive load test

```bash
make loadtest.receive
```

Requires OBConnector Server (`:8101`) and Worker (`:8102`) to already be running. Sends 5 HTTP POST messages to the server and polls the async status endpoint for each.

### Run the send load test with the mock receiver

```bash
# Terminal 1 — start the mock receiver
make mock

# Terminal 2 — run the send test
make loadtest.send
```

The mock receiver on `:8090` simulates the external system. The send test publishes CDC events that the Worker picks up and forwards to the mock.

### Clean reset (purge topics and restart)

```bash
make down && make purge && make infra && make up-local
```

Steps:
1. `make down` — stops all services and Docker Compose containers.
2. `make purge` — deletes all OBConnector Kafka topics so no stale messages carry over.
3. `make infra` — brings infrastructure back up with a clean Redpanda state.
4. `make up-local` — starts all services fresh.

### Verify environment before first run

```bash
make check-java && make check-db
```

Run these before any `up*` target to confirm the JDK and database are correctly configured. Both are also called automatically as prerequisites by `make up`, `make up-local`, and `make up-kafka`.

### Monitor running services

```bash
# In one terminal
make logs

# In another terminal
make status
```

`make logs` tails all `.run/*.log` files interleaved. `make status` shows PID liveness and Docker container state at a point in time.
