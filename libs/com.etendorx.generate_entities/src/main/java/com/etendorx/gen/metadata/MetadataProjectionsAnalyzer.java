package com.etendorx.gen.metadata;

import com.etendorx.gen.beans.Projection;
import com.etendorx.gen.beans.ProjectionEntity;
import com.etendorx.gen.beans.ProjectionEntityField;

public class MetadataProjectionsAnalyzer {

  /**
   * Get projection based on projection name, entity name and field name
   *
   * @param projection
   * @param entityName
   * @param identity
   * @return
   */
  public static ProjectionEntity getProjectionEntity(Projection projection, String entityName,
      String externalName, Boolean identity) {
    ProjectionEntity projectionEntity;
    if (projection.getEntities().containsKey(entityName)) {
      projectionEntity = projection.getEntities().get(entityName);
    } else {
      projectionEntity = new ProjectionEntity(entityName, externalName, identity);
      projection.getEntities().put(entityName, projectionEntity);
    }
    return projectionEntity;
  }

  /**
   * Get projection entity field based on projection entity, field name, field value and field type
   *
   * @param projectionEntity
   * @param fieldName
   * @param fieldValue
   * @param fieldType
   * @return
   */
  public static ProjectionEntityField getProjectionEntityField(ProjectionEntity projectionEntity,
      String fieldName, String fieldValue, String fieldType) {
    ProjectionEntityField projectionEntityField;
    if (projectionEntity.getFields().containsKey(fieldName)) {
      projectionEntityField = projectionEntity.getFields().get(fieldName);
    } else {
      projectionEntityField = new ProjectionEntityField(fieldName, fieldValue, fieldType);
      projectionEntity.getFields().put(fieldName, projectionEntityField);
    }
    return projectionEntityField;
  }

}
