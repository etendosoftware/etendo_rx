package com.etendorx.das.handler;

import com.etendorx.eventhandler.transaction.RestCallTransactionHandler;
import com.etendorx.utils.auth.key.context.UserContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * This class is used to disable triggers when a rest call is made.
 */
@Component
public class RestCallTransactionHandlerImpl implements RestCallTransactionHandler {
  private final boolean isPostgreSQL;
  @Autowired
  private TransactionTemplate transactionTemplate;

  @Autowired
  private DataSource dataSource;

  private final UserContext userContext;
  @PersistenceContext
  EntityManager entityManager;

  public RestCallTransactionHandlerImpl(UserContext userContext,
      @Value("${spring.datasource.url}") String datasourceUrl,
      TransactionTemplate transactionTemplate, DataSource dataSource) {
    this.userContext = userContext;
    this.isPostgreSQL = StringUtils.contains(datasourceUrl, "postgresql");
    this.transactionTemplate = transactionTemplate;
    this.dataSource = dataSource;
  }

  private void call(String sql) {
    if (!userContext.isTriggerEnabled()) {
      transactionTemplate.executeWithoutResult(status -> {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try {
          var stmt = connection.prepareStatement(sql);
          stmt.execute();
        } catch (SQLException e) {
          throw new RuntimeException(e);
        }
      });
    }
  }

  @Override
  public void begin() {
    this.call(getDisableStatement());
  }

  @Override
  @Transactional(Transactional.TxType.REQUIRES_NEW)
  public void commit() {
    transactionTemplate.executeWithoutResult(status -> {
      Connection connection = DataSourceUtils.getConnection(dataSource);
      try {
        var stmt = connection.prepareStatement(getEnableStatement());
        stmt.execute();
      } catch (SQLException e) {
        throw new RuntimeException("An error has ocurred while enabling triggers", e);
      }
    });
  }

  private String getDisableStatement() {
    if (isPostgreSQL) {
      return "SELECT set_config('my.triggers_disabled','Y',true)";
    } else {
      return "INSERT INTO AD_SESSION_STATUS (ad_session_status_id, ad_client_id, ad_org_id, isactive, created, createdby, updated, updatedby, isimporting)" + " VALUES (get_uuid(), '0', '0', 'Y', now(), '0', now(), '0', 'Y')";
    }
  }

  private String getEnableStatement() {
    if (isPostgreSQL) {
      return "SELECT set_config('my.triggers_disabled','N',true)";
    } else {
      return "DELETE FROM AD_SESSION_STATUS WHERE isimporting = 'Y'";
    }
  }

}
