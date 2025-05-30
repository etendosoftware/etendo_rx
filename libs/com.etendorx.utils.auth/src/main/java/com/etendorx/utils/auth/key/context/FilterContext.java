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

import com.etendorx.utils.auth.key.config.JwtClassicConfig;
import com.etendorx.utils.auth.key.exceptions.ForbiddenException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

@Component
@Slf4j
public class FilterContext extends OncePerRequestFilter {
  public static final String HEADER_TOKEN = "X-TOKEN";
  private static final String HEADER_AUTHORIZATION = "Authorization";
  public static final String TRUE = "true";
  public static final String FALSE = "false";
  public static final String NO_ACTIVE_FILTER_PARAMETER = "_noActiveFilter";
  public static final String TRIGGER_ENABLED_PARAMETER = "_triggerEnabled";
  private static final String DATE_TIME_FORMAT_PARAMETER = "_dateTimeFormat";
  private static final String DATE_FORMAT_PARAMETER = "_dateFormat";
  private static final String TIME_ZONE_PARAMETER = "_timeZone";
  @Autowired
  private UserContext userContext;
  @Autowired(required = false)
  private Set<AllowedURIS> allowedURIS;
  @Value("${public-key:}")
  String publicKey;
  @Value("${auth.token:}")
  String tokenYaml;
  @Autowired(required = false)
  private JwtClassicConfig jwtClassicConfig;

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {
    String token = request.getHeader(HEADER_TOKEN);
    if (StringUtils.isEmpty(token)) {
      String authHeader = request.getHeader(HEADER_AUTHORIZATION);
      token = StringUtils.substringAfter(authHeader, "Bearer ");
      if (StringUtils.isEmpty(token)) {
        token = tokenYaml;
      }
    }
    if (!StringUtils.isEmpty(token)) {
      // The token can be signed by RX Auth key or Etendo Classic SWS key
      setUserContextFromToken(publicKey, jwtClassicConfig, token, request);
    } else {
      if (allowedURIS == null) {
        throw new ForbiddenException("No URIs are allowed for this service");
      }
      Optional<AllowedURIS> optionalAllowedUri = allowedURIS.stream()
          .filter(uri -> uri.isAllowed(request.getRequestURI()))
          .findFirst();
      if (optionalAllowedUri.isEmpty()) {
        throw new ForbiddenException(
            request.getRequestURI() + " is not allowed for this credentials");
      }
    }
    AppContext.setCurrentUser(userContext);
    filterChain.doFilter(request, response);
  }

  public void setUserContextFromToken(String publicKey, JwtClassicConfig classicConfig, String token, HttpServletRequest request) {
    setUserContextFromToken(userContext, publicKey, classicConfig , token, request);
  }

  public static void setUserContextFromToken(UserContext userContext, String publicKey,
      JwtClassicConfig jwtClassicConfig, String token, HttpServletRequest req) {
    TokenUtil.convertToken(userContext, publicKey, jwtClassicConfig, token);
    // Get request parameters and set them in the user context
    String noActiveFilterParameter = req.getParameter(NO_ACTIVE_FILTER_PARAMETER);
    String triggerEnabledParam = req.getParameter(TRIGGER_ENABLED_PARAMETER);
    String dateFormatParam = req.getParameter(DATE_FORMAT_PARAMETER);
    String dateTimeFormatParam = req.getParameter(DATE_TIME_FORMAT_PARAMETER);
    String timeZoneParam = req.getParameter(TIME_ZONE_PARAMETER);
    String restMethod = req.getMethod();
    boolean noActiveFilter = !parseBooleanParameter(noActiveFilterParameter,
        NO_ACTIVE_FILTER_PARAMETER, false);
    userContext.setActive(noActiveFilter);
    userContext.setAuthToken(token);
    userContext.setRestMethod(restMethod);
    userContext.setRestUri(req.getRequestURI());
    boolean isTriggerEnabled = parseBooleanParameter(triggerEnabledParam, TRIGGER_ENABLED_PARAMETER,
        true);
    userContext.setTriggerEnabled(isTriggerEnabled);
    if (StringUtils.isNotBlank(dateTimeFormatParam)) {
      userContext.setDateTimeFormat(dateTimeFormatParam);
    }
    if (StringUtils.isNotBlank(dateFormatParam)) {
      userContext.setDateFormat(dateFormatParam);
    }
    if (StringUtils.isNotBlank(dateFormatParam)) {
      userContext.setTimeZone(timeZoneParam);
    }
    String externalSystemId = req.getParameter("externalSystemId");
    if(StringUtils.isNotBlank(externalSystemId)) {
      userContext.setExternalSystemId(externalSystemId);
    }

  }

  private static boolean parseBooleanParameter(String paramValueStr, String nameParam,
      boolean defaultValue) {
    boolean valueParam;
    if (paramValueStr == null) {
      valueParam = defaultValue;
    } else {
      if (!StringUtils.equalsAny(paramValueStr, TRUE, FALSE)) {
        throw new IllegalArgumentException(
            String.format("Invalid value for '%s' parameter: %s", nameParam, paramValueStr));
      }
      valueParam = StringUtils.equals(paramValueStr, TRUE);
    }
    return valueParam;
  }
}
