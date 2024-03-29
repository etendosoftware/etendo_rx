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

package org.openbravo.base.model;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Used by the {@link ModelProvider ModelProvider}, maps the AD_Table table in the application
 * dictionary. The {@link Entity Entity} is initialized from a Table.
 *
 * @author iperdomo
 */

public class Table extends ModelObject {
  private static final Logger log = LogManager.getLogger();

  private Entity entity;
  private String dataOrigin;
  private String tableName;
  private boolean view;
  private boolean isDeletable;
  private List<Column> columns = new ArrayList<Column>();
  private List<Column> primaryKeyColumns = null;
  private List<Column> identifierColumns = null;
  private List<Column> parentColumns = null;
  private String className = null;

  private String accessLevel;

  private Package thePackage;

  private String treeType;

  public String getTreeType() {
    return treeType;
  }

  public void setTreeType(String treeType) {
    this.treeType = treeType;
  }

  public String getDataOrigin() {
    return dataOrigin;
  }

  public void setDataOrigin(String dataOrigin) {
    this.dataOrigin = dataOrigin;
  }

  public String getTableName() {
    return tableName;
  }

  public void setTableName(String tableName) {
    this.tableName = tableName;
  }

  /**
   * Note the columns are not set by hibernate or through a hibernate mapping. For performance
   * reasons they are set explicitly in the {@link ModelProvider}. See the assignColumnsToTable
   * method in that class.
   * <p>
   * This collection is only set and used within the {@link ModelProvider} initialize method. It
   * should not be used in other places. In other cases perform a direct database query to get the
   * columns of a table.
   *
   * @return the list of Column instances of this table
   */
  public List<Column> getColumns() {
    return columns;
  }

  /**
   * Note the columns are not set by hibernate or through a hibernate mapping. For performance
   * reasons they are set explicitly in the {@link ModelProvider}. See the assignColumnsToTable
   * method in that class.
   */
  public void setColumns(List<Column> columns) {
    this.columns = columns;
  }

  public List<Column> getPrimaryKeyColumns() {
    if (primaryKeyColumns == null) {
      primaryKeyColumns = new ArrayList<Column>();

      for (final Column c : getColumns()) {
        if (c.isKey()) {
          primaryKeyColumns.add(c);
        }
      }
    }
    return primaryKeyColumns;
  }

  public void setPrimaryKeyColumns(List<Column> primaryKeyColumns) {
    this.primaryKeyColumns = primaryKeyColumns;
  }

  public List<Column> getIdentifierColumns() {
    if (identifierColumns == null) {
      identifierColumns = new ArrayList<Column>();
      for (final Column c : getColumns()) {
        if (c.isIdentifier()) {
          identifierColumns.add(c);
        }
      }
    }
    return identifierColumns;
  }

  public void setParentColumns(List<Column> parentColums) {
    this.parentColumns = parentColums;
  }

  public List<Column> getParentColumns() {
    if (parentColumns == null) {
      parentColumns = new ArrayList<Column>();
      for (final Column c : getColumns()) {
        if (c.isParent()) {
          parentColumns.add(c);
        }
      }
    }
    return parentColumns;
  }

  public void setIdentifierColumns(List<Column> identifierColumns) {
    this.identifierColumns = identifierColumns;
  }

  public void setView(boolean view) {
    this.view = view;
  }

  public boolean isView() {
    return view;
  }

  public String getNotNullClassName() {
    if (getClassName() == null || getClassName().trim().length() == 0) {
      return getName();
    }
    return getClassName();
  }

  public String getClassName() {
    return className;
  }

  public void setClassName(String className) {
    this.className = className;
  }

  public void setReferenceTypes(ModelProvider modelProvider) {
    for (final Column c : columns) {
      if (!c.isPrimitiveType()) {
        c.setReferenceType();
      }
    }
  }

  public String getPackageName() {
    if (getThePackage() != null) {
      return getThePackage().getJavaPackage();
    }
    log.error("Can not determine package name, no package defined for table " + getName());
    // ugly but effective
    return "no.package.defined.for.table." + getName();
  }

  public Entity getEntity() {
    return entity;
  }

  public void setEntity(Entity entity) {
    this.entity = entity;
  }

  @Override
  public String toString() {
    return getTableName();
  }

  public boolean isDeletable() {
    return isDeletable;
  }

  public void setDeletable(boolean isDeletable) {
    this.isDeletable = isDeletable;
  }

  public String getAccessLevel() {
    return accessLevel;
  }

  public void setAccessLevel(String accessLevel) {
    this.accessLevel = accessLevel;
  }

  public Package getThePackage() {
    return thePackage;
  }

  public void setThePackage(Package thePackage) {
    this.thePackage = thePackage;
  }
}
