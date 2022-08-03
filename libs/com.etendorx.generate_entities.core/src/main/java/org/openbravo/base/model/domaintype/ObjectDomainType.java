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

import java.sql.Timestamp;

/**
 * TODO: research when this one is used.
 *
 * @author mtaal
 */

public class ObjectDomainType extends BasePrimitiveDomainType {

  /**
   * @return class of the {@link Timestamp}
   */
  @Override public Class<?> getPrimitiveType() {
    return Object.class;
  }

  @Override public Object createFromString(String strValue) {
    return strValue;
  }

  @Override public String getXMLSchemaType() {
    return "xs:anyType";
  }
}
