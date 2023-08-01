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

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.etendorx.utils.auth.key.JwtKeyUtils;
import com.etendorx.utils.auth.key.exceptions.ForbiddenException;

@Component
public class FilterContext extends OncePerRequestFilter {
  public static final String HEADER_TOKEN = "X-TOKEN";
  public static final String TRUE = "true";
  public static final String FALSE = "false";
  public static final String ACTIVE_PARAMETER = "active";
  public static final String TRIGGER_ENABLED_PARAMETER = "triggerEnabled";
  @Autowired
  private UserContext userContext;
  @Autowired(required = false)
  private AllowedURIS allowedURIS;

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {
    String token = request.getHeader(HEADER_TOKEN);
    if (!StringUtils.isEmpty(token)) {
      setUserContextFromToken(token, request);
    } else if (allowedURIS == null || !allowedURIS.isAllowed(request.getRequestURI())) {
      throw new ForbiddenException();
    }
    AppContext.setCurrentUser(userContext);
    filterChain.doFilter(request, response);
  }

  public void setUserContextFromToken(String token, HttpServletRequest request) {
    setUserContextFromToken(new UserContext(), token, request);
  }

  public static void setUserContextFromToken(UserContext userContext, String token, HttpServletRequest req) {
    String activeParam = req.getParameter(ACTIVE_PARAMETER);
    String triggerEnabledParam = req.getParameter(TRIGGER_ENABLED_PARAMETER);
    String restMethod = req.getMethod();
    Map<String, Object> tokenValuesMap = ContextUtils.getTokenValues(token);
    userContext.setUserId((String) tokenValuesMap.get(JwtKeyUtils.USER_ID_CLAIM));
    userContext.setClientId((String) tokenValuesMap.get(JwtKeyUtils.CLIENT_ID_CLAIM));
    userContext.setOrganizationId((String) tokenValuesMap.get(JwtKeyUtils.ORG_ID));
    userContext.setRoleId((String) tokenValuesMap.get(JwtKeyUtils.ROLE_ID));
    userContext.setSearchKey((String) tokenValuesMap.get(JwtKeyUtils.SERVICE_SEARCH_KEY));
    userContext.setServiceId((String) tokenValuesMap.get(JwtKeyUtils.SERVICE_ID));
    boolean active = getBooleanParameter(activeParam, ACTIVE_PARAMETER);
    userContext.setActive(active);
    userContext.setAuthToken(token);
    userContext.setRestMethod(restMethod);
    boolean isTriggerEnabled = getBooleanParameter(triggerEnabledParam, TRIGGER_ENABLED_PARAMETER);
    userContext.setTriggerEnabled(isTriggerEnabled);
  }

  private static boolean getBooleanParameter(String paramValueStr, String nameParam) {
    boolean valueParam;
    if (paramValueStr == null) {
      valueParam = true;
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
