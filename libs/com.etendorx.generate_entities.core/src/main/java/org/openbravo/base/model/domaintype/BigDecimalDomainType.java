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

import java.math.BigDecimal;

/**
 * The type for a decimal column.
 *
 * @author mtaal
 */

public class BigDecimalDomainType extends BasePrimitiveDomainType {

  /**
   * @return class of the {@link BigDecimal}
   */
  @Override public Class<?> getPrimitiveType() {
    return BigDecimal.class;
  }

  public static class Quantity extends BigDecimalDomainType {
    @Override public String getFormatId() {
      return "qty";
    }
  }

  public static class GeneralQuantity extends BigDecimalDomainType {
    @Override public String getFormatId() {
      return "generalQty";
    }
  }

  public static class Number extends BigDecimalDomainType {
    @Override public String getFormatId() {
      return "generalQty";
    }
  }

  public static class Amount extends BigDecimalDomainType {
    @Override public String getFormatId() {
      return "euro";
    }
  }

  public static class Price extends BigDecimalDomainType {
    @Override public String getFormatId() {
      return "price";
    }
  }

  @Override public Object createFromString(String strValue) {
    if (strValue == null || strValue.trim().length() == 0) {
      return null;
    }
    return new BigDecimal(strValue);
  }

  @Override public String getXMLSchemaType() {
    return "ob:decimal";
  }

}
