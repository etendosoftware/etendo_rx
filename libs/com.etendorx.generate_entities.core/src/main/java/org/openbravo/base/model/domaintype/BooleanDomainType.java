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
 * The type for a yes/no or boolean column.
 *
 * @author mtaal
 */

public class BooleanDomainType extends BasePrimitiveDomainType {

  /**
   * @return class of the {@link Boolean}
   */
  @Override public Class<?> getPrimitiveType() {
    return Boolean.class;
  }

  @Override public Object createFromString(String strValue) {
    if (strValue == null || strValue.trim().length() == 0) {
      return null;
    }
    if (strValue.equalsIgnoreCase("Y")) {
      return Boolean.TRUE;
    }
    if (strValue.equalsIgnoreCase("N")) {
      return Boolean.FALSE;
    }
    return Boolean.valueOf(strValue);
  }

  @Override public String getXMLSchemaType() {
    return "ob:boolean";
  }

}
