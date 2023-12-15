package com.etendoerp.etendorx.model.repository;

import com.etendoerp.etendorx.model.ETRXModule;
import com.fasterxml.jackson.annotation.JsonIncludeProperties;
import org.apache.commons.lang3.StringUtils;
import org.openbravo.base.model.ModelObject;
import org.openbravo.base.model.Table;

import java.util.Set;

@JsonIncludeProperties({ "entityName", "searches" })
public class ETRXRepository extends ModelObject {

  private ETRXModule module;
  private Table table;
  private String entityName;
  private Set<ETRXEntitySearch> searches;

  public ETRXModule getModule() {
    return module;
  }

  public void setModule(ETRXModule module) {
    this.module = module;
  }

  public Table getTable() {
    return table;
  }

  public void setTable(Table table) {
    this.table = table;
    if (table != null && !StringUtils.isBlank(table.getTableName())) {
      this.setEntityName(table.getName());
    }
  }

  public String getEntityName() {
    return entityName;
  }

  public void setEntityName(String entityName) {
    this.entityName = entityName;
  }

  public Set<ETRXEntitySearch> getSearches() {
    return searches;
  }

  public void setSearches(Set<ETRXEntitySearch> searches) {
    this.searches = searches;
  }
}
