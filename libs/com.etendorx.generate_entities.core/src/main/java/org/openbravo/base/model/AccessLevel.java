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

package org.openbravo.base.model;

import org.etendorx.base.exception.OBException;

/**
 * Defines the available accesslevels used for an entity.
 *
 * @author Martin Taal
 */
public enum AccessLevel {
  SYSTEM, CLIENT, ORGANIZATION, CLIENT_ORGANIZATION, SYSTEM_CLIENT, ALL;

  /**
   * Returns raw value for the access level as its stored in the database
   */
  public int getDbValue() {
    switch (this) {
      case SYSTEM:
        return 4;
      case ORGANIZATION:
        return 1;
      case CLIENT_ORGANIZATION:
        return 3;
      case SYSTEM_CLIENT:
        return 6;
      case ALL:
        return 7;
      // client is not implemented..
      case CLIENT:
      default:
        throw new OBException("getDbValue called with illegal value: " + name());
    }
  }
}
