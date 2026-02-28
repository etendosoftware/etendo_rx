# EtendoRX OBConnector — Getting Started

This guide covers everything needed to clone, configure, build, and run the EtendoRX OBConnector locally. All commands assume a Unix-like shell (bash/zsh/fish) on macOS or Linux.

---

## 1. Prerequisites

### Java 17

Java 17 is required. **Amazon Corretto 17 is the recommended distribution.**

> **Important:** GraalVM is NOT compatible. It causes Lombok annotation processing failures at compile time. If your `JAVA_HOME` points to a GraalVM installation, the build will fail with cryptic errors about missing generated classes.

To verify your Java version:

```bash
java -version
# Must output: openjdk version "17.x.x" ...
```

To install Amazon Corretto 17 on macOS via Homebrew:

```bash
brew install --cask corretto17
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
```

The Makefile auto-detects Java 17 using `/usr/libexec/java_home -v 17` on macOS, falling back to `$HOME/Library/Java/JavaVirtualMachines/corretto-17.0.18/Contents/Home`. You can override it explicitly:

```bash
export JAVA_HOME=/path/to/corretto-17
```

### Docker and Docker Compose

Required for running the message broker infrastructure (Redpanda or Kafka) and related services.

```bash
docker --version        # Docker 20.x or later
docker compose version  # Docker Compose v2.x or later
```

### Git

Required for cloning and managing submodules.

```bash
git --version
```

### PostgreSQL with Etendo ERP

The services connect to a running Etendo Classic (OpenBravo-based) PostgreSQL database. Default connection parameters:

| Parameter | Default value |
|-----------|--------------|
| Host      | localhost     |
| Port      | 5432          |
| Database  | etendo        |
| User      | tad           |
| Password  | tad           |

The database must have Etendo ERP installed and initialized. The DAS service generates entity classes from the database schema at startup — a missing or empty database will cause the build to fail.

You can verify connectivity at any time after configuring `gradle.properties` with:

```bash
make check-db
```

### GitHub Personal Access Token

The Gradle build pulls dependencies from the private Maven registry at `maven.pkg.github.com/etendosoftware`. You need a GitHub account with access to the Etendo organization and a Personal Access Token (PAT) with at least `read:packages` scope.

Generate a token at: https://github.com/settings/tokens

---

## 2. Clone and Setup

```bash
git clone git@github.com:etendosoftware/etendo_rx.git
cd etendo_rx
git submodule update --init --recursive
```

The `--recursive` flag is required. Several core services (DAS, Auth, Edge, Config Server, Async Process) live in submodules under the project root. Skipping this step results in empty module directories and a broken build.

---

## 3. Configure gradle.properties

Copy the example file and edit it:

```bash
cp gradle.properties.example gradle.properties
```

Open `gradle.properties` and set the following values:

```properties
# --- Database (Etendo Classic) ---
bbdd.url=jdbc:postgresql://localhost:5432
bbdd.sid=etendo
bbdd.user=tad
bbdd.password=tad

# --- GitHub Maven registry ---
githubUser=your-github-username
githubToken=ghp_your_personal_access_token

# --- Context name (used as Docker Compose project name) ---
context.name=etendo_conn
```

**Notes:**

- `bbdd.url` must NOT include the database name — that comes from `bbdd.sid`. The Makefile constructs the full JDBC URL as `${bbdd.url}/${bbdd.sid}`.
- `context.name` is used as the Docker Compose project prefix. Containers will be named `etendo_conn-obconn-*`. Changing this value after the first run requires stopping and removing the old containers manually.
- The `githubToken` value is sensitive. Do not commit `gradle.properties` to version control (it is listed in `.gitignore`).

---

## 4. Generate Configuration

```bash
make config
```

This command does two things:

1. Copies every `rxconfig/*.yaml.template` file to `rxconfig/*.yaml` (skipping files that already exist, so it is safe to re-run).
2. Injects the database connection parameters from `gradle.properties` into `rxconfig/das.yaml`, updating the `url`, `username`, and `password` fields under the datasource section.

The generated YAML files in `rxconfig/` are the runtime configuration consumed by each service when running with the `local` Spring profile (i.e., without a Config Server).

Run `make config` again any time you change database credentials in `gradle.properties`.

---

## 5. Start Infrastructure

The OBConnector requires a Kafka-compatible message broker. Two options are available:

### Option A: Redpanda (default, recommended)

```bash
make infra
```

Redpanda is a Kafka-compatible broker implemented in C++. It is significantly lighter than the full Confluent Kafka stack and starts in a few seconds. This is the default choice for local development.

After startup the following endpoints are available:

| Service | URL |
|---------|-----|
| Redpanda Broker (Kafka protocol) | `localhost:29092` |
| Redpanda Console (web UI) | http://localhost:9093 |
| Kafka Connect API | http://localhost:8083 |
| Jaeger UI (distributed tracing) | http://localhost:16686 |

### Option B: Full Confluent Kafka Stack

```bash
make infra-kafka
```

This starts the full Confluent platform, which includes a ZooKeeper-dependent Kafka broker, Kafka UI, Kafka Connect UI, and a dedicated PostgreSQL instance for Debezium. Use this option if you need to test the real Debezium CDC connector behavior.

Additional endpoints (beyond what Redpanda provides):

| Service | URL |
|---------|-----|
| Kafka Connect UI | http://localhost:8002 |
| PostgreSQL for Debezium | `localhost:5465` |

### Verify Infrastructure

```bash
make infra-ps
```

Shows the status of all running infrastructure containers. All containers should show `Up` status before proceeding.

To tail infrastructure logs:

```bash
make infra-logs
```

> **Timing note:** Redpanda typically takes 3-5 seconds to be ready. The full Kafka stack can take 30-60 seconds. If services fail to connect to the broker on first startup, wait and retry.

---

## 6. Build

```bash
make build
```

This compiles the four OBConnector modules in order:

1. `com.etendorx.integration.obconn.common` — shared domain model and utilities
2. `com.etendorx.integration.obconn.lib` — core business logic (mapping, transformation)
3. `com.etendorx.integration.obconn.server` — HTTP inbound API (port 8101)
4. `com.etendorx.integration.obconn.worker` — Kafka consumer/producer (port 8102)

Individual modules can be built separately if needed:

```bash
make build-lib     # lib module only
make build-server  # server module only
make build-worker  # worker module only
```

**If the worker build fails on entity generation**, you can skip its compilation to unblock other modules:

```bash
./gradlew :com.etendorx.integration.obconn.server:build \
  -x :com.etendorx.integration.to_openbravo.worker:compileJava
```

The first build will be slow (5-15 minutes) because it downloads all dependencies. Subsequent builds use the Gradle cache and are significantly faster.

---

## 7. Run

### Fastest: Local Mode (no Config Server)

```bash
make up-local
```

This is the recommended mode for day-to-day development. It skips the Config Server and Auth/Edge gateway services, using local YAML files from `rxconfig/` directly via `-Dspring.profiles.active=local`.

What `make up-local` does, in order:

1. Runs `check-java` and `check-db` preflight checks.
2. Starts infrastructure (Redpanda) via `make infra`.
3. Runs `make config` to ensure YAML files are up to date.
4. Checks if DAS is already running on `:8092`; if not, generates entities from the database schema, compiles DAS, and starts it.
5. Starts OBConnector Server (`:8101`), OBConnector Worker (`:8102`), Async Process (`:8099`), and Mock Receiver (`:8090`) in background processes.
6. Waits for each service to respond on its `/actuator/health` endpoint (timeout: 120 seconds).
7. Prints a summary of all running endpoints.

All processes run in the background. PIDs and logs are tracked under `.run/`:

```
.run/
  das.pid
  das.log
  obconn-server.pid
  obconn-server.log
  obconn-worker.pid
  obconn-worker.log
  async.pid
  async.log
  mock-receiver.pid
  mock-receiver.log
```

### Full Mode (with Config Server)

```bash
make up
```

This starts the complete EtendoRX stack including Config Server (`:8888`), Auth Service (`:8094`), DAS (`:8092`), and Edge Gateway (`:8096`), in addition to the OBConnector Server and Worker. Config Server must be fully ready before the other services are launched (the Makefile polls `/actuator/health` on `:8888`).

Use this mode when you need to test JWT authentication flows or the edge gateway routing behavior.

### Kafka Mode (full stack + Confluent Kafka)

```bash
make up-kafka
```

Same as `make up` but uses the full Confluent Kafka infrastructure instead of Redpanda. Useful for testing production-equivalent Debezium CDC connector behavior.

---

## 8. Verify

### Service Status

```bash
make status
```

Shows:
- Docker Compose container status (infrastructure).
- For each `.run/*.pid` file: whether the process is `RUNNING` or `STOPPED`.

### Health Endpoints

Each service exposes a Spring Boot Actuator health endpoint:

| Service | Health URL |
|---------|-----------|
| Config Server | http://localhost:8888/actuator/health |
| Auth | http://localhost:8094/actuator/health |
| DAS | http://localhost:8092/actuator/health |
| Edge | http://localhost:8096/actuator/health |
| OBConn Server | http://localhost:8101/actuator/health |
| OBConn Worker | http://localhost:8102/actuator/health |
| Async Process | http://localhost:8099/actuator/health |

A healthy response looks like:

```json
{"status":"UP"}
```

### Dev Portal

```bash
make portal
```

Opens a lightweight static HTML service browser at http://localhost:8199. Lists all services, their ports, and quick links.

### Dashboard

If `dashboard.enabled=true` is set in the worker configuration:

```
http://localhost:8102/dashboard
```

The dashboard shows real-time sync job status, retry queue depth, dead-letter topic contents, and throughput metrics.

### Tail Logs

```bash
make logs
```

Tails all `.run/*.log` files simultaneously. Use `Ctrl+C` to stop.

---

## 9. First Sync Test

Once all services are running, use the load test targets to verify end-to-end behavior.

### Receive Workflow (external system → Etendo)

```bash
make loadtest.receive
```

Sends a series of BusinessPartner JSON payloads via HTTP POST to the OBConnector Server at `http://localhost:8101/api/sync/`. The server publishes them to the `obconnector.receive` Kafka topic. The Worker picks them up, transforms them, and calls DAS to persist them in the Etendo database.

Expected output: the command exits after sending 5 messages per thread (default: 1 thread). Check the Worker logs or dashboard for processing results.

### Send Workflow (Etendo → external system)

```bash
make loadtest.send
```

Simulates a Debezium CDC event on the `default.public.c_bpartner` Kafka topic, as if a record changed in the Etendo database. The Worker consumes the event, transforms it, and calls the Mock Receiver at `http://localhost:8090` to simulate delivery to an external system.

Expected output: the Mock Receiver logs the received payload. Check the Worker logs for the full transformation and delivery trace.

### Run Both

```bash
make loadtest
```

Runs `loadtest.send` followed by `loadtest.receive` sequentially.

---

## 10. Stopping

```bash
make down
```

This kills all background service processes tracked in `.run/*.pid` and then stops all Docker infrastructure containers via `make infra-down`. It handles both Redpanda and Kafka compose files.

To stop only the infrastructure containers without touching the services:

```bash
make infra-down
```

---

## 11. Troubleshooting

### JAVA_HOME points to wrong JVM

**Symptom:** Build fails with errors like `cannot find symbol` on Lombok-generated methods, or `Fatal error compiling: invalid target release: 17`.

**Fix:** Ensure `JAVA_HOME` points to a Java 17 installation, not GraalVM or any other version.

```bash
make check-java
# Should print: OK — Java 17 (/path/to/corretto-17)

export JAVA_HOME=$(/usr/libexec/java_home -v 17)
```

If you have multiple JDKs installed, use `/usr/libexec/java_home -V` to list them all and identify the correct path.

### Database connection fails

**Symptom:** `make check-db` prints `FAIL — Cannot connect to PostgreSQL`, or DAS crashes at startup with a `Connection refused` or authentication error.

**Fix:**

1. Verify `gradle.properties` has the correct `bbdd.*` values.
2. Verify the database is running: `pg_isready -h localhost -p 5432`.
3. Verify the user and password: `psql -h localhost -U tad -d etendo`.
4. Run `make config` after any credential change to re-inject them into `rxconfig/das.yaml`.

### Kafka broker not ready

**Symptom:** OBConnector Worker fails to start with `org.apache.kafka.common.errors.TimeoutException: Topic not available`, or services print repeated `WARN` messages about broker connection refused.

**Fix:**

1. Wait 10-15 seconds after `make infra` before starting services. Redpanda needs a moment to initialize its internal topics.
2. Check container status: `make infra-ps`.
3. Tail container logs: `make infra-logs`.
4. If using Confluent Kafka (`make infra-kafka`), wait up to 60 seconds for all components to be ready.

### Port already in use

**Symptom:** A service fails to bind its port with `Address already in use`.

**Fix:**

1. Check what is running: `make status`.
2. If a stale process is listed as `STOPPED` but the port is occupied, find and kill it:
   ```bash
   lsof -ti:8101 | xargs kill -9   # example for port 8101
   ```
3. Re-run `make up-local` — it checks each port before starting and skips already-running services.

### Build fails on to_openbravo.worker

**Symptom:** The build exits with a compilation error inside `com.etendorx.integration.to_openbravo.worker`.

**Fix:** Exclude that subproject from compilation while working on the OBConnector:

```bash
./gradlew :com.etendorx.integration.obconn.common:build \
          :com.etendorx.integration.obconn.lib:build \
          :com.etendorx.integration.obconn.server:build \
          :com.etendorx.integration.obconn.worker:build \
          -x :com.etendorx.integration.to_openbravo.worker:compileJava
```

### Topic messages are stale from a previous run

**Symptom:** The Worker processes old or duplicate events from a previous test session.

**Fix:** Purge all OBConnector Kafka topics and let them be auto-recreated:

```bash
make purge
```

This deletes the following topics: `obconnector.send`, `obconnector.receive`, their DLT variants, all retry topics (`-retry-10000`, `-retry-20000`, `-retry-40000`, `-retry-60000`), and the Debezium source topic `default.public.c_bpartner`. Topics are automatically recreated when the producer or consumer reconnects.

---

## Quick Reference

| Command | Description |
|---------|-------------|
| `make up-local` | Fastest startup: Redpanda + services, no Config Server |
| `make up` | Full stack: Redpanda + Config Server + all services |
| `make up-kafka` | Full stack with Confluent Kafka instead of Redpanda |
| `make down` | Stop all services and infrastructure |
| `make status` | Show running services and containers |
| `make logs` | Tail all service logs |
| `make infra` | Start Redpanda only |
| `make infra-kafka` | Start Confluent Kafka stack only |
| `make infra-down` | Stop infrastructure containers |
| `make infra-ps` | Show infrastructure container status |
| `make infra-logs` | Tail infrastructure container logs |
| `make config` | Generate YAML config from templates |
| `make build` | Compile all modules |
| `make test` | Run all unit tests |
| `make check-java` | Verify Java 17 is available |
| `make check-db` | Test PostgreSQL connectivity |
| `make loadtest.receive` | Send test payloads via HTTP (Receive workflow) |
| `make loadtest.send` | Simulate Debezium CDC event (Send workflow) |
| `make purge` | Delete and reset all OBConnector Kafka topics |
| `make portal` | Open Dev Portal at http://localhost:8199 |
