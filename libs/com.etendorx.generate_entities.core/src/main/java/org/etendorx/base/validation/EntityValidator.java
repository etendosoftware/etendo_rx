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

import org.openbravo.base.model.BaseOBObjectDef;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.Property;

/**
 * Validates an entity, a list of property validators is kept which are called one by one for a
 * passed entity instance and its property values.
 *
 * @author mtaal
 */

public class EntityValidator {

  private boolean validateRequired = false;
  private Entity entity;

  /**
   * Validates the values of the properties of the entityObject. The validation messages are
   * collected into one ValidationException.
   *
   * @param entityObject the entity instance
   */
  public void validate(Object entityObject) {
    if (!validateRequired) {
      return;
    }
    final org.etendorx.base.validation.ValidationException ve = new ValidationException();
    for (final Property p : entity.getProperties()) {
      final PropertyValidator pv = p.getValidator();
      if (pv != null) {
        final Object value = ((BaseOBObjectDef) entityObject).get(p.getName());
        final String msg = pv.validate(value);
        if (msg != null) {
          ve.addMessage(p, msg);
        }
      }
    }
    if (ve.hasMessages()) {
      throw ve;
    }
  }

  public Entity getEntity() {
    return entity;
  }

  public void setEntity(Entity entity) {
    this.entity = entity;
  }

  /**
   * Initializes this EntityValidator by creating a validator for each {@link Property Property}.
   */
  public void initialize() {
    for (final Property p : entity.getProperties()) {
      if (StringPropertyValidator.isValidationRequired(p)) {
        final StringPropertyValidator spv = new StringPropertyValidator();
        spv.setProperty(p);
        spv.initialize();
        p.setValidator(spv);
        validateRequired = true;
      } else if (NumericPropertyValidator.isValidationRequired(p)) {
        final NumericPropertyValidator nv = new NumericPropertyValidator();
        nv.setProperty(p);
        nv.initialize();
        p.setValidator(nv);
        validateRequired = true;
      }
    }
  }
}
