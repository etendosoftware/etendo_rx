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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/**
 * The type used in the Product Characteristic reference.
 */
public class ProductCharacteristicsDomainType extends StringDomainType {
  final private static Logger log = LogManager.getLogger();

  /**
   * Columns that use the ProductCharacteristics reference store in the database a String
   * representation of the product characteristics. Under certain circumstances the value sent from
   * the client to the datasource is not that String, but a JSON object that contains it in its
   * dbValue property. In that case, return the dbValue property
   *
   * @param value
   *     the value sent from the client to the datasource
   * @return the String representation of the product characteristics
   */
  public static Object fixValue(Object value) {
    Object fixedValue = value;
    // if the value is a JSONObject that contains the dbValue property, return that property
    if (value instanceof JSONObject) {
      JSONObject jsonObjectValue = (JSONObject) value;
      if (jsonObjectValue.has("dbValue")) {
        try {
          fixedValue = jsonObjectValue.get("dbValue");
        } catch (JSONException e) {
          log.error("Exception while getting a value from a json object", e);
        }
      }
    }
    return fixedValue;
  }
}
