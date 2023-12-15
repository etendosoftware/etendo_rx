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

import java.lang.reflect.Constructor;

/**
 * The base class for primitive property types. Subclasses only need to implement
 * {@link PrimitiveDomainType#getPrimitiveType()}.
 *
 * @author mtaal
 */
public abstract class BasePrimitiveDomainType extends BaseDomainType
    implements PrimitiveDomainType {

  private Constructor<Object> constructor;

  /*
   * (non-Javadoc)
   *
   * @see org.etendorx.base.model.domaintype.PrimitiveDomainType#getHibernateType()
   */
  @Override
  public Class<?> getHibernateType() {
    return getPrimitiveType();
  }

  /*
   * (non-Javadoc)
   *
   * @see org.etendorx.base.model.domaintype.DomainType#checkIsValidValue(org.etendorx.base.model.
   * Property , java.lang.Object)
   */
  @Override
  public void checkIsValidValue(Property property, Object value) throws ValidationException {
    if (value == null) {
      return;
    }
    if (!getPrimitiveType().isInstance(value)) {
      final ValidationException ve = new ValidationException();
      ve.addMessage(property,
          "Property " + property + " only allows instances of " + getPrimitiveType().getName() + " but the value is an instanceof " + value.getClass()
              .getName());
      throw ve;
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see org.etendorx.base.model.domaintype.PrimitiveDomainType#getFormatId()
   */
  @Override
  public String getFormatId() {
    return null;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.etendorx.base.model.domaintype.PrimitiveDomainType#convertToString(java.lang.Object)
   */
  @Override
  public String convertToString(Object value) {
    if (value == null) {
      return EMPTY_STRING;
    }
    return value.toString();
  }

  /*
   * (non-Javadoc)
   *
   * @see org.etendorx.base.model.domaintype.PrimitiveDomainType#createFromString(java.lang.String)
   */
  @SuppressWarnings("unchecked")
  @Override
  public Object createFromString(String strValue) {
    if (strValue == null || strValue.length() == 0) {
      return null;
    }

    try {
      if (constructor == null) {
        final Class<Object> clz = (Class<Object>) getPrimitiveType();
        constructor = clz.getConstructor(String.class);
      }
      return constructor.newInstance(strValue);
    } catch (Exception e) {
      throw new IllegalArgumentException(e);
    }
  }
}
