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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openbravo.base.model.Column;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.RefTree;
import org.openbravo.base.model.Table;

import java.util.ArrayList;
import java.util.List;

/**
 * The type of columns which have a tree reference.
 */

public class TreeDomainType extends BaseForeignKeyDomainType {
  private static final Logger log = LogManager.getLogger();

  private Column column;
  private String tableName;

  @Override
  public List<Class<?>> getClasses() {
    List<Class<?>> listOfClasses = new ArrayList<>();
    listOfClasses.add(RefTree.class);
    return listOfClasses;
  }

  @Override
  public void initialize() {

    Session session = ModelProvider.getInstance().getSession();

    //@formatter:off
    String hql =
      "select r " +
        "  from RefTree as r " +
        " where r.referenceId = :referenceId";
    //@formatter:on
    Query<RefTree> query = session.createQuery(hql, RefTree.class)
      .setParameter("referenceId", getReference().getId());
    final List<RefTree> list = query.list();
    if (list.isEmpty()) {
      // a base reference
      if (getReference().getParentReference() == null) {
        return;
      }
      log.error("No tree reference definition found for reference " + getReference());
      return;
    } else if (list.size() > 1) {
      log.warn(
        "Reference " + getReference() + " has more than one tree definition, only one is really used");
    }
    final RefTree treeReference = list.get(0);
    Table table = treeReference.getTable();
    if (table == null) {
      throw new IllegalStateException(
        "The tree reference " + treeReference.getIdentifier() + " is used in a foreign key reference but no table has been set");
    }
    tableName = table.getTableName();
    if (treeReference.getColumn() == null) {
      Column keyColumn = readKeyColumn(session, table);
      if (keyColumn != null) {
        column = keyColumn;
      }
    } else {
      column = treeReference.getColumn();
    }
  }

  private Column readKeyColumn(Session session, Table table) {
    //@formatter:off
    String hql =
      "select c " +
        "  from Column as c " +
        " where c.table = :table " +
        "   and c.key = true " +
        " order by c.position asc";
    //@formatter:on
    Query<Column> query = session.createQuery(hql, Column.class).setParameter("table", table);

    List<Column> keyColumns = query.list();
    if (keyColumns.isEmpty()) {
      return null;
    }
    return keyColumns.get(0);
  }

  @Override
  public Column getForeignKeyColumn(String columnName) {
    while (!column.isKey() && column.getDomainType() instanceof ForeignKeyDomainType) {
      column = ((ForeignKeyDomainType) column.getDomainType()).getForeignKeyColumn(
        column.getColumnName());
      tableName = column.getTable().getName();
    }
    return column;
  }

  @Override
  protected String getReferedTableName(String columnName) {
    return tableName;
  }
}
