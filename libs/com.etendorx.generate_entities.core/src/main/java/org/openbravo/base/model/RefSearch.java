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
import org.openbravo.base.model.domaintype.SearchDomainType;

/**
 * Used by the {@link ModelProvider ModelProvider}, maps the AD_Reference table in the application
 * dictionary.
 *
 * @author iperdomo
 */

public class RefSearch extends ModelObject {
  private static final Logger log = LogManager.getLogger();

  private String reference;
  private Reference referenceObject;
  private Column column;

  public Reference getReferenceObject() {
    return referenceObject;
  }

  public void setReferenceObject(Reference referenceObj) {
    this.referenceObject = referenceObj;
    reference = referenceObj.getId();
    final DomainType domainType = referenceObj.getDomainType();
    if (!(domainType instanceof SearchDomainType)) {
      log.error(
          "Domain type of reference " + referenceObj.getId() + " is not a TableDomainType but a " + domainType);
    } else {
      ((SearchDomainType) domainType).setRefSearch(this);
    }
  }

  public Column getColumn() {
    return column;
  }

  public void setColumn(Column column) {
    this.column = column;
  }

  /**
   * Deprecated use {@link #getReferenceObject()}
   */
  public String getReference() {
    return reference;
  }

  /**
   * Deprecated use {@link #setReferenceObject(Reference)}
   */
  public void setReference(String reference) {
    this.reference = reference;
  }
}
