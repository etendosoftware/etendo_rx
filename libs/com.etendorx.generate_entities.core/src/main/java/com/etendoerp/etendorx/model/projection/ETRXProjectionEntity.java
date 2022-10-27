package com.etendoerp.etendorx.model.projection;

import com.fasterxml.jackson.annotation.JsonIncludeProperties;

import org.apache.commons.lang.StringUtils;
import org.openbravo.base.model.ModelObject;
import org.openbravo.base.model.Table;

import java.util.Set;

@JsonIncludeProperties({ "identity", "name", "fields" })
public class ETRXProjectionEntity extends ModelObject {

  private ETRXProjection projection;
  private Table table;
  private Boolean identity;
  private String name;
  private Set<ETRXEntityField> fields;

  public ETRXProjection getProjection() {
    return projection;
  }

  public void setProjection(ETRXProjection projection) {
    this.projection = projection;
  }

  public Table getTable() {
    return table;
  }

  public void setTable(Table table) {
    this.table = table;
    if (table != null && !StringUtils.isBlank(table.getTableName())) {
      this.setName(table.getName());
    }
  }

  public Boolean getIdentity() {
    return identity;
  }

  public void setIdentity(Boolean identity) {
    this.identity = identity;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public void setName(String name) {
    this.name = name;
  }

  public Set<ETRXEntityField> getFields() {
    return fields;
  }

  public void setFields(Set<ETRXEntityField> fields) {
    this.fields = fields;
  }
}
