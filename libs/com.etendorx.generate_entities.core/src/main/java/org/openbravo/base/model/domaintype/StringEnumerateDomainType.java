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
 * The type of a column which can only have a value from a pre-defined set.
 *
 * @author mtaal
 */

public class StringEnumerateDomainType extends BaseEnumerateDomainType<String> {

  /**
   * As a standard only a string/varchar column can have enumerates.
   *
   * @return class of {@link String}.
   */
  @Override public Class<?> getPrimitiveType() {
    return String.class;
  }

  @Override public Object createFromString(String strValue) {
    if (strValue == null || strValue.length() == 0) {
      return null;
    }
    return strValue;
  }

  @Override public String getXMLSchemaType() {
    return "ob:string";
  }
}
