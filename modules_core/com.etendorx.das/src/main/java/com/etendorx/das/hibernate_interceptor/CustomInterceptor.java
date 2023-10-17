package com.etendorx.das.hibernate_interceptor;

import com.etendorx.das.utils.DefaultFilters;
import com.etendorx.utils.auth.key.context.AppContext;
import com.etendorx.utils.auth.key.context.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CustomInterceptor implements StatementInspector {

    @Override
    public String inspect(String sql) {
        UserContext userContext = AppContext.getCurrentUser();
        String userId = userContext.getUserId();
        String clientId = userContext.getClientId();
        String roleId = userContext.getRoleId();
        boolean isActive = userContext.isActive();
        String restMethod = userContext.getRestMethod();
      return DefaultFilters.addFilters(sql, userId, clientId, roleId, isActive, restMethod);
    }
}

