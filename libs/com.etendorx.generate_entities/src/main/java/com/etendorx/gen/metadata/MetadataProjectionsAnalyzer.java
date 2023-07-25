package com.etendorx.gen.metadata;

import com.etendorx.gen.beans.Projection;
import com.etendorx.gen.beans.ProjectionEntity;
import com.etendorx.gen.beans.ProjectionEntityField;

public class MetadataProjectionsAnalyzer {

  public static ProjectionEntity getProjectionEntity(Projection projection, String entityName, Boolean identity) {
    ProjectionEntity projectionEntity;
    if (projection.getEntities().containsKey(entityName)) {
      projectionEntity = projection.getEntities().get(entityName);
    } else {
      projectionEntity = new ProjectionEntity(entityName, identity);
      projection.getEntities().put(entityName, projectionEntity);
    }
    return projectionEntity;
  }

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
