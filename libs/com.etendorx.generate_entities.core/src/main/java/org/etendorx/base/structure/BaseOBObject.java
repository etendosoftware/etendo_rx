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

package org.etendorx.base.structure;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.etendorx.base.exception.OBSecurityException;
import org.etendorx.base.provider.OBNotSingleton;
import org.etendorx.base.util.Check;
import org.etendorx.base.util.CheckException;
import org.etendorx.base.validation.ValidationException;
import org.etendorx.dal.core.OBContext;
import org.etendorx.dal.core.OBInterceptor;
import org.openbravo.base.model.BaseOBObjectDef;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;

import java.io.Serializable;

/**
 * Base business object, the root of the inheritance tree for all business objects. The class model
 * here combines an inheritance structure with interface definitions. The inheritance structure is
 * used to enable some re-use of code. The interfaces are used to tag a certain implementation with
 * the functionality it provides. The outside world should use the interfaces to determine if an
 * object supports specific functionality.
 *
 * @author mtaal
 */

public abstract class BaseOBObject
    implements BaseOBObjectDef, Identifiable, DynamicEnabled, OBNotSingleton, Serializable {
  public static final String ID = "id";

  private static final Logger log = LogManager.getLogger();

  private static final long serialVersionUID = 1L;

  private Entity model = null;

  // is used to force an insert of this object. This is usefull if the id of
  // the
  // object should be preserved when it is imported
  private boolean newOBObject = false;

  // contains all the data, data is indexed by the index of the property
  // in the entity, property.getIndexInEntity()
  private Object[] data = null;
  ;

  // computed once therefore an object type
  private Boolean isDerivedReadable;

  // if set to true then derived readable is not checked
  private boolean allowRead = false;

  // Translated data, it is initialized when trying to get translated value for a translatable
  // property
  private BaseOBObject dataTrl;

  // Takes true, when dataTrl has been tried to initialize (regardless it has any value) in this way
  // it is tried only once
  private boolean hasLookedForTrl = false;

  // is used to set default data in a constructor of the generated class
  // without a security check
  protected void setDefaultValue(String propName, Object value) {
    if (!getEntity().hasProperty(propName)) {
      // ignoring warning as this always happens when the database needs to be updated
      // or when uninstalling modules.
      return;
    }
    try {
      getEntity().checkValidPropertyAndValue(propName, value);
      Check.isNotNull(value, "Null default values are not allowed");
      setDataValue(propName, value);
    } catch (ValidationException ve) {
      // do not fail here so that build tasks can still continue
      log.error(ve.getMessage(), ve);
    } catch (CheckException ce) {
      // do not fail here so that build tasks can still continue
      log.error(ce.getMessage(), ce);
    }
  }

  private Object getData(String propName) {
    return getDataValue(getEntity().getProperty(propName));
  }

  private Object getDataValue(Property p) {
    if (data == null) {
      // nothing set in this case anyway
      return null;
    }
    return data[p.getIndexInEntity()];
  }

  private void setDataValue(String propName, Object value) {
    if (data == null) {
      data = new Object[getEntity().getProperties().size()];
    }
    final Property p = getEntity().getProperty(propName);
    if (p.getIndexInEntity() >= data.length) {
      throw new IllegalArgumentException(
          "Property index (" + p.getIndexInEntity() + ") is larger than or equal to property list size (" + data.length + "). " + "This happens when setting property " + propName + " " + p + " with value " + value + " in entity " + getEntity());
    }
    data[p.getIndexInEntity()] = value;
  }

  @Override
  public Object getId() {
    return get(ID);
  }

  @Override
  public void setId(Object id) {
    set(ID, id);
  }

  @Override
  public abstract String getEntityName();

  @Override
  public String getIdentifier() {
    return IdentifierProvider.getInstance().getIdentifier(this);
  }

  /**
   * Returns the value of the {@link Property Property} identified by the propName. This method does
   * security checking. If a security violation occurs then a OBSecurityException is thrown.
   *
   * @param propName the name of the {@link Property Property} for which the value is requested
   * @see BaseOBObject#get(String, Language)
   */
  @Override
  public Object get(String propName) {
    //return get(propName, null);
    final Property p = getEntity().getProperty(propName);
    checkDerivedReadable(p);
    return getDataValue(p);
  }

  /**
   * Set a value for the {@link Property Property} identified by the propName. This method checks
   * the correctness of the value and performs security checks.
   *
   * @param propName the name of the {@link Property Property} being set
   * @param value    the value being set
   * @throws OBSecurityException , ValidationException
   */
  @Override
  public void set(String propName, Object value) {
    final Property p = getEntity().getProperty(propName);
    p.checkIsValidValue(value);
    checkDerivedReadable(p);
    p.checkIsWritable();
    setValue(propName, value);
  }

  protected void checkDerivedReadable(Property p) {
    final OBContext obContext = OBContext.getOBContext();
    // obContext can be null in the OBContext initialize method
    if (!isAllowRead() && obContext != null && obContext.isInitialized() && !obContext.isInAdministratorMode()) {
      if (isDerivedReadable == null) {
        // don't check derived readable for views
        isDerivedReadable = obContext.getEntityAccessChecker().isDerivedReadable(getEntity());
      }

      if (isDerivedReadable && !p.allowDerivedRead()) {
        throw new OBSecurityException(
            "Entity " + getEntity() + " is not directly readable, only id and identifier properties are readable, property " + p + " is neither of these.");
      }
    }
  }

  /**
   * Sets a value in the object without any security or validation checking. Should be used with
   * care. Is used by the subclasses and system classes.
   *
   * @param propName the name of the {@link Property Property} being set
   * @param value
   */
  public void setValue(String propName, Object value) {
    setDataValue(propName, value);
  }

  /**
   * Returns the value of {@link Property Property} identified by the propName. This method does not
   * do security checking.
   *
   * @param propName the name of the property for which the value is requested.
   * @return the value
   */
  public Object getValue(String propName) {
    return getData(propName);
  }

  /**
   * Return the entity of this object. The {@link Entity Entity} again gives access to the
   * {@link Property Properties} of this object.
   *
   * @return the Entity of this object
   */
  @Override
  public Entity getEntity() {
    if (model == null) {
      model = ModelProvider.getInstance().getEntity(getEntityName());
    }
    return model;
  }

  /**
   * Validates the content of this object using the property validators.
   */
  public void validate() {
    getEntity().validate(this);
  }

  @Override
  public String toString() {
    final Entity e = getEntity();
    final StringBuilder sb = new StringBuilder();
    // and also display all values
    for (final Property p : e.getIdentifierProperties()) {
      Object value = get(p.getName());
      if (value != null) {
        if (sb.length() == 0) {
          sb.append("(");
        } else {
          sb.append(", ");
        }
        if (value instanceof BaseOBObject) {
          value = ((BaseOBObject) value).getId();
        }
        sb.append(p.getName()).append(": ").append(value);
      }
    }
    if (sb.length() > 0) {
      sb.append(")");
    }
    return getEntityName() + "(" + getId() + ") " + sb.toString();
  }

  /**
   * Returns true if the id is null or the object is set to new explicitly. After flushing the
   * object to the database then new object is set to false.
   *
   * @return false if the id is set and this is not a new object, true otherwise.
   * @see OBInterceptor#postFlush(java.util.Iterator)
   */
  public boolean isNewOBObject() {
    return getId() == null || newOBObject;
  }

  public void setNewOBObject(boolean newOBObject) {
    this.newOBObject = newOBObject;
  }

  public boolean isAllowRead() {
    return allowRead;
  }

  /**
   * Sets if the object maybe read also by non-authorized users. Can only be called in admin mode
   * (see {@link OBContext#setAdminMode()}.
   *
   * @param allowRead
   */
  public void setAllowRead(boolean allowRead) {
    if (!OBContext.getOBContext().isInAdministratorMode()) {
      throw new OBSecurityException("setAllowRead may only be called in admin mode");
    }
    this.allowRead = allowRead;
  }
}
