package com.etendorx.entities.metadata;

import java.util.HashMap;
import java.util.Map;

public abstract class EntityMetadata {
  private String tableName ;
  private String entityName;
  private String adTableId;
  private final Map<String, FieldMetadata> fields = new HashMap<>();

  public String getTableName() {
    return tableName;
  }

  public void setTableName(String tableName) {
    this.tableName = tableName;
  }

  public String getEntityName() {
    return entityName;
  }

  public void setEntityName(String entityName) {
    this.entityName = entityName;
  }

  public String getAdTableId() {
    return adTableId;
  }

  public void setAdTableId(String adTableId) {
    this.adTableId = adTableId;
  }

  public Map<String, FieldMetadata> getFields() {
    return fields;
  }
}
