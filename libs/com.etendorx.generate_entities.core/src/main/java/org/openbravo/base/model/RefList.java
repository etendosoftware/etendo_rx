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
import org.openbravo.base.model.domaintype.StringEnumerateDomainType;

/**
 * A limited mapping of the ref_list to support validation of string types.
 *
 * @author mtaal
 */
public class RefList extends ModelObject {
  private static final Logger log = LogManager.getLogger();

  private Reference reference;

  private String value;

  public Reference getReference() {
    return reference;
  }

  public void setReference(Reference reference) {
    this.reference = reference;
    final DomainType domainType = reference.getDomainType();
    if (!(domainType instanceof StringEnumerateDomainType)) {
      log.error(
          "Domain type of reference " + reference.getId() + " is not a TableDomainType but a " + domainType);
    } else {
      ((StringEnumerateDomainType) domainType).addEnumerateValue(value);
    }
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

}
