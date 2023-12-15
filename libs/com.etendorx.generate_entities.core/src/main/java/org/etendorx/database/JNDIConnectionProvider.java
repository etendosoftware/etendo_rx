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
import org.etendorx.exception.NoConnectionAvailableException;
import org.etendorx.exception.PoolNotFoundException;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import java.io.FileInputStream;
import java.io.Serializable;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class JNDIConnectionProvider implements ConnectionProvider {
  protected static Logger log4j = LogManager.getLogger();
  protected static Map<String, org.etendorx.database.JNDIConnectionProvider.PoolInfo> pools = new HashMap();
  protected String defaultPoolName = "";

  public JNDIConnectionProvider(String file, boolean isRelative) throws PoolNotFoundException {
    if (log4j.isDebugEnabled()) {
      log4j.debug("Creating JNDIConnectionProviderImpl from file " + file);
    }

    try {
      Properties properties = new Properties();
      try (FileInputStream fis = new FileInputStream(file)) {
        properties.load(fis);
      }
      String poolName = properties.getProperty("bbdd.poolName", "myPool");
      if (log4j.isDebugEnabled()) {
        log4j.debug("poolName: " + poolName);
      }

      String jndiResourceName = properties.getProperty("JNDI.resourceName");
      if (log4j.isDebugEnabled()) {
        log4j.debug("jndiResourceName: " + jndiResourceName);
      }

      String dbSessionConfig = properties.getProperty("bbdd.sessionConfig");
      if (log4j.isDebugEnabled()) {
        log4j.debug("dbSessionConfig: " + dbSessionConfig);
      }

      String rdbms = properties.getProperty("bbdd.rdbms");
      if (log4j.isDebugEnabled()) {
        log4j.debug("rdbms: " + rdbms);
      }

      Context initctx = new InitialContext();
      Context ctx = (Context) initctx.lookup("java:/comp/env");
      if (log4j.isDebugEnabled()) {
        log4j.debug("Connected to java:/comp/env");
      }

      DataSource ds = (DataSource) ctx.lookup(jndiResourceName);
      if (log4j.isDebugEnabled()) {
        log4j.debug("Datasource retrieved from JNDI server. Resource " + jndiResourceName);
      }

      pools.put(poolName,
          new org.etendorx.database.JNDIConnectionProvider.PoolInfo(poolName, ds, rdbms,
              dbSessionConfig));
      if (log4j.isDebugEnabled()) {
        log4j.debug("Added to pools");
      }

      if ("".equals(this.defaultPoolName)) {
        this.defaultPoolName = poolName;
      }

      Connection con = null;
      try {
        log4j.info("Initializing connection...");
        con = ds.getConnection();
        log4j.info(" Got connection {}", con);
        try (PreparedStatement pstmt = con.prepareStatement(dbSessionConfig)) {
          log4j.debug("Prepared statement with query: {}", dbSessionConfig);
          try (var ignored = pstmt.executeQuery()) {
            log4j.debug("Executed query");
          }
        }
        log4j.debug("Connection initialized");
      } finally {
        if (con != null) {
          con.close();
        }
      }
      log4j.debug("Created JNDI ConnectionProvider");
    } catch (Exception var17) {
      log4j.error("Error creating JNDI connection", var17);
      throw new PoolNotFoundException(
          "Failed when creating database connections pool: " + var17.getMessage());
    }
  }

  public Connection getConnection() throws NoConnectionAvailableException {
    return this.getConnection(this.defaultPoolName);
  }

  public Connection getConnection(String poolName) throws NoConnectionAvailableException {
    Connection conn = null;

    try {
      conn = ((org.etendorx.database.JNDIConnectionProvider.PoolInfo) pools.get(
          poolName)).ds.getConnection();
      return conn;
    } catch (SQLException var4) {
      throw new NoConnectionAvailableException(var4.getMessage());
    }
  }

  public String getRDBMS() {
    return this.getRDBMS(this.defaultPoolName);
  }

  public String getRDBMS(String poolName) {
    return ((org.etendorx.database.JNDIConnectionProvider.PoolInfo) pools.get(poolName)).rdbms;
  }

  protected boolean releaseConnection(Connection conn) {
    if (conn == null) {
      return false;
    } else {
      try {
        conn.close();
        return true;
      } catch (Exception var3) {
        log4j.error("Error releasing connection", var3);
        return false;
      }
    }
  }

  public Connection getTransactionConnection() throws NoConnectionAvailableException, SQLException {
    Connection conn = this.getConnection();
    if (conn == null) {
      throw new NoConnectionAvailableException("Couldn't get an available connection");
    } else {
      conn.setAutoCommit(false);
      return conn;
    }
  }

  public void releaseCommitConnection(Connection conn) throws SQLException {
    if (conn != null) {
      conn.commit();
      this.releaseConnection(conn);
    }

  }

  public void releaseRollbackConnection(Connection conn) throws SQLException {
    if (conn != null) {
      conn.rollback();
      this.releaseConnection(conn);
    }

  }

  public PreparedStatement getPreparedStatement(String SQLPreparedStatement) throws Exception {
    return this.getPreparedStatement(this.defaultPoolName, SQLPreparedStatement);
  }

  public PreparedStatement getPreparedStatement(String poolName, String SQLPreparedStatement)
      throws Exception {
    if (poolName != null && !poolName.equals("")) {
      if (log4j.isDebugEnabled()) {
        log4j.debug("connection requested");
      }

      Connection conn = this.getConnection(poolName);
      if (log4j.isDebugEnabled()) {
        log4j.debug("connection established");
      }

      return this.getPreparedStatement(conn, SQLPreparedStatement);
    } else {
      throw new PoolNotFoundException("Can't get the pool. No pool name specified");
    }
  }

  public PreparedStatement getPreparedStatement(Connection conn, String SQLPreparedStatement)
      throws SQLException {
    if (conn != null && SQLPreparedStatement != null && !SQLPreparedStatement.equals("")) {
      PreparedStatement ps = null;

      try {
        if (log4j.isDebugEnabled()) {
          log4j.debug("preparedStatement requested");
        }

        ps = conn.prepareStatement(SQLPreparedStatement, 1004, 1007);
        if (log4j.isDebugEnabled()) {
          log4j.debug("preparedStatement received");
        }

        return ps;
      } catch (SQLException var5) {
        log4j.error("getPreparedStatement: " + SQLPreparedStatement + "\n" + var5);
        this.releaseConnection(conn);
        throw var5;
      }
    } else {
      return null;
    }
  }

  public CallableStatement getCallableStatement(String SQLCallableStatement) throws Exception {
    return this.getCallableStatement(this.defaultPoolName, SQLCallableStatement);
  }

  public CallableStatement getCallableStatement(String poolName, String SQLCallableStatement)
      throws Exception {
    if (poolName != null && !poolName.equals("")) {
      Connection conn = this.getConnection(poolName);
      return this.getCallableStatement(conn, SQLCallableStatement);
    } else {
      throw new PoolNotFoundException("Can't get the pool. No pool name specified");
    }
  }

  public CallableStatement getCallableStatement(Connection conn, String SQLCallableStatement)
      throws SQLException {
    if (conn != null && SQLCallableStatement != null && !SQLCallableStatement.equals("")) {
      CallableStatement cs = null;

      try {
        cs = conn.prepareCall(SQLCallableStatement);
        return cs;
      } catch (SQLException var5) {
        log4j.error("getCallableStatement: " + SQLCallableStatement + "\n" + var5);
        this.releaseConnection(conn);
        throw var5;
      }
    } else {
      return null;
    }
  }

  public Statement getStatement() throws Exception {
    return this.getStatement(this.defaultPoolName);
  }

  public Statement getStatement(String poolName) throws Exception {
    if (poolName != null && !poolName.equals("")) {
      Connection conn = this.getConnection(poolName);
      return this.getStatement(conn);
    } else {
      throw new PoolNotFoundException("Can't get the pool. No pool name specified");
    }
  }

  public Statement getStatement(Connection conn) throws SQLException {
    if (conn == null) {
      return null;
    } else {
      try {
        return conn.createStatement(1004, 1007);
      } catch (SQLException var3) {
        log4j.error("getStatement: " + var3);
        this.releaseConnection(conn);
        throw var3;
      }
    }
  }

  public void releasePreparedStatement(PreparedStatement preparedStatement) throws SQLException {
    if (preparedStatement != null) {
      Connection conn = null;

      try {
        conn = preparedStatement.getConnection();
        preparedStatement.close();
        this.releaseConnection(conn);
      } catch (SQLException var4) {
        log4j.error("releasePreparedStatement: " + var4);
        this.releaseConnection(conn);
        throw var4;
      }
    }
  }

  public void releaseCallableStatement(CallableStatement callableStatement) throws SQLException {
    if (callableStatement != null) {
      Connection conn = null;

      try {
        conn = callableStatement.getConnection();
        callableStatement.close();
        this.releaseConnection(conn);
      } catch (SQLException var4) {
        log4j.error("releaseCallableStatement: " + var4);
        this.releaseConnection(conn);
        throw var4;
      }
    }
  }

  public void releaseStatement(Statement statement) throws SQLException {
    if (statement != null) {
      Connection conn = null;

      try {
        conn = statement.getConnection();
        statement.close();
        this.releaseConnection(conn);
      } catch (SQLException var4) {
        log4j.error("releaseStatement: " + var4);
        this.releaseConnection(conn);
        throw var4;
      }
    }
  }

  public void releaseTransactionalStatement(Statement statement) throws SQLException {
    if (statement != null) {
      statement.close();
    }
  }

  public void releaseTransactionalPreparedStatement(PreparedStatement preparedStatement)
      throws SQLException {
    if (preparedStatement != null) {
      preparedStatement.close();
    }
  }

  public void destroy() {
  }

  public String getStatus() {
    return "Not implemented yet";
  }

  protected class PoolInfo implements Serializable {
    private static final long serialVersionUID = 1L;
    public String name = null;
    public DataSource ds = null;
    public String rdbms = null;
    public String dbSession = null;

    public PoolInfo(String name, DataSource ds, String rdbms, String dbSession) {
      this.name = name;
      this.ds = ds;
      this.rdbms = rdbms;
      this.dbSession = dbSession;
    }
  }
}
