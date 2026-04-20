package com.etendorx.das.metadata.models;

/**
 * Immutable metadata record representing a single field mapping in a projection entity.
 * This record is suitable for caching and provides all information needed to map
 * an entity property to a DTO field.
 *
 * @param id                         Unique identifier for this field mapping
 * @param name                       Field name in the DTO
 * @param property                   Entity property name (for DIRECT_MAPPING)
 * @param fieldMapping               Type of field mapping (DM, JM, CV, JP)
 * @param mandatory                  Whether this field is required
 * @param identifiesUnivocally       Whether this field uniquely identifies the entity
 * @param line                       Display order/sequence number
 * @param javaMappingQualifier       Spring bean qualifier for JAVA_MAPPING type
 * @param constantValue              Static value for CONSTANT_VALUE type
 * @param jsonPath                   JsonPath expression for JSON_PATH type
 * @param relatedProjectionEntityId  ID of related projection entity (for nested objects)
 * @param createRelated              Whether to create related entities automatically
 */
public record FieldMetadata(
    String id,
    String name,
    String property,
    FieldMappingType fieldMapping,
    boolean mandatory,
    boolean identifiesUnivocally,
    Long line,
    String javaMappingQualifier,
    String constantValue,
    String jsonPath,
    String relatedProjectionEntityId,
    boolean createRelated
) {}
