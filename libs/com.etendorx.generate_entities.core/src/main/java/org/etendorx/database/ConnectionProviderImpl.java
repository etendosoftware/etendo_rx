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

import org.apache.commons.dbcp.ConnectionFactory;
import org.apache.commons.dbcp.PoolableConnectionFactory;
import org.apache.commons.dbcp.PoolingDriver;
import org.apache.commons.pool.KeyedObjectPoolFactory;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.commons.pool.impl.StackKeyedObjectPoolFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.etendorx.exception.NoConnectionAvailableException;
import org.etendorx.exception.PoolNotFoundException;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.*;
import java.util.Properties;

public class ConnectionProviderImpl implements ConnectionProvider {
  static Logger log4j = LogManager.getLogger();
  String defaultPoolName;
  String bbdd;
  String rdbms;
  String contextName;
  private String externalPoolClassName;
  private static ExternalConnectionPool externalConnectionPool;

  public ConnectionProviderImpl(Properties properties) throws PoolNotFoundException {
    this.defaultPoolName = "";
    this.bbdd = "";
    this.rdbms = "";
    this.contextName = "etendo";
    this.create(properties, false, "etendo");
  }

  public ConnectionProviderImpl(String file) throws PoolNotFoundException {
    this(file, false, "etendo");
  }

  public ConnectionProviderImpl(String file, String _context) throws PoolNotFoundException {
    this(file, false, _context);
  }

  public ConnectionProviderImpl(String file, boolean isRelative, String _context)
    throws PoolNotFoundException {
    this.defaultPoolName = "";
    this.bbdd = "";
    this.rdbms = "";
    this.contextName = "etendo";
    this.create(file, isRelative, _context);
  }

  private void create(String file, boolean isRelative, String _context)
    throws PoolNotFoundException {
    Properties properties = new Properties();

    try(FileInputStream fis = new FileInputStream(file)) {
      properties.load(fis);
      this.create(properties, isRelative, _context);
    } catch (IOException var6) {
      log4j.error("Error loading properties", var6);
    }

  }

  private void create(Properties properties, boolean isRelative, String providedContext) throws PoolNotFoundException {
    log4j.debug("Creating ConnectionProviderImpl");

    // Set context if provided
    if (providedContext != null && !providedContext.isEmpty()) {
      this.contextName = providedContext;
    }

    String poolName = properties.getProperty("bbdd.poolName", "myPool");
    this.externalPoolClassName = properties.getProperty("db.externalPoolClassName", "");
    String dbDriver = properties.getProperty("bbdd.driver", "");
    String dbServer = properties.getProperty("bbdd.url", "");
    String dbLogin = properties.getProperty("bbdd.user", "");
    String dbPassword = properties.getProperty("bbdd.password", "");
    int minConns = Integer.parseInt(properties.getProperty("bbdd.minConns", "1"));
    int maxConns = Integer.parseInt(properties.getProperty("bbdd.maxConns", "10"));
    double maxConnTime = Double.parseDouble(properties.getProperty("maxConnTime", "0.5"));
    String dbSessionConfig = properties.getProperty("bbdd.sessionConfig", "");
    String rdbmsType = properties.getProperty("bbdd.rdbms", "");

    // Update dbServer if rdbmsType is POSTGRE
    if ("POSTGRE".equalsIgnoreCase(rdbmsType)) {
      dbServer = dbServer + "/" + properties.getProperty("bbdd.sid");
    }

    if (log4j.isDebugEnabled()) {
      log4j.debug("poolName: {}", poolName);
      log4j.debug("externalPoolClassName: {}", this.externalPoolClassName);
      log4j.debug("dbDriver: {}", dbDriver);
      log4j.debug("dbServer: {}", dbServer);
      log4j.debug("dbLogin: {}", dbLogin);
      log4j.debug("dbPassword: {}", dbPassword);
      log4j.debug("minConns: {}", minConns);
      log4j.debug("maxConns: {}", maxConns);
      log4j.debug("maxConnTime: {}", maxConnTime);
      log4j.debug("dbSessionConfig: {}", dbSessionConfig);
      log4j.debug("rdbms: {}", rdbmsType);
    }

    if (this.externalPoolClassName != null && !this.externalPoolClassName.isEmpty()) {
      try {
        externalConnectionPool = ExternalConnectionPool.getInstance(this.externalPoolClassName);
      } catch (Exception e) {
        externalConnectionPool = null;
        this.externalPoolClassName = null;
      }
    }

    try {
      this.addNewPool(dbDriver, dbServer, dbLogin, dbPassword, minConns, maxConns, maxConnTime,
          dbSessionConfig, rdbmsType, poolName);
    } catch (Exception e) {
      log4j.error(e);
      throw new PoolNotFoundException("Failed when creating database connections pool", e);
    }
  }


  public void destroy(String name) throws Exception {
    if (externalConnectionPool != null) {
      externalConnectionPool.closePool();
      externalConnectionPool = null;
    } else {
      PoolingDriver driver = (PoolingDriver) DriverManager.getDriver("jdbc:apache:commons:dbcp:");
      driver.closePool(name);
    }

  }

  public void reload(String file, boolean isRelative, String _context) throws Exception {
    this.destroy();
    this.create(file, isRelative, _context);
  }

  public void destroy() throws Exception {
    this.destroy(this.defaultPoolName);
  }

  public void addNewPool(String dbDriver, String dbServer, String dbLogin, String dbPassword,
                         int minConns, int maxConns, double maxConnTime, String dbSessionConfig, String _rdbms,
                         String name) throws Exception {
    if (this.defaultPoolName == null || this.defaultPoolName.equals("")) {
      this.defaultPoolName = name;
      this.bbdd = dbServer;
      this.rdbms = _rdbms;
    }

    if (externalConnectionPool == null) {
      log4j.debug("Loading underlying JDBC driver.");

      try {
        Class.forName(dbDriver);
      } catch (ClassNotFoundException var17) {
        throw new Exception(var17);
      }

      log4j.debug("Done.");
      GenericObjectPool connectionPool = new GenericObjectPool((PoolableObjectFactory) null);
      connectionPool.setWhenExhaustedAction((byte) 2);
      connectionPool.setMaxActive(maxConns);
      connectionPool.setTestOnBorrow(false);
      connectionPool.setTestOnReturn(false);
      connectionPool.setTestWhileIdle(false);
      KeyedObjectPoolFactory keyedObject = new StackKeyedObjectPoolFactory();
      ConnectionFactory connectionFactory = new OpenbravoDriverManagerConnectionFactory(dbServer,
        dbLogin, dbPassword, dbSessionConfig, _rdbms);
      new PoolableConnectionFactory(connectionFactory, connectionPool, keyedObject, (String) null,
        false, true);
      Class.forName("org.apache.commons.dbcp.PoolingDriver");
      PoolingDriver driver = (PoolingDriver) DriverManager.getDriver("jdbc:apache:commons:dbcp:");
      driver.registerPool(this.contextName + "_" + name, connectionPool);
    }
  }

  public ObjectPool getPool(String poolName) throws PoolNotFoundException {
    if (poolName != null && !poolName.equals("")) {
      ObjectPool connectionPool = null;

      try {
        PoolingDriver driver = (PoolingDriver) DriverManager.getDriver("jdbc:apache:commons:dbcp:");
        connectionPool = driver.getConnectionPool(this.contextName + "_" + poolName);
      } catch (SQLException var4) {
        log4j.error(var4);
      }

      if (connectionPool == null) {
        throw new PoolNotFoundException(poolName + " not found");
      } else {
        return connectionPool;
      }
    } else {
      throw new PoolNotFoundException("Couldn´t get an unnamed pool");
    }
  }

  public ObjectPool getPool() throws PoolNotFoundException {
    return this.getPool(this.defaultPoolName);
  }

  public Connection getConnection() throws NoConnectionAvailableException {
    return this.getConnection(this.defaultPoolName);
  }

  public Connection getConnection(String poolName) throws NoConnectionAvailableException {
    if (poolName != null && !poolName.equals("")) {
      Connection conn = SessionInfo.getSessionConnection();
      if (conn == null) {
        conn = this.getNewConnection(poolName);
        SessionInfo.setSessionConnection(conn);
      }

      return conn;
    } else {
      throw new NoConnectionAvailableException("Couldn´t get a connection for an unnamed pool");
    }
  }

  private Connection getNewConnection(String poolName) throws NoConnectionAvailableException {
    // Check for empty poolName
    if (poolName == null || poolName.trim().isEmpty()) {
      throw new NoConnectionAvailableException("Couldn't get a connection for an unnamed pool");
    }

    // Check and instantiate externalConnectionPool if necessary
    if (externalConnectionPool == null && this.externalPoolClassName != null && !this.externalPoolClassName.trim().isEmpty()) {
      try {
        externalConnectionPool = ExternalConnectionPool.getInstance(this.externalPoolClassName);
      } catch (ReflectiveOperationException throwable) {
        externalConnectionPool = null;
        this.externalPoolClassName = null;
      }
    }

    // Obtain the connection from the relevant pool
    return (externalConnectionPool != null)
        ? externalConnectionPool.getConnection()
        : this.getCommonsDbcpPoolConnection(poolName);
  }


  private Connection getCommonsDbcpPoolConnection(String poolName)
    throws NoConnectionAvailableException {
    if (poolName != null && !poolName.equals("")) {
      Connection conn = null;

      try {
        conn = DriverManager.getConnection(
          "jdbc:apache:commons:dbcp:" + this.contextName + "_" + poolName);
        return conn;
      } catch (SQLException var4) {
        log4j.error("Error getting connection", var4);
        throw new NoConnectionAvailableException(
          "There are no connections available in jdbc:apache:commons:dbcp:" + this.contextName + "_" + poolName);
      }
    } else {
      throw new NoConnectionAvailableException("Couldn´t get a connection for an unnamed pool");
    }
  }

  public String getRDBMS() {
    return this.rdbms;
  }

  public boolean releaseConnection(Connection conn) {
    if (conn == null) {
      return false;
    } else {
      try {
        conn.setAutoCommit(true);
        if (SessionInfo.getSessionConnection() == null) {
          log4j.debug("close connection directly (no connection in session)");
          if (!conn.isClosed()) {
            conn.close();
          }
        }

        return true;
      } catch (Exception var3) {
        log4j.error("Error on releaseConnection", var3);
        return false;
      }
    }
  }

  private boolean closeConnection(Connection conn) {
    if (conn == null) {
      return false;
    } else {
      try {
        conn.setAutoCommit(true);
        conn.close();
        return true;
      } catch (Exception var3) {
        log4j.error("Error on closeConnection", var3);
        return false;
      }
    }
  }

  public Connection getTransactionConnection() throws NoConnectionAvailableException, SQLException {
    Connection conn = this.getNewConnection(this.defaultPoolName);
    if (conn == null) {
      throw new NoConnectionAvailableException("Couldn´t get an available connection");
    } else {
      conn.setAutoCommit(false);
      return conn;
    }
  }

  public void releaseCommitConnection(Connection conn) throws SQLException {
    if (conn != null) {
      conn.commit();
      this.closeConnection(conn);
    }
  }

  public void releaseRollbackConnection(Connection conn) throws SQLException {
    if (conn != null) {
      if (!conn.isClosed()) {
        conn.rollback();
        this.closeConnection(conn);
      }
    }
  }

  public PreparedStatement getPreparedStatement(String SQLPreparedStatement) throws Exception {
    return this.getPreparedStatement(this.defaultPoolName, SQLPreparedStatement);
  }

  public PreparedStatement getPreparedStatement(String poolName, String SQLPreparedStatement)
    throws Exception {
    if (poolName != null && !poolName.equals("")) {
      log4j.debug("connection requested");
      Connection conn = this.getConnection(poolName);
      log4j.debug("connection established");
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
        log4j.debug("preparedStatement requested");
        ps = conn.prepareStatement(SQLPreparedStatement, 1004, 1007);
        log4j.debug("preparedStatement received");
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

  public String getStatus() {
    return "Not implemented yet";
  }
}
