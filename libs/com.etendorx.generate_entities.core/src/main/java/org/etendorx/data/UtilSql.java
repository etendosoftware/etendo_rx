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

package org.etendorx.data;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class UtilSql {
  public UtilSql() {
  }

  public static boolean setValue(PreparedStatement ps, int posicion, int tipo, String strDefault,
                                 String _strValor) {
    String strValor = _strValor;

    try {
      if (strValor == null) {
        strValor = strDefault;
      }

      if (strValor != null) {
        if (strValor.compareTo("") == 0) {
          ps.setNull(posicion, tipo);
        } else {
          switch (tipo) {
            case -1:
              ps.setString(posicion, strValor);
              break;
            case 0:
              ps.setDouble(posicion, Double.valueOf(strValor));
              break;
            case 2:
              ps.setLong(posicion, Long.valueOf(strValor));
              break;
            case 12:
              ps.setString(posicion, strValor);
          }
        }
      } else {
        ps.setNull(posicion, tipo);
      }

      return true;
    } catch (Exception var7) {
      var7.printStackTrace();
      return false;
    }
  }

  public static String getValue(ResultSet result, String strField) throws SQLException {
    String strValueReturn = result.getString(strField);
    if (result.wasNull()) {
      strValueReturn = "";
    }

    return strValueReturn;
  }

  public static String getValue(ResultSet result, int pos) throws SQLException {
    String strValueReturn = result.getString(pos);
    if (result.wasNull()) {
      strValueReturn = "";
    }

    return strValueReturn;
  }

  public static String getDateValue(ResultSet result, String strField, String strDateFormat)
    throws SQLException {
    Date date = result.getDate(strField);
    String strValueReturn;
    if (result.wasNull()) {
      strValueReturn = "";
    } else {
      SimpleDateFormat formatter = new SimpleDateFormat(strDateFormat);
      strValueReturn = formatter.format(date);
    }

    return strValueReturn;
  }

  public static String getDateTimeValue(ResultSet result, String strField, String strDateFormat)
    throws SQLException {
    Timestamp timestamp = result.getTimestamp(strField);
    String strValueReturn;
    if (result.wasNull()) {
      strValueReturn = "";
    } else {
      SimpleDateFormat formatter = new SimpleDateFormat(strDateFormat);
      strValueReturn = formatter.format(timestamp);
    }

    return strValueReturn;
  }

  public static String getDateValue(ResultSet result, String strField) throws SQLException {
    return getDateValue(result, strField, "dd-MM-yyyy");
  }

  public static String getBlobValue(ResultSet result, String strField) throws SQLException {
    String strValueReturn = "";
    Blob blob = result.getBlob(strField);
    if (result.wasNull()) {
      strValueReturn = "";
    } else {
      int length = (int) blob.length();
      if (length > 0) {
        strValueReturn = new String(blob.getBytes(1L, length));
      }
    }

    return strValueReturn;
  }

  public static String getStringCallableStatement(CallableStatement cs, int intField)
    throws SQLException {
    String strValueReturn = cs.getString(intField);
    if (strValueReturn == null) {
      strValueReturn = "";
    }

    return strValueReturn;
  }
}
