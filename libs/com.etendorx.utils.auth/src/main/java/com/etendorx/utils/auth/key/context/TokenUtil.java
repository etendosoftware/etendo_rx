/*
 * Copyright 2022-2023  Futit Services SL
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
import com.etendorx.utils.auth.key.config.JwtClassicConfig;
import com.etendorx.utils.auth.key.exceptions.ForbiddenException;

import java.util.Map;

public class TokenUtil {

  TokenUtil() {
  }

  /**
   * Set the user context from the token
   *
   * @param publicKey the public key
   * @param token     the token
   */
  public static void convertToken(UserContext userContext, String publicKey, JwtClassicConfig jwtClassicConfig, String token) {
    Map<String, Object> tokenValuesMap = null;
    if(publicKey != null) {
      try {
        tokenValuesMap = ContextUtils.getTokenValues(publicKey, token);
      } catch (Exception ignored) {
      }
    }
    if(tokenValuesMap == null && jwtClassicConfig != null) {
      try {
        tokenValuesMap = ContextUtils.getTokenValues(jwtClassicConfig, token);
      } catch (Exception ignored) {
      }
    }
    if(tokenValuesMap == null) {
      throw new ForbiddenException("Invalid token");
    }
    userContext.setUserId((String) tokenValuesMap.get(JwtKeyUtils.USER_ID_CLAIM));
    userContext.setClientId((String) tokenValuesMap.get(JwtKeyUtils.CLIENT_ID_CLAIM));
    userContext.setOrganizationId((String) tokenValuesMap.get(JwtKeyUtils.ORG_ID));
    userContext.setRoleId((String) tokenValuesMap.get(JwtKeyUtils.ROLE_ID));
    userContext.setSearchKey((String) tokenValuesMap.get(JwtKeyUtils.SERVICE_SEARCH_KEY));
    userContext.setServiceId((String) tokenValuesMap.get(JwtKeyUtils.SERVICE_ID));
  }
}
