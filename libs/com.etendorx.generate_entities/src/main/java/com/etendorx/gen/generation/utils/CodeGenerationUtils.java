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
import com.etendoerp.etendorx.model.projection.ETRXEntityFieldMap;
import com.etendorx.gen.generation.GeneratePaths;
import org.apache.commons.lang3.StringUtils;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;

import java.io.*;
import java.nio.charset.StandardCharsets;

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

}
