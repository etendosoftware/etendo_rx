package com.etendoerp.etendorx.model.projection;

import com.fasterxml.jackson.annotation.JsonIncludeProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openbravo.base.model.ModelObject;

@JsonIncludeProperties({"name", "value"})
public class ETRXEntityField extends ModelObject {

  private ETRXProjectionEntity entity;
  private String property;

  public ETRXProjectionEntity getEntity() {
    return entity;
  }

  public void setEntity(ETRXProjectionEntity entity) {
    this.entity = entity;
  }

  @JsonProperty("value")
  public String getProperty() {
    return property;
  }

  public void setProperty(String property) {
    this.property = property;
  }

}
