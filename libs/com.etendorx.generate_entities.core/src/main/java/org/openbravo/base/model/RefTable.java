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
import org.openbravo.base.model.domaintype.DomainType;
import org.openbravo.base.model.domaintype.TableDomainType;

/**
 * Used by the {@link ModelProvider ModelProvider}, maps the AD_Ref_Table table in the application
 * dictionary.
 *
 * @author iperdomo
 */

public class RefTable extends ModelObject {
  private static final Logger log = LogManager.getLogger();

  private Reference reference;

  private Column column;

  private Column displayColumn;

  private boolean displayedValue;

  public Column getColumn() {
    return column;
  }

  public void setColumn(Column column) {
    this.column = column;
  }

  public Reference getReference() {
    return reference;
  }

  public void setReference(Reference reference) {
    this.reference = reference;
    final DomainType domainType = reference.getDomainType();
    if (!(domainType instanceof TableDomainType)) {
      log.error(
          "Domain type of reference " + reference.getId() + " is not a TableDomainType but a " + domainType);
    } else {
      ((TableDomainType) domainType).setRefTable(this);
    }
  }

  public Column getDisplayColumn() {
    return displayColumn;
  }

  public void setDisplayColumn(Column displayColumn) {
    this.displayColumn = displayColumn;
  }

  public boolean getDisplayedValue() {
    return this.displayedValue;
  }

  public void setDisplayedValue(boolean displayedValue) {
    this.displayedValue = displayedValue;
  }
}
