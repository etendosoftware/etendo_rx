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

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.etendorx.utils.auth.key.JwtKeyUtils;
import com.etendorx.utils.auth.key.exceptions.ForbiddenException;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class FilterContext extends OncePerRequestFilter {
  public static final String HEADER_TOKEN = "X-TOKEN";
  public static final String TRUE = "true";
  public static final String FALSE = "false";
  public static final String NO_ACTIVE_FILTER_PARAMETER = "_noActiveFilter";
  public static final String TRIGGER_ENABLED_PARAMETER = "triggerEnabled";
  private static final String DATE_FORMAT_PARAMETER = "_dateFormat";
  private static final String TIME_ZONE_PARAMETER = "_timeZone";
  @Autowired
  private UserContext userContext;
  @Autowired(required = false)
  private Set<AllowedURIS> allowedURIS;
  @Value("${public-key:}")
  String publicKey;

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {
    String token = request.getHeader(HEADER_TOKEN);
    if (!StringUtils.isEmpty(token)) {
      setUserContextFromToken(token, request);
    } else {
      if (allowedURIS == null) {
        throw new ForbiddenException("No URIs are allowed for this service");
      }
      Optional<AllowedURIS> optionalAllowedUri = allowedURIS.stream()
          .filter(uri -> uri.isAllowed(request.getRequestURI()))
          .findFirst();
      if (optionalAllowedUri.isEmpty()) {
        throw new ForbiddenException(request.getRequestURI() + " is not allowed for this credentials");
      }
    }
    AppContext.setCurrentUser(userContext);
    filterChain.doFilter(request, response);
  }

  public void setUserContextFromToken(String token, HttpServletRequest request) {
    setUserContextFromToken(userContext, publicKey, token, request);
  }



  public static void setUserContextFromToken(UserContext userContext, String token, HttpServletRequest req) {
    TokenUtil.convertToken(userContext, token);
    String noActiveFilterParameter = req.getParameter(NO_ACTIVE_FILTER_PARAMETER);
    String triggerEnabledParam = req.getParameter(TRIGGER_ENABLED_PARAMETER);
    String dateFormatParam = req.getParameter(DATE_FORMAT_PARAMETER);
    String timeZoneParam = req.getParameter(TIME_ZONE_PARAMETER);
    String restMethod = req.getMethod();
    boolean noActiveFilter = !parseBooleanParameter(noActiveFilterParameter, NO_ACTIVE_FILTER_PARAMETER, false);
    userContext.setActive(noActiveFilter);
    userContext.setAuthToken(token);
    userContext.setRestMethod(restMethod);
    boolean isTriggerEnabled = parseBooleanParameter(triggerEnabledParam, TRIGGER_ENABLED_PARAMETER, true);
    userContext.setTriggerEnabled(isTriggerEnabled);
    if(StringUtils.isNotBlank(dateFormatParam)) {
      userContext.setDateFormat(dateFormatParam);
    }
    if(StringUtils.isNotBlank(dateFormatParam)) {
      userContext.setTimeZone(timeZoneParam);
    }
  }

  private static boolean parseBooleanParameter(String paramValueStr, String nameParam, boolean defaultValue) {
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
