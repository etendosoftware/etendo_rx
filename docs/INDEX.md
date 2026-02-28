# EtendoRX — Technical Documentation Index

EtendoRX is a reactive microservices platform built on **Spring Boot 3.1.4 / Spring Cloud 2022.0.4 / Java 17** for Etendo ERP integrations. It provides a composable runtime for data access, authentication, API routing, asynchronous task processing, and synchronization workflows.

---

## Service Catalog

| Service | Port | Purpose |
|---|---|---|
| Config Server | 8888 | Spring Cloud Config — centralized configuration for all services |
| Auth | 8094 | JWT authentication — token issuance and validation |
| DAS | 8092 | Data Access Service — REST API layer over the Etendo database |
| Edge | 8096 | API Gateway — routing, load balancing, and request filtering |
| AsyncProcess | 8099 | Async task processing — background job execution and scheduling |
| OBConnector Server | 8101 | Sync REST API — orchestration endpoint for synchronization flows |
| OBConnector Worker | 8102 | Sync execution engine — worker that executes sync pipeline steps |

---

## Platform Documentation

| Document | Description |
|---|---|
| [architecture.md](./architecture.md) | Platform architecture, service dependency graph, and service catalog with roles and responsibilities |
| [getting-started.md](./getting-started.md) | Prerequisites, local installation steps, environment setup, and first run walkthrough |
| [makefile-reference.md](./makefile-reference.md) | All build and deployment commands exposed through the project Makefile |
| [configuration.md](./configuration.md) | Configuration files, Spring Cloud Config properties, per-service overrides, and environment variables |
| [infrastructure.md](./infrastructure.md) | Docker Compose setup, Kafka and Redpanda message broker configuration, and Jaeger distributed tracing |

---

## Module Documentation

| Module | Index |
|---|---|
| OBConnector | [com.etendorx.integration.obconnector — Documentation Index](../modules/com.etendorx.integration.obconnector/docs/INDEX.md) |

---

## Diagrams

| File | Description |
|---|---|
| [00-async-process.plantuml](./00-async-process.plantuml) | Sequence diagram of the async process flow — job submission, worker pickup, and result handling |
| [01-async-deployment-diagram.plantuml](./01-async-deployment-diagram.plantuml) | Deployment diagram showing async infrastructure components and their relationships |
