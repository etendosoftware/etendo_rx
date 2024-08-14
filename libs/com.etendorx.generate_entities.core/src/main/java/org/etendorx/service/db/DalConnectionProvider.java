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

package org.etendorx.service.db;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.etendorx.base.session.OBPropertiesProvider;
import org.etendorx.dal.core.SessionHandler;
import org.etendorx.dal.service.OBDal;
import org.etendorx.database.ConnectionProvider;
import org.etendorx.database.ExternalConnectionPool;
import org.etendorx.exception.NoConnectionAvailableException;
import org.hibernate.HibernateException;

import java.sql.*;
import java.util.Properties;

/**
 * A connection provider which is created on the basis of the current connection of the DAL (see
 * {@link OBDal#getConnection()}).
 * <p>
 * It read the properties through the {@link OBPropertiesProvider}.
 * <p>
 * Note: this implementation
 * <ul>
 * <li>does not support connection pooling</li>
 * <li>does not close the connection</li>
 * <li>it flushes the hibernate session before returning a connection by default, but this can be
 * overriden by using the constructor with the flush parameter ({@link OBDal#flush()})</li>
 * </ul>
 * <p>
 * {@code DalConnectionProvider} is not thread safe, the same instance should never be accessed by
 * two different threads.
 *
 * @author mtaal
 */
public class DalConnectionProvider implements ConnectionProvider {
  private static final Logger log = LogManager.getLogger();
  private Connection connection;
  private Properties properties;
  // This parameter can be used to define whether the OBDal needs to be flushed when the connection
  // is retrieved or not
  private boolean flush = true;
  private String pool;

  @Override
  public void destroy() throws Exception {
    // never close
  }

  public DalConnectionProvider() {
    pool = ExternalConnectionPool.DEFAULT_POOL;
  }

  private DalConnectionProvider(String poolName) {
    pool = poolName;
    flush = false;
  }

  public static DalConnectionProvider getReadOnlyConnectionProvider() {
    return new DalConnectionProvider(ExternalConnectionPool.READONLY_POOL);
  }

  /**
   * @param flush if set to true, the getConnection method will flush the OBDal instance.
   */
  public DalConnectionProvider(boolean flush) {
    pool = ExternalConnectionPool.DEFAULT_POOL;
    this.flush = flush;
  }

  @Override
  public Connection getConnection() throws NoConnectionAvailableException {
    try {
      if (connection == null || connection.isClosed()) {
        connection = OBDal.getInstance(pool).getConnection(false);
      }
    } catch (SQLException sqlex) {
      log.error("Error checking connection of {} pool", pool, sqlex);
    } catch (HibernateException hex) {
      // Handle the case of a connection retrieved from Hibernate pool which has been already
      // closed. In this case the connection is marked as not usable and when we try to check its
      // status a HibernateException is thrown.
      connection = OBDal.getInstance(pool).getConnection(false);
    }

    if (flush) {
      OBDal.getInstance(pool).flush();
    }
    return connection;
  }

  public void setConnection(Connection connection) {
    this.connection = connection;
  }

  @Override
  public String getRDBMS() {
    return getProperties().getProperty("bbdd.rdbms");
  }

  private boolean closeConnection(Connection conn) {
    if (conn == null) {
      return false;
    }
    try {
      conn.setAutoCommit(true);
      conn.close();
    } catch (Exception ex) {
      return false;
    }
    return true;
  }

  @Override
  public Connection getTransactionConnection() throws NoConnectionAvailableException, SQLException {
    Connection conn = SessionHandler.getInstance().getNewConnection(pool);

    if (conn == null) {
      throw new NoConnectionAvailableException("Couldn't get an available connection");
    }
    conn.setAutoCommit(false);
    return conn;
  }

  @Override
  public void releaseCommitConnection(Connection conn) throws SQLException {
    if (conn == null) {
      return;
    }
    conn.commit();
    closeConnection(conn);
  }

  @Override
  public void releaseRollbackConnection(Connection conn) throws SQLException {
    if (conn == null) {
      return;
    }
    conn.rollback();
    closeConnection(conn);
  }

  @Override
  public PreparedStatement getPreparedStatement(String SQLPreparedStatement) throws Exception {
    return getPreparedStatement(getConnection(), SQLPreparedStatement);
  }

  @Override
  public PreparedStatement getPreparedStatement(String poolName, String SQLPreparedStatement)
      throws Exception {
    return getPreparedStatement(getConnection(), SQLPreparedStatement);
  }

  @Override
  public PreparedStatement getPreparedStatement(Connection conn, String SQLPreparedStatement)
      throws SQLException {
    PreparedStatement ps = conn.prepareStatement(SQLPreparedStatement,
        ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
    return ps;
  }

  @Override
  public CallableStatement getCallableStatement(String SQLCallableStatement) throws Exception {
    return getCallableStatement("", SQLCallableStatement);
  }

  @Override
  public CallableStatement getCallableStatement(String poolName, String SQLCallableStatement)
      throws Exception {
    Connection conn = getConnection();
    return getCallableStatement(conn, SQLCallableStatement);
  }

  @Override
  public CallableStatement getCallableStatement(Connection conn, String SQLCallableStatement)
      throws SQLException {
    if (conn == null || SQLCallableStatement == null || SQLCallableStatement.equals("")) {
      return null;
    }
    CallableStatement cs = null;
    try {
      cs = conn.prepareCall(SQLCallableStatement);
    } catch (SQLException e) {
      throw e;
    }
    return (cs);
  }

  @Override
  public Statement getStatement() throws Exception {
    return getStatement("");
  }

  @Override
  public Statement getStatement(String poolName) throws Exception {
    Connection conn = getConnection();
    return getStatement(conn);
  }

  @Override
  public Statement getStatement(Connection conn) throws SQLException {
    if (conn == null) {
      return null;
    }
    try {
      return (conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY));
    } catch (SQLException e) {
      throw e;
    }
  }

  @Override
  public void releasePreparedStatement(PreparedStatement preparedStatement) throws SQLException {
    if (preparedStatement == null) {
      return;
    }
    preparedStatement.close();
  }

  @Override
  public void releaseCallableStatement(CallableStatement callableStatement) throws SQLException {
    if (callableStatement == null) {
      return;
    }
    callableStatement.close();
  }

  @Override
  public void releaseStatement(Statement statement) throws SQLException {
    if (statement == null) {
      return;
    }
    statement.close();
  }

  @Override
  public void releaseTransactionalStatement(Statement statement) throws SQLException {
    if (statement == null) {
      return;
    }
    statement.close();
  }

  @Override
  public void releaseTransactionalPreparedStatement(PreparedStatement preparedStatement)
      throws SQLException {
    if (preparedStatement == null) {
      return;
    }
    preparedStatement.close();
  }

  @Override
  public String getStatus() {
    return "Not implemented";
  }

  public Properties getProperties() {
    if (properties == null) {
      properties = OBPropertiesProvider.getInstance().getOpenbravoProperties();
    }
    return properties;
  }

  public void setProperties(Properties properties) {
    this.properties = properties;
  }
}
