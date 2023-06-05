package com.etendorx.das.hibernate_interceptor;

import com.etendorx.das.utils.DefaultFilters;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.EmptyInterceptor;
import org.springframework.stereotype.Component;

import static com.etendorx.utils.auth.key.context.AppContext.getCurrentUser;


@Component
@Slf4j
public class CustomInterceptor extends EmptyInterceptor {

    @Override
    public String onPrepareStatement (String sql) {
        String userId = getCurrentUser().getUserId();
        String clientId = getCurrentUser().getClientId();
        String orgId = getCurrentUser().getOrganizationId();
        String isActive = getCurrentUser().getActive();
        String restMethod = getCurrentUser().getRestMethod();
        String finalQuery = DefaultFilters.addFilters(restMethod, sql, userId, clientId, orgId, isActive);
        return super.onPrepareStatement(finalQuery);
    }
}

