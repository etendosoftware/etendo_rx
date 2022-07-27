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

import org.etendorx.base.provider.OBProvider;
import org.etendorx.base.provider.OBSingleton;
import org.etendorx.base.session.OBPropertiesProvider;
import org.etendorx.dal.core.DalUtil;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Provides the identifier/title of an object using the {@link Entity#getIdentifierProperties()
 * identifierProperties} of the {@link Entity Entity}.
 *
 * Note: the getIdentifier can also be generated in the java entity but the current approach makes
 * it possible to change the identifier definition at runtime.
 *
 * @author mtaal
 */

public class IdentifierProvider implements OBSingleton {

  public static final String SEPARATOR = " - ";
  private static IdentifierProvider instance;

  private SimpleDateFormat dateFormat = null;
  private SimpleDateFormat dateTimeFormat = null;
  private SimpleDateFormat timeFormat = null;

  public static synchronized IdentifierProvider getInstance() {
    if (instance == null) {
      instance = OBProvider.getInstance().get(IdentifierProvider.class);
    }
    return instance;
  }

  public static synchronized void setInstance(IdentifierProvider instance) {
    IdentifierProvider.instance = instance;
  }

  /**
   * Returns the identifier of the object. The identifier is computed using the identifier
   * properties of the Entity of the object. It is translated (if applicable) to the current
   * language
   *
   * @param o
   *     the object for which the identifier is generated
   * @return the identifier
   */
  public String getIdentifier(Object o) {
    return getIdentifier(o, true /*, lang */);
  }

  // identifyDeep determines if refered to objects are used
  // to identify the object
  private String getIdentifier(Object o, boolean identifyDeep) {
    // TODO: add support for null fields
    final StringBuilder sb = new StringBuilder();
    final org.etendorx.base.structure.DynamicEnabled dob = (DynamicEnabled) o;
    final String entityName = ((org.etendorx.base.structure.Identifiable) dob).getEntityName();
    final List<Property> identifiers = ModelProvider.getInstance()
        .getEntity(entityName)
        .getIdentifierProperties();

    for (final Property identifier : identifiers) {
      if (sb.length() > 0) {
        sb.append(SEPARATOR);
      }
      Property property = ((org.etendorx.base.structure.BaseOBObject) dob).getEntity()
          .getProperty(identifier.getName());
      Object value;

      if (property.hasDisplayColumn()) {
        Property displayColumnProperty = DalUtil.getPropertyFromPath(
            property.getReferencedProperty().getEntity(), property.getDisplayPropertyName());
        org.etendorx.base.structure.BaseOBObject referencedObject = (org.etendorx.base.structure.BaseOBObject) dob.get(
            property.getName());
        if (referencedObject == null) {
          continue;
        }
        if (displayColumnProperty.hasDisplayColumn()) {
          // Allowing one level deep of displayed column pointing to references with display column
          value = ((org.etendorx.base.structure.BaseOBObject) dob.get(
              property.getDisplayPropertyName())).get(
              displayColumnProperty.getDisplayPropertyName());
        } else if (!displayColumnProperty.isPrimitive()) {
          // Displaying identifier for non primitive properties

          value = ((org.etendorx.base.structure.BaseOBObject) referencedObject.get(
              property.getDisplayPropertyName())).getIdentifier();
        } else {
          value = referencedObject.get(property.getDisplayPropertyName() /*, language*/);
        }

        // Assign displayColumnProperty to apply formatting if needed
        property = displayColumnProperty;
      } else if (property.isTranslatable()) {
        // Trying to get id of translatable object.
        Object id = dob.get("id");
        if (id instanceof org.etendorx.base.structure.BaseOBObject) {
          // When the object is created for a drop down list filter, it is incorrect: id is not a
          // String but a BaseOBject. This code deals with this exception.

          // TODO: once issue #23706 is fixed, this should not be needed anymore
          id = ((org.etendorx.base.structure.BaseOBObject) id).get("id");
        }

        if (id instanceof String) {
          value = dob.get((String) id);
        } else {
          // give up, couldn't find the id
          value = dob.get(identifier.getName());
        }

      } else if (!property.isPrimitive() && identifyDeep) {
        if (dob.get(property.getName()) != null) {
          value = ((BaseOBObject) dob.get(property.getName())).getIdentifier();
        } else {
          value = "";
        }
      } else {
        value = dob.get(identifier.getName());
      }

      if (value instanceof org.etendorx.base.structure.Identifiable && identifyDeep) {
        sb.append(getIdentifier(value, false /*, language*/));
      } else if (value != null) {

        // TODO: add number formatting...
        if (property.isDate() || property.isDatetime() || property.isAbsoluteDateTime()) {
          value = formatDate(property, (Date) value);
        } else if (property.isTime() || property.isAbsoluteTime()) {
          value = formatTime(property, (Date) value);
        }

        sb.append(value);
      }
    }
    if (identifiers.size() == 0) {
      return entityName + " (" + ((Identifiable) dob).getId() + ")";
    }
    return sb.toString();
  }

  protected String getSeparator() {
    return SEPARATOR;
  }

  private synchronized String formatDate(Property property, Date date) {
    if (date == null) {
      return "";
    }
    if (dateFormat == null) {
      final String dateFormatString = OBPropertiesProvider.getInstance()
          .getOpenbravoProperties()
          .getProperty("dateFormat.java");
      final String dateTimeFormatString = OBPropertiesProvider.getInstance()
          .getOpenbravoProperties()
          .getProperty("dateTimeFormat.java");
      dateFormat = new SimpleDateFormat(dateFormatString);
      dateTimeFormat = new SimpleDateFormat(dateTimeFormatString);
    }
    if (property.isDatetime() || property.isAbsoluteDateTime()) {
      return dateTimeFormat.format(date);
    } else {
      return dateFormat.format(date);
    }
  }

  private synchronized String formatTime(Property property, Date date) {
    if (date == null) {
      return "";
    }
    if (timeFormat == null) {
      final String dateTimeFormatString = OBPropertiesProvider.getInstance()
          .getOpenbravoProperties()
          .getProperty("dateTimeFormat.java");
      if (dateTimeFormatString.toUpperCase().endsWith("A")) {
        timeFormat = new SimpleDateFormat("hh:mm:ss a");
      } else {
        timeFormat = new SimpleDateFormat("HH:mm:ss");
      }
    }
    return timeFormat.format(date);
  }
}
