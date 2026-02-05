package com.etendorx.gen.generation;

import com.etendoerp.etendorx.model.projection.ETRXProjection;
import com.etendoerp.etendorx.model.projection.ETRXProjectionEntity;
import com.etendorx.gen.beans.Projection;
import com.etendorx.gen.beans.ProjectionEntity;
import com.etendorx.gen.beans.ProjectionEntityField;
import org.apache.commons.lang3.StringUtils;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.base.model.Table;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ProjectionsConverter {

  private static final String DEFAULT_TYPE = "Object";

  /**
   * Converts the ETRX projections to projections
   *
   * @param paths
   * @param etrxProjections
   */
  public List<Projection> convert(GeneratePaths paths, List<ETRXProjection> etrxProjections) {
    List<Projection> projections = new ArrayList<>();
    for (ETRXProjection etrxProjection : etrxProjections) {
      Projection projection = new Projection(etrxProjection);
      File moduleLocation = getModuleLocation(paths.pathEtendoRx,
          etrxProjection.getModule().getRxJavaPackage());
      projection.setModuleLocation(moduleLocation);
      convert(projection, etrxProjection.getEntities());
      projections.add(projection);
    }
    return projections;
  }

  private File getModuleLocation(String pathEtendoRx, String rxJavaPackage) {
    var directories = new ArrayList<>(Arrays.asList("../modules_rx", "./modules"));
    String defaultDir = null;
    for (String directory : directories) {
      if (new File(directory).exists()) {
        defaultDir = directory;
        break;
      }
    }
    var moduleLocation = new File(
        pathEtendoRx + File.separator + defaultDir + File.separator + rxJavaPackage);
    if (moduleLocation.exists()) {
      moduleLocation.mkdirs();
    }
    return moduleLocation;
  }

  /**
   * Converts the ETRX projections to projections and adds the entities to the projection
   *
   * @param projection
   * @param entities
   */
  public void convert(Projection projection, Set<ETRXProjectionEntity> entities) {
    for (ETRXProjectionEntity entity : entities) {
      if(StringUtils.equals(entity.getMappingType(), "R")) {
        projection.getEntities().put(entity.getTable().getName(), convertEntity(entity));
      }
    }
  }

  /**
   * Converts the entities to projections and adds the entities to the projection
   *
   * @param projection
   * @param entities
   */
  public void convert(Projection projection, List<Entity> entities) {
    for (Entity entity : entities) {
      projection.getEntities().put(entity.getName(), convertEntity(entity));
    }
  }

  /**
   * Converts the ETRX projection entity to projection entity
   *
   * @param entity
   */
  private ProjectionEntity convertEntity(ETRXProjectionEntity entity) {
    var projection = new ProjectionEntity(entity.getTable().getName(), entity.getExternalName(), entity.getIdentity());
    entity.getFields().forEach(f -> {
      var type = resolvePropertyType(entity.getTable(), f.getProperty());
      var field = new ProjectionEntityField(f.getName(), f.getProperty(), type);

      // Set projectedEntity and projectedField for navigated properties
      if (f.getProperty() != null && f.getProperty().contains(".")) {
        String[] parts = f.getProperty().split("\\.", 2);
        field.setProjectedEntity(parts[0]);
        field.setProjectedField(parts[1]);
      }

      projection.getFields().put(field.getName(), field);
    });
    return projection;
  }

  /**
   * Resolves the type of a property, supporting navigated properties (e.g., businessPartner.name)
   *
   * @param table    The starting table
   * @param property The property path (may contain dots for navigation)
   * @return The resolved type name, or "Object" if not found
   */
  private String resolvePropertyType(Table table, String property) {
    if (property == null || property.isEmpty()) {
      return DEFAULT_TYPE;
    }

    if (!property.contains(".")) {
      return resolveSimpleProperty(table, property);
    }

    return resolveNavigatedProperty(table, property);
  }

  private String resolveSimpleProperty(Table table, String property) {
    String type = ModelProvider.getInstance().getColumnTypeName(table,
        table.getName() + "." + property);
    return type != null ? type : DEFAULT_TYPE;
  }

  private String resolveNavigatedProperty(Table table, String property) {
    String[] parts = property.split("\\.", 2);
    String navigationProperty = parts[0];
    String remainingPath = parts[1];

    Entity currentEntity = ModelProvider.getInstance().getEntityByTableName(table.getTableName());
    if (currentEntity == null) {
      return DEFAULT_TYPE;
    }

    Property navProp = currentEntity.getProperty(navigationProperty, false);
    if (navProp == null || navProp.getTargetEntity() == null) {
      return DEFAULT_TYPE;
    }

    String targetTableName = navProp.getTargetEntity().getTableName();
    if (targetTableName == null) {
      return DEFAULT_TYPE;
    }

    Table targetTable = ModelProvider.getInstance().getTable(targetTableName);
    if (targetTable == null) {
      return DEFAULT_TYPE;
    }

    return resolvePropertyType(targetTable, remainingPath);
  }

  /**
   * Converts the entity to projection entity
   *
   * @param entity
   */
  private ProjectionEntity convertEntity(Entity entity) {
    var projectionEntity = new ProjectionEntity(entity.getName(), entity.getName(), false);
    projectionEntity.setPackageName(entity.getPackageName());
    projectionEntity.setClassName(entity.getClassName());
    return projectionEntity;
  }

  /**
   * Gets the entities map from the entities list
   *
   * @param entities
   */
  public Map<String, ProjectionEntity> getEntitiesMap(List<Entity> entities) {
    Projection projection = new Projection("default");
    convert(projection, entities);
    return projection.getEntities();
  }
}
