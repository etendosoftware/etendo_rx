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

import org.etendorx.base.exception.OBException;

/**
 * Is thrown when an Exception situation occurs in the XML to Entity or the Entity to XML code.
 *
 * @author mtaal
 * @see EntityXMLConverter
 * @see XMLEntityConverter
 */
public class EntityXMLException extends OBException {

  private static final long serialVersionUID = 1L;

  public EntityXMLException() {
    super();
  }

  public EntityXMLException(String message, Throwable cause) {
    super(message, cause);
  }

  public EntityXMLException(String message) {
    super(message);
  }

  public EntityXMLException(Throwable cause) {
    super(cause);
  }
}
