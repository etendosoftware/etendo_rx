# Codebase Structure

**Analysis Date:** 2026-02-05

## Directory Layout

```
etendo_rx/
├── _buildSrc/                                 # Gradle build source directory
├── bin/                                       # Compiled output executables
├── build/                                     # Build artifacts (generated, temporary)
├── chroma.db/                                 # Chromatic database (vector embeddings)
├── config/                                    # Static configuration files
├── docs/                                      # Documentation
├── gradle/                                    # Gradle wrapper
├── libs/                                      # Shared library modules
│   ├── com.etendorx.clientrest_core/        # REST client core library
│   ├── com.etendorx.das_core/               # DAS core library
│   ├── com.etendorx.generate_entities/      # Entity generation library
│   ├── com.etendorx.generate_entities.core/ # Entity generation core
│   ├── com.etendorx.generate_entities.extradomaintype/ # Domain type extensions
│   ├── com.etendorx.lib.asyncprocess/       # Async processing library
│   ├── com.etendorx.lib.kafka/              # Kafka integration library
│   ├── com.etendorx.utils.auth/             # Authentication utilities
│   └── com.etendorx.utils.common/           # Common utilities
├── logs/                                      # Runtime log files
├── modules/                                   # Integration and application modules
│   ├── com.etendoerp.etendorx/              # ERP-specific module
│   ├── com.etendorx.auth.client/            # Auth client for services
│   ├── com.etendorx.integration.obconnector/       # Obconnector integration
│   ├── com.etendorx.integration.to_openbravo/     # OpenBravo sync integration
│   ├── com.etendorx.integration.mobilesync/       # Mobile sync integration
│   ├── com.etendorx.integration.petclinic/        # PetClinic demo integration
│   ├── com.etendorx.mapping.tutorial/      # Mapping tutorial module
│   ├── com.etendorx.subapp.product/        # Product sub-application
│   ├── com.tutorial.mappings/               # Tutorial mappings
│   └── com.tutorial.rxtutorial/             # RX tutorial module
├── modules_bk/                                # Backup modules (not used)
├── modules_core/                              # Core microservices
│   ├── com.etendorx.asyncprocess/           # Async job processing service
│   ├── com.etendorx.auth/                   # JWT authentication service
│   ├── com.etendorx.configserver/           # Spring Cloud Config Server
│   ├── com.etendorx.das/                    # Data access & sync service
│   ├── com.etendorx.edge/                   # API Gateway (Spring Cloud Gateway)
│   └── com.etendorx.webflux/                # WebFlux reactive service
├── modules_gen/                               # Generated modules
│   ├── com.etendorx.clientrest/             # Generated REST client
│   ├── com.etendorx.entities/               # Generated entity classes
│   ├── com.etendorx.entitiesModel/          # Generated entity models
│   └── com.etendorx.grpc.common/            # Generated gRPC common definitions
├── modules_test/                              # Test support modules
│   ├── com.etendorx.test.eventhandler/      # Event handler test utilities
│   ├── com.etendorx.test.grpc/              # gRPC test containers
│   └── com.etendorx.test.testcontainer/     # Spring test containers
├── pipelines/                                 # CI/CD pipeline definitions
│   ├── publish-snapshots/                    # Snapshot publication pipeline
│   └── run-tests/                            # Test execution pipeline
├── resources/                                 # Resource files
│   ├── docker-images/                        # Docker build files
│   ├── dynamic-das/                          # Dynamic DAS resources
│   └── dynamic-gradle/                       # Dynamic Gradle resources
├── rxconfig/                                  # RX runtime configuration
│   ├── application.yaml                      # Main app configuration
│   ├── auth.yaml                             # Auth service config
│   ├── asyncprocess.yaml                     # Async process config
│   ├── das.yaml                              # DAS service config
│   ├── edge.yaml                             # Edge gateway config
│   ├── obconnector.yaml                      # Obconnector config
│   ├── server.yaml                           # Server config
│   └── worker.yaml                           # Worker config
├── src/                                       # Root-level source (minimal)
│   └── main/java/com/etendorx/Main.java     # Placeholder main class
├── .planning/                                 # GSD planning documentation
│   └── codebase/                             # Codebase analysis docs
├── build.gradle                               # Root Gradle build configuration
├── gradle.properties                          # Gradle project properties
├── gradle.properties.template                 # Template for gradle.properties
├── settings.gradle                            # Gradle multi-project configuration
├── README.md                                  # Project README
└── CONNECTORS.md                              # Connector documentation
```

## Directory Purposes

**libs/:**
- Purpose: Reusable shared libraries used by multiple services and modules
- Contains: Utility classes, library implementations, common configurations
- Key files: Build configurations for each library module

**modules_core/:**
- Purpose: Core microservices that form the backbone of Etendo RX
- Contains: Main application classes, controllers, service logic, configuration
- Key files: `build.gradle`, `*Application.java` entry points, `src/main/java` source trees

**modules_gen/:**
- Purpose: Auto-generated code from configuration or code generation tools
- Contains: Entity classes, REST client stubs, gRPC definitions
- Key files: Generated source directories, not typically hand-edited

**modules/:**
- Purpose: Domain-specific integration and application modules
- Contains: Integration-specific workers, API implementations, tutorial code
- Key files: `build.gradle`, application-specific source code

**modules_test/:**
- Purpose: Testing infrastructure and test utilities
- Contains: TestContainer configurations, test utilities, mocking frameworks
- Key files: Test infrastructure beans, annotation processors

**rxconfig/:**
- Purpose: Runtime configuration for all services
- Contains: YAML files defining service behavior, database connections, Kafka topics, API routes
- Key files: `application.yaml` (shared config), service-specific YAML files

**pipelines/:**
- Purpose: CI/CD pipeline definitions for GitHub Actions
- Contains: YAML workflow definitions
- Key files: `.github/workflows/` linked configurations

## Key File Locations

**Entry Points:**
- `modules_core/com.etendorx.auth/src/main/java/com/etendorx/auth/JwtauthApplication.java`: Auth service bootstrap
- `modules_core/com.etendorx.edge/src/main/java/com/etendorx/edge/EdgeApplication.java`: Edge gateway bootstrap
- `modules_core/com.etendorx.das/src/main/java/com/etendorx/das/EtendorxDasApplication.java`: DAS service bootstrap
- `modules_core/com.etendorx.asyncprocess/src/main/java/com/etendorx/asyncprocess/AsyncProcessDbApp.java`: AsyncProcess bootstrap
- `modules_core/com.etendorx.configserver/src/main/java/com/etendorx/configserver/ConfigServerApplication.java`: ConfigServer bootstrap

**Configuration:**
- `rxconfig/application.yaml`: Central application configuration (public keys, service URLs, OpenTelemetry)
- `rxconfig/auth.yaml`: Auth service configuration (OAuth2, JWT settings)
- `rxconfig/das.yaml`: DAS service configuration (database connection, gRPC)
- `rxconfig/edge.yaml`: Edge gateway routes and JWT configuration
- `rxconfig/asyncprocess.yaml`: Kafka and async processing configuration
- `rxconfig/obconnector.yaml`: OBConnector integration configuration
- `build.gradle`: Root build configuration (version, plugins, JaCoCo, test configs)
- `settings.gradle`: Multi-project build configuration (includes all modules)
- `gradle.properties.template`: Template for Gradle properties (GitHub credentials, repository URL)

**Core Logic:**
- `modules_core/com.etendorx.auth/src/main/java/com/etendorx/auth/auth/AuthController.java`: Auth endpoints
- `modules_core/com.etendorx.auth/src/main/java/com/etendorx/auth/auth/jwt/JwtService.java`: JWT generation/validation
- `modules_core/com.etendorx.edge/src/main/java/com/etendorx/edge/filters/auth/JwtAuthenticationFilter.java`: JWT validation filter
- `modules_core/com.etendorx.das/src/main/java/com/etendorx/das/handler/EventHandlerEntities.java`: Event handler registry
- `modules_core/com.etendorx.das/src/main/java/com/etendorx/das/externalid/ExternalIdServiceImpl.java`: External ID tracking
- `modules_core/com.etendorx.asyncprocess/src/main/java/com/etendorx/asyncprocess/service/AsyncProcessService.java`: Async job execution

**Testing:**
- `modules_core/com.etendorx.auth/src/test/java/com/etendorx/auth/`: Auth service tests
- `modules_core/com.etendorx.das/src/test/java/com/etendorx/das/`: DAS service tests
- `modules_test/com.etendorx.test.testcontainer/src/`: TestContainer configurations

## Naming Conventions

**Files:**
- Application entry points: `*Application.java` (e.g., `EtendorxDasApplication.java`)
- Controllers: `*Controller.java` (e.g., `AuthController.java`, `AsyncProcessController.java`)
- Services: `*Service.java` or `*ServiceImpl.java` (e.g., `JwtService.java`, `ExternalIdServiceImpl.java`)
- Interfaces: `*Service.java` or short name without "Impl" suffix
- Configuration classes: `*Configuration.java` or `*Configurator.java`
- Test classes: `*Test.java` (e.g., `AuthControllerTest.java`)
- Module names: Reverse domain notation with dots (e.g., `com.etendorx.asyncprocess`)

**Directories:**
- Java packages mirror module structure: `com/etendorx/[module]/[layer]`
- Core packages: `src/main/java`, test packages: `src/test/java`
- Configuration: `src/main/resources` or `rxconfig/`
- Generated sources: `src-gen/main/java`, `src/generated/java`

## Where to Add New Code

**New Feature in existing service:**
- Primary code: `modules_core/[service]/src/main/java/com/etendorx/[service]/[layer]/`
- Tests: `modules_core/[service]/src/test/java/com/etendorx/[service]/[layer]/`
- Example: New DAS feature goes in `modules_core/com.etendorx.das/src/main/java/com/etendorx/das/[layer]/`

**New Integration Module:**
- Implementation: `modules/com.etendorx.integration.[name]/src/main/java/com/etendorx/integration/[name]/`
- Configuration: `modules/com.etendorx.integration.[name]/build.gradle`, config files in `rxconfig/`
- Tests: `modules/com.etendorx.integration.[name]/src/test/java/`

**New Shared Library:**
- Implementation: `libs/com.etendorx.lib.[name]/src/main/java/com/etendorx/lib/[name]/`
- Configuration: `libs/com.etendorx.lib.[name]/build.gradle`
- Tests: `libs/com.etendorx.lib.[name]/src/test/java/`

**Event Handler for Integration:**
- Location: `modules/com.etendorx.integration.[name]/src/main/java/[package]/handler/`
- Implement: `com.etendorx.das.handler.EventHandler` interface
- Register: Auto-discovered by component scan or added to `EventHandlerEntities.java`

**New REST API Endpoint:**
- Controller: `modules_core/com.etendorx.[service]/src/main/java/com/etendorx/[service]/controller/`
- Service: `modules_core/com.etendorx.[service]/src/main/java/com/etendorx/[service]/service/`
- Example: Auth endpoints in `modules_core/com.etendorx.auth/src/main/java/com/etendorx/auth/auth/`

## Special Directories

**build/:**
- Purpose: Temporary build artifacts
- Generated: Yes
- Committed: No (in .gitignore)
- Contains: Compiled classes, JAR files, reports

**modules_gen/:**
- Purpose: Generated source code
- Generated: Yes (from code generation tasks)
- Committed: Yes (generated sources tracked in repo)
- Contains: Entity POJOs, REST client stubs, gRPC definitions

**rxconfig/:**
- Purpose: Runtime configuration for services
- Generated: No (manually maintained)
- Committed: Yes
- Contains: YAML files for each service, template files with .template extension

**.planning/codebase/:**
- Purpose: GSD codebase analysis documentation
- Generated: Yes (from `/gsd:map-codebase` command)
- Committed: Yes
- Contains: ARCHITECTURE.md, STRUCTURE.md, CONVENTIONS.md, TESTING.md, STACK.md, INTEGRATIONS.md, CONCERNS.md

---

*Structure analysis: 2026-02-05*
