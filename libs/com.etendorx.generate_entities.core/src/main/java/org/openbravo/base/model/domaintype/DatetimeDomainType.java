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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * The type for a datetime column.
 *
 * @author mtaal
 */

public class DatetimeDomainType extends BasePrimitiveDomainType {

  private final SimpleDateFormat xmlDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S'Z'");

  public DatetimeDomainType() {
    xmlDateFormat.setLenient(true);
  }

  /**
   * @return class of the {@link Date}
   */
  @Override
  public Class<?> getPrimitiveType() {
    return Date.class;
  }

  @Override
  public synchronized String convertToString(Object value) {
    if (value == null) {
      return EMPTY_STRING;
    }
    return xmlDateFormat.format(value);
  }

  @Override
  public synchronized Object createFromString(String strValue) {
    try {
      if (strValue == null || strValue.trim().length() == 0) {
        return null;
      }
      return xmlDateFormat.parse(strValue);
    } catch (ParseException e) {
      throw new IllegalArgumentException(e);
    }
  }

  @Override
  public String getXMLSchemaType() {
    return "ob:dateTime";
  }
}
