/*
 * Copyright 2022  Futit Services SL
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
package org.openbravo.userinterface.selector.model.domaintype;

import org.openbravo.base.model.Column;
import org.openbravo.base.model.ModelObject;
import org.openbravo.base.model.Table;

/**
 * The selector read from the database. Note the Column/Table and other types from the
 * org.openbravo.base.model package should be used, not the generated ones!
 *
 * @author mtaal
 */
public class SelectorDefinition extends ModelObject {

  private Table table;
  private Column column;
  private String referenceId;
  private DatasourceDefinition datasourceDefinition;

  public Column getColumn() {
    return column;
  }

  public void setColumn(Column column) {
    this.column = column;
  }

  public String getReferenceId() {
    return referenceId;
  }

  public void setReferenceId(String referenceId) {
    this.referenceId = referenceId;
  }

  public Table getTable() {
    return table;
  }

  public void setTable(Table table) {
    this.table = table;
  }

  public DatasourceDefinition getDatasourceDefinition() {
    return datasourceDefinition;
  }

  public void setDatasourceDefinition(DatasourceDefinition datasourceDefinition) {
    this.datasourceDefinition = datasourceDefinition;
  }
}
