# Coding Conventions

**Analysis Date:** 2026-02-05

## Naming Patterns

**Files:**
- Class files use PascalCase: `MappingUtilsImpl.java`, `RestCallTransactionHandlerImpl.java`, `DefaultFilters.java`
- Implementation classes use `Impl` suffix: `MappingUtilsImpl`, `PostSyncServiceImpl`, `ExternalIdServiceImpl`
- Test classes use `Test` or `Tests` suffix: `MappingUtilsImplTest.java`, `BindedRestControllerTest.java`, `BaseDTORepositoryDefaultTests.java`
- Projection test classes use `TestProjection` suffix: `UserTestProjection.java`, `OrderTestProjection.java`
- Exception classes use `Exception` suffix: `ExternalIdException.java`

**Classes and Types:**
- Interface names are descriptive and service-oriented: `MappingUtils`, `PostSyncService`, `RestCallTransactionHandler`
- Configuration classes use `Configuration` or `Configurator` suffix: `EventHandlerAutoConfiguration.java`, `JwtClassicConfigurator.java`
- Spring component classes: `@Component`, `@Service`, `@Repository` annotations
- Strategy implementations and handlers: `EtendoNamingStrategy`, `CustomInterceptor`, `EventHandlerEntities`

**Variables:**
- camelCase for all variables and method parameters
- Constants use UPPER_SNAKE_CASE: `SUPER_USER_ID`, `SUPER_USER_CLIENT_ID`, `SELECT`, `INSERT`, `GET_METHOD`, `POST_METHOD`, `DELETE_METHOD`
- Constants are public static final in utility classes
- Private instance variables initialized in constructors or through dependency injection
- Method-local variables use descriptive camelCase: `clientId`, `userId`, `roleId`, `isActive`, `userContext`

**Methods:**
- Getter methods use `get` prefix: `getDisableStatement()`, `getEnableStatement()`, `getQueryInfo()`
- Predicate methods use `is` prefix: `isSuperUser()`, `isAuthService()`, `isTriggerEnabled()`
- Handler/processor methods use descriptive verbs: `handleBaseObject()`, `handleBaseSerializableObject()`, `handleDateObject()`, `handlePersistentBag()`
- Utility methods use `add`, `apply`, `execute`, `replace` verbs: `addFilters()`, `applyFilters()`, `executeStatement()`, `replaceInQuery()`

## Code Style

**Formatting:**
- Java 17 compatibility (sourceCompatibility = JavaVersion.VERSION_17)
- Spring Boot 3.1.4 and Spring Cloud 2022.0.4 compatibility
- Gradle build system for project structure and dependencies
- Apache License 2.0 copyright headers on all files (Copyright 2022-2024 Futit Services SL)

**Linting:**
- No explicit static analysis tool configured (no checkstyle, spotbugs, or sonarqube reference found)
- Code follows standard Java conventions and Spring Boot best practices
- NOSONAR comments used to suppress false positives in tests (line 103, 120 in DefaultFiltersTest.java)

## Import Organization

**Order:**
1. Standard Java imports (java.*, javax.*)
2. Jakarta (jakarta.*) - modern Java EE spec
3. Third-party libraries (org.*, com.*, net.*)
4. Project-specific imports (com.etendorx.*)
5. Lombok annotations imported explicitly

**Standard Package Groups:**
- Database/ORM: `javax.sql`, `java.sql`, `jakarta.persistence`, `jakarta.transaction`
- Collections: `java.util.*`
- Spring Framework: `org.springframework.boot.*`, `org.springframework.data.*`, `org.springframework.stereotype.*`, `org.springframework.transaction.*`
- Spring Cloud/Data Rest: `org.springframework.data.domain.*`, `org.springframework.boot.test.*`
- Lombok: `lombok.extern.*` for annotations like `@Slf4j`, `@Log4j2`, `@Data`, `@AllArgsConstructor`
- SQL Parsing: `net.sf.jsqlparser.*`
- Annotations: `org.jetbrains.annotations.NotNull`, `jakarta.annotation.Nullable`
- Validation: `jakarta.validation.Validator`

**Path Aliases:**
- Not detected in build configuration, uses full package paths throughout

## Error Handling

**Patterns:**
- Custom exceptions extend `RuntimeException` for unchecked exceptions: `ExternalIdException`
- Exception constructors accept message and optional cause (Throwable): `ExternalIdException(String message, Throwable cause)`
- SQL/Database errors wrapped in RuntimeException with descriptive messages: `throw new RuntimeException("An error occurred while executing the SQL statement", e)`
- Try-catch blocks used selectively for recoverable operations (date parsing, SQL execution)
- Exception messages include context and operation details: "An error occurred while executing the SQL statement"
- Switch statement default cases throw IllegalArgumentException for invalid enum/parameter values: `throw new IllegalArgumentException("Unknown HTTP method: " + restMethod)`
- Validation errors handled through Spring's `@Valid` annotation and `Validator` interface
- HTTP error responses use `ResponseStatusException` with appropriate status codes (e.g., HttpStatus.NOT_FOUND)

## Logging

**Framework:** Lombok @Slf4j (SLF4J) and @Log4j2 annotations

**Patterns:**
- Most classes use `@Slf4j` annotation to inject logger: `private static final org.slf4j.Logger log`
  - Used in: `DefaultFilters`, `MappingUtilsImpl`, `EventHandlerEntities`, `SpringContext`, `AuditServiceInterceptorImpl`, `CustomInterceptor`
- External ID services use `@Log4j2`: `PostSyncServiceImpl`, `ExternalIdServiceImpl`
- Logging operations:
  - Error logging with context: `log.error("[ processSql ] - Unknown HTTP method: " + restMethod)`
  - Errors captured in catch blocks but not always logged (comment-only in some cases)
  - No extensive logging framework configuration found in properties or XML

**Usage:**
- Error conditions logged with method context in square brackets: `[ methodName ]`
- Concatenation with String + operator used in log messages
- Logging level not explicitly configured; defaults to INFO

## Comments

**When to Comment:**
- JavaDoc comments used for public methods: `/** This class provides utility methods ... */`
- Method-level documentation explains parameters (`@param`), return values, and behavior
- Internal logic comments explain complex operations: "If the date cannot be formatted, try to format it with the user's date format"
- Class-level JavaDoc explains purpose: "A utility class for applying default filters to SQL queries based on user ID, client ID, role ID..."
- Comments on business logic and conditional branches

**JavaDoc/TSDoc:**
- Standard JavaDoc format with `/**...*/` for classes and public methods
- `@param` tags document method parameters
- `@return` tags document return values
- `@throws` tags document exceptions
- Type parameter documentation: `<[Name]>` for generics
- All classes in main source include Apache License copyright header with year range
- Comments at line 45-48 in DefaultFilters.java explain constructor throwing: "Private constructor to prevent instantiation of the DefaultFilters utility class"

## Function Design

**Size:**
- Methods range from 5-50 lines, with most utility methods between 10-30 lines
- Single responsibility principle: each method handles one specific task
- Complex operations split into private helper methods:
  - `DefaultFilters.addFilters()` (public API) delegates to `getDefaultWhereClause()`, `applyFilters()`, `getQueryInfo()`
  - `MappingUtilsImpl.handleBaseObject()` delegates to `handleBaseSerializableObject()`, `handlePersistentBag()`, `handleDateObject()`

**Parameters:**
- Constructor injection for Spring components: dependencies passed to constructor, assigned to final fields
- Method parameters explicitly named and documented in JavaDoc: `userId`, `clientId`, `roleId`, `isActive`, `restMethod`
- Boolean flags used to control behavior: `isActive`, `isActiveFilter`, `isTriggerEnabled`
- Varargs and collections used where multiple values expected
- No builder patterns detected; direct constructor injection preferred

**Return Values:**
- Methods return specific types (String, Object, Boolean, Page<T>, ResponseEntity<T>)
- Optional pattern used in repositories: `Optional.of()`, `Optional.empty()`
- Null handling: methods check for null and return appropriately
- Wrapped responses for REST: `ResponseEntity<T>` with HTTP status
- Generic types used for DTOs: `BaseDTOModel`, `BaseDTORepositoryDefault<Entity, ReadDTO, WriteDTO>`

## Module Design

**Exports:**
- Spring Boot auto-configuration: classes marked with `@Configuration` or `@AutoConfiguration` are auto-discovered
- Component scanning configured in `@SpringBootApplication` with basePackages
- Bean definitions using `@Bean` methods in configuration classes
- Repositories exposed through Spring Data JPA interfaces extending `JpaRepository` or `BaseDTORepositoryDefault`
- Services exposed through `@Service` or `@Component` annotations

**Barrel Files:**
- Not detected; imports use full paths from source tree
- Test suite files collect multiple test classes: `EtendoRXUnitTestsSuite.java`, `EtendoRXSpringBootTestsSuite.java`
  - Use `@Suite`, `@SelectClasses`, `@SuiteDisplayName` annotations from JUnit Platform Suite
  - Organize tests by type: unit tests in one suite, integration/spring boot tests in another

**Package Organization:**
- Layered package structure:
  - `com.etendorx.das` - main DAS module
  - `com.etendorx.das.handler` - Spring lifecycle handlers and configuration
  - `com.etendorx.das.utils` - utility classes (DefaultFilters, MappingUtils, MetadataUtil)
  - `com.etendorx.das.hibernate_interceptor` - database interceptors and auditing
  - `com.etendorx.das.externalid` - external ID service implementations
  - `com.etendorx.das.connector` - external integrations (OBCon field mapping)
  - `com.etendorx.das.configuration` - Spring configuration beans
- Test structure mirrors main packages with `test` subdirectories
- Test-specific packages: `com.etendorx.das.unit`, `com.etendorx.das.test`, `com.etendorx.das.test.projections`

---

*Convention analysis: 2026-02-05*
