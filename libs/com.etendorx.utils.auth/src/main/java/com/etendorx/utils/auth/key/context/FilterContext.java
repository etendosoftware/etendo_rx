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

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

@Component
public class FilterContext extends OncePerRequestFilter {
  public static final String HEADER_TOKEN = "X-TOKEN";
  public static final String TRUE = "true";
  public static final String FALSE = "false";

  @Autowired
  private UserContext userContext;

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {
    String token = request.getHeader(HEADER_TOKEN);
    if (token != null && !token.isBlank()) {
      setUserContextFromToken(token, request);
    } else {
      throw new ForbiddenException();
    }
    AppContext.setCurrentUser(userContext);
    filterChain.doFilter(request, response);
  }

  public void setUserContextFromToken(String token, HttpServletRequest request) {
    setUserContextFromToken(userContext, token, request.getParameter("active"), request.getMethod());
  }

  public static void setUserContextFromToken(UserContext userContext, String token, String activeParam,
      String restMethod) {
    Map<String, Object> tokenValuesMap = ContextUtils.getTokenValues(token);
    userContext.setUserId((String) tokenValuesMap.get(JwtKeyUtils.USER_ID_CLAIM));
    userContext.setClientId((String) tokenValuesMap.get(JwtKeyUtils.CLIENT_ID_CLAIM));
    userContext.setOrganizationId((String) tokenValuesMap.get(JwtKeyUtils.ORG_ID));
    userContext.setRoleId((String) tokenValuesMap.get(JwtKeyUtils.ROLE_ID));
    userContext.setSearchKey((String) tokenValuesMap.get(JwtKeyUtils.SERVICE_SEARCH_KEY));
    userContext.setServiceId((String) tokenValuesMap.get(JwtKeyUtils.SERVICE_ID));
    boolean active = false;
    if(activeParam == null) {
        active = true;
    } else {
      if (!StringUtils.equalsAny(activeParam, TRUE, FALSE)) {
        throw new IllegalArgumentException("Invalid value for 'active' parameter: " + active);
      }
      active = activeParam.equals(TRUE);
    }
    userContext.setActive(active);
    userContext.setAuthToken(token);
    userContext.setRestMethod(restMethod);
  }
}
