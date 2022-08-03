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

package org.etendorx.base.session;

/**
 * Helper class which combines the relevant information of a column used within an UniqueConstraint.
 * It contains a table, columnname and uniqueconstraintname.
 *
 * @author mtaal
 */

public class UniqueConstraintColumn {

  private String tableName;
  private String columnName;
  private String uniqueConstraintName;

  public String getTableName() {
    return tableName;
  }

  public void setTableName(String tableName) {
    this.tableName = tableName;
  }

  public String getColumnName() {
    return columnName;
  }

  public void setColumnName(String columnName) {
    this.columnName = columnName;
  }

  public String getUniqueConstraintName() {
    return uniqueConstraintName;
  }

  public void setUniqueConstraintName(String uniqueConstraintName) {
    this.uniqueConstraintName = uniqueConstraintName;
  }

}
