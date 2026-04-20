# OBConnector Infrastructure

This document describes the local development infrastructure for the OBConnector module. Two compose stacks are provided, selectable via `make` targets. Both expose the same external port assignments so that application configuration does not change between modes.

---

## Two Infrastructure Modes

| Mode | Make Target | Compose File | Use Case |
|------|-------------|--------------|----------|
| Redpanda (default) | `make infra` | `docker-compose.redpanda.yml` | Development, CI, lightweight environments |
| Full Kafka (Confluent) | `make infra-kafka` | `docker-compose.yml` | Production parity, Confluent feature testing |

The Redpanda stack is the recommended default for development. It eliminates the Zookeeper dependency, uses significantly less memory (512 MB for the broker), and starts faster. Both stacks provide Kafka-compatible endpoints on the same ports, so no application config changes are required when switching modes.

---

## Redpanda Stack (Default)

File: `modules/com.etendorx.integration.obconnector/infraestructure/docker-compose.redpanda.yml`

### Services

#### Redpanda Broker

```yaml
image: docker.redpanda.com/redpandadata/redpanda:v24.1.1
```

Redpanda is a Kafka-compatible streaming platform written in C++. It does not require Zookeeper — coordination is handled internally via Raft consensus. This single-node configuration is suitable for local development.

| Parameter | Value |
|-----------|-------|
| Image | `redpandadata/redpanda:v24.1.1` |
| External port (Kafka) | `29092` |
| Internal port (Kafka) | `9092` (container-to-container) |
| Pandaproxy (HTTP) | `18082` (external), `8082` (internal) |
| Schema Registry | `18081` (external), `8081` (internal) |
| Memory limit | `512M` |
| CPU threads | `1` (`--smp 1`) |

The broker advertises two listener addresses:
- `internal://redpanda:9092` — used by other containers (Kafka Connect, etc.)
- `external://localhost:29092` — used by host-machine clients (application code, `rpk` CLI)

A health check using `rpk cluster health --exit-when-healthy` gates dependent services; Kafka Connect and the console will not start until the broker is healthy.

#### Redpanda Console

```yaml
image: docker.redpanda.com/redpandadata/console:v2.6.0
```

A web UI for browsing topics, inspecting messages, monitoring consumer group lag, and viewing partition assignments.

| Parameter | Value |
|-----------|-------|
| Image | `redpandadata/console:v2.6.0` |
| Host port | `9093` (mapped to container port `8080`) |
| Config | Mounted from `./redpanda-console-config.yml` |

The console depends on the broker being healthy before starting. Configuration is provided via a mounted config file at `/tmp/config.yml` inside the container.

Access: `http://localhost:9093`

#### Kafka Connect (Debezium)

```yaml
image: quay.io/debezium/connect:2.3.0.Final
```

Kafka Connect running the Debezium connector plugins. Debezium implements Change Data Capture (CDC) — it monitors the PostgreSQL Write-Ahead Log (WAL) and publishes row-level change events (INSERT, UPDATE, DELETE) to Kafka topics.

| Parameter | Value |
|-----------|-------|
| Image | `quay.io/debezium/connect:2.3.0.Final` |
| Host port | `8083` |
| Bootstrap servers | `redpanda:9092` (internal listener) |
| Connector group ID | `1` |
| Config storage topic | `my_connect_configs` |
| Offset storage topic | `my_connect_offsets` |
| Status storage topic | `my_connect_statuses` |

Connectors are configured at runtime via the REST API at `http://localhost:8083`. The `CONNECT_REST_ADVERTISED_HOST_NAME` is set to `kafka-connect` to allow inter-container communication.

#### Jaeger

```yaml
image: jaegertracing/all-in-one:latest
```

Jaeger is an end-to-end distributed tracing system. The OBConnector propagates `runId`, `workflow`, `entity`, and `entityId` through SLF4J MDC and OpenTelemetry trace headers, enabling full request tracing across the server and worker.

| Parameter | Value |
|-----------|-------|
| Image | `jaegertracing/all-in-one:latest` |
| UI port | `16686` |
| gRPC (OTLP) | `4317` |
| Model port | `14250` |
| OTLP enabled | `true` |

Access: `http://localhost:16686`

---

## Full Kafka Stack (Confluent)

File: `modules/com.etendorx.integration.obconnector/infraestructure/docker-compose.yml`

This stack uses the official Confluent images and includes a dedicated PostgreSQL instance pre-configured for Debezium (logical replication enabled). It adds Zookeeper as the Kafka coordination layer and includes a Kafka Connect UI for visual connector management.

### Services

#### Zookeeper

```yaml
image: confluentinc/cp-zookeeper:latest
```

Apache ZooKeeper provides distributed coordination for Kafka brokers. Required by the Confluent Kafka image.

| Parameter | Value |
|-----------|-------|
| Host port | `22181` (mapped to container port `2181`) |
| Client port | `2181` |
| Tick time | `2000ms` |

#### Kafka Broker

```yaml
image: confluentinc/cp-kafka:latest
```

Apache Kafka broker from Confluent. Depends on Zookeeper for metadata and leader election.

| Parameter | Value |
|-----------|-------|
| Host port (external) | `29092` |
| Host port (internal) | `9092` |
| Broker ID | `1` |
| Zookeeper connection | `zookeeper:2181` |
| Offsets replication factor | `1` (single-node, no replication) |

Listeners:
- `PLAINTEXT://kafka:9092` — inter-container communication
- `PLAINTEXT_HOST://localhost:29092` — host-machine client access

#### Kafka UI

```yaml
image: provectuslabs/kafka-ui:latest
```

Web UI for managing Kafka topics, consumer groups, and messages. Equivalent to Redpanda Console in the Redpanda stack.

| Parameter | Value |
|-----------|-------|
| Host port | `9093` (mapped to container port `8080`) |
| Cluster name | `local` |
| Bootstrap servers | `kafka:9092` |
| Metrics port | `9997` |

Access: `http://localhost:9093`

#### Debezium PostgreSQL

```yaml
image: quay.io/debezium/example-postgres:2.3.0.Final
```

A PostgreSQL instance pre-configured with logical replication enabled (`wal_level=logical`), which is required for Debezium CDC. This is an isolated database for integration testing and is separate from the main Etendo database.

| Parameter | Value |
|-----------|-------|
| Host port | `5465` (mapped to container port `5432`) |
| User | `postgres` |
| Password | `syspass` |

#### Kafka Connect (Debezium)

```yaml
image: quay.io/debezium/connect:2.3.0.Final
```

Same Debezium Connect image as in the Redpanda stack, but connected to the Confluent Kafka broker instead.

| Parameter | Value |
|-----------|-------|
| Host port | `8083` |
| Bootstrap servers | `kafka:9092` |
| Group ID | `1` |

#### Kafka Connect UI

```yaml
image: landoop/kafka-connect-ui
```

A visual interface for managing Kafka Connect connectors. Allows creating, updating, and monitoring connector configurations without using the REST API directly.

| Parameter | Value |
|-----------|-------|
| Host port | `8002` (mapped to container port `8000`) |
| Connect URL | `http://kafka-connect:8083/` |

Access: `http://localhost:8002`

#### Jaeger

Same image and configuration as in the Redpanda stack. See the Redpanda section above.

---

## Kafka Topics

The following topics are used by the OBConnector module. They are created automatically on first use or via the `make purge` target.

| Topic | Direction | Description |
|-------|-----------|-------------|
| `obconnector.send` | Etendo → External | CDC events captured from Etendo's PostgreSQL WAL. `DbzListener` consumes these events and triggers the Send workflow (`MAP → PRE_LOGIC → SYNC → POST_LOGIC → PROCESS_DATA → POST_ACTION`). |
| `obconnector.receive` | External → Etendo | Messages pushed by an external system into Etendo. The Receive workflow processes these into Etendo entities via the DAS. |
| `obconnector.send.DLT` | Dead Letter | Failed messages from the Send workflow. Stored by `@DltHandler` in `DltReplayService`'s in-memory queue. Replayed via `replayAll()`. |
| `obconnector.receive.DLT` | Dead Letter | Failed messages from the Receive workflow. Same DLT handling as the Send DLT. |
| `async-process-execution` | Internal | Workflow step status updates published by the AsyncProcess service. Tracks step progression and completion state for idempotent retry. |

### Dead Letter Topic (DLT) Behavior

When a message fails all retry attempts, Spring Cloud Stream routes it to the corresponding `.DLT` topic. The `DltReplayService` stores the failed message in a `ConcurrentLinkedQueue` (max size configurable via `dlt.max.queue.size`, default 1000). Failed messages can be replayed to their original topics via `replayAll()`, which re-publishes each queued message.

---

## Debezium CDC (Change Data Capture)

Debezium monitors the PostgreSQL Write-Ahead Log (WAL) in real time. When a row is inserted, updated, or deleted in a monitored table, Debezium reads the change from the WAL and publishes a structured event to the corresponding Kafka topic.

### How It Works

1. PostgreSQL must have `wal_level=logical` configured (the Debezium example image has this pre-configured; the main Etendo database requires manual configuration).
2. A Debezium PostgreSQL connector is registered via the REST API at `http://localhost:8083/connectors`.
3. The connector reads the WAL via the PostgreSQL logical replication protocol.
4. Change events are serialized in Debezium JSON format and published to Kafka topics (by default, one topic per table: `<server>.<schema>.<table>`).
5. The OBConnector's `DbzListener` subscribes to the `obconnector.send` topic and processes each event through the Send workflow.

### Message Deduplication

Before forwarding a CDC event to the workflow, `DbzListener` checks `MessageDeduplicationService`. The dedup key is a composite of `entity|id|verb|SHA-256(data)`. Duplicate messages within the TTL window (default 300 seconds, configurable via `dedup.ttl.seconds`) are silently dropped to prevent double-processing caused by Kafka at-least-once delivery.

### Connector Configuration via REST

Register a connector:

```bash
curl -X POST http://localhost:8083/connectors \
  -H "Content-Type: application/json" \
  -d '{
    "name": "obconnector-source",
    "config": {
      "connector.class": "io.debezium.connector.postgresql.PostgresConnector",
      "database.hostname": "host.docker.internal",
      "database.port": "5432",
      "database.user": "etendo",
      "database.password": "etendo",
      "database.dbname": "etendo",
      "topic.prefix": "obconnector",
      "table.include.list": "public.c_order,public.m_product"
    }
  }'
```

List registered connectors:

```bash
curl http://localhost:8083/connectors
```

Check connector status:

```bash
curl http://localhost:8083/connectors/obconnector-source/status
```

---

## Topic Management

### Purge (Clean State)

```bash
make purge
```

Deletes and recreates all OBConnector topics. Use this to reset the message state during development without restarting the entire stack.

For the Redpanda stack, this uses `rpk topic delete` followed by `rpk topic create`. For the Kafka stack, this uses `kafka-topics.sh --delete` followed by `kafka-topics.sh --create`.

Topics recreated by `make purge`:
- `obconnector.send`
- `obconnector.receive`
- `obconnector.send.DLT`
- `obconnector.receive.DLT`
- `async-process-execution`

---

## Complete Ports Reference

### Infrastructure Services

| Service | Port | Stack | Purpose |
|---------|------|-------|---------|
| Kafka / Redpanda Broker | `29092` | Both | External Kafka-compatible message broker endpoint |
| Kafka / Redpanda Broker | `9092` | Both | Internal broker endpoint (container-to-container) |
| Redpanda Console | `9093` | Redpanda | Topic browser, consumer groups, message viewer |
| Kafka UI | `9093` | Kafka | Topic browser, consumer groups, message viewer |
| Redpanda Pandaproxy | `18082` | Redpanda | HTTP/REST Kafka proxy |
| Redpanda Schema Registry | `18081` | Redpanda | Avro schema registry |
| Kafka Connect | `8083` | Both | CDC connector management REST API |
| Kafka Connect UI | `8002` | Kafka only | Visual connector management |
| Zookeeper | `22181` | Kafka only | Kafka broker coordination |
| Debezium PostgreSQL | `5465` | Kafka only | Example PostgreSQL with logical replication |
| Jaeger UI | `16686` | Both | Distributed tracing UI |
| Jaeger OTLP gRPC | `4317` | Both | OpenTelemetry trace ingestion |
| Jaeger model | `14250` | Both | Jaeger internal model port |

### Application Services

| Service | Port | Purpose |
|---------|------|---------|
| Config Server | `8888` | Central configuration server (Spring Cloud Config) |
| Auth | `8094` | Authentication service, issues JWT tokens |
| DAS | `8092` | Data Access Service — Etendo entity read/write |
| Edge | `8096` | API Gateway — routes external requests to services |
| AsyncProcess | `8099` | Async task processing, publishes to `async-process-execution` |
| OBConnector Server | `8101` | Sync REST API (`PUT /api/sync/{model}/{id}`, `POST /api/sync/{model}`) |
| OBConnector Worker | `8102` | Sync workflow execution (Receive and Send runners) |
| Dev Portal | `8199` | Development portal |
| Mock Receiver | `8090` | Test external system, simulates the remote endpoint |

---

## Startup Order

### Redpanda Stack

1. **Redpanda** starts and passes the health check (`rpk cluster health`).
2. **Redpanda Console** starts after Redpanda is healthy.
3. **Kafka Connect** starts after Redpanda is healthy.
4. **Jaeger** starts independently (no dependencies).

### Full Kafka Stack

1. **Zookeeper** starts.
2. **Kafka** depends on Zookeeper.
3. **Kafka UI**, **Kafka Connect**, and **PostgreSQL** depend on Kafka (implicit via links/environment).
4. **Kafka Connect UI** depends on Kafka Connect.
5. **Jaeger** starts independently.

Application services (Config Server, Auth, DAS, Edge, etc.) must be started separately after the infrastructure stack is running.
