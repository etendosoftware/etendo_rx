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
package com.etendoerp.etendorx.model.projection;

import com.fasterxml.jackson.annotation.JsonIncludeProperties;
import org.apache.commons.lang3.StringUtils;
import org.openbravo.base.model.ModelObject;
import org.openbravo.base.model.Table;

import java.util.Set;

@JsonIncludeProperties({"identity", "name", "fields"})
public class ETRXProjectionEntity extends ModelObject {

  private ETRXProjection projection;
  private Table table;
  private Boolean identity;
  private String name;
  private String mappingType;

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

  public String getMappingType() {
    return mappingType;
  }

  public void setMappingType(String mappingType) {
    this.mappingType = mappingType;
  }
}
