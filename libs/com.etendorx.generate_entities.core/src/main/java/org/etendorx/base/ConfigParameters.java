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

import javax.servlet.ServletContext;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.Properties;

public class ConfigParameters implements Serializable {
  static final long serialVersionUID = 1L;
  public static final String CONFIG_ATTRIBUTE = "openbravoConfig";
  private final String strBaseConfigPath;
  public final String strBaseDesignPath;
  private final boolean isFullPathBaseDesignPath;
  public final String strDefaultDesignPath;
  public final String strLocalReplaceWith;
  public final String strBBDD = null;
  public final String strVersion;
  public final String strParentVersion;
  public String prefix;
  public final String strContext;
  private final String strFileFormat;
  public final String strSystemLanguage;
  public final String strDefaultServlet;
  private final String stcFileProperties;
  public final String strReplaceWhat;
  private final String poolFileName;
  public final String strTextDividedByZero;
  private static final Logger log4j = LogManager.getLogger();
  public final String loginServlet;
  public final String strServletSinIdentificar;
  private final String strServletGoBack;
  public final String strFTPDirectory;
  public final Long periodicBackgroundTime;
  public final String strLogFileAcctServer;
  private final Properties propFileProperties;
  private static final String DEFAULT_JAVA_DATETIME_FORMAT = "dd-MM-yyyy HH:mm:ss";
  private static final String DEFAULT_SQL_DATETIME_FORMAT = "DD-MM-YYYY HH24:MI:SS";

  public static org.etendorx.base.ConfigParameters retrieveFrom(ServletContext context) {
    org.etendorx.base.ConfigParameters params = (org.etendorx.base.ConfigParameters) context.getAttribute(
        "openbravoConfig");
    if (params == null) {
      params = new org.etendorx.base.ConfigParameters(context);
      params.storeIn(context);
    }

    return params;
  }

  public ConfigParameters(ServletContext context) {
    this.prefix = context.getRealPath("/");
    if (!this.prefix.endsWith("/")) {
      this.prefix = this.prefix + "/";
    }

    this.strContext = this.extractContext(this.getActualPathContext());
    this.strBaseConfigPath = this.getResolvedParameter(context, "BaseConfigPath");
    log4j.debug("context: {}", this.strContext);
    log4j.debug("************************prefix: {}", this.prefix);
    this.stcFileProperties = this.prefix + "/" + this.strBaseConfigPath + "/Openbravo.properties";
    this.propFileProperties = this.loadOBProperties();
    String s = "FormatFile";
    this.strFileFormat = this.getResolvedParameter(context, s);
    this.strBaseDesignPath = this.trimTrailing(this.getResolvedParameter(context, "BaseDesignPath"),
        "/");
    this.isFullPathBaseDesignPath = this.determineIsFullDesignPath();
    this.strDefaultDesignPath = this.getResolvedParameter(context, "DefaultDesignPath");
    this.strDefaultServlet = this.getResolvedParameter(context, "DefaultServlet");
    this.strReplaceWhat = this.getResolvedParameter(context, "ReplaceWhat");
    log4j.debug("BaseConfigPath: {}", this.strBaseConfigPath);
    log4j.debug("BaseDesignPath: {}", this.strBaseDesignPath);
    this.strVersion = this.getResolvedParameter(context, "Version");
    this.strParentVersion = this.getResolvedParameter(context, "Parent_Version");
    this.strSystemLanguage = this.getSystemLanguage();
    this.strLocalReplaceWith = this.getResolvedParameter(context, "ReplaceWith");
    this.strTextDividedByZero = this.getResolvedParameter(context, "TextDividedByZero");
    this.poolFileName = this.getResolvedParameter(context, "PoolFile");
    this.loginServlet = this.getResolvedParameter(context, "LoginServlet");
    this.strServletSinIdentificar = this.getResolvedParameter(context, "LoginServlet");
    this.strServletGoBack = this.getResolvedParameter(context, "ServletGoBack");
    log4j.debug("strServletGoBack: {}", this.strServletGoBack);
    this.periodicBackgroundTime = this.asLong(
        this.getResolvedParameter(context, "PeriodicBackgroundTime"));
    String var10001 = this.prefix;
    this.strLogFileAcctServer = var10001 + "/" + this.strBaseConfigPath + "/" + this.getResolvedParameter(
        context, "LogFileAcctServer");
    this.strFTPDirectory = this.getResolvedParameter(context, "AttachmentDirectory");

    try {
      File f = new File(this.strFTPDirectory);
      if (!f.exists()) {
        f.mkdir();
      }
    } catch (Exception var4) {
      log4j.error(var4);
    }

  }

  private String getResolvedParameter(ServletContext context, String name) {
    String value = context.getInitParameter(name);
    return value != null ?
        value.replace("@actual_path_context@", this.getActualPathContext())
            .replace("@application_context@", this.getApplicationContext()) :
        value;
  }

  private String getApplicationContext() {
    return this.strContext;
  }

  private String getActualPathContext() {
    return this.prefix;
  }

  public void storeIn(ServletContext context) {
    context.setAttribute("openbravoConfig", this);
  }

  private String getSystemLanguage() {
    String var10000 = System.getProperty("user.language");
    return var10000 + "_" + System.getProperty("user.country");
  }

  private String trimTrailing(String str, String trim) {
    return str.endsWith(trim) ? str.substring(0, str.length() - trim.length()) : str;
  }

  private Long asLong(String str) {
    if (str != null && str.length() != 0) {
      try {
        return Long.parseLong(str);
      } catch (NumberFormatException var3) {
        log4j.error(var3);
        return null;
      }
    } else {
      return null;
    }
  }

  private String extractContext(String _prefix) {
    String path = "/";
    int firstPath = _prefix.lastIndexOf(path);
    if (firstPath == -1) {
      path = "\\";
      firstPath = _prefix.lastIndexOf(path);
    }

    if (firstPath != -1) {
      int secondPath = _prefix.lastIndexOf(path, firstPath - 1);
      return _prefix.substring(secondPath + 1, firstPath);
    } else {
      return null;
    }
  }

  private boolean determineIsFullDesignPath() {
    try {
      File testPrefix = new File(this.strBaseDesignPath);
      return testPrefix.exists();
    } catch (Exception var2) {
      return false;
    }
  }

  public String getPoolFilePath() {
    return this.prefix + "/" + this.strBaseConfigPath + "/" + this.poolFileName;
  }

  public String getBaseDesignPath() {
    return this.isFullPathBaseDesignPath ?
        this.strBaseDesignPath :
        this.prefix + "/" + this.strBaseDesignPath;
  }

  public String getXmlEngineFileFormatPath() {
    return this.prefix + "/" + this.strBaseConfigPath + "/" + this.strFileFormat;
  }

  public String getOpenbravoPropertiesPath() {
    return this.stcFileProperties;
  }

  public String getFormatPath() {
    return this.prefix + "/" + this.strBaseConfigPath + "/Format.xml";
  }

  public boolean havePeriodicBackgroundTime() {
    return this.periodicBackgroundTime != null;
  }

  public long getPeriodicBackgroundTime() {
    return this.havePeriodicBackgroundTime() ? this.periodicBackgroundTime : 0L;
  }

  public boolean haveLogFileAcctServer() {
    return this.strLogFileAcctServer != null && !this.strLogFileAcctServer.equals("");
  }

  public String getOBProperty(String skey, String sdefault) {
    return this.propFileProperties.getProperty(skey, sdefault);
  }

  public String getOBProperty(String skey) {
    return this.propFileProperties.getProperty(skey);
  }

  public Properties getOBProperties() {
    return this.propFileProperties;
  }

  public Properties loadOBProperties() {
    Properties obProperties = new Properties();

    try (FileInputStream fis = new FileInputStream(this.stcFileProperties)) {
      obProperties.load(fis);
      log4j.info("Properties file: " + this.stcFileProperties);
      overrideProperties(obProperties, this.stcFileProperties);
    } catch (IOException var3) {
      log4j.error("IO error reading properties", var3);
    }

    return obProperties;
  }

  public static void overrideProperties(Properties obProperties, String path) {
    if (obProperties == null) {
      log4j.warn("Openbravo.properties was not set, not trying to override it");
    } else {
      String propFilePath = path.replace("Openbravo.properties", "");
      if (propFilePath != null && !propFilePath.isEmpty()) {
        String absPath = System.getProperty("properties.path");
        File propertiesFile = null;
        if (absPath != null && !absPath.isEmpty()) {
          propertiesFile = new File(absPath);
          log4j.info(
              "Looking for override properties file in " + absPath + ". Found: " + propertiesFile.exists());
        } else {
          String fileName = getMachineName();
          if (fileName == null || fileName.isEmpty()) {
            log4j.debug("Override fileName env variable is not defined.");
            return;
          }

          fileName = fileName + ".Openbravo.properties";
          propertiesFile = new File(propFilePath, fileName);
        }

        if (!propertiesFile.exists()) {
          log4j.debug("No override file can be found at {}", propertiesFile.getAbsolutePath());
        } else {
          log4j.info("Loading override properties file from {}", propertiesFile.getAbsolutePath());
          Properties overrideProperties = new Properties();
          FileInputStream fis = null;

          try {
            fis = new FileInputStream(propertiesFile.getAbsolutePath());
            overrideProperties.load(fis);
            Enumeration<Object> em = overrideProperties.keys();

            while (em.hasMoreElements()) {
              String obProperty = (String) em.nextElement();
              String overrideValue = overrideProperties.getProperty(obProperty);
              Object object = obProperties.setProperty(obProperty, overrideValue);
              log4j.info("Overriding property {}: {} -> {}", obProperty, object,
                  obProperties.getProperty(obProperty));
            }
          } catch (Exception var19) {
            log4j.error("Error loading override Openbravo.properties from {} {}", propertiesFile,
                var19);
          } finally {
            if (fis != null) {
              try {
                fis.close();
              } catch (IOException var18) {
                log4j.error("Error closing input stream for {}", propertiesFile);
              }
            }

          }

        }
      } else {
        log4j.debug("Could not determine context path");
      }
    }
  }

  public static String getMachineName() {
    String name = System.getProperty("machine.name");
    if (StringUtils.isEmpty(name)) {
      try {
        name = InetAddress.getLocalHost().getHostName();
        log4j.info("Checking override properties for {}", name);
      } catch (UnknownHostException var2) {
        log4j.error("Error when getting host name", var2);
      }
    }

    return name;
  }

  public String getJavaDateTimeFormat() {
    return this.getOBProperty("dateTimeFormat.java", "dd-MM-yyyy HH:mm:ss");
  }

  public String getSqlDateTimeFormat() {
    return this.getOBProperty("dateTimeFormat.sql", "DD-MM-YYYY HH24:MI:SS");
  }
}
