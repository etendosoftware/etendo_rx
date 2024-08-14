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

import org.etendorx.base.exception.OBException;
import org.etendorx.base.session.OBPropertiesProvider;
import org.etendorx.base.validation.ValidationException;
import org.openbravo.base.model.*;

import java.lang.reflect.Field;

/**
 * The base class for property types which refer to an entity (foreign key).
 *
 * @author mtaal
 */

public abstract class BaseForeignKeyDomainType extends BaseDomainType
    implements ForeignKeyDomainType {

  /**
   * @return the refered to column based on the table encoded in the table name of the passed
   * column. This method also handles exceptional column names in a specific way.
   */
  @Override
  public Column getForeignKeyColumn(String columnName) {

    try {
      return getModelProvider().getTable(getReferedTableName(columnName))
          .getPrimaryKeyColumns()
          .get(0);
    } catch (final Exception e) {
      if (OBPropertiesProvider.isFriendlyWarnings()) {
        // won't be logged
        throw new IllegalArgumentException(
            "Reference column for " + columnName + " not found in runtime model [ref: " + getReference().getId() + ", encountered exception " + e.getMessage(),
            e);
      } else {
        throw new OBException(
            "Reference column for " + columnName + " not found in runtime model [ref: " + getReference().getId() + ", encountered exception " + e.getMessage(),
            e);
      }
    }
  }

  /**
   * Needs to be implemented by subclass. If the subclass can not return the tablename then it can
   * return null. The tablename is used to check that valid object is set in properties with this
   * domain type.
   *
   * @return the table name of the table name refered to by this domain type.
   */
  protected String getReferedTableName(String columnName) {
    // Removing _ID from tableName based on Openbravo's naming
    // convention
    String tableName = columnName.substring(0, columnName.length() - 3);

    // TODO: solve references in the application dictionary
    // Special Cases
    if (tableName.equals("Ref_OrderLine")) {
      tableName = "C_OrderLine";
    }

    if (columnName.equals("C_Settlement_Cancel_ID") || columnName.equals(
        "C_Settlement_Generate_ID")) {
      tableName = "C_Settlement";
    }

    if (columnName.equals("Fact_Acct_Ref_ID")) {
      tableName = "Fact_Acct";
    }

    if (columnName.equals("Account_ID")) {
      tableName = "C_ElementValue";
    }

    if (columnName.equalsIgnoreCase("CreatedBy") || columnName.equalsIgnoreCase("UpdatedBy")) {
      tableName = "AD_User";
    }

    // the reference can be null if the domain type is
    // used on its own
    if (getReference() != null) {
      if (getReference().getId().equals(Reference.PRODUCT_ATTRIBUTE)) {
        tableName = "M_AttributeSetInstance";
      }

      if (getReference().getId().equals(Reference.IMAGE_BLOB)) {
        tableName = "AD_Image";
      }
    }
    return tableName;
  }

  @Override
  public void checkIsValidValue(Property property, Object value) throws ValidationException {
    if (value == null) {
      return;
    }
    if (!(value instanceof BaseOBObjectDef)) {
      final ValidationException ve = new ValidationException();
      ve.addMessage(property,
          "Property " + property + " only allows reference instances of type " + BaseOBObjectDef.class.getName() + " but the value is an instanceof " + value.getClass()
              .getName());
      throw ve;
    }

    final BaseOBObjectDef obObject = (BaseOBObjectDef) value;

    // note object equalness is required!
    // use a special way to get the entity name
    // to prevent proxy loading
    final String entityName = getEntityName(obObject);
    final Entity entity = ModelProvider.getInstance().getEntity(entityName);

    if (getReferedEntity(property) != null && entity != getReferedEntity(property)) {
      final ValidationException ve = new ValidationException();
      ve.addMessage(property, "Property " + property + " only allows entity: " + getReferedEntity(
          property) + " but the value (" + value + ") has entity " + obObject.getEntity());
      throw ve;
    }
    return;
  }

  // Note can't use DalUtil.getEntityName as then we get compile time
  // dependencies on generated classes.
  private String getEntityName(BaseOBObjectDef o) {
    if (o instanceof org.hibernate.proxy.HibernateProxy) {
      try {
        final Class<?> clz = ((org.hibernate.proxy.HibernateProxy) o).getHibernateLazyInitializer()
            .getPersistentClass();
        final Field fld = clz.getField("ENTITY_NAME");
        return (String) fld.get(null);
      } catch (final Exception e) {
        throw new OBException(e);
      }
    }
    return o.getEntity().getName();
  }

  /**
   * Returns the entity refered to by this foreign key domain type. Note that for TableDir domain
   * types this method always returns null.
   *
   * @param property property for this domain type, the property is needed because the domain type is
   *                 shared by different properties.
   * @return the entity to which this domain type refers, is null in case of TableDir.
   */
  protected Entity getReferedEntity(Property property) {
    return ModelProvider.getInstance()
        .getEntityByTableName(getReferedTableName(property.getColumnName()));
  }

}
