SHELL := /bin/bash
.DEFAULT_GOAL := help

# --- Paths ---
ROOT        := $(shell pwd)
GRADLE      := JAVA_HOME=$(JAVA_HOME) ./gradlew
INFRA       := $(ROOT)/modules/com.etendorx.integration.obconnector/infraestructure
RXCONFIG    := $(ROOT)/rxconfig
PROPS       := $(ROOT)/gradle.properties
JAVA_HOME   ?= $(shell /usr/libexec/java_home -v 17 2>/dev/null || echo "$$HOME/Library/Java/JavaVirtualMachines/corretto-17.0.18/Contents/Home")

# --- Config from gradle.properties ---
DB_URL      := $(shell grep '^bbdd.url=' $(PROPS) | cut -d= -f2- | sed 's/\\:/:/g')
DB_SID      := $(shell grep '^bbdd.sid=' $(PROPS) | cut -d= -f2-)
DB_USER     := $(shell grep '^bbdd.user=' $(PROPS) | cut -d= -f2-)
DB_PASS     := $(shell grep '^bbdd.password=' $(PROPS) | cut -d= -f2-)
CTX_NAME    := $(shell grep '^context.name=' $(PROPS) 2>/dev/null | cut -d= -f2-)
ifeq ($(CTX_NAME),)
  CTX_NAME  := etendo
endif
COMPOSE     := docker-compose -p $(CTX_NAME)-obconn

# --- Colors ---
CYAN  := \033[36m
GREEN := \033[32m
YELLOW:= \033[33m
DIM   := \033[2m
RESET := \033[0m

# --- rxconfig.path: absolute path passed to bootRun so services find their yaml ---
BOOTRUN_ARGS := -Dspring.profiles.active=local
PIDS_DIR     := $(ROOT)/.run
MAX_WAIT   := 120

# Wait for a service port to be ready: $(call wait_for,name,port)
define wait_for
	@printf "  Waiting for $(1) on :$(2) "; \
	elapsed=0; \
	while ! curl -sf http://localhost:$(2)/actuator/health > /dev/null 2>&1; do \
		if [ $$elapsed -ge $(MAX_WAIT) ]; then \
			echo -e " $(YELLOW)TIMEOUT$(RESET)"; \
			logfile=$(PIDS_DIR)/$$(echo $(1) | tr ' ' '-' | tr '[:upper:]' '[:lower:]').log; \
			if [ -f "$$logfile" ]; then \
				echo -e "  $(YELLOW)--- Last 20 lines of $$logfile ---$(RESET)"; \
				tail -20 "$$logfile"; \
				echo -e "  $(YELLOW)---$(RESET)"; \
			fi; \
			exit 1; \
		fi; \
		printf "."; \
		sleep 2; \
		elapsed=$$((elapsed + 2)); \
	done; \
	if [ $$elapsed -lt $(MAX_WAIT) ]; then \
		echo -e " $(GREEN)OK$(RESET) ($${elapsed}s)"; \
	fi
endef

# ==============================================================================
# PREFLIGHT CHECKS
# ==============================================================================

.PHONY: check-db check-java

check-db: ## Test PostgreSQL connection using gradle.properties config
	@echo -e "$(CYAN)Checking PostgreSQL connection...$(RESET)"
	@echo -e "  URL:  $(DB_URL)/$(DB_SID)"
	@echo -e "  User: $(DB_USER)"
	@DB_HOST=$$(echo "$(DB_URL)" | sed 's|jdbc:postgresql://||' | cut -d: -f1); \
	DB_PORT=$$(echo "$(DB_URL)" | sed 's|jdbc:postgresql://||' | cut -d: -f2); \
	if PGPASSWORD=$(DB_PASS) psql -h "$$DB_HOST" -p "$$DB_PORT" -U $(DB_USER) -d $(DB_SID) -c "SELECT 1" > /dev/null 2>&1; then \
		echo -e "  $(GREEN)OK$(RESET) — PostgreSQL is reachable"; \
	else \
		echo -e "  $(YELLOW)FAIL$(RESET) — Cannot connect to PostgreSQL"; \
		echo -e "  Check gradle.properties (bbdd.*) and ensure Etendo Classic DB is running."; \
		exit 1; \
	fi

check-java: ## Verify Java 17 is available
	@if $(JAVA_HOME)/bin/java -version 2>&1 | grep -q "17\."; then \
		echo -e "  $(GREEN)OK$(RESET) — Java 17 ($(JAVA_HOME))"; \
	else \
		echo -e "  $(YELLOW)FAIL$(RESET) — Java 17 not found at JAVA_HOME=$(JAVA_HOME)"; \
		exit 1; \
	fi

# ==============================================================================
# INFRASTRUCTURE
# ==============================================================================

.PHONY: infra infra-kafka infra-down infra-logs infra-ps

infra: ## Start infra with Redpanda (default, lightweight)
	@echo -e "$(CYAN)Starting infrastructure (Redpanda)...$(RESET)"
	cd $(INFRA) && $(COMPOSE) -f docker-compose.redpanda.yml up -d
	@echo ""
	@echo -e "$(GREEN)  Infrastructure ready$(RESET)"
	@echo -e "  Redpanda Broker       localhost:29092"
	@echo -e "  Redpanda Console      http://localhost:9093"
	@echo -e "  Kafka Connect API     http://localhost:8083"
	@echo -e "  Jaeger UI             http://localhost:16686"
	@echo ""

infra-kafka: ## Start infra with Kafka (heavier alternative)
	@echo -e "$(CYAN)Starting infrastructure (Kafka)...$(RESET)"
	cd $(INFRA) && $(COMPOSE) up -d
	@echo ""
	@echo -e "$(GREEN)  Infrastructure ready$(RESET)"
	@echo -e "  Kafka Broker          localhost:29092"
	@echo -e "  Kafka UI              http://localhost:9093"
	@echo -e "  Kafka Connect API     http://localhost:8083"
	@echo -e "  Kafka Connect UI      http://localhost:8002"
	@echo -e "  Jaeger UI             http://localhost:16686"
	@echo -e "  PostgreSQL (Debezium) localhost:5465"
	@echo ""

infra-down: ## Stop all infra containers
	@echo -e "$(YELLOW)Stopping infrastructure...$(RESET)"
	cd $(INFRA) && $(COMPOSE) down 2>/dev/null; \
	cd $(INFRA) && $(COMPOSE) -f docker-compose.redpanda.yml down 2>/dev/null; true

infra-logs: ## Tail infra logs
	cd $(INFRA) && $(COMPOSE) logs -f --tail=50

infra-ps: ## Show running infra containers
	cd $(INFRA) && $(COMPOSE) ps 2>/dev/null; \
	cd $(INFRA) && $(COMPOSE) -f docker-compose.redpanda.yml ps 2>/dev/null; true

# ==============================================================================
# CONFIG
# ==============================================================================

.PHONY: config

config: ## Generate config files from templates (rxconfig/*.yaml from *.yaml.template)
	@echo -e "$(CYAN)Generating config from templates...$(RESET)"
	@for f in $(RXCONFIG)/*.yaml.template; do \
		target="$${f%.template}"; \
		if [ ! -f "$$target" ]; then \
			cp "$$f" "$$target"; \
			echo -e "  $(GREEN)Created$(RESET) $$(basename $$target)"; \
		else \
			echo -e "  $(YELLOW)Exists$(RESET)  $$(basename $$target) (skipped)"; \
		fi \
	done
	@echo -e "  $(CYAN)Injecting DB config from gradle.properties into das.yaml...$(RESET)"
	@sed -i.bak \
		-e 's|url:.*jdbc:.*|url: $(DB_URL)/$(DB_SID)|' \
		-e 's|username:.*|username: $(DB_USER)|' \
		-e 's|password:.*|password: $(DB_PASS)|' \
		$(RXCONFIG)/das.yaml && rm -f $(RXCONFIG)/das.yaml.bak
	@echo -e "  $(GREEN)OK$(RESET) das.yaml updated with gradle.properties DB config"

# ==============================================================================
# BUILD
# ==============================================================================

.PHONY: build build-lib build-server build-worker test test-lib

build: ## Build all modules
	@echo -e "$(CYAN)Building all modules...$(RESET)"
	$(GRADLE) :com.etendorx.integration.obconn.common:build \
	           :com.etendorx.integration.obconn.lib:build \
	           :com.etendorx.integration.obconn.server:build \
	           :com.etendorx.integration.obconn.worker:build

build-lib: ## Build only the lib module
	$(GRADLE) :com.etendorx.integration.obconn.lib:build

build-server: ## Build only the server module
	$(GRADLE) :com.etendorx.integration.obconn.server:build

build-worker: ## Build only the worker module
	$(GRADLE) :com.etendorx.integration.obconn.worker:build

test: ## Run all unit tests
	@echo -e "$(CYAN)Running tests...$(RESET)"
	$(GRADLE) :com.etendorx.integration.obconn.lib:test \
	           :com.etendorx.integration.obconn.server:test \
	           :com.etendorx.integration.obconn.worker:test

test-lib: ## Run lib unit tests only
	$(GRADLE) :com.etendorx.integration.obconn.lib:test

# ==============================================================================
# RUN SERVICES (each in foreground — use separate terminals or `make up`)
# ==============================================================================

.PHONY: run-config run-auth run-das run-edge run-server run-worker run-async

run-config: ## Start Config Server (port 8888) — MUST start first
	@echo -e "$(GREEN)Starting Config Server on :8888$(RESET)"
	$(GRADLE) :com.etendorx.configserver:bootRun

run-auth: ## Start Auth Service (port 8094)
	@echo -e "$(GREEN)Starting Auth on :8094$(RESET)"
	$(GRADLE) :com.etendorx.auth:bootRun $(BOOTRUN_ARGS)

run-das: ## Start DAS (port 8092)
	@echo -e "$(GREEN)Starting DAS on :8092$(RESET)"
	$(GRADLE) :com.etendorx.das:bootRun $(BOOTRUN_ARGS)

run-edge: ## Start Edge Gateway (port 8096)
	@echo -e "$(GREEN)Starting Edge on :8096$(RESET)"
	$(GRADLE) :com.etendorx.edge:bootRun $(BOOTRUN_ARGS)

run-server: ## Start OBConnector Server (port 8101)
	@echo -e "$(GREEN)Starting OBConnector Server on :8101$(RESET)"
	$(GRADLE) :com.etendorx.integration.obconn.server:bootRun $(BOOTRUN_ARGS)

run-worker: ## Start OBConnector Worker (port 8102)
	@echo -e "$(GREEN)Starting OBConnector Worker on :8102$(RESET)"
	$(GRADLE) :com.etendorx.integration.obconn.worker:bootRun $(BOOTRUN_ARGS)

run-async: ## Start Async Process service (port 8099)
	@echo -e "$(GREEN)Starting Async Process on :8099$(RESET)"
	$(GRADLE) :com.etendorx.asyncprocess:bootRun $(BOOTRUN_ARGS)

# ==============================================================================
# ORCHESTRATED STARTUP (background processes)
# ==============================================================================

.PHONY: up up-local up-kafka down status logs portal loadtest

up: check-java check-db infra config ## Start everything with Config Server
	@mkdir -p $(PIDS_DIR)
	@rm -f $(PIDS_DIR)/*.log $(PIDS_DIR)/*.pid
	@echo ""
	@echo -e "$(CYAN)Starting Config Server...$(RESET)"
	@$(GRADLE) :com.etendorx.configserver:bootRun > $(PIDS_DIR)/configserver.log 2>&1 & echo $$! > $(PIDS_DIR)/configserver.pid
	$(call wait_for,Config Server,8888)
	@echo ""
	@echo -e "$(CYAN)Starting Auth + DAS + Edge...$(RESET)"
	@$(GRADLE) :com.etendorx.auth:bootRun $(BOOTRUN_ARGS) > $(PIDS_DIR)/auth.log 2>&1 & echo $$! > $(PIDS_DIR)/auth.pid
	@$(GRADLE) :com.etendorx.das:bootRun $(BOOTRUN_ARGS) > $(PIDS_DIR)/das.log 2>&1 & echo $$! > $(PIDS_DIR)/das.pid
	@$(GRADLE) :com.etendorx.edge:bootRun $(BOOTRUN_ARGS) > $(PIDS_DIR)/edge.log 2>&1 & echo $$! > $(PIDS_DIR)/edge.pid
	$(call wait_for,Auth,8094)
	$(call wait_for,DAS,8092)
	$(call wait_for,Edge,8096)
	@echo ""
	@echo -e "$(CYAN)Starting OBConnector Server + Worker...$(RESET)"
	@$(GRADLE) :com.etendorx.integration.obconn.server:bootRun $(BOOTRUN_ARGS) > $(PIDS_DIR)/obconn-server.log 2>&1 & echo $$! > $(PIDS_DIR)/obconn-server.pid
	@$(GRADLE) :com.etendorx.integration.obconn.worker:bootRun $(BOOTRUN_ARGS) > $(PIDS_DIR)/obconn-worker.log 2>&1 & echo $$! > $(PIDS_DIR)/obconn-worker.pid
	$(call wait_for,OBConn Server,8101)
	$(call wait_for,OBConn Worker,8102)
	@$(MAKE) --no-print-directory _banner _CONFIGSRV="  Config Server         http://localhost:8888" _AUTH="  Auth Service          http://localhost:8094" _EDGE="  Edge Gateway          http://localhost:8096"

up-local: check-java check-db infra config ## Start everything WITHOUT Config Server (fastest)
	@mkdir -p $(PIDS_DIR)
	@echo ""
	@if curl -sf http://localhost:8092/actuator/health > /dev/null 2>&1; then \
		echo -e "  DAS                   $(GREEN)already running$(RESET)"; \
	else \
		printf "  Generating entities... "; \
		$(GRADLE) generate.entities -x test > $(PIDS_DIR)/generate.log 2>&1 \
			&& echo -e "$(GREEN)OK$(RESET)" \
			|| { echo -e "$(YELLOW)FAIL$(RESET)"; tail -20 $(PIDS_DIR)/generate.log; exit 1; }; \
		printf "  Compiling DAS... "; \
		$(GRADLE) :com.etendorx.das:build -x test > $(PIDS_DIR)/das-build.log 2>&1 \
			&& echo -e "$(GREEN)OK$(RESET)" \
			|| { echo -e "$(YELLOW)FAIL$(RESET)"; tail -20 $(PIDS_DIR)/das-build.log; exit 1; }; \
		echo -e "  $(CYAN)Starting DAS...$(RESET)"; \
		$(GRADLE) :com.etendorx.das:bootRun $(BOOTRUN_ARGS) > $(PIDS_DIR)/das.log 2>&1 & echo $$! > $(PIDS_DIR)/das.pid; \
	fi
	$(call wait_for,DAS,8092)
	@echo ""
	@if curl -sf http://localhost:8101/actuator/health > /dev/null 2>&1; then \
		echo -e "  OBConn Server         $(GREEN)already running$(RESET)"; \
	else \
		echo -e "  $(CYAN)Starting OBConnector Server...$(RESET)"; \
		$(GRADLE) :com.etendorx.integration.obconn.server:bootRun $(BOOTRUN_ARGS) > $(PIDS_DIR)/obconn-server.log 2>&1 & echo $$! > $(PIDS_DIR)/obconn-server.pid; \
	fi
	@if curl -sf http://localhost:8102/actuator/health > /dev/null 2>&1; then \
		echo -e "  OBConn Worker         $(GREEN)already running$(RESET)"; \
	else \
		echo -e "  $(CYAN)Starting OBConnector Worker...$(RESET)"; \
		$(GRADLE) :com.etendorx.integration.obconn.worker:bootRun $(BOOTRUN_ARGS) > $(PIDS_DIR)/obconn-worker.log 2>&1 & echo $$! > $(PIDS_DIR)/obconn-worker.pid; \
	fi
	@if curl -sf http://localhost:8099/actuator/health > /dev/null 2>&1; then \
		echo -e "  Async Process         $(GREEN)already running$(RESET)"; \
	else \
		echo -e "  $(CYAN)Starting Async Process...$(RESET)"; \
		$(GRADLE) :com.etendorx.asyncprocess:bootRun $(BOOTRUN_ARGS) > $(PIDS_DIR)/async.log 2>&1 & echo $$! > $(PIDS_DIR)/async.pid; \
	fi
	@if curl -sf http://localhost:8090/ > /dev/null 2>&1; then \
		echo -e "  Mock Receiver         $(GREEN)already running$(RESET)"; \
	else \
		echo -e "  $(CYAN)Starting Mock Receiver...$(RESET)"; \
		$(GRADLE) :com.etendorx.integration.obconn.loadtest:bootRun --args='--spring.profiles.active=mock' > $(PIDS_DIR)/mock-receiver.log 2>&1 & echo $$! > $(PIDS_DIR)/mock-receiver.pid; \
	fi
	$(call wait_for,OBConn Server,8101)
	$(call wait_for,OBConn Worker,8102)
	$(call wait_for,Async Process,8099)
	@sleep 2 && echo -e "  Mock Receiver         $(GREEN)OK$(RESET) (:8090)"
	@$(MAKE) --no-print-directory _banner _CONFIGSRV="  Config Server         $(DIM)skipped (local mode)$(RESET)" _AUTH="  Auth Service          $(DIM)skipped (local mode)$(RESET)" _EDGE="  Edge Gateway          $(DIM)skipped (local mode)$(RESET)"

up-kafka: check-java check-db infra-kafka config ## Start everything with Kafka instead of Redpanda
	@mkdir -p $(PIDS_DIR)
	@rm -f $(PIDS_DIR)/*.log $(PIDS_DIR)/*.pid
	@echo ""
	@echo -e "$(CYAN)Starting Config Server...$(RESET)"
	@$(GRADLE) :com.etendorx.configserver:bootRun > $(PIDS_DIR)/configserver.log 2>&1 & echo $$! > $(PIDS_DIR)/configserver.pid
	$(call wait_for,Config Server,8888)
	@echo ""
	@echo -e "$(CYAN)Starting Auth + DAS + Edge...$(RESET)"
	@$(GRADLE) :com.etendorx.auth:bootRun $(BOOTRUN_ARGS) > $(PIDS_DIR)/auth.log 2>&1 & echo $$! > $(PIDS_DIR)/auth.pid
	@$(GRADLE) :com.etendorx.das:bootRun $(BOOTRUN_ARGS) > $(PIDS_DIR)/das.log 2>&1 & echo $$! > $(PIDS_DIR)/das.pid
	@$(GRADLE) :com.etendorx.edge:bootRun $(BOOTRUN_ARGS) > $(PIDS_DIR)/edge.log 2>&1 & echo $$! > $(PIDS_DIR)/edge.pid
	$(call wait_for,Auth,8094)
	$(call wait_for,DAS,8092)
	$(call wait_for,Edge,8096)
	@echo ""
	@echo -e "$(CYAN)Starting OBConnector Server + Worker...$(RESET)"
	@$(GRADLE) :com.etendorx.integration.obconn.server:bootRun $(BOOTRUN_ARGS) > $(PIDS_DIR)/obconn-server.log 2>&1 & echo $$! > $(PIDS_DIR)/obconn-server.pid
	@$(GRADLE) :com.etendorx.integration.obconn.worker:bootRun $(BOOTRUN_ARGS) > $(PIDS_DIR)/obconn-worker.log 2>&1 & echo $$! > $(PIDS_DIR)/obconn-worker.pid
	$(call wait_for,OBConn Server,8101)
	$(call wait_for,OBConn Worker,8102)
	@$(MAKE) --no-print-directory _banner _CONFIGSRV="  Config Server         http://localhost:8888" _AUTH="  Auth Service          http://localhost:8094" _EDGE="  Edge Gateway          http://localhost:8096"

_banner:
	@echo ""
	@echo -e "$(GREEN)=========================================$(RESET)"
	@echo -e "$(GREEN)  All services ready$(RESET)"
	@echo -e "$(GREEN)=========================================$(RESET)"
	@echo ""
	@echo -e "$(CYAN)  Infrastructure$(RESET)"
	@echo -e "  Redpanda Broker       localhost:29092"
	@echo -e "  Redpanda Console      http://localhost:9093"
	@echo -e "  Kafka Connect API     http://localhost:8083"
	@echo -e "  Jaeger UI             http://localhost:16686"
	@echo ""
	@echo -e "$(CYAN)  EtendoRX Core$(RESET)"
	@echo -e "$(_CONFIGSRV)"
	@echo -e "$(_AUTH)"
	@echo -e "  DAS                   http://localhost:8092"
	@echo -e "$(_EDGE)"
	@echo ""
	@echo -e "$(CYAN)  OBConnector$(RESET)"
	@echo -e "  Server API            http://localhost:8101/api/sync/"
	@echo -e "  Worker                http://localhost:8102"
	@echo -e "  Dashboard             http://localhost:8102/dashboard  $(YELLOW)(requires dashboard.enabled=true)$(RESET)"
	@echo -e "  Mock Receiver         http://localhost:8090  $(DIM)(simulates external system)$(RESET)"
	@echo ""
	@echo -e "$(CYAN)  Database$(RESET)"
	@echo -e "  PostgreSQL            $(DB_URL)/$(DB_SID) $(DIM)(Etendo Classic)$(RESET)"
	@echo ""
	@echo -e "$(GREEN)=========================================$(RESET)"
	@echo -e "  Logs:    $(CYAN)make logs$(RESET)"
	@echo -e "  Status:  $(CYAN)make status$(RESET)"
	@echo -e "  Stop:    $(CYAN)make down$(RESET)"
	@echo -e "$(GREEN)=========================================$(RESET)"

down: ## Stop all services + infra
	@echo -e "$(YELLOW)Stopping services...$(RESET)"
	@if [ -d $(PIDS_DIR) ]; then \
		for pidfile in $(PIDS_DIR)/*.pid; do \
			if [ -f "$$pidfile" ]; then \
				pid=$$(cat "$$pidfile"); \
				name=$$(basename "$$pidfile" .pid); \
				if kill -0 "$$pid" 2>/dev/null; then \
					kill "$$pid" 2>/dev/null && echo -e "  Stopped $$name ($$pid)"; \
				fi; \
				rm -f "$$pidfile"; \
			fi \
		done; \
	fi
	@$(MAKE) infra-down
	@echo -e "$(GREEN)All stopped.$(RESET)"

status: ## Show status of all services
	@echo -e "$(CYAN)=== Infrastructure ===$(RESET)"
	@cd $(INFRA) && (docker-compose ps 2>/dev/null || docker-compose -f docker-compose.redpanda.yml ps 2>/dev/null) || true
	@echo ""
	@echo -e "$(CYAN)=== Services ===$(RESET)"
	@if [ -d $(PIDS_DIR) ]; then \
		for pidfile in $(PIDS_DIR)/*.pid; do \
			if [ -f "$$pidfile" ]; then \
				pid=$$(cat "$$pidfile"); \
				name=$$(basename "$$pidfile" .pid); \
				if kill -0 "$$pid" 2>/dev/null; then \
					echo -e "  $(GREEN)RUNNING$(RESET)  $$name (pid $$pid)"; \
				else \
					echo -e "  $(YELLOW)STOPPED$(RESET)  $$name"; \
				fi \
			fi \
		done; \
	else \
		echo "  No services started via Makefile"; \
	fi

logs: ## Tail all service logs
	@exec tail -f $(PIDS_DIR)/*.log

portal: ## Open Dev Portal (service browser on :8199)
	@echo -e "$(GREEN)Dev Portal$(RESET) → http://localhost:8199"
	@cd $(ROOT)/portal && exec python3 -m http.server 8199

loadtest: ## Run load test against connector (via Kafka)
	@echo -e "$(CYAN)Running load test...$(RESET)"
	$(GRADLE) :com.etendorx.integration.obconn.loadtest:bootRun $(BOOTRUN_ARGS)

mock: ## Start mock external receiver on :8090 (for Send workflow testing)
	@echo -e "$(CYAN)Starting Mock Receiver on :8090...$(RESET)"
	$(GRADLE) :com.etendorx.integration.obconn.loadtest:bootRun --args='--spring.profiles.active=mock'

# ==============================================================================
# HELP
# ==============================================================================

.PHONY: help

help: ## Show this help
	@echo ""
	@echo -e "$(CYAN)OBConnector Development$(RESET)"
	@echo ""
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | \
		awk 'BEGIN {FS = ":.*?## "}; {printf "  $(GREEN)%-18s$(RESET) %s\n", $$1, $$2}'
	@echo ""
	@echo -e "$(CYAN)Quick start:$(RESET)"
	@echo "  make up-local       # Fastest: Redpanda + services (no Config Server)"
	@echo "  make up             # Redpanda + Config Server + services"
	@echo "  make up-kafka       # Kafka + Config Server + services"
	@echo "  make down           # Stop everything"
	@echo ""
