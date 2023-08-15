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

/**
 * The ModelReference implements the reference extensions used for the Data Access Layer. See
 * <a href
 * ="http://wiki.etendorx.com/wiki/Projects/Reference_Extension/Technical_Documentation#DAL">here
 * </a> for more information.
 *
 * @author mtaal
 */

public interface ForeignKeyDomainType extends DomainType {

  /**
   * The foreign key column to which a certain column refers. Is only relevant if this reference is
   * a foreign key.
   *
   * @param columnName
   *   the refering foreign key column
   *
   * @return the refered-to column, often the primary key column of the table.
   */
  Column getForeignKeyColumn(String columnName);

}
