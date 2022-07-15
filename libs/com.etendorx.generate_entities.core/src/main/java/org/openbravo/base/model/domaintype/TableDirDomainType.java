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

/**
 * The type of column in the following cases:
 * <ul>
 * <li>TABLEDIR</li>
 * <li>SEARCH and the reference value is not set</li>
 * <li>IMAGE</li>
 * <li>PRODUCT_ATTRIBUTE</li>
 * <li>RESOURCE_ASSIGNMENT</li>
 * <li>IMAGE_BLOB</li>
 * </ul>
 *
 * @author mtaal
 */

public class TableDirDomainType extends BaseForeignKeyDomainType {

  /*
   * (non-Javadoc)
   *
   * @see org.etendorx.base.model.domaintype.BaseForeignKeyDomainType#getReferedTableName()
   */
  protected String getReferedTableName() {
    return null;
  }
}
