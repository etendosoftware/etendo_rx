package com.etendorx.utils.auth.key.context;

import com.etendorx.utils.auth.key.JwtKeyUtils;

import java.util.Map;

public class TokenUtil {
  public static void convertToken(UserContext userContext, String token) {
    Map<String, Object> tokenValuesMap = ContextUtils.getTokenValues(token);
    userContext.setUserId((String) tokenValuesMap.get(JwtKeyUtils.USER_ID_CLAIM));
    userContext.setClientId((String) tokenValuesMap.get(JwtKeyUtils.CLIENT_ID_CLAIM));
    userContext.setOrganizationId((String) tokenValuesMap.get(JwtKeyUtils.ORG_ID));
    userContext.setRoleId((String) tokenValuesMap.get(JwtKeyUtils.ROLE_ID));
    userContext.setSearchKey((String) tokenValuesMap.get(JwtKeyUtils.SERVICE_SEARCH_KEY));
    userContext.setServiceId((String) tokenValuesMap.get(JwtKeyUtils.SERVICE_ID));
  }
}
