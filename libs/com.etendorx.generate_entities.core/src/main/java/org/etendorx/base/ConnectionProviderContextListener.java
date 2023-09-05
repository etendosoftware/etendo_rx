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

package org.etendorx.base;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.etendorx.database.ConnectionProvider;
import org.etendorx.database.ConnectionProviderImpl;
import org.etendorx.database.JNDIConnectionProvider;
import org.etendorx.exception.PoolNotFoundException;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.io.FileInputStream;
import java.util.Properties;

public class ConnectionProviderContextListener implements ServletContextListener {
  public static final String POOL_ATTRIBUTE = "openbravoPool";
  private static Logger log4j = LogManager.getLogger();
  private static ConnectionProvider pool;

  public ConnectionProviderContextListener() {
  }

  public void contextInitialized(ServletContextEvent event) {
    ServletContext context = event.getServletContext();
    ConfigParameters configParameters = ConfigParameters.retrieveFrom(context);

    try {
      pool = this.createPool(configParameters);
      context.setAttribute("openbravoPool", pool);
    } catch (PoolNotFoundException var5) {
      log4j.error("Unable to create a connection pool", var5);
    }

  }

  public void contextDestroyed(ServletContextEvent event) {
    ServletContext context = event.getServletContext();
    destroyPool(getPool(context));
    context.removeAttribute("openbravoPool");
  }

  public static ConnectionProvider getPool(ServletContext context) {
    return (ConnectionProvider) context.getAttribute("openbravoPool");
  }

  public static ConnectionProvider getPool() {
    return pool;
  }

  public static void reloadPool(ServletContext context) throws Exception {
    ConnectionProvider connectionPool = getPool(context);
    if (connectionPool instanceof ConnectionProviderImpl) {
      ConfigParameters configParameters = ConfigParameters.retrieveFrom(context);
      String strPoolFile = configParameters.getPoolFilePath();
      boolean isRelative = !strPoolFile.startsWith("/") && !strPoolFile.substring(1, 1).equals(":");
      ((ConnectionProviderImpl) connectionPool).reload(strPoolFile, isRelative,
        configParameters.strContext);
    }

  }

  private ConnectionProvider createPool(ConfigParameters configParameters)
    throws PoolNotFoundException {
    return createXmlPool(configParameters);
  }

  private static ConnectionProvider createXmlPool(ConfigParameters configParameters)
    throws PoolNotFoundException {
    try {
      String strPoolFile = configParameters.getPoolFilePath();
      boolean isRelative = !strPoolFile.startsWith("/") && !strPoolFile.substring(1, 1).equals(":");
      return (ConnectionProvider) (useJNDIConnProvider(strPoolFile) ?
        new JNDIConnectionProvider(strPoolFile, isRelative) :
        new ConnectionProviderImpl(strPoolFile, isRelative, configParameters.strContext));
    } catch (Exception var3) {
      throw new PoolNotFoundException(var3.getMessage(), var3);
    }
  }

  private static boolean useJNDIConnProvider(String strPoolFile) {
    Properties properties = new Properties();
    String jndiUsage = null;

    try(FileInputStream fis = new FileInputStream(strPoolFile)) {
      properties.load(fis);
      String externalPool = properties.getProperty("db.externalPoolClassName");
      if (externalPool != null && !externalPool.isEmpty()) {
        return false;
      }

      jndiUsage = properties.getProperty("JNDI.usage");
    } catch (Exception var4) {
      log4j.error("Error checking JNDI mode file: {} {}", strPoolFile, var4);
    }

    return StringUtils.equals("yes", jndiUsage);
  }

  private static void destroyPool(ConnectionProvider connectionPool) {
    if (connectionPool != null && connectionPool instanceof JNDIConnectionProvider) {
      try {
        ((JNDIConnectionProvider) connectionPool).destroy();
      } catch (Exception var3) {
        log4j.error(var3);
      }
    } else if (connectionPool != null && connectionPool instanceof ConnectionProvider) {
      try {
        connectionPool.destroy();
      } catch (Exception var2) {
        log4j.error(var2);
      }
    }

  }
}
