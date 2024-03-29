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

package com.etendorx.utils.auth.key.context;

import com.etendorx.utils.auth.key.JwtKeyUtils;
import com.etendorx.utils.auth.key.exceptions.ForbiddenException;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ContextUtils {

  public static Map<String, Object> getTokenValues(HttpServletRequest request) {
    String tokenHeader = request.getHeaders("X-TOKEN").nextElement();
    return getTokenValues(tokenHeader);
  }

  public static Map<String, Object> getTokenValues(String token) {
    Map<String, Object> tokenValuesMap = JwtKeyUtils.getTokenValues(token);
    if(tokenValuesMap == null) {
      throw new ForbiddenException("Invalid token");
    }
    if(tokenValuesMap.containsKey("aud") && StringUtils.equals((String) tokenValuesMap.get("aud"), "sws")) {
      // SWS
      // Convert the token values to the expected values
      Map<String, Object> convertedMap = new HashMap<>();
      convertedMap.put(JwtKeyUtils.USER_ID_CLAIM, tokenValuesMap.get("user"));
      convertedMap.put(JwtKeyUtils.CLIENT_ID_CLAIM, tokenValuesMap.get("client"));
      convertedMap.put(JwtKeyUtils.ORG_ID, tokenValuesMap.get("organization"));
      convertedMap.put(JwtKeyUtils.ROLE_ID, tokenValuesMap.get("role"));
      convertedMap.put(JwtKeyUtils.SERVICE_ID, "");
      tokenValuesMap = convertedMap;
    }
    JwtKeyUtils.validateTokenValues(tokenValuesMap,
          List.of(JwtKeyUtils.USER_ID_CLAIM, JwtKeyUtils.CLIENT_ID_CLAIM, JwtKeyUtils.ORG_ID,
              JwtKeyUtils.ROLE_ID, JwtKeyUtils.SERVICE_ID));
    return tokenValuesMap;
  }

}
