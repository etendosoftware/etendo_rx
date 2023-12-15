package com.etendoerp.etendorx.model.repository;

import com.fasterxml.jackson.annotation.JsonIncludeProperties;
import org.openbravo.base.model.ModelObject;

@JsonIncludeProperties({ "name", "type" })
public class ETRXSearchParam extends ModelObject {

  private ETRXEntitySearch entitySearch;
  private String name;
  private String type;

  public ETRXEntitySearch getEntitySearch() {
    return entitySearch;
  }

  public void setEntitySearch(ETRXEntitySearch entitySearch) {
    this.entitySearch = entitySearch;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public void setName(String name) {
    this.name = name;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }
}
