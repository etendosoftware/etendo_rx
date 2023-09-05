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

package org.etendorx.database;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SessionInfo {
  private static final String JDBC_CONNECTION_POOL_CLASS_NAME = "org.openbravo.apachejdbcconnectionpool.JdbcExternalConnectionPool";
  private static final Logger log4j = LogManager.getLogger();
  public static final String IMPORT_ENTRY_PROCESS = "IE";
  private static boolean isAuditActive = false;
  private static boolean usageAuditActive = false;
  private static boolean initialized = false;
  private static ThreadLocal<String> sessionId = new ThreadLocal();
  private static ThreadLocal<String> userId = new ThreadLocal();
  private static ThreadLocal<String> processType = new ThreadLocal();
  private static ThreadLocal<String> processId = new ThreadLocal();
  private static ThreadLocal<String> command = new ThreadLocal();
  private static ThreadLocal<String> queryProfile = new ThreadLocal();
  private static ThreadLocal<Connection> sessionConnection = new ThreadLocal();
  private static ThreadLocal<Boolean> changedInfo = new ThreadLocal();
  private static ThreadLocal<String> moduleId = new ThreadLocal();
  private static ThreadLocal<Boolean> auditThisThread = new ThreadLocal();

  public SessionInfo() {
  }

  public static void init() {
    sessionId.set(null);
    userId.set(null);
    processType.set(null);
    processId.set(null);
    changedInfo.set(null);
    moduleId.set(null);
    command.set(null);
    queryProfile.set(null);
    auditThisThread.set(true);
    Connection conn = (Connection) sessionConnection.get();

    try {
      if (conn != null && !conn.isClosed()) {
        log4j.debug("Close session's connection");
        conn.setAutoCommit(true);
        conn.close();
      }
    } catch (SQLException var2) {
      log4j.error("Error closing sessionConnection", var2);
    }

    sessionConnection.set(null);
  }

  public static void initDB(Connection conn, String rdbms) {
    if (adContextInfoShouldBeCreated(conn, rdbms)) {
      createAdContextInfoTable(conn);
    }

  }

  private static void createAdContextInfoTable(Connection conn) {
    String sql = "CREATE TEMPORARY TABLE AD_CONTEXT_INFO(AD_USER_ID VARCHAR(32),   AD_SESSION_ID VARCHAR(32),  PROCESSTYPE VARCHAR(60),   PROCESSID VARCHAR(32)) on commit preserve rows";

    try {
      PreparedStatement psCreate = getPreparedStatement(conn, sql);
      if(psCreate == null) {
        throw new IllegalStateException("Error creating AD_CONTEXT_INFO table");
      }
      try {
        psCreate.execute();
      } catch (SQLException var6) {
        log4j.error("Error creating AD_CONTEXT_INFO table", var6);
      } finally {
        psCreate.close();
      }
    } catch (SQLException var7) {
      log4j.error("Error initializating audit infrastructure", var7);
      throw new IllegalStateException(var7);
    }
  }

  private static boolean adContextInfoShouldBeCreated(Connection conn, String rdbms) {
    if (isAuditActive && isPosgreSQL(rdbms) && !isReadOnly(conn)) {
      if (usingJdbcConnectionPool()) {
        return true;
      } else {
        return !adContextInfoExists(conn);
      }
    } else {
      return false;
    }
  }

  private static boolean isReadOnly(Connection conn) {
    boolean readOnly = false;

    try {
      readOnly = conn.isReadOnly();
    } catch (SQLException var3) {
      log4j.error("Error checking if the connection is read only", var3);
    }

    return readOnly;
  }

  private static boolean isPosgreSQL(String rdbms) {
    return rdbms != null && rdbms.equals("POSTGRE");
  }

  private static boolean usingJdbcConnectionPool() {
    return org.etendorx.database.ExternalConnectionPool.getInstance() == null ?
      false :
      "org.openbravo.apachejdbcconnectionpool.JdbcExternalConnectionPool".equals(
        ExternalConnectionPool.getInstance().getClass().getName());
  }

  private static boolean adContextInfoExists(Connection conn) {
    String query = "select count(*) from information_schema.tables where table_name='ad_context_info' and table_type = 'LOCAL TEMPORARY'";
    try (
        PreparedStatement psQuery = conn.prepareStatement(query);
        ResultSet rs = psQuery.executeQuery()
    ) {
      return rs.next() && !rs.getString(1).equals("0");
    } catch (SQLException ex) {
      log4j.error("Error checking if the ad_context_info table exists", ex);
    }
    return false;
  }

  /**
   * @deprecated
   */
  @Deprecated
  static void setDBSessionInfo(Connection conn, boolean onlyIfChanged) {
    if (!isAuditActive || onlyIfChanged && (changedInfo.get() == null || !(Boolean) changedInfo.get())) {
      if (log4j.isDebugEnabled()) {
        boolean var10001 = isAuditActive;
        log4j.debug(
          "No session info set isAuditActive: {} - changes in info: {}", var10001, changedInfo.get());
      }
    } else {
      saveContextInfoIntoDB(conn);
    }
  }

  /**
   * @deprecated
   */
  @Deprecated
  public static void setDBSessionInfo(Connection conn) {
    saveContextInfoIntoDB(conn);
  }

  public static void saveContextInfoIntoDB(Connection conn) {
    if (isAuditActive) {
      PreparedStatement psCleanUp = null;
      PreparedStatement psInsert = null;

      try {
        boolean infoModified = Boolean.TRUE.equals(
          changedInfo.get()) || sessionConnection.get() == null || !conn.equals(
          sessionConnection.get());
        if (infoModified && !Boolean.FALSE.equals(auditThisThread.get()) && !isReadOnly(conn)) {
          if (log4j.isDebugEnabled()) {
            Logger var10000 = log4j;
            String var10001 = getUserId();
            var10000.debug(
              "saving DB context info " + var10001 + " - " + getSessionId() + " - " + getProcessType() + " - " + getProcessId());
          }

          psCleanUp = getPreparedStatement(conn, "delete from ad_context_info");
          psCleanUp.executeUpdate();
          psInsert = getPreparedStatement(conn,
            "insert into ad_context_info (ad_user_id, ad_session_id, processType, processId) values (?, ?, ?, ?)");
          psInsert.setString(1, getUserId());
          psInsert.setString(2, getSessionId());
          psInsert.setString(3, getProcessType());
          psInsert.setString(4, getProcessId());
          psInsert.executeUpdate();
          if (conn == sessionConnection.get()) {
            changedInfo.set(false);
          }

          return;
        }
      } catch (Exception var7) {
        log4j.error("Error setting audit info", var7);
        return;
      } finally {
        releasePreparedStatement(psCleanUp);
        releasePreparedStatement(psInsert);
      }

    }
  }

  /**
   * @deprecated
   */
  @Deprecated
  public static void setDBSessionInfo(Connection conn, String rdbms) {
    if (isAuditActive) {
      initDB(conn, rdbms);
      setDBSessionInfo(conn);
    }
  }

  static Connection getSessionConnection() {
    Connection conn = (Connection) sessionConnection.get();

    try {
      if (conn == null || conn.isClosed()) {
        return null;
      }
    } catch (SQLException var2) {
      log4j.error("Error checking connection", var2);
      return null;
    }

    log4j.debug("Reuse session's connection");
    return conn;
  }

  private static PreparedStatement getPreparedStatement(Connection conn, String sql)
    throws SQLException {
    if (conn != null && sql != null && !sql.equals("")) {
      PreparedStatement ps = null;

      try {
        log4j.trace("preparedStatement requested");
        ps = conn.prepareStatement(sql, 1004, 1007);
      } catch (SQLException var6) {
        log4j.error("getPreparedStatement: " + sql, var6);

        try {
          conn.setAutoCommit(true);
          conn.close();
        } catch (Exception var5) {
          log4j.error("Could not close PreparedStatement for " + sql, var5);
        }
      }

      return ps;
    } else {
      return null;
    }
  }

  private static void releasePreparedStatement(PreparedStatement ps) {
    if (ps != null) {
      try {
        ps.close();
      } catch (Exception var2) {
        log4j.error("Error closing PreparedStatement", var2);
      }
    }

  }

  public static void setUserId(String user) {
    if (user == null || !user.equals(getUserId())) {
      userId.set(user);
      changedInfo.set(true);
    }

  }

  public static String getUserId() {
    return (String) userId.get();
  }

  public static void setProcessId(String processId) {
    if (processId == null || !processId.equals(getProcessId())) {
      org.etendorx.database.SessionInfo.processId.set(processId);
      changedInfo.set(true);
    }

  }

  public static String getProcessId() {
    return (String) processId.get();
  }

  public static void setProcessType(String processType) {
    if (processType == null || !processType.equals(getProcessType())) {
      org.etendorx.database.SessionInfo.processType.set(processType);
      changedInfo.set(true);
    }

  }

  public static String getProcessType() {
    return (String) processType.get();
  }

  public static void setSessionId(String session) {
    if (session == null || !session.equals(getSessionId())) {
      sessionId.set(session);
      changedInfo.set(true);
    }

  }

  public static void infoChanged() {
    changedInfo.set(true);
  }

  public static String getCommand() {
    return (String) command.get();
  }

  public static void setCommand(String comm) {
    command.set(comm);
  }

  public static String getQueryProfile() {
    return (String) queryProfile.get();
  }

  public static void setQueryProfile(String profile) {
    queryProfile.set(profile);
  }

  public static String getSessionId() {
    return (String) sessionId.get();
  }

  public static void setAuditActive(boolean isAuditActive) {
    org.etendorx.database.SessionInfo.isAuditActive = isAuditActive;
    initialized = true;
  }

  public static boolean isInitialized() {
    return initialized;
  }

  static void setSessionConnection(Connection conn) {
    sessionConnection.set(conn);
  }

  public static String getModuleId() {
    return (String) moduleId.get();
  }

  public static void setModuleId(String moduleId) {
    org.etendorx.database.SessionInfo.moduleId.set(moduleId);
  }

  public static boolean isUsageAuditActive() {
    return usageAuditActive;
  }

  public static void setUsageAuditActive(boolean usageAuditActive) {
    org.etendorx.database.SessionInfo.usageAuditActive = usageAuditActive;
  }

  public static void auditThisThread(boolean shouldAudit) {
    auditThisThread.set(shouldAudit);
  }
}
