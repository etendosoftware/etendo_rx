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

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.etendorx.utils.auth.key.JwtKeyUtils;
import com.etendorx.utils.auth.key.config.JwtClassicConfig;
import com.etendorx.utils.auth.key.exceptions.ForbiddenException;
import jakarta.servlet.http.HttpServletRequest;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ContextUtils {

  public static Map<String, Object> getTokenValues(String publicKey, HttpServletRequest request) {
    String tokenHeader = request.getHeaders("X-TOKEN").nextElement();
    return getTokenValues(publicKey, tokenHeader);
  }

  public static Map<String, Object> getTokenValues(String publicKey, String token) {
    Map<String, Object> tokenValuesMap = JwtKeyUtils.getTokenValues(publicKey, token);
    if (tokenValuesMap == null) {
      throw new ForbiddenException("Invalid token");
    }
    validate(tokenValuesMap);
    return tokenValuesMap;
  }

  public static Map<String, Object> getTokenValues(JwtClassicConfig jwtClassicConfig, String token)
      throws UnsupportedEncodingException {

    // Etendo Classic JWT Token compatibility
    Algorithm algorithm = Algorithm.HMAC256(jwtClassicConfig.getPrivateKey());
    JWTVerifier verifier = JWT.require(algorithm).withIssuer("sws").build();
    DecodedJWT jwt = verifier.verify(token);
    // Convert the token values to the expected values
    Map<String, Object> convertedMap = new HashMap<>();
    convertedMap.put(JwtKeyUtils.USER_ID_CLAIM, jwt.getClaim("user").asString());
    convertedMap.put(JwtKeyUtils.CLIENT_ID_CLAIM, jwt.getClaim("client").asString());
    convertedMap.put(JwtKeyUtils.ORG_ID, jwt.getClaim("organization").asString());
    convertedMap.put(JwtKeyUtils.ROLE_ID, jwt.getClaim("role").asString());
    convertedMap.put(JwtKeyUtils.SERVICE_ID, "");

    validate(convertedMap);
    return convertedMap;
  }

  private static void validate(Map<String, Object> dataMap) {
    JwtKeyUtils.validateTokenValues(dataMap,
        List.of(JwtKeyUtils.USER_ID_CLAIM, JwtKeyUtils.CLIENT_ID_CLAIM, JwtKeyUtils.ORG_ID,
            JwtKeyUtils.ROLE_ID, JwtKeyUtils.SERVICE_ID));
  }
}
