package com.etendorx.das.hibernate_interceptor;

import com.etendorx.das.utils.DefaultFilters;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.EmptyInterceptor;
import org.springframework.stereotype.Component;

import com.etendorx.utils.auth.key.context.AppContext;
import com.etendorx.utils.auth.key.context.UserContext;

@Component
@Slf4j
public class CustomInterceptor extends EmptyInterceptor {

    @Override
    public String onPrepareStatement (String sql) {
        UserContext userContext = AppContext.getCurrentUser();
        String userId = userContext.getUserId();
        String clientId = userContext.getClientId();
        String roleId = userContext.getRoleId();
        boolean isActive = userContext.isActive();
        String restMethod = userContext.getRestMethod();
        String finalQuery = DefaultFilters.addFilters(sql, userId, clientId, roleId, isActive, restMethod);
        return super.onPrepareStatement(finalQuery);
    }
}

