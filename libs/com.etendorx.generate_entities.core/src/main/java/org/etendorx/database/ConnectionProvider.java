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

//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

import org.etendorx.exception.NoConnectionAvailableException;

import java.sql.*;

public interface ConnectionProvider {
  Connection getConnection() throws NoConnectionAvailableException;

  String getRDBMS();

  Connection getTransactionConnection() throws NoConnectionAvailableException, SQLException;

  void releaseCommitConnection(Connection var1) throws SQLException;

  void releaseRollbackConnection(Connection var1) throws SQLException;

  PreparedStatement getPreparedStatement(String var1, String var2) throws Exception;

  PreparedStatement getPreparedStatement(String var1) throws Exception;

  PreparedStatement getPreparedStatement(Connection var1, String var2) throws SQLException;

  void releasePreparedStatement(PreparedStatement var1) throws SQLException;

  Statement getStatement(String var1) throws Exception;

  Statement getStatement() throws Exception;

  Statement getStatement(Connection var1) throws SQLException;

  void releaseStatement(Statement var1) throws SQLException;

  void releaseTransactionalStatement(Statement var1) throws SQLException;

  void releaseTransactionalPreparedStatement(PreparedStatement var1) throws SQLException;

  CallableStatement getCallableStatement(String var1, String var2) throws Exception;

  CallableStatement getCallableStatement(String var1) throws Exception;

  CallableStatement getCallableStatement(Connection var1, String var2) throws SQLException;

  void releaseCallableStatement(CallableStatement var1) throws SQLException;

  void destroy() throws Exception;

  String getStatus();
}
