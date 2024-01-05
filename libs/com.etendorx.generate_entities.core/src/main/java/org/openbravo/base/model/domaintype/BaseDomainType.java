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

import java.util.ArrayList;
import java.util.List;

/**
 * The base class for all property types.
 *
 * @author mtaal
 */

public abstract class BaseDomainType implements DomainType {

  private Reference reference;
  private ModelProvider modelProvider;

  /**
   * Method is empty in this class, subclasses should override and call super.initialize() (to allow
   * future additional initialization in this class).
   * <p>
   * Note: any subclass should clean-up and close database connections or hibernate sessions. If
   * this is not done then the update.database task may hang when disabling foreign keys.
   */
  @Override
  public void initialize() {
  }

  @Override
  public void setReference(Reference reference) {
    this.reference = reference;
  }

  @Override
  public Reference getReference() {
    return reference;
  }

  @Override
  public ModelProvider getModelProvider() {
    return modelProvider;
  }

  @Override
  public void setModelProvider(ModelProvider modelProvider) {
    this.modelProvider = modelProvider;
  }

  @Override
  public void checkObjectIsValid(BaseOBObjectDef obObject, Property property)
      throws ValidationException {
    checkIsValidValue(property, obObject.get(property.getName()));
  }

  /**
   * This method should be implemented by DomainTypes which require the usage of certain
   * non-standard entities in the initialize() method.
   *
   * @return The returned list should contain the classes of the entities which need to be accessed
   */
  public List<Class<?>> getClasses() {
    return new ArrayList<Class<?>>();
  }

}
