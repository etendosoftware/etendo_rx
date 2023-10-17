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

import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.Map;

public class ContextUtils {

  public static Map<String, Object> getTokenValues(String publicKey, HttpServletRequest request) {
    String tokenHeader = request.getHeaders("X-TOKEN").nextElement();
    return getTokenValues(publicKey, tokenHeader);
  }

  public static Map<String, Object> getTokenValues(String publicKey, String token) {
    Map<String, Object> tokenValuesMap = JwtKeyUtils.getTokenValues(publicKey, token);
    JwtKeyUtils.validateTokenValues(tokenValuesMap,
        List.of(JwtKeyUtils.USER_ID_CLAIM, JwtKeyUtils.CLIENT_ID_CLAIM, JwtKeyUtils.ORG_ID,
            JwtKeyUtils.ROLE_ID /*, JwtKeyUtils.SERVICE_ID*/));
    return tokenValuesMap;
  }

}
