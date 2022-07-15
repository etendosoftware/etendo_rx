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

import org.etendorx.base.exception.OBException;
import org.openbravo.base.model.Column;
import org.openbravo.base.model.RefSearch;

/**
 * The type of columns which have a search reference.
 *
 * @author mtaal
 */

public class SearchDomainType extends BaseForeignKeyDomainType {
  private RefSearch refSearch;

  /**
   * @return the column based on the RefSearch ({@link #setRefSearch(RefSearch)}).
   */
  @Override public Column getForeignKeyColumn(String columnName) {
    // handles a special case that reference value is not set in a column
    // in that case the reference is the search reference directly
    if (getRefSearch() == null) {
      return super.getForeignKeyColumn(columnName);
    }
    return getRefSearch().getColumn();
  }

  public RefSearch getRefSearch() {
    return refSearch;
  }

  public void setRefSearch(RefSearch refSearch) {
    this.refSearch = refSearch;
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * org.etendorx.base.model.domaintype.BaseForeignKeyDomainType#getReferedTableName(java.lang.
   * String)
   */
  @Override protected String getReferedTableName(String columnName) {
    if (getRefSearch() == null) {
      return super.getReferedTableName(columnName);
    }
    try {
      return getRefSearch().getColumn().getTable().getName();
    } catch (Exception e) {
      throw new OBException(e);
    }
  }
}
