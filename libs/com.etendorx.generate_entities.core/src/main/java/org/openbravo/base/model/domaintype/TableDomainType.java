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

package org.openbravo.base.model.domaintype;

import org.openbravo.base.model.Column;
import org.openbravo.base.model.RefTable;

/**
 * The type of columns which have a table reference.
 *
 * @author mtaal
 */

public class TableDomainType extends BaseForeignKeyDomainType {
  private RefTable refTable;

  /**
   * @return the column based on the RefTable ({@link #setRefTable(RefTable)}).
   */
  @Override public Column getForeignKeyColumn(String columnName) {
    // handles a special case that reference value is not set in a column
    // in that case the reference is the table reference directly
    if (getRefTable() == null) {
      return super.getForeignKeyColumn(columnName);
    }
    return getRefTable().getColumn();
  }

  public RefTable getRefTable() {
    return refTable;
  }

  public void setRefTable(RefTable refTable) {
    this.refTable = refTable;
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * org.etendorx.base.model.domaintype.BaseForeignKeyDomainType#getReferedTableName(java.lang.
   * String)
   */
  @Override protected String getReferedTableName(String columnName) {
    if (getRefTable() == null) {
      return super.getReferedTableName(columnName);
    }
    return getRefTable().getColumn().getTable().getName();
  }
}
