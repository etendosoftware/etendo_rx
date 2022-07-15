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

/**
 * Defines the interface for a propertyvalidator.
 *
 * @author mtaal
 */

public interface PropertyValidator {

  /**
   * Validate the value against constraints implemented in the validator. If the validation fails a
   * message is returned. If validation passes then null is returned.
   *
   * @param value
   *     the value to check
   * @return null if validation passes, otherwise a validation message is returned
   */
  public String validate(Object value);
}
