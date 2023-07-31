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
package com.etendoerp.etendorx.model.mapping;

import org.openbravo.base.model.ModelObject;
import org.openbravo.base.model.Table;

public class ETRXJavaMapping extends ModelObject {

  private String id;
  private String name;
  private String description;
  private String qualifier;
  private String mappingType;
  private Table table;

  @Override
  public String getId() {
    return id;
  }

  @Override
  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public String getQualifier() {
    return qualifier;
  }

  public String getMappingType() {
    return mappingType;
  }

  public Table getTable() {
    return table;
  }

  @Override
  public void setName(String name) {
    this.name = name;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public void setQualifier(String qualifier) {
    this.qualifier = qualifier;
  }

  public void setMappingType(String mappingType) {
    this.mappingType = mappingType;
  }

  public void setTable(Table table) {
    this.table = table;
  }
}
