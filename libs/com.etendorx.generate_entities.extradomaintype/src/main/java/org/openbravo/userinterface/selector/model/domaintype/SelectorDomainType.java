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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openbravo.base.model.Column;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Table;
import org.openbravo.base.model.domaintype.BaseForeignKeyDomainType;
import org.openbravo.base.model.domaintype.ForeignKeyDomainType;

import java.util.ArrayList;
import java.util.List;

/**
 * Implements the domain type for a selector.
 *
 * @author mtaal
 */
public class SelectorDomainType extends BaseForeignKeyDomainType {
  private static final Logger log = LogManager.getLogger();

  private Column column;
  private String tableName;

  @Override
  public List<Class<?>> getClasses() {
    List<Class<?>> listOfClasses = new ArrayList<>();
    listOfClasses.add(SelectorDefinition.class);
    listOfClasses.add(DatasourceDefinition.class);
    return listOfClasses;
  }

  // Note: implementation should clean-up and close database connections or hibernate sessions. If
  // this is not done then the update.database task may hang when disabling foreign keys.
  @Override
  public void initialize() {

    Session session = ModelProvider.getInstance().getSession();

    //@formatter:off
    String hql =
      "select s " +
        "  from SelectorDefinition as s " +
        " where s.referenceId = :referenceId";
    //@formatter:on
    Query<SelectorDefinition> query = session.createQuery(hql, SelectorDefinition.class)
        .setParameter("referenceId", getReference().getId());
    final List<SelectorDefinition> list = query.list();
    if (list.isEmpty()) {
      // a base reference
      if (getReference().getParentReference() == null) {
        return;
      }
      log.error("No selector definition found for reference " + getReference());
      return;
    } else if (list.size() > 1) {
      log.warn(
          "Reference " + getReference() + " has more than one selector definition, only one is really used");
    }
    final SelectorDefinition selectorDefinition = list.get(0);
    Table table = selectorDefinition.getTable();
    if (table == null && selectorDefinition.getDatasourceDefinition() != null) {
      table = selectorDefinition.getDatasourceDefinition().getTable();
    }
    if (table == null) {
      throw new IllegalStateException(
          "The selector " + selectorDefinition.getIdentifier() + " is used in a foreign key reference but no table has been set");
    }
    tableName = table.getTableName();
    if (selectorDefinition.getColumn() == null) {
      final List<Column> columns = readColumns(session, table);
      for (Column col : columns) {
        if (col.isKey()) {
          column = col;
          break;
        }
      }
    } else {
      column = selectorDefinition.getColumn();
    }
  }

  private List<Column> readColumns(Session session, Table table) {
    //@formatter:off
    String hql =
      "select c " +
        "  from Column as c " +
        " where c.table = :table " +
        " order by c.position asc";
    //@formatter:on
    return session.createQuery(hql, Column.class).setParameter("table", table).list();
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * org.openbravo.base.model.domaintype.BaseForeignKeyDomainType#getForeignKeyColumn(java.lang.
   * String)
   */
  @Override
  public Column getForeignKeyColumn(String columnName) {
    while (!column.isKey() && column.getDomainType() instanceof ForeignKeyDomainType) {
      column = ((ForeignKeyDomainType) column.getDomainType()).getForeignKeyColumn(
          column.getColumnName());
      tableName = column.getTable().getName();
    }
    return column;
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * org.openbravo.base.model.domaintype.BaseForeignKeyDomainType#getReferedTableName(java.lang.
   * String)
   */
  @Override
  protected String getReferedTableName(String columnName) {
    return tableName;
  }
}
