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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.etendorx.base.util.Check;
import org.openbravo.base.model.Property;

/**
 * Validates string properties (e.g. the max field length of a string).
 *
 * @author mtaal
 */

public class StringPropertyValidator extends BasePropertyValidator {
  private static final int MAX_SHOWN_LENGTH = 100;
  private int fieldLength = -1;
  private static final Logger log = LogManager.getLogger();

  static boolean isValidationRequired(Property p) {
    if (p.isPrimitive() && p.getPrimitiveType() == String.class) {
      if (p.getFieldLength() > 0 || p.doCheckAllowedValue()) {
        // TODO special case, repair in next release
        // https://issues.openbravo.com/view.php?id=8624
        // Validation should not check field length of id/foreign key columns
        if (p.getFieldLength() != 32 && p.isId()) {
          return false;
        }
        return true;
      }
    }
    return false;
  }

  public int getFieldLength() {
    return fieldLength;
  }

  public void setFieldLength(int fieldLength) {
    this.fieldLength = fieldLength;
  }

  public void initialize() {
    if (getProperty().getFieldLength() <= 0) {
      log.error(
          "Fieldlength of property " + getProperty().getName() + " should be larger than 0 for validation");
    } else {
      setFieldLength(getProperty().getFieldLength());
    }
  }

  @Override public String validate(Object value) {
    if (value == null) {
      // mandatory is checked in Hibernate
      return null;
    }
    Check.isInstanceOf(value, String.class);
    final String str = (String) value;
    if (str.length() > getFieldLength()) {
      String val = str.length() <= MAX_SHOWN_LENGTH ? str : (str.substring(0, 100) + "...");

      return "Value too long. Length " + str.length() + ", maximum allowed " + getFieldLength() + " [" + val + "]";
    }
    return null;
  }
}
