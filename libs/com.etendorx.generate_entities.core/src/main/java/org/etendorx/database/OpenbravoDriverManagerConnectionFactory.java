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

import java.sql.*;
import java.util.Properties;

class OpenbravoDriverManagerConnectionFactory implements ConnectionFactory {
  protected String _connectUri;
  protected String _uname;
  protected String _passwd;
  protected Properties _props;
  protected String _dbSessionConfig;
  protected String _rdbsm;

  public OpenbravoDriverManagerConnectionFactory(String connectUri, Properties props) {
    this._connectUri = null;
    this._uname = null;
    this._passwd = null;
    this._props = null;
    this._connectUri = connectUri;
    this._props = props;
  }

  public OpenbravoDriverManagerConnectionFactory(String connectUri, String uname, String passwd,
      String dbSessionConfig, String rdbsm) {
    this._props = null;
    this._dbSessionConfig = null;
    this._connectUri = connectUri;
    this._uname = uname;
    this._passwd = passwd;
    this._dbSessionConfig = dbSessionConfig;
    this._rdbsm = rdbsm;
  }

  public Connection createConnection() throws SQLException {
    Connection conn = null;
    if (null == this._props) {
      if (this._uname == null) {
        conn = DriverManager.getConnection(this._connectUri);
      } else {
        conn = DriverManager.getConnection(this._connectUri, this._uname, this._passwd);
      }
    } else {
      conn = DriverManager.getConnection(this._connectUri, this._props);
    }

    if (conn != null && this._dbSessionConfig != null) {
      this.executeDefaultSQL(conn);
    }

    return conn;
  }

  private void executeDefaultSQL(Connection conn) {
    Statement stmt = null;
    ResultSet rset = null;

    try {
      stmt = conn.createStatement();
      if (!this._dbSessionConfig.equals("")) {
        rset = stmt.executeQuery(this._dbSessionConfig);
      }

      SessionInfo.initDB(conn, this._rdbsm);
    } catch (SQLException var21) {
      var21.printStackTrace();
    } finally {
      try {
        if (rset != null) {
          rset.close();
        }
      } catch (Exception var20) {
      }

      try {
        if (stmt != null) {
          stmt.close();
        }
      } catch (Exception var19) {
      }

      try {
        conn.commit();
      } catch (Exception var18) {
      }

    }

  }
}
