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

//import com.etendoerp.redis.interfaces.CachedSet;

import org.etendorx.base.validation.ValidationException;
import org.openbravo.base.model.Property;

import java.util.HashSet;
import java.util.Set;

/**
 * The type of a column which can only have a value from a pre-defined set.
 *
 * @author mtaal
 */

public abstract class BaseEnumerateDomainType<E> extends BasePrimitiveDomainType
    implements EnumerateDomainType {

  private final Set<E> enumerateValues = new HashSet<>();

  /**
   * @return the set of enumerate values
   */
  @Override
  public Set<E> getEnumerateValues() {
    return enumerateValues;
  }

  public void addEnumerateValue(E enumerateValue) {
    enumerateValues.add(enumerateValue);
  }

  /**
   * @return class of {@link Object}.
   */
  @Override
  public Class<?> getPrimitiveType() {
    return Object.class;
  }

  @Override
  public void checkIsValidValue(Property property, Object value)
      throws ValidationException {
    super.checkIsValidValue(property, value);

    if (!getEnumerateValues().contains(value)) {
      final ValidationException ve = new ValidationException();
      ve.addMessage(property,
          "Property " + property + ", value (" + value + ") is not allowed, it should be one of the following values: " + getEnumerateValues() + " but it is value " + value);
      throw ve;
    }
  }

}
