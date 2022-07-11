package com.etendorx.das.handler.context;

import com.etendorx.utils.auth.key.JwtKeyUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

public class ContextUtils {

    public static Map<String, Object> getTokenValues(HttpServletRequest request) {
        String tokenHeader = request.getHeaders("X-TOKEN").nextElement();
        return getTokenValues(tokenHeader);
    }

    public static Map<String, Object> getTokenValues(String token) {
        Map<String, Object> tokenValuesMap = JwtKeyUtils.getTokenValues(token);
        JwtKeyUtils.validateTokenValues(tokenValuesMap,
                List.of(JwtKeyUtils.USER_ID_CLAIM, JwtKeyUtils.CLIENT_ID_CLAIM, JwtKeyUtils.ORG_ID_CLAIM));
        return tokenValuesMap;
    }

}
