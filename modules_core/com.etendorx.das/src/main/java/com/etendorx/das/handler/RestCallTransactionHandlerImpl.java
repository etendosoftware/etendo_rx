/*
 * Copyright 2022-2023  Futit Services SL
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
package com.etendorx.das.handler;

import com.etendorx.eventhandler.transaction.RestCallTransactionHandler;
import com.etendorx.utils.auth.key.context.UserContext;
import jakarta.transaction.Transactional;
import org.apache.commons.lang3.StringUtils;
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
  private final TransactionTemplate transactionTemplate;
  private final DataSource dataSource;
  private final UserContext userContext;

  public RestCallTransactionHandlerImpl(UserContext userContext,
      @Value("${spring.datasource.url}") String datasourceUrl,
      TransactionTemplate transactionTemplate, DataSource dataSource) {
    this.userContext = userContext;
    this.isPostgreSQL = StringUtils.contains(datasourceUrl, "postgresql");
    this.transactionTemplate = transactionTemplate;
    this.dataSource = dataSource;
  }

  /**
   * Must be called on transaction begin.
   */
  @Override
  public void begin() {
    executeStatement(getDisableStatement());
  }

  /**
   * Must be called on transaction commit. This method will enable triggers.
   */
  @Override
  @Transactional(Transactional.TxType.REQUIRES_NEW)
  public void commit() {
    executeStatement(getEnableStatement());
  }

  /**
   * Executes the given SQL statement in case the user context has triggers disabled.
   *
   * @param sql
   */
  private void executeStatement(String sql) {
    if (!userContext.isTriggerEnabled()) {
      transactionTemplate.executeWithoutResult(status -> {
        try {
          Connection connection = DataSourceUtils.getConnection(dataSource);
          var stmt = connection.prepareStatement(sql);
          stmt.execute();
        } catch (SQLException e) {
          throw new RuntimeException("An error occurred while executing the SQL statement", e);
        }
      });
    }
  }

  /**
   * Returns the SQL statement to disable triggers. This statement is different depending on the
   * database engine.
   *
   * @return SQL statement
   */
  private String getDisableStatement() {
    if (isPostgreSQL) {
      return "SELECT set_config('my.triggers_disabled','Y',true)";
    } else {
      return "INSERT INTO AD_SESSION_STATUS (ad_session_status_id, ad_client_id, ad_org_id, isactive, created, createdby, updated, updatedby, isimporting)" + " VALUES (get_uuid(), '0', '0', 'Y', now(), '0', now(), '0', 'Y')";
    }
  }

  /**
   * Returns the SQL statement to enable triggers. This statement is different depending on the
   * database engine.
   *
   * @return SQL statement
   */
  private String getEnableStatement() {
    if (isPostgreSQL) {
      return "SELECT set_config('my.triggers_disabled','N',true)";
    } else {
      return "DELETE FROM AD_SESSION_STATUS WHERE isimporting = 'Y'";
    }
  }

}
