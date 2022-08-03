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

import org.apache.commons.codec.binary.Base64;

import java.io.UnsupportedEncodingException;

/**
 * The type for a binary (image for example) column.
 *
 * @author mtaal
 */

public class BinaryDomainType extends BasePrimitiveDomainType {

  @Override public Class<?> getPrimitiveType() {
    return byte[].class;
  }

  @Override public String convertToString(Object value) {
    try {
      if (value == null) {
        return EMPTY_STRING;
      }
      return new String(Base64.encodeBase64((byte[]) value), "UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new IllegalArgumentException(e);
    }
  }

  @Override public Object createFromString(String strValue) {
    try {
      if (strValue == null || strValue.trim().length() == 0) {
        return null;
      }
      return Base64.decodeBase64(strValue.getBytes("UTF-8"));
    } catch (UnsupportedEncodingException e) {
      throw new IllegalArgumentException(e);
    }
  }

  @Override public String getXMLSchemaType() {
    return "ob:base64Binary";
  }

}
