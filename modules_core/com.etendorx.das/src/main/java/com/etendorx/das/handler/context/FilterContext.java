package com.etendorx.das.handler.context;

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

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String token = request.getHeader("X-TOKEN");
        if (token != null && !token.isBlank()) {
            UserContext userContext = new UserContext();
            Map<String, Object> tokenValuesMap = ContextUtils.getTokenValues(token);

            userContext.setUserId((String) tokenValuesMap.get(JwtKeyUtils.USER_ID_CLAIM));
            userContext.setClientId((String) tokenValuesMap.get(JwtKeyUtils.CLIENT_ID_CLAIM));
            userContext.setOrganizationId((String) tokenValuesMap.get(JwtKeyUtils.ORG_ID_CLAIM));
            AppContext.setCurrentUser(userContext);
        }
        filterChain.doFilter(request, response);
    }
}
