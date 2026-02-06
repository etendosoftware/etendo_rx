package com.etendorx.das.metadata.models;

/**
 * Enum representing the four types of field mappings in projection metadata.
 * Each type corresponds to a different strategy for mapping entity properties to DTO fields.
 */
public enum FieldMappingType {
    /**
     * Direct Mapping - Direct property-to-field mapping from entity to DTO
     */
    DIRECT_MAPPING("DM"),

    /**
     * Java Mapping - Custom Java converter using a Spring-qualified bean
     */
    JAVA_MAPPING("JM"),

    /**
     * Constant Value - Static constant value (not from entity property)
     */
    CONSTANT_VALUE("CV"),

    /**
     * JSON Path - JsonPath extraction from a JSON field
     */
    JSON_PATH("JP");

    private final String code;

    FieldMappingType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    /**
     * Converts a database code to the corresponding FieldMappingType enum.
     *
     * @param code the 2-character database code (DM, JM, CV, JP)
     * @return the matching FieldMappingType
     * @throws IllegalArgumentException if the code is not recognized
     */
    public static FieldMappingType fromCode(String code) {
        for (FieldMappingType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown field mapping code: " + code);
    }
}
