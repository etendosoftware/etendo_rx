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

package org.etendorx.base.validation;

import org.etendorx.base.util.Check;
import org.openbravo.base.model.Property;

import java.math.BigDecimal;

/**
 * Validates numeric properties (min and maxvalue).
 *
 * @author mtaal
 */

public class NumericPropertyValidator extends BasePropertyValidator {

  private BigDecimal minValue;
  private BigDecimal maxValue;

  static boolean isValidationRequired(Property p) {
    if (p.isPrimitive() && (p.getPrimitiveType() == Float.class || p.getPrimitiveType() == BigDecimal.class || p.getPrimitiveType() == Integer.class || p.getPrimitiveType() == Long.class)) {
      if (p.getMinValue() != null || p.getMaxValue() != null) {
        return true;
      }
    }
    return false;
  }

  public void initialize() {
    Check.isTrue(getProperty().getFieldLength() > 0,
        "Fieldlength should be larger than 0 for validation");
    if (getProperty().getMinValue() != null) {
      minValue = new BigDecimal(getProperty().getMinValue());
    }
    if (getProperty().getMaxValue() != null) {
      maxValue = new BigDecimal(getProperty().getMaxValue());
    }
  }

  @Override
  public String validate(Object value) {
    if (value == null) {
      // mandatory is checked in Hibernate and in the property itself
      return null;
    }
    final BigDecimal localValue;
    if (float.class.isAssignableFrom(value.getClass()) || Float.class.isAssignableFrom(
        value.getClass())) {
      localValue = new BigDecimal((Float) value);
    } else if (int.class.isAssignableFrom(value.getClass()) || Integer.class.isAssignableFrom(
        value.getClass())) {
      localValue = new BigDecimal((Integer) value);
    } else if (long.class.isAssignableFrom(value.getClass()) || Long.class.isAssignableFrom(
        value.getClass())) {
      localValue = new BigDecimal((Long) value);
    } else {
      Check.isInstanceOf(value, BigDecimal.class);
      localValue = (BigDecimal) value;
    }

    if (minValue != null) {
      if (minValue.compareTo(localValue) > 0) {
        return "Value (" + value + ") is smaller than the min value: " + minValue;
      }
    }
    if (maxValue != null) {
      if (maxValue.compareTo(localValue) < 0) {
        return "Value (" + localValue + ") is larger than the max value: " + maxValue;
      }
    }
    return null;
  }
}
