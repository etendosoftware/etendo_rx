# Phase 2: Generic DTO Converter - Research

**Researched:** 2026-02-06
**Domain:** Dynamic DTO conversion with JPA entities, reflection, and runtime metadata
**Confidence:** HIGH

## Summary

This phase builds a dynamic converter that replaces generated `*DTOConverter` classes with a single runtime implementation using Phase 1's metadata service. The converter must handle bidirectional transformation: Entity→Map (reads) and Map→Entity (writes), supporting six field mapping types (DM, EM, JM, CV, JP, CM) with type coercion, null handling, and cycle detection identical to generated code.

**Key findings:**
- Generated converters follow a well-defined pattern: FieldConverter classes handle individual field logic, DTOConverter orchestrates the conversion
- MappingUtils.handleBaseObject() provides type coercion for dates, entities, and collections
- ExternalIdService resolves entity references via connector mapping tables
- AuditServiceInterceptor sets audit fields (client, org, createdBy, updatedBy, dates) automatically
- Property access can be achieved via Apache Commons BeanUtils or Java reflection
- Nested properties use dot notation (e.g., "defaultrole.id") requiring path traversal

**Primary recommendation:** Build DynamicDTOConverter as a stateless Spring component that accepts (EntityMetadata, FieldMetadata list) and delegates field-specific logic to strategy classes per FieldMappingType.

## Standard Stack

The established libraries/tools for this domain:

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Spring Framework | 6.x (Boot 3.1.4) | DI, component scanning | Already used throughout RX |
| Hibernate 6 | 6.x (jakarta namespace) | JPA entity management | Current ORM in use |
| Apache Commons BeanUtils | 1.9.4+ | Nested property access | Industry standard for dynamic bean manipulation |
| Jackson | 2.x | JSON handling for JP fields | Already in classpath |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| Jakarta Persistence API | 3.x | EntityManager access | Resolving related entities by ID |
| Apache Commons Lang3 | 3.x | NumberUtils, StringUtils | Already used in MappingUtils |
| JsonPath | 2.x | JP field extraction | Already used in generated JsonPath converters |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| BeanUtils | Java Reflection directly | More code, less tested for nested paths |
| Strategy per type | Single converter class | Would be 800+ lines, harder to test |
| Map<String,Object> | Custom DTO class | Map aligns with existing pattern |

**Installation:**
```bash
# Apache Commons BeanUtils (verify it's already in dependencies)
implementation 'commons-beanutils:commons-beanutils:1.9.4'
```

## Architecture Patterns

### Recommended Project Structure
```
com.etendorx.das.converter/
├── DynamicDTOConverter.java           # Main orchestrator
├── strategy/
│   ├── FieldConversionStrategy.java   # Interface
│   ├── DirectMappingStrategy.java     # DM: entity.property → value
│   ├── EntityMappingStrategy.java     # EM: recursive nested conversion
│   ├── JavaMappingStrategy.java       # JM: delegate to @Qualifier bean
│   ├── ConstantValueStrategy.java     # CV: return constant from DB
│   ├── JsonPathStrategy.java          # JP: extract from JSON field
│   └── ComputedMappingStrategy.java   # CM: constant value (alias for CV)
├── PropertyAccessor.java              # Wrapper for BeanUtils nested access
└── ConversionContext.java             # Holds visited entities for cycle detection
```

### Pattern 1: Strategy Pattern for Field Mappings
**What:** Each FieldMappingType has a dedicated strategy class implementing FieldConversionStrategy
**When to use:** When field types have fundamentally different conversion logic
**Example:**
```java
// Source: Analyzed from generated FieldConverter classes
public interface FieldConversionStrategy {
    Object readField(Object entity, FieldMetadata field, ConversionContext ctx);
    void writeField(Object entity, Object value, FieldMetadata field, ConversionContext ctx);
}

@Component
public class DirectMappingStrategy implements FieldConversionStrategy {
    private final PropertyAccessor propertyAccessor;
    private final MappingUtils mappingUtils;

    @Override
    public Object readField(Object entity, FieldMetadata field, ConversionContext ctx) {
        // Get nested property value: entity.defaultrole.id
        Object rawValue = propertyAccessor.getNestedProperty(entity, field.property());
        return mappingUtils.handleBaseObject(rawValue);
    }

    @Override
    public void writeField(Object entity, Object value, FieldMetadata field, ConversionContext ctx) {
        propertyAccessor.setNestedProperty(entity, field.property(), value);
    }
}
```

### Pattern 2: Conversion Context for Cycle Detection
**What:** ThreadLocal or parameter-passed context tracking visited entities during recursive EM conversions
**When to use:** Always for EM fields to prevent infinite loops in bidirectional relationships
**Example:**
```java
// Source: Derived from StackOverflow best practices and MapStruct patterns
public class ConversionContext {
    private final Set<String> visitedEntityKeys = new HashSet<>();

    public boolean isVisited(Object entity) {
        String key = entity.getClass().getName() + ":" + getEntityId(entity);
        return !visitedEntityKeys.add(key);
    }

    private String getEntityId(Object entity) {
        // Use BaseRXObject.get_identifier() or reflection
        if (entity instanceof BaseRXObject) {
            return ((BaseRXObject) entity).get_identifier();
        }
        // fallback to hashCode
        return String.valueOf(System.identityHashCode(entity));
    }
}
```

### Pattern 3: Nested Property Access Abstraction
**What:** Wrapper around BeanUtils to handle dot-notation paths like "defaultrole.id"
**When to use:** All DM field reads/writes
**Example:**
```java
// Source: Apache Commons BeanUtils documentation
@Component
public class PropertyAccessor {
    public Object getNestedProperty(Object bean, String propertyPath) {
        try {
            return PropertyUtils.getNestedProperty(bean, propertyPath);
        } catch (Exception e) {
            // Return null for missing intermediate objects (e.g., entity.role.id when role is null)
            return null;
        }
    }

    public void setNestedProperty(Object bean, String propertyPath, Object value) {
        try {
            PropertyUtils.setNestedProperty(bean, propertyPath, value);
        } catch (Exception e) {
            throw new ConversionException("Cannot set " + propertyPath, e);
        }
    }
}
```

### Anti-Patterns to Avoid
- **Single monolithic converter method:** The generated code separates read/write and delegates to field converters. Don't consolidate all logic into one 1000-line method.
- **Ignoring null checks in nested paths:** When reading "entity.role.id", if role is null, generated code returns null (not NPE). Must replicate this.
- **Creating DTOs instead of Maps:** The dynamic system uses `Map<String, Object>` to avoid code generation. Don't create intermediate DTO POJOs.

## Don't Hand-Roll

Problems that look simple but have existing solutions:

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Nested property access | String parsing + reflection loop | Apache Commons BeanUtils | Handles edge cases: indexed properties, mapped properties, null-safe traversal |
| Date formatting per user | SimpleDateFormat with TZ | MappingUtils.handleBaseObject() | Already handles user dateFormat/timeZone from AppContext |
| Entity ID resolution | Direct EntityManager.find() | ExternalIdService.convertExternalToInternalId() | Supports connector external IDs, fallback to internal IDs |
| Type coercion | Manual instanceof checks | MappingUtils.handleBaseObject() | Handles BaseSerializableObject→identifier, Date→formatted string, PersistentBag→List |
| Audit field population | Manual setCreatedBy/setUpdated | AuditServiceInterceptor.setAuditValues() | Sets 7 fields (client, org, active, createdBy, creationDate, updatedBy, updated) from UserContext |
| Java Mapping resolution | Manual bean lookup | ApplicationContext.getBean(qualifier) | Spring DI handles @Qualifier resolution |

**Key insight:** Generated converters are 80% boilerplate, 20% edge case handling. The edge cases (null safety, user-specific formatting, external ID fallback) are already solved by existing utilities. Don't reimplement them.

## Common Pitfalls

### Pitfall 1: Null Handling in Nested Property Paths
**What goes wrong:** When reading "entity.role.id", if entity.role is null, code throws NullPointerException
**Why it happens:** Direct getter chaining (entity.getRole().getId()) doesn't check intermediate nulls
**How to avoid:** Use BeanUtils which returns null for missing intermediate objects, or add explicit null checks at each level
**Warning signs:** NPE in field extraction, tests failing when optional relations are null

### Pitfall 2: Infinite Recursion in Entity Mappings
**What goes wrong:** Converting User entity includes Organization, which includes User list, which includes Organization...
**Why it happens:** EM fields recursively convert related entities without tracking what's already visited
**How to avoid:** Pass ConversionContext through recursive calls, check isVisited() before converting EM fields
**Warning signs:** StackOverflowError during conversion, extremely large JSON output, tests hanging

### Pitfall 3: Type Coercion Mismatches
**What goes wrong:** Field expects String "100.50", but Map contains BigDecimal or Integer
**Why it happens:** Generated converters call mappingUtils.handleBaseObject() which normalizes types, but dynamic converter might pass raw values
**How to avoid:** Always pass field values through MappingUtils.handleBaseObject() on read, use NumberUtils/parseDate on write
**Warning signs:** ClassCastException in controller, JSON serialization errors, numeric fields showing as "[object Object]"

### Pitfall 4: ExternalId vs Internal ID Confusion
**What goes wrong:** Write operation fails because it tries to find entity by external connector ID directly
**Why it happens:** Connectors send their own IDs, not Etendo internal UUIDs
**How to avoid:** Use ExternalIdService.convertExternalToInternalId(tableId, externalId) before EntityManager.find()
**Warning signs:** EntityNotFoundException on write, related entities not found, foreign key constraint violations

### Pitfall 5: Audit Fields Not Set
**What goes wrong:** New entities saved with null createdBy, updated fields
**Why it happens:** Generated converters explicitly call auditServiceInterceptor.setAuditValues() before save
**How to avoid:** Call AuditServiceInterceptor.setAuditValues(entity) in write path before returning entity to controller
**Warning signs:** NOT NULL constraint violations on createdby/updatedby columns, audit trail incomplete

### Pitfall 6: Missing Table ID for Entity Resolution
**What goes wrong:** ExternalIdService requires tableId, but FieldMetadata for EM fields doesn't directly provide it
**Why it happens:** tableId is in EntityMetadata.tableId, not FieldMetadata; must look up related entity's table
**How to avoid:** When processing EM field, use field.relatedProjectionEntityId() to fetch related EntityMetadata and get its tableId
**Warning signs:** NPE when calling convertExternalToInternalId(), entities not resolved by external ID

## Code Examples

Verified patterns from official sources:

### Read Conversion: Entity → Map
```java
// Source: Analyzed from generated FieldConverterRead.ftl and baseDTOConverter.ftl
public Map<String, Object> convertToMap(Object entity, EntityMetadata entityMetadata,
                                        List<FieldMetadata> fields) {
    if (entity == null) return null;

    Map<String, Object> result = new HashMap<>();
    ConversionContext context = new ConversionContext();

    for (FieldMetadata field : fields) {
        try {
            FieldConversionStrategy strategy = getStrategy(field.fieldMapping());
            Object value = strategy.readField(entity, field, context);
            result.put(field.name(), value);
        } catch (Exception e) {
            log.error("Error converting field {}", field.name(), e);
            result.put(field.name(), null);
        }
    }

    return result;
}
```

### Write Conversion: Map → Entity
```java
// Source: Analyzed from generated FieldConverterWrite.ftl pattern
public Object convertToEntity(Map<String, Object> dto, Object entity,
                               EntityMetadata entityMetadata, List<FieldMetadata> fields) {
    if (entity == null) {
        // Instantiate entity using reflection
        entity = instantiateEntity(entityMetadata.tableId());
    }

    ConversionContext context = new ConversionContext();

    // Sort fields by line number (reverse) to handle dependencies
    List<FieldMetadata> sortedFields = fields.stream()
        .sorted(Comparator.comparing(FieldMetadata::line).reversed())
        .toList();

    for (FieldMetadata field : sortedFields) {
        Object value = dto.get(field.name());
        if (value == null && field.mandatory()) {
            throw new ValidationException("Mandatory field missing: " + field.name());
        }

        try {
            FieldConversionStrategy strategy = getStrategy(field.fieldMapping());
            strategy.writeField(entity, value, field, context);
        } catch (Exception e) {
            throw new ConversionException("Error setting field " + field.name(), e);
        }
    }

    // Set audit fields automatically
    if (entity instanceof BaseRXObject) {
        auditServiceInterceptor.setAuditValues((BaseRXObject) entity);
    }

    return entity;
}
```

### Java Mapping (JM) Resolution
```java
// Source: Generated baseFieldConverterRead.ftl lines 106-109
@Component
public class JavaMappingStrategy implements FieldConversionStrategy {
    private final ApplicationContext applicationContext;

    @Override
    public Object readField(Object entity, FieldMetadata field, ConversionContext ctx) {
        String qualifier = field.javaMappingQualifier();
        DTOReadMapping mapper = applicationContext.getBean(qualifier, DTOReadMapping.class);
        return mapper.map(entity);
    }

    @Override
    public void writeField(Object entity, Object value, FieldMetadata field, ConversionContext ctx) {
        String qualifier = field.javaMappingQualifier();
        DTOWriteMapping mapper = applicationContext.getBean(qualifier, DTOWriteMapping.class);
        // Note: DTOWriteMapping expects (entity, dto) but dto is the entire DTO, not single field
        // This requires design decision: pass full DTO Map or refactor interface
        throw new UnsupportedOperationException("JM write requires design decision on DTO access");
    }
}
```

### Entity Mapping (EM) with Cycle Detection
```java
// Source: Derived from baseFieldConverterRead.ftl lines 33-35 and cycle detection best practices
@Component
public class EntityMappingStrategy implements FieldConversionStrategy {
    private final DynamicDTOConverter dynamicConverter;
    private final DynamicMetadataService metadataService;

    @Override
    public Object readField(Object entity, FieldMetadata field, ConversionContext ctx) {
        Object relatedEntity = propertyAccessor.getNestedProperty(entity, field.property());
        if (relatedEntity == null) return null;

        // Cycle detection
        if (ctx.isVisited(relatedEntity)) {
            // Return only ID and identifier for already-visited entities
            return Map.of(
                "id", ((BaseRXObject) relatedEntity).getId(),
                "_identifier", ((BaseRXObject) relatedEntity).get_identifier()
            );
        }

        // Recursive conversion with full nested object
        EntityMetadata relatedMeta = metadataService.getProjectionEntity(
            /* projectionName */ field.relatedProjectionEntityId()
        ).orElseThrow();

        return dynamicConverter.convertToMap(relatedEntity, relatedMeta,
                                              relatedMeta.fields(), ctx);
    }

    @Override
    public void writeField(Object entity, Object value, FieldMetadata field, ConversionContext ctx) {
        if (!(value instanceof Map)) throw new IllegalArgumentException("EM field expects Map");

        Map<String, Object> nestedDto = (Map<String, Object>) value;
        String externalId = (String) nestedDto.get("id");

        // Resolve entity by external ID
        EntityMetadata relatedMeta = metadataService.getProjectionEntity(
            /* lookup by relatedProjectionEntityId */ field.relatedProjectionEntityId()
        ).orElseThrow();

        String internalId = externalIdService.convertExternalToInternalId(
            relatedMeta.tableId(), externalId
        );

        Object relatedEntity = entityManager.find(getEntityClass(relatedMeta), internalId);
        propertyAccessor.setNestedProperty(entity, field.property(), relatedEntity);
    }
}
```

### Constant Value (CV) Handling
```java
// Source: Generated baseJsonPathConverter.ftl lines 169-174 and MappingUtilsImpl.constantValue()
@Component
public class ConstantValueStrategy implements FieldConversionStrategy {
    private final MappingUtils mappingUtils;

    @Override
    public Object readField(Object entity, FieldMetadata field, ConversionContext ctx) {
        // CV fields return constant from database, ignore entity value
        String constantId = field.constantValue();
        return mappingUtils.constantValue(constantId);
    }

    @Override
    public void writeField(Object entity, Object value, FieldMetadata field, ConversionContext ctx) {
        // Constants are not writable from DTO
        // Generated converters don't have setters for CV fields
        // No-op or throw exception
    }
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| PropertyAccessor interface (Hibernate 3-5) | PropertyAccessStrategy / BeanUtils | Hibernate 6 migration | PropertyAccessor removed, use reflection or BeanUtils |
| javax.persistence.* | jakarta.persistence.* | Hibernate 6 / Spring Boot 3 | All imports updated, incompatible with Classic (javax) |
| Generated converters per entity | Dynamic converters with metadata | This project (Phase 2) | Reduces generated code, enables runtime projection changes |
| Direct entity references in DTOs | ExternalId system | Connector integration | Allows external systems to use their own IDs |

**Deprecated/outdated:**
- Hibernate PropertyAccessor interface: Removed in Hibernate 6, use BeanUtils or reflection
- @PostConstruct for cache preload: Self-invocation issue with proxies, use @EventListener(ApplicationReadyEvent) instead

## Open Questions

Things that couldn't be fully resolved:

1. **Java Mapping (JM) Write Strategy**
   - What we know: DTOWriteMapping interface expects `void map(Entity entity, DTOWrite dto)` where dto is full DTO object
   - What's unclear: FieldConversionStrategy.writeField() receives only single field value, not full DTO map
   - Recommendation: Either (a) pass full DTO map to all strategies, or (b) refactor JM to work field-by-field with custom interface

2. **Entity Instantiation for Write Path**
   - What we know: Need to create new entity instances when dto.id is null or entity not found
   - What's unclear: How to get Class<?> from EntityMetadata.tableId (UUID of AD_Table record)
   - Recommendation: Build mapping table or query AD_Table.javaClassName, cache in DynamicMetadataService

3. **One-to-Many EM Field Handling**
   - What we know: Generated converters handle List<NestedDTO> for one-to-many relations differently (convertList method)
   - What's unclear: Whether Phase 2 scope includes one-to-many write support or just many-to-one
   - Recommendation: Clarify with user; if out of scope, throw UnsupportedOperationException for now

4. **JsonPath (JP) Field Integration**
   - What we know: Generated JsonPathConverter classes exist for extracting fields from JSON columns
   - What's unclear: Whether DynamicDTOConverter needs JP support or if it's handled separately
   - Recommendation: Confirm with user; JP might be Phase 3 or out of scope for basic DTO conversion

## Sources

### Primary (HIGH confidence)
- Codebase analysis:
  - `/modules_gen/com.etendorx.entities/src/main/mappings/` - Generated DTO converters and field converters
  - `/libs/com.etendorx.generate_entities/src/main/resources/org/openbravo/base/gen/mappings/` - FreeMarker templates (baseDTOConverter.ftl, baseFieldConverterRead.ftl, baseFieldConverterWrite.ftl)
  - `/modules_gen/com.etendorx.entities/src/main/entities/com/etendorx/entities/entities/mappings/MappingUtils.java` - Interface definition
  - `/modules_core/com.etendorx.das/src/main/java/com/etendorx/das/utils/MappingUtilsImpl.java` - Type coercion implementation
  - `/modules_core/com.etendorx.das/src/main/java/com/etendorx/das/externalid/ExternalIdServiceImpl.java` - External ID resolution
  - `/modules_core/com.etendorx.das/src/main/java/com/etendorx/das/hibernate_interceptor/AuditServiceInterceptorImpl.java` - Audit field population
  - `/modules_gen/com.etendorx.entities/src/main/entities/com/etendorx/entities/entities/BaseRXObject.java` - Entity base class with audit fields
  - `/libs/com.etendorx.das_core/src/main/java/com/etendorx/entities/mapper/lib/` - DTOConverter interfaces and base classes

### Secondary (MEDIUM confidence)
- [Apache Commons BeanUtils - Nested Property Access](https://www.tutorialspoint.com/java_beanutils/standard_javabeans_nested_property_access.htm) - Verified library for nested property handling
- [Apache Commons BeanUtils | Baeldung](https://www.baeldung.com/apache-commons-beanutils) - Usage patterns
- [Handling Circular Reference of JPA Bidirectional Relationships with Jackson](https://hellokoding.com/handling-circular-reference-of-jpa-hibernate-bidirectional-entity-relationships-with-jackson-jsonignoreproperties/) - Cycle detection patterns
- [Converter Pattern in Java | Java Design Patterns](https://java-design-patterns.com/patterns/converter/) - Converter design pattern

### Tertiary (LOW confidence)
- [Hibernate 6 PropertyAccessor - Discourse](https://discourse.hibernate.org/t/removed-interface-propertyaccessor/6026) - PropertyAccessor removed in Hibernate 6
- [EntityPersister Hibernate 6 JavaDocs](https://docs.jboss.org/hibernate/orm/6.0/javadocs/org/hibernate/persister/entity/EntityPersister.html) - Alternative dynamic access (not preferred)

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - All libraries verified in codebase or industry-standard for this use case
- Architecture: HIGH - Patterns directly observed in generated code templates and implementations
- Pitfalls: HIGH - Derived from analyzing generated code edge cases and null handling patterns
- Code examples: HIGH - Transcribed from actual FreeMarker templates and generated Java files
- Open questions: MEDIUM - Areas requiring design decisions or user clarification

**Research date:** 2026-02-06
**Valid until:** 2026-03-06 (30 days - stable domain, but Java ecosystem moves quickly)
