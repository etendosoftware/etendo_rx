# Phase 1: Dynamic Metadata Service - Research

**Researched:** 2026-02-05
**Domain:** Spring Boot 3 metadata caching service with JPA entity relationships
**Confidence:** HIGH

## Summary

This phase requires building a DynamicMetadataService that loads projection/entity/field metadata from database tables (`etrx_projection`, `etrx_projection_entity`, `etrx_entity_field`) at runtime and caches it in memory. The service must support four field mapping types (DM, JM, CV, JP) and provide query APIs for downstream components.

The codebase already has:
1. **Generated JPA entities** for all three metadata tables with proper relationships
2. **Existing repositories** (ETRX_ProjectionRepository, ETRX_Entity_FieldRepository) extending BaseDASRepository
3. **Current MetadataUtil** infrastructure that provides field metadata from EntityMetadata beans
4. **Spring Boot 3.1.4** with Jakarta Persistence (JPA 3.0+)

The new DynamicMetadataService should complement (not replace) the existing MetadataUtil, providing runtime projection metadata while MetadataUtil continues serving generated entity metadata.

**Primary recommendation:** Use Spring Cache abstraction with Caffeine provider for in-memory caching, Java records for immutable metadata models, and constructor injection pattern for service dependencies. Load cache at startup via @PostConstruct and provide manual invalidation via @CacheEvict.

## Standard Stack

The established libraries/tools for this domain:

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Spring Boot | 3.1.4 | Application framework | Already in use, provides caching infrastructure |
| Spring Data JPA | 3.1.x (from Spring Boot) | Repository abstraction | Already in use, BaseDASRepository pattern established |
| Caffeine | 3.1.x | In-memory cache provider | Recommended by Spring Boot, high performance, successor to Guava |
| Jakarta Persistence API | 3.1.x | JPA standard | Required for Spring Boot 3.x |
| Lombok | Latest | Boilerplate reduction | Already in use (ETRXProjection entities use @Getter/@Setter) |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| spring-boot-starter-cache | 3.1.4 | Spring caching support | Required for @Cacheable/@CacheEvict |
| JUnit 5 | 5.9.x | Testing framework | Unit tests (from spring-boot-starter-test) |
| Mockito | 5.x | Mocking framework | Repository mocking (from spring-boot-starter-test) |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Caffeine | Redis | Redis adds network latency, requires external service. Overkill for single-instance metadata cache |
| Caffeine | Ehcache | Ehcache is mature but Caffeine has better performance and Spring Boot auto-configures it |
| Java Records | POJOs | Records provide immutability, but POJOs offer more flexibility. Records preferred for cache data |

**Installation:**
```bash
# Add to build.gradle dependencies
implementation 'org.springframework.boot:spring-boot-starter-cache:3.1.4'
implementation 'com.github.ben-manes.caffeine:caffeine:3.1.8'
```

## Architecture Patterns

### Recommended Project Structure
```
modules_core/com.etendorx.das/src/main/java/com/etendorx/das/
├── metadata/
│   ├── DynamicMetadataService.java        # Main service interface
│   ├── DynamicMetadataServiceImpl.java    # Implementation
│   ├── models/
│   │   ├── ProjectionMetadata.java        # Immutable projection model (record)
│   │   ├── EntityMetadata.java            # Immutable entity model (record)
│   │   └── FieldMetadata.java             # Immutable field model (record)
│   └── config/
│       └── MetadataCacheConfig.java       # Cache configuration
└── test/
    └── metadata/
        └── DynamicMetadataServiceTest.java
```

### Pattern 1: Service Layer with Constructor Injection
**What:** Spring @Service with final repository dependencies injected via constructor
**When to use:** All service classes in Spring Boot 3
**Example:**
```java
// Source: Spring Boot official documentation + codebase pattern
@Service
public class DynamicMetadataServiceImpl implements DynamicMetadataService {
    private final ETRX_ProjectionRepository projectionRepository;
    private final ETRX_Entity_FieldRepository fieldRepository;

    // No @Autowired needed for single constructor (Spring 4.3+)
    public DynamicMetadataServiceImpl(
        ETRX_ProjectionRepository projectionRepository,
        ETRX_Entity_FieldRepository fieldRepository
    ) {
        this.projectionRepository = projectionRepository;
        this.fieldRepository = fieldRepository;
    }
}
```

### Pattern 2: Immutable Metadata Models with Java Records
**What:** Use Java records for cache data structures
**When to use:** Data that should be immutable after loading from DB
**Example:**
```java
// Source: Java 16+ best practices
public record ProjectionMetadata(
    String id,
    String name,
    boolean grpc,
    List<EntityMetadata> entities
) {}

public record EntityMetadata(
    String id,
    String name,
    String tableId,
    boolean identity,
    List<FieldMetadata> fields
) {}

public record FieldMetadata(
    String id,
    String name,
    String property,
    String fieldMapping,  // "DM", "JM", "CV", "JP"
    boolean mandatory,
    String javaMappingQualifier,
    String constantValue,
    String jsonPath,
    String relatedEntityId
) {}
```

### Pattern 3: Spring Cache Abstraction with Caffeine
**What:** Use @Cacheable, @CacheEvict with Caffeine as provider
**When to use:** Method-level caching with automatic key generation
**Example:**
```java
// Source: Spring Boot caching documentation
@Service
public class DynamicMetadataServiceImpl implements DynamicMetadataService {

    @Cacheable(value = "projections", key = "#name")
    public Optional<ProjectionMetadata> getProjection(String name) {
        return projectionRepository.findByName(name)
            .map(this::toProjectionMetadata);
    }

    @CacheEvict(value = "projections", allEntries = true)
    public void invalidateCache() {
        // Cache automatically cleared by annotation
    }
}
```

### Pattern 4: Cache Configuration
**What:** Configure Caffeine via Spring Boot properties or Java config
**When to use:** Always - provides control over cache behavior
**Example:**
```java
// Source: Spring Boot Caffeine documentation
@Configuration
@EnableCaching
public class MetadataCacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("projections", "entities", "fields");
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofHours(24))
            .recordStats());
        return cacheManager;
    }
}
```

### Pattern 5: Startup Cache Preloading
**What:** Load all projections into cache at application startup
**When to use:** When cache must be ready before first request
**Example:**
```java
// Source: Spring Boot initialization best practices
@Service
public class DynamicMetadataServiceImpl implements DynamicMetadataService {

    @PostConstruct
    public void preloadCache() {
        log.info("Preloading projection metadata cache...");
        List<ETRXProjection> allProjections = projectionRepository.findAll();
        allProjections.forEach(p -> getProjection(p.getName())); // Triggers @Cacheable
        log.info("Loaded {} projections into cache", allProjections.size());
    }
}
```

### Anti-Patterns to Avoid
- **Mutable cache objects:** Never modify returned metadata objects - use immutable records to prevent cache corruption
- **Caching entity objects directly:** Transform JPA entities to immutable DTOs/records before caching to avoid lazy-loading issues
- **Ignoring N+1 queries:** Use @EntityGraph or JOIN FETCH when loading projection hierarchies to avoid multiple DB queries
- **@Autowired field injection:** Use constructor injection instead - better testability and makes dependencies explicit

## Don't Hand-Roll

Problems that look simple but have existing solutions:

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Cache key generation | Custom String concatenation | Spring @Cacheable with SpEL | Spring handles null parameters, complex keys, and method signature changes automatically |
| Cache eviction | Manual Map.clear() calls | @CacheEvict with allEntries=true | Spring provides declarative cache clearing, supports multiple caches, integrates with transactions |
| JPA entity to DTO conversion | Manual field copying loops | Record pattern + constructor/builder | Records provide immutability, equals/hashCode, toString automatically. Less error-prone than manual copying |
| Repository query methods | Custom JPQL for simple lookups | Spring Data method naming | `Optional<ETRXProjection> findByName(String name)` auto-generates query, handles null safety |
| Testing with database | Real database for unit tests | @DataJpaTest + TestEntityManager | Spring Boot provides H2 in-memory DB, transaction rollback, and faster test execution |

**Key insight:** Spring Boot 3's caching abstraction handles 90% of caching complexity (key generation, eviction strategies, statistics, provider switching). Building a custom cache means reimplementing thread safety, memory management, and eviction policies that Caffeine already provides.

## Common Pitfalls

### Pitfall 1: Lazy Loading Exceptions in Cached Objects
**What goes wrong:** Caching JPA entity objects directly, then accessing lazy-loaded relationships outside transaction scope causes LazyInitializationException
**Why it happens:** Hibernate proxies can't fetch data when entity is detached from persistence context (i.e., after being returned from cache)
**How to avoid:**
- Transform JPA entities to immutable records/DTOs BEFORE caching
- Eagerly fetch all needed relationships during initial load
- Use `@EntityGraph` or JOIN FETCH in repository queries
**Warning signs:** LazyInitializationException in logs, NullPointerException on relationship access

### Pitfall 2: Cache Not Working Due to Self-Invocation
**What goes wrong:** Calling `@Cacheable` method from another method in same class bypasses Spring's proxy, cache never triggers
**Why it happens:** Spring AOP uses proxies, self-calls don't go through proxy layer
**How to avoid:**
- Inject the service into itself via `@Lazy` autowiring OR
- Extract cached methods to separate component OR
- Use AspectJ compile-time weaving (complex, rarely needed)
**Warning signs:** Cache statistics show 0 hits, method always executes even with same parameters

### Pitfall 3: Incorrect Field Mapping Type Handling
**What goes wrong:** Not checking `fieldMapping` value before accessing related fields (e.g., accessing `javaMapping` when fieldMapping="CV")
**Why it happens:** ETRXEntityField has multiple nullable relationships - only one is populated based on `fieldMapping` value
**How to avoid:**
- Use switch/pattern matching on `fieldMapping` value: "DM" (direct), "JM" (java mapping), "CV" (constant value), "JP" (json path)
- Validate that expected relationship is non-null before dereferencing
- Create separate record types for each mapping type OR use nullable fields with clear documentation
**Warning signs:** NullPointerException when accessing javaMapping/constantValue/relatedEntity

### Pitfall 4: N+1 Query Problem When Loading Projections
**What goes wrong:** Loading projection loads entities one-by-one, then fields one-by-one, causing hundreds of DB queries
**Why it happens:** Default JPA FetchType.LAZY loads relationships on-demand
**How to avoid:**
```java
// BAD: Causes N+1 queries
@Query("SELECT p FROM ETRX_Projection p")
List<ETRXProjection> findAll();

// GOOD: Single query with joins
@Query("SELECT DISTINCT p FROM ETRX_Projection p " +
       "LEFT JOIN FETCH p.eTRXProjectionEntityList e " +
       "LEFT JOIN FETCH e.eTRXEntityFieldList")
List<ETRXProjection> findAllWithEntitiesAndFields();
```
**Warning signs:** Hibernate SQL logs show hundreds of SELECT statements, slow cache preload time

### Pitfall 5: Using @Cacheable on @PostConstruct Method
**What goes wrong:** Cache proxy may not be fully initialized during @PostConstruct phase, caching silently fails
**Why it happens:** Spring creates cache infrastructure after bean construction but before full initialization
**How to avoid:**
- Call cached methods from @PostConstruct (not on @PostConstruct itself)
- Or use @EventListener(ApplicationReadyEvent.class) for guaranteed cache availability
**Warning signs:** Cache empty after startup, but works fine on subsequent calls

### Pitfall 6: Forgetting to Enable Caching
**What goes wrong:** @Cacheable annotations ignored, cache never stores anything
**Why it happens:** Spring requires explicit @EnableCaching on configuration class
**How to avoid:** Add @EnableCaching to main application class or dedicated @Configuration class
**Warning signs:** No cache-related logs on startup, cache statistics always show 0 entries

## Code Examples

Verified patterns from official sources:

### Loading Projection with All Relationships (Avoid N+1)
```java
// Source: Spring Data JPA best practices
@Repository
public interface ETRX_ProjectionRepository extends BaseDASRepository<ETRXProjection> {

    // Single query loads entire projection hierarchy
    @Query("SELECT DISTINCT p FROM ETRX_Projection p " +
           "LEFT JOIN FETCH p.eTRXProjectionEntityList e " +
           "LEFT JOIN FETCH e.eTRXEntityFieldList " +
           "WHERE p.name = :name")
    Optional<ETRXProjection> findByNameWithRelations(@Param("name") String name);

    @Query("SELECT DISTINCT p FROM ETRX_Projection p " +
           "LEFT JOIN FETCH p.eTRXProjectionEntityList e " +
           "LEFT JOIN FETCH e.eTRXEntityFieldList")
    List<ETRXProjection> findAllWithRelations();
}
```

### Service Implementation with Caching
```java
// Source: Spring Boot caching + codebase patterns
@Service
@Slf4j
public class DynamicMetadataServiceImpl implements DynamicMetadataService {

    private final ETRX_ProjectionRepository projectionRepository;

    public DynamicMetadataServiceImpl(ETRX_ProjectionRepository projectionRepository) {
        this.projectionRepository = projectionRepository;
    }

    @PostConstruct
    public void preloadCache() {
        log.info("Preloading projection metadata cache...");
        List<ETRXProjection> projections = projectionRepository.findAllWithRelations();
        projections.forEach(p -> getProjection(p.getName())); // Triggers @Cacheable
        log.info("Loaded {} projections into cache", projections.size());
    }

    @Override
    @Cacheable(value = "projections", key = "#name")
    public Optional<ProjectionMetadata> getProjection(String name) {
        log.debug("Loading projection from DB: {}", name);
        return projectionRepository.findByNameWithRelations(name)
            .map(this::toProjectionMetadata);
    }

    @Override
    @CacheEvict(value = "projections", allEntries = true)
    public void invalidateCache() {
        log.info("Cache invalidated - all projection metadata cleared");
    }

    private ProjectionMetadata toProjectionMetadata(ETRXProjection entity) {
        return new ProjectionMetadata(
            entity.getId(),
            entity.getName(),
            entity.getGRPC(),
            entity.getETRXProjectionEntityList().stream()
                .map(this::toEntityMetadata)
                .toList()
        );
    }

    private EntityMetadata toEntityMetadata(ETRXProjectionEntity entity) {
        return new EntityMetadata(
            entity.getId(),
            entity.getName(),
            entity.getTableEntity().getId(),
            entity.getIdentity(),
            entity.getETRXEntityFieldList().stream()
                .map(this::toFieldMetadata)
                .toList()
        );
    }

    private FieldMetadata toFieldMetadata(ETRXEntityField field) {
        // Handle different mapping types
        return new FieldMetadata(
            field.getId(),
            field.getName(),
            field.getProperty(),
            field.getFieldMapping(), // "DM", "JM", "CV", "JP"
            field.getIsmandatory(),
            // Only populated for JM type
            field.getJavaMapping() != null ? field.getJavaMapping().getQualifier() : null,
            // Only populated for CV type
            field.getEtrxConstantValue() != null ? field.getEtrxConstantValue().getDefaultValue() : null,
            // Only populated for JP type
            field.getJsonpath(),
            // Only populated for related entity mappings
            field.getEtrxProjectionEntityRelated() != null ? field.getEtrxProjectionEntityRelated().getId() : null
        );
    }
}
```

### Testing with @DataJpaTest
```java
// Source: Spring Boot testing best practices
@DataJpaTest
class DynamicMetadataServiceTest {

    @Autowired
    private ETRX_ProjectionRepository projectionRepository;

    @Autowired
    private TestEntityManager entityManager;

    private DynamicMetadataService service;

    @BeforeEach
    void setUp() {
        service = new DynamicMetadataServiceImpl(projectionRepository);
    }

    @Test
    void testGetProjection_Found() {
        // Arrange
        ETRXProjection projection = new ETRXProjection();
        projection.setName("TestProjection");
        projection.setGRPC(true);
        entityManager.persistAndFlush(projection);

        // Act
        Optional<ProjectionMetadata> result = service.getProjection("TestProjection");

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().name()).isEqualTo("TestProjection");
        assertThat(result.get().grpc()).isTrue();
    }
}
```

### Mock-Based Unit Testing
```java
// Source: Spring Boot testing patterns
@ExtendWith(MockitoExtension.class)
class DynamicMetadataServiceUnitTest {

    @Mock
    private ETRX_ProjectionRepository projectionRepository;

    @InjectMocks
    private DynamicMetadataServiceImpl service;

    @Test
    void testGetProjection_NotFound() {
        // Arrange
        when(projectionRepository.findByNameWithRelations("NonExistent"))
            .thenReturn(Optional.empty());

        // Act
        Optional<ProjectionMetadata> result = service.getProjection("NonExistent");

        // Assert
        assertThat(result).isEmpty();
        verify(projectionRepository).findByNameWithRelations("NonExistent");
    }
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Guava Cache | Caffeine Cache | Spring Boot 2.0+ | Caffeine provides better performance, eviction policies, and statistics. Spring Boot auto-configures Caffeine over Guava |
| javax.persistence | jakarta.persistence | Spring Boot 3.0 | Namespace change from javax to jakarta for JPA, all imports must update |
| @PostConstruct from javax | @PostConstruct from jakarta | Spring Boot 3.0 | Same behavior, different package (jakarta.annotation vs javax.annotation) |
| Field injection (@Autowired) | Constructor injection | Spring 4.3+ | Constructor injection preferred, no @Autowired needed for single constructor |
| POJO with getters/setters | Java Records | Java 16+ | Records provide immutability, reduce boilerplate by 80%, better for cache data |

**Deprecated/outdated:**
- **Guava Cache:** Replaced by Caffeine - Spring Boot 3 no longer auto-configures Guava
- **Simple ConcurrentHashMap cache:** Use Spring Cache abstraction - provides provider flexibility, statistics, and declarative API
- **Manual @EntityGraph configuration:** Spring Data JPA now supports `@EntityGraph` annotation directly on repository methods

## Open Questions

Things that couldn't be fully resolved:

1. **Relationship between MetadataUtil and DynamicMetadataService**
   - What we know: MetadataUtil provides FieldMetadata from EntityMetadata beans (generated code), DynamicMetadataService will load from etrx_* tables
   - What's unclear: Should DynamicMetadataService implement MetadataUtil interface, or be a separate parallel service?
   - Recommendation: Keep separate - MetadataUtil serves generated entities, DynamicMetadataService serves runtime projections. They have different data sources and purposes.

2. **Cache invalidation trigger mechanism**
   - What we know: Requirements specify "manual trigger initially, later event-driven"
   - What's unclear: What constitutes "manual trigger" - REST endpoint, admin UI, scheduled task?
   - Recommendation: Provide REST endpoint for Phase 1 (`POST /api/metadata/invalidate`), defer event-driven triggers to later phases.

3. **Field mapping type validation**
   - What we know: Four types exist (DM, JM, CV, JP), stored as String in `field_mapping` column with default "DM"
   - What's unclear: Are there enum constants defined somewhere, or should we create them?
   - Recommendation: Create FieldMappingType enum in DynamicMetadataService module with values: DIRECT_MAPPING("DM"), JAVA_MAPPING("JM"), CONSTANT_VALUE("CV"), JSON_PATH("JP"). Use for type safety.

4. **getProjectionEntity vs getFields method signatures**
   - What we know: Requirements specify `getProjectionEntity(projectionName, entityName)` and `getFields(projectionEntityId)`
   - What's unclear: Should getProjectionEntity return full EntityMetadata or just navigate to it?
   - Recommendation: `getProjectionEntity` should return Optional<EntityMetadata>, not require DB lookup if projection already cached. `getFields` returns List<FieldMetadata> for given entity ID.

## Sources

### Primary (HIGH confidence)
- [Spring Boot Caching Documentation](https://docs.spring.io/spring-boot/reference/io/caching.html) - Official Spring Boot 3 caching reference
- [Spring Data JPA Projections](https://docs.spring.io/spring-data/jpa/reference/repositories/projections.html) - Projection patterns and EntityGraph usage
- [Caffeine Cache with Spring Boot](https://www.baeldung.com/spring-boot-caffeine-cache) - Integration guide for Caffeine provider
- [Constructor Injection Best Practices](https://docs.spring.io/spring-boot/reference/using/spring-beans-and-dependency-injection.html) - Official Spring Boot DI patterns
- Codebase analysis: ETRXProjection, ETRXProjectionEntity, ETRXEntityField JPA entities (generated code)
- Codebase analysis: ETRX_ProjectionRepository, BaseDASRepository patterns

### Secondary (MEDIUM confidence)
- [Locality Aware Caching in Spring Boot Clusters](https://medium.com/@AlexanderObregon/locality-aware-caching-in-spring-boot-clusters-dc54a5747224) - Jan 2026, metadata cache use cases
- [Caching Data with Spring Cache, Spring Data JPA, Jakarta Persistence](https://medium.com/oracledevs/caching-data-with-spring-cache-spring-data-jpa-jakarta-persistence-and-the-oracle-ai-database-2229d822f871) - Jan 2026, class-level cache configuration
- [Spring Boot @DataJpaTest Testing](https://www.bezkoder.com/spring-boot-unit-test-jpa-repo-datajpatest/) - Repository testing patterns
- [Java Records Best Practices](https://www.javacodegeeks.com/2025/07/java-record-classes-best-practices-and-real-world-use-cases.html) - 2025 guide on immutable data carriers
- [Cache Pre-heating in Spring Boot](https://medium.com/@umeshcapg/cache-pre-heating-in-spring-boot-3a032c1408cf) - @PostConstruct preload patterns
- [Spring Boot Startup Optimization](https://oneuptime.com/blog/post/2026-02-01-spring-boot-startup-optimization/view) - Feb 2026, initialization best practices

### Tertiary (LOW confidence)
- WebSearch results on Spring Boot caching patterns - general best practices, not version-specific
- WebSearch results on JPA caching strategies - some pre-Jakarta namespace examples

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - Codebase already uses Spring Boot 3.1.4, JPA entities exist, Caffeine is Spring Boot recommended
- Architecture: HIGH - Patterns verified against Spring Boot official docs and existing codebase (MetadataUtilImpl, OBCONFieldMapping)
- Pitfalls: HIGH - N+1 queries, lazy loading, self-invocation are documented Spring/Hibernate issues with known solutions
- Field mapping types: MEDIUM - Confirmed DM/JM/CV/JP exist in code (ETRXEntityField default="DM", OBCONFieldMapping usage), but no enum/constants found

**Research date:** 2026-02-05
**Valid until:** 2026-03-05 (30 days - stable technology stack)
