package com.etendorx.das.hibernate_interceptor;

import org.hibernate.EmptyInterceptor;
import org.springframework.stereotype.Component;

import com.etendorx.das.utils.DefaultFilters;
import com.etendorx.utils.auth.key.context.AppContext;
import com.etendorx.utils.auth.key.context.UserContext;

import lombok.extern.slf4j.Slf4j;


@Component
@Slf4j
public class CustomInterceptor extends EmptyInterceptor {

    @Override
    public String onPrepareStatement (String sql) {
        UserContext userContext = AppContext.getCurrentUser();
        String userId = userContext.getUserId();
        String clientId = userContext.getClientId();
        String orgId = userContext.getOrganizationId();
        boolean isActive = userContext.isActive();
        String restMethod = userContext.getRestMethod();
        String finalQuery = DefaultFilters.addFilters(restMethod, sql, userId, clientId, orgId, isActive);
        return super.onPrepareStatement(finalQuery);
    }
}

