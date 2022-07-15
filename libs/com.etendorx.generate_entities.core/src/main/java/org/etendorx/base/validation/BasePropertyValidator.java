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

import org.openbravo.base.model.Property;

/**
 * Base class for property validators. Property validators check length and min and max constraints
 * of property values.
 *
 * @author mtaal
 */

public abstract class BasePropertyValidator
    implements org.etendorx.base.validation.PropertyValidator {

  private Property property;

  public Property getProperty() {
    return property;
  }

  public void setProperty(Property property) {
    this.property = property;
  }

  /**
   * @see PropertyValidator#validate(Object)
   */
  @Override public abstract String validate(Object value);
}
