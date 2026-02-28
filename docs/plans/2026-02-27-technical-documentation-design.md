# Technical Documentation Design

**Date:** 2026-02-27
**Audience:** Internal developers + External integrators
**Structure:** Two-level (platform + connector module)

## Overview

Create comprehensive English technical documentation for EtendoRX platform and the OBConnector integration module. Documentation lives in two locations matching the repo structure.

## Location 1: EtendoRX Platform (`etendo_rx/docs/`)

| File | Description |
|------|-------------|
| `INDEX.md` | Master table of contents with links to all platform and connector docs |
| `architecture.md` | Platform services (DAS, Auth, Edge, Config, AsyncProcess), ports, interaction diagram |
| `getting-started.md` | Prerequisites, clone, configure, build, run, verify |
| `makefile-reference.md` | All 33 Makefile targets with descriptions, args, examples, dependency order |
| `configuration.md` | rxconfig/ YAML files, gradle.properties, template system, env vars |
| `infrastructure.md` | Docker Compose (Redpanda vs Kafka), Jaeger, Debezium, ports reference |

## Location 2: OBConnector Module (`modules/com.etendorx.integration.obconnector/docs/`)

| File | Description |
|------|-------------|
| `INDEX.md` | Connector-specific TOC |
| `architecture.md` | Module structure (common/lib/server/worker/loadtest), dependency graph, class hierarchy |
| `workflows.md` | Send & Receive pipelines: each step (MAP→POST_ACTION) with HTTP calls, data flow, step messages |
| `api-reference.md` | Server REST API: POST/PUT/GET endpoints with request/response examples |
| `dashboard.md` | Setup, access, HTMX polling, step detail messages, endpoints |
| `worker.md` | Filters, converters, operations, FK resolution (subEntityMap), DAS HTTP calls |
| `loadtest.md` | Send vs Receive load tests, config options, Makefile targets, mock receiver |
| `resilience.md` | Dedup, DLT replay, HTTP retry, saga compensation, idempotent steps, health indicator |
| `extending.md` | Adding new entities, custom post-actions, custom converters |
| `configuration.md` | Worker/Server properties, auth bypass, token setup, connector instance |

## Principles

- Technical and detailed — include class names, file paths, config keys, HTTP examples
- Each file is self-contained but links to related docs
- Code examples where useful (config snippets, curl commands, Java snippets)
- No screenshots — describe UI elements textually
- Architecture diagrams in ASCII/text format
