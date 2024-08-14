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

import org.etendorx.base.validation.ValidationException;
import org.openbravo.base.model.Property;

import java.math.BigDecimal;

/**
 * The type for an integer/long column.
 *
 * @author mtaal
 */

public class LongDomainType extends BasePrimitiveDomainType {

  /**
   * @return class of the {@link Long}
   */
  @Override
  public Class<?> getPrimitiveType() {
    return Long.class;
  }

  @Override
  public void checkIsValidValue(Property property, Object value) throws ValidationException {
    if (value == null) {
      return;
    }
    if (Integer.class.isInstance(value)) {
      // is allowed
      return;
    }
    super.checkIsValidValue(property, value);
  }

  @Override
  public String getFormatId() {
    return "integer";
  }

  @Override
  public Object createFromString(String strValue) {
    if (strValue == null || strValue.trim().length() == 0) {
      return null;
    }
    return Long.valueOf(new BigDecimal(strValue).longValueExact());
  }

  @Override
  public String getXMLSchemaType() {
    return "ob:long";
  }

}
