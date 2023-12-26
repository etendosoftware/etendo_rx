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
package com.etendorx.gen.metadata;

import com.etendorx.gen.beans.ProjectionEntity;
import com.etendorx.gen.beans.ProjectionEntityField;
import com.etendorx.gen.util.CodeGenerationException;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.Property;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Util class needed for code generation
 *
 * @author Sebastian Barrozo
 */

public class MetadataUtil {
  private static final Logger log = LogManager.getLogger();

  /**
   * Obtains the absolute path of the baseRestController.ftl package where all the java files will be created.
   * Ex: given the module 'com.test.mymodule'
   * returns 'module.absolutePath + /src-gen/main/java/com/test/mymodule'
   *
   * @param moduleLocation
   */
  public static String getBasePackageGenLocationPath(File moduleLocation) {
    return getSrcGenLocation(
        moduleLocation) + File.separator + "/main/java" + File.separator + getBasePackageGenPath(
        moduleLocation);
  }

  /**
   * Obtains the absolute path where all generated files will be stored
   *
   * @param moduleLocation
   */
  public static String getSrcGenLocation(File moduleLocation) {
    return moduleLocation.getAbsolutePath() + File.separator + "src-gen";
  }

  /**
   * Parses the name of the module to a package path.
   * Ex: 'com.etendorx.mymodule' -> 'com/etendorx/mymodule'
   *
   * @param moduleLocation
   */
  public static String getBasePackageGenPath(File moduleLocation) {
    return moduleLocation.getName().replace("\\.", "/");
  }

  /**
   * Generates a Map between the 'newClassname' (the name of the {@link ProjectionEntity} defined in a projection) and the corresponding {@link Entity}.
   *
   * @param entities List of entities to map
   * @return Map
   */
  public static Map<String, Entity> generateEntitiesMap(List<Entity> entities) {
    Map<String, Entity> entityMap = new HashMap<>();
    entities.forEach(entity -> {
      String newClassName = entity.getName();
      entityMap.put(newClassName, entity);
    });
    return entityMap;
  }

  /**
   * Creates a new {@link ProjectionEntity} based on a {@link Entity}
   *
   * @param entityModel {@link Entity}
   * @return {@link ProjectionEntity}
   */
  public static ProjectionEntity generateProjectionEntity(Entity entityModel) {
    String newClassName = entityModel.getName();
    ProjectionEntity projectionEntity = new ProjectionEntity(newClassName, entityModel.getName(), false);

    // Filter the valid properties of the entityModel
    var filteredProperties = entityModel.getProperties()
        .stream()
        .filter(property -> !property.isComputedColumn() && !StringUtils.isBlank(
            property.getTypeName()) && (property.isId() || (property.isPrimitive() && !property.getPrimitiveType()
            .isArray()) || (property.getTargetEntity() != null && !property.isOneToMany() && !property.getTargetEntity()
            .isView())));

    // Fill the projectionEntity fields with the entityModel filteredProperties
    filteredProperties.forEach(property -> {
      ProjectionEntityField projectionEntityField = generateProjectionEntityField(property);
      projectionEntity.getFields().put(projectionEntityField.getName(), projectionEntityField);
    });

    return projectionEntity;
  }

  /**
   * Creates a new {@link ProjectionEntityField} based on a {@link Property}
   *
   * @param propertyModel {@link Property}
   * @return {@link ProjectionEntityField}
   */
  public static ProjectionEntityField generateProjectionEntityField(Property propertyModel) {
    return new ProjectionEntityField(propertyModel.getName(), null, propertyModel.getTypeName(),
        generateClassName(propertyModel));
  }

  /**
   * Generates the class name of a {@link Property}
   *
   * @param propertyModel
   */
  static String generateClassName(Property propertyModel) {
    String className = "";
    if (propertyModel.getTargetEntity() != null && propertyModel.getTargetEntity()
        .getName() != null) {
      var tableNameSplit = propertyModel.getTargetEntity().getTableName().split("_");
      var cn = "";
      for (String s : tableNameSplit) {
        cn += s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
      }
      className = cn;
    }
    return className;
  }

  @FunctionalInterface
  public interface HandlingConsumer<T, E extends CodeGenerationException> {
    static <T> Consumer<T> handlingConsumerBuilder(
        HandlingConsumer<T, CodeGenerationException> handlingConsumer) {
      return obj -> {
        try {
          handlingConsumer.accept(obj);
        } catch (CodeGenerationException ex) {
          throw new RuntimeException(ex);
        }
      };
    }

    void accept(T target) throws E;
  }

}
