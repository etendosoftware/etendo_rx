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
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

@Component
public class FilterContext extends OncePerRequestFilter {
  public static final String HEADER_TOKEN = "X-TOKEN";

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
    String token = request.getHeader(HEADER_TOKEN);

    if (token != null && !token.isBlank()) {
      setUserContextFromToken(token, request);
    }
    filterChain.doFilter(request, response);
  }

  public static void setUserContextFromToken(String token, HttpServletRequest request) {
    AppContext.setAuthToken(token);
    UserContext userContext = new UserContext();
    Map<String, Object> tokenValuesMap = ContextUtils.getTokenValues(token);
    userContext.setUserId((String) tokenValuesMap.get(JwtKeyUtils.USER_ID_CLAIM));
    userContext.setClientId((String) tokenValuesMap.get(JwtKeyUtils.CLIENT_ID_CLAIM));
    userContext.setOrganizationId((String) tokenValuesMap.get(JwtKeyUtils.ORG_ID));
    userContext.setRoleId((String) tokenValuesMap.get(JwtKeyUtils.ROLE_ID));
    userContext.setSearchKey((String) tokenValuesMap.get(JwtKeyUtils.SERVICE_SEARCH_KEY));
    userContext.setServiceId((String) tokenValuesMap.get(JwtKeyUtils.SERVICE_ID));
    userContext.setActive(request.getParameter("active"));
    AppContext.setCurrentUser(userContext);
  }
}
