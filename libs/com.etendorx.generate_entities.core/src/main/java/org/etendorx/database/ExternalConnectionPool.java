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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

public abstract class ExternalConnectionPool {
  static Logger log = LogManager.getLogger();
  public static final String DEFAULT_POOL = "DEFAULT";
  public static final String READONLY_POOL = "RO";
  private static org.etendorx.database.ExternalConnectionPool instance;
  private static final String PG_TOO_MANY_CONNECTIONS = "53300";
  private static final String ORA_CONNECTION_REFUSED = "66000";

  public ExternalConnectionPool() {
  }

  public static final synchronized org.etendorx.database.ExternalConnectionPool getInstance(
      String externalConnectionPoolClassName) throws ReflectiveOperationException {
    if (instance == null) {
      instance = (org.etendorx.database.ExternalConnectionPool) Class.forName(
          externalConnectionPoolClassName).getDeclaredConstructor().newInstance();
    }

    return instance;
  }

  public static final org.etendorx.database.ExternalConnectionPool getInstance() {
    return instance;
  }

  public void closePool() {
    instance = null;
  }

  public void loadInterceptors(List<PoolInterceptorProvider> interceptors) {
  }

  public abstract Connection getConnection();

  public Connection getConnection(String poolName) {
    return this.getConnection();
  }

  protected List<Class<? extends Exception>> getExhaustedExceptions() {
    return Collections.emptyList();
  }

  public boolean hasNoConnections(Throwable t) {
    if (t == null) {
      return false;
    } else {
      boolean isOutOfPhysicalConns;
      if (t instanceof SQLException) {
        String state = ((SQLException) t).getSQLState();
        isOutOfPhysicalConns = "53300".equals(state) || "66000".equals(state);
      } else {
        isOutOfPhysicalConns = false;
      }

      return isOutOfPhysicalConns || this.getExhaustedExceptions().stream().anyMatch((e) -> {
        return e.isAssignableFrom(t.getClass());
      }) || this.hasNoConnections(t.getCause());
    }
  }
}
