/*
 * Copyright 2022-2023  Futit Services SL
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.etendorx.gen.generation.utils;

import com.etendoerp.etendorx.model.projection.ETRXEntityField;
import com.etendoerp.etendorx.model.projection.ETRXProjectionEntity;
import com.etendorx.gen.generation.GeneratePaths;
import org.apache.commons.lang3.StringUtils;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Utility class for code generation.
 */
public class CodeGenerationUtils {

  /**
   * Private constructor to prevent instantiation.
   */
  private CodeGenerationUtils() {
  }

  /**
   * Singleton instance of CodeGenerationUtils.
   */
  private static CodeGenerationUtils instance = null;

  /**
   * A list of fully qualified names of numeric classes.
   * This list is used to check if a given class name represents a numeric type.
   */
  public static final List<String> NUMERIC_CLASSES = List.of("java.math.Integer", "java.math.BigDecimal", "java.lang.Double",
      "java.lang.Float", "java.lang.Long");

  public static String getNull() {
    return null;
  }

  /**
   * Returns the singleton instance of CodeGenerationUtils.
   * If the instance is null, it initializes it.
   *
   * @return the singleton instance of CodeGenerationUtils
   */
  public static CodeGenerationUtils getInstance() {
    if (instance == null) {
      instance = new CodeGenerationUtils();
    }
    return instance;
  }

  /**
   * Returns a Writer for the given parameters.
   *
   * @param mappingPrefix the prefix for the mapping
   * @param outFileName the output file name
   * @param path the path for the file
   * @return a Writer for the file
   * @throws FileNotFoundException if the file cannot be found
   */
  public Writer getWriter(String mappingPrefix, String outFileName, GeneratePaths path)
      throws FileNotFoundException {
    final String packageJPARepo = path.pathEntitiesRx.substring(
        path.pathEntitiesRx.lastIndexOf('/')) + ".mappings";
    final String fullPathJPARepo = path.pathEntitiesRx + "/src/main/mappings/" + packageJPARepo.replace(
        '.', '/');
    final String repositoryClass = mappingPrefix + outFileName;
    new File(fullPathJPARepo).mkdirs();
    var outFileRepo = new File(fullPathJPARepo, repositoryClass);

    return new BufferedWriter(
        new OutputStreamWriter(new FileOutputStream(outFileRepo), StandardCharsets.UTF_8));
  }

  /**
   * Returns the Property for the given ETRXEntityField.
   *
   * @param field the ETRXEntityField to get the Property for
   * @return the Property for the given ETRXEntityField
   */
  private Property getProperty(ETRXEntityField field) {
    for (Property p : ModelProvider.getInstance()
        .getEntity(field.getEntity().getTable().getName())
        .getProperties()) {
      if (p.getName().equals(field.getProperty())) {
        return p;
      }
    }
    return null;
  }

  /**
   * Checks if the given ETRXEntityField is a one-to-many relationship.
   *
   * @param field the ETRXEntityField to check
   * @return true if the ETRXEntityField is a one-to-many relationship, false otherwise
   */
  public boolean isOneToMany(ETRXEntityField field) {
    var property = getProperty(field);
    return property != null && property.isOneToMany() && field.getEtrxProjectionEntityRelated() != null;
  }

  /**
   * Returns the return type for the given ETRXEntityField.
   *
   * @param field the ETRXEntityField to get the return type for
   * @return the return type for the given ETRXEntityField
   */
  public String getReturnType(ETRXEntityField field) {
    var property = getProperty(field);
    if (property != null) {
      return property.getTargetEntity().getClassName();
    }
    return "Object";
  }

  /**
   * Returns the base name for the given ETRXEntityField.
   *
   * @param field the ETRXEntityField to get the base name for
   * @return the base name for the given ETRXEntityField
   */
  private String getBaseName(ETRXEntityField field) {
    String dtoName;
    dtoName = field.getEtrxProjectionEntityRelated()
        .getProjection()
        .getName()
        .toUpperCase() + field.getEtrxProjectionEntityRelated().getExternalName();
    return dtoName;
  }

  /**
   * Returns the DTO for the given ETRXEntityField and mapping type.
   *
   * @param field the ETRXEntityField to get the DTO for
   * @param mappingType the mapping type
   * @return the DTO for the given ETRXEntityField and mapping type
   */
  public String getDto(ETRXEntityField field, String mappingType) {
    String dtoName = getBaseName(field) + "DTO";
    if (StringUtils.equals(mappingType, "R")) {
      dtoName = dtoName + "Read";
    } else {
      dtoName = dtoName + "Write";
    }
    return dtoName;
  }

  /**
   * Returns the DTO converter for the given ETRXEntityField.
   *
   * @param field the ETRXEntityField to get the DTO converter for
   * @return the DTO converter for the given ETRXEntityField
   */
  public String getDTOConverter(ETRXEntityField field) {
    return getBaseName(field) + "DTOConverter";
  }

  /**
   * Returns the JsonPath converter for the given ETRXEntityField.
   * It retrieves the base name of the field and appends "JsonPathConverter" to it.
   *
   * @param field the ETRXEntityField to get the JsonPath converter for
   * @return the JsonPath converter for the given ETRXEntityField
   */
  public String getRetriever(ETRXEntityField field) {
    return getBaseName(field) + "JsonPathRetriever";
  }

  /**
   * Returns the repository name for the given ETRXEntityField.
   * It retrieves the Property for the field, and if the Property is not null, it gets the target entity of the Property,
   * and returns the name of the target entity concatenated with "Repository".
   * If the Property is null, it throws an IllegalArgumentException.
   *
   * @param field the ETRXEntityField to get the repository name for
   * @return the repository name for the given ETRXEntityField
   * @throws IllegalArgumentException if the Property for the field is null
   */
  public String getRepository(ETRXEntityField field) {
    var property = getProperty(field);
    if(property != null) {
      var targetEntity = property.getTargetEntity();
      return targetEntity.getName() + "Repository";
    } else {
      throw new IllegalArgumentException("Property not found for field: " + field);
    }
  }

  /**
   * Returns the JsonPath converter for the given ETRXEntityField.
   *
   * @param field the ETRXEntityField to get the JsonPath converter for
   * @return the JsonPath converter for the given ETRXEntityField
   */
  public String getJsonPathConverter(ETRXEntityField field) {
    return getBaseName(field) + "JsonPathConverter";
  }

  /**
   * Returns the parent field for the given ETRXEntityField.
   *
   * @param field the ETRXEntityField to get the parent field for
   * @return the parent field for the given ETRXEntityField
   */
  public String getParentField(ETRXEntityField field) {
    String tableName = field.getEtrxProjectionEntityRelated().getTable().getName();
    var props = ModelProvider.getInstance()
        .getEntity(tableName)
        .getProperties();
    for (Property p : props) {
      if(p.isParent()) {
        return p.getName();
      }
    }
    return null;
  }

  /**
   * Returns the table ID for the given ETRXEntityField.
   * If the related ETRXProjectionEntity and its table are not null, it returns the table's ID.
   * Otherwise, it retrieves the first segment of the property, finds the corresponding Property in the ModelProvider,
   * and returns the table ID of the target entity of the Property.
   * If no matching Property is found, it returns null.
   *
   * @param field the ETRXEntityField to get the table ID for
   * @return the table ID for the given ETRXEntityField, or null if no matching Property is found
   */
  public String getPropertyTableId(ETRXEntityField field) {
    if (field.getEtrxProjectionEntityRelated() != null && field.getEtrxProjectionEntityRelated().getTable() != null) {
      return field.getEtrxProjectionEntityRelated().getTable().getId();
    } else {
      // Get first segment of property
      String[] segments = field.getProperty().split("\\.");
      String strProperty = segments[0];
      String tableName = field.getEntity().getTable().getName();
      for (Property p : ModelProvider.getInstance()
          .getEntity(tableName)
          .getProperties()) {
        if (StringUtils.equals(p.getName(), strProperty)) {
          return p.getTargetEntity().getTableId();
        }
      }
    }
    return null;
  }

  /**
   * Returns the first segment of the given property string.
   * The property string is split by the "." character, and the first segment is returned.
   * If the property string is null, null is returned.
   *
   * @param property the property string to get the first segment from
   * @return the first segment of the property string, or null if the property string is null
   */
  public String firstProperty(String property) {
    if (property == null) {
      return null;
    }
    String[] parts = property.split("\\.");
    return parts.length > 0 ? parts[0] : null;
  }

  /**
   * Returns the primitive type of the given ETRXEntityField in the ETRXProjectionEntity.
   * If the full qualified type of the field is null, it retrieves the primitive type from the ModelProvider.
   * Otherwise, it returns null.
   *
   * @param entity the ETRXProjectionEntity to get the primitive type for
   * @param field the ETRXEntityField to get the primitive type for
   * @return the primitive type of the given ETRXEntityField in the ETRXProjectionEntity, or null if the full qualified type is not null
   */
  public String getPrimitiveType(ETRXProjectionEntity entity, ETRXEntityField field) {
    if(getFullQualifiedType(entity, field) == null) {
      return ModelProvider.getInstance()
          .getColumnPrimitiveType(entity.getTable(),
              entity.getTable().getName() + "." + firstProperty(field.getProperty()));
    } else {
      return null;
    }
  }

  /**
   * Checks if the provided class name is a numeric type.
   *
   * @param className the fully qualified name of the class to check (e.g., "java.lang.Integer", "java.lang.Double", etc.)
   * @return true if the class name is in the list of predefined numeric classes, false otherwise
   */
  public boolean isNumeric(String className) {
    return NUMERIC_CLASSES.contains(className);
  }

  /**
   * Returns the appropriate numeric parser method from the NumberUtils class based on the provided class name.
   *
   * @param className the fully qualified name of the numeric class (e.g., "java.lang.Integer", "java.lang.Double", etc.)
   * @return the name of the NumberUtils method that can be used to parse a string into an instance of the provided class.
   *         For example, if "java.lang.Integer" is provided, the method will return "NumberUtils.createInteger".
   *         If the provided class name does not match any of the predefined cases, the method will return "NumberUtils.createNumber",
   *         which can handle any numeric type.
   */
  public String getNumericParser(String className) {
    switch (className) {
      case "java.math.Integer":
        return "NumberUtils.createInteger";
      case "java.math.BigDecimal":
        return "NumberUtils.createBigDecimal";
      case "java.lang.Double":
        return "NumberUtils.createDouble";
      case "java.lang.Float":
        return "NumberUtils.createFloat";
      case "java.lang.Long":
        return "NumberUtils.createLong";
      default:
        return "NumberUtils.createNumber";
    }
  }

  /**
   * Returns the fully qualified type of the given ETRXEntityField in the ETRXProjectionEntity.
   * It retrieves the fully qualified type from the ModelProvider using the table of the entity and the first property of the field.
   *
   * @param entity the ETRXProjectionEntity to get the fully qualified type for
   * @param field the ETRXEntityField to get the fully qualified type for
   * @return the fully qualified type of the given ETRXEntityField in the ETRXProjectionEntity
   */
  public String getFullQualifiedType(ETRXProjectionEntity entity, ETRXEntityField field) {
    return ModelProvider.getInstance().getColumnTypeFullQualified(entity.getTable(), entity.getTable().getName() +"." + firstProperty(field.getProperty()));
  }
}
