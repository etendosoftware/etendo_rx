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
import org.openbravo.base.model.BaseOBObjectDef;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.base.model.Reference;

/**
 * The ModelReference implements the reference extensions used for the Data Access Layer. See
 * <a href
 * ="http://wiki.etendorx.com/wiki/Projects/Reference_Extension/Technical_Documentation#DAL">here
 * </a> for more information.
 *
 * @author mtaal
 */

public interface DomainType {

  /**
   * The ModelReference is instantiated for each reference record in AD_Reference. When it is
   * instantiated the original id of the reference record is passed in using this method.
   *
   * @param reference
   *     the record for which the ModelReference is instantiated.
   */
  void setReference(Reference reference);

  /**
   * @return the {@link Reference} record for which this instance is created.
   */
  Reference getReference();

  /**
   * The reference classes are instantiated by the {@link ModelProvider}. The ModelProvider can be
   * used by the reference class to obtain other model related information.
   *
   * @param modelProvider
   *     the ModelProvider instance responsible for building the internal and in-memory model.
   */
  void setModelProvider(ModelProvider modelProvider);

  /**
   * @return the ModelProvider used in this instance
   */
  ModelProvider getModelProvider();

  /**
   * Is called during the initialization of the model layer. Is called after the
   * {@link #setModelProvider(ModelProvider)}.
   * <p>
   * Note: any subclass should clean-up and close database connections or hibernate sessions. If
   * this is not done then the update.database task may hang when disabling foreign keys.
   */
  void initialize();

  /**
   * Checks if a certain value is valid according to the type of the reference.
   *
   * @param property
   *     the property which has this value
   * @param value
   *     the value
   */
  void checkIsValidValue(Property property, Object value) throws ValidationException;

  /**
   * Checks if a certain property has a valid value taking into account other values in the object.
   *
   * @param obObject
   *     the overall object to check
   * @param property
   *     the property to check
   */
  void checkObjectIsValid(BaseOBObjectDef obObject, Property property) throws ValidationException;
}
