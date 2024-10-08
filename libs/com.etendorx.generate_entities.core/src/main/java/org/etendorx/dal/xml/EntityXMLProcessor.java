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

package org.etendorx.dal.xml;

import org.etendorx.base.structure.BaseOBObject;
import org.openbravo.base.model.Property;

import java.util.List;

/**
 * The entity xml processor is used to intercept specific actions during import and xml conversion
 * of business objects.
 *
 * @author mtaal
 */

public interface EntityXMLProcessor {

  /**
   * This method is called after the import process has parsed the xml and created the in-memory
   * object graph of objects which are inserted and updated in the database.
   * <p>
   * This method can access the database using the Data Access Layer. It will operate in the same
   * transaction as the import process itself.
   *
   * @param newObjects     the list of objects which will be inserted into the database
   * @param updatedObjects the list of objects which will be updated in the database
   */
  public void process(List<BaseOBObject> newObjects, List<BaseOBObject> updatedObjects);

  /**
   * This method allows you to correct a value just before it is being set in an object which is
   * being imported.
   * <p>
   * This method is called just before a new primitive or refernence value is set in an imported
   * object. This is called during an import of an object so the object maybe in an invalid state.
   * <p>
   * This method is not called for one-to-many properties.
   *
   * @param owner         the owner object of the property
   * @param property      the property being set
   * @param importedValue the value converted from the import xml
   * @return a new value which is used by the import process
   */
  public Object replaceValue(BaseOBObject owner, Property property, Object importedValue);
}
