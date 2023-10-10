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

package org.etendorx.base.session;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.io.SAXReader;
import org.etendorx.base.ConfigParameters;
import org.etendorx.base.exception.OBException;
import org.etendorx.base.provider.OBConfigFileProvider;
import org.etendorx.dal.xml.XMLUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

/**
 * This class implements a central location where the Openbravo.properties are read and made
 * available for the rest of the application.
 *
 * @author mtaal
 */
public class OBPropertiesProvider {
  private final Logger log = LogManager.getLogger();

  private static OBPropertiesProvider instance = new OBPropertiesProvider();

  private static boolean friendlyWarnings = false;

  public static boolean isFriendlyWarnings() {
    return friendlyWarnings;
  }

  public static void setFriendlyWarnings(boolean doFriendlyWarnings) {
    friendlyWarnings = doFriendlyWarnings;
  }

  private Properties obProperties = null;
  private Document formatXML;

  public static synchronized OBPropertiesProvider getInstance() {
    return instance;
  }

  public static synchronized void setInstance(OBPropertiesProvider instance) {
    OBPropertiesProvider.instance = instance;
  }

  public Properties getOpenbravoProperties() {
    if (obProperties == null) {
      readPropertiesFromDevelopmentProject();
    }
    return obProperties;
  }

  public void setFormatXML(InputStream is) {
    try {
      SAXReader reader = XMLUtil.getInstance().newSAXReader();
      formatXML = reader.read(is);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  public void setProperties(InputStream is) {
    if (obProperties != null) {
      log.debug("Openbravo properties have already been set, setting them again");
    }
    log.debug("Setting Openbravo.properties through input stream");
    obProperties = new Properties();
    try {
      obProperties.load(is);
      is.close();

      if (OBConfigFileProvider.getInstance() == null || OBConfigFileProvider.getInstance()
        .getServletContext() == null) {
        log.debug("ServletContext is not set, not trying to override Openbravo.properties");
        return;
      }

      ConfigParameters.overrideProperties(obProperties,
        OBConfigFileProvider.getInstance().getServletContext().getRealPath("/WEB-INF"));
    } catch (final Exception e) {
      throw new OBException(e);
    }
  }

  public void setProperties(Properties props) {
    if (obProperties != null) {
      log.debug("Openbravo properties have already been set, setting them again");
    }
    log.debug("Setting openbravo.properties through properties");
    obProperties = new Properties();
    obProperties.putAll(props);
  }

  public void setProperties(String fileLocation) {
    log.debug("Setting Openbravo.properties through file: {}", fileLocation);
    obProperties = new Properties();
    try(FileInputStream fis = new FileInputStream(fileLocation)) {
      obProperties.load(fis);
    } catch (final Exception e) {
      throw new OBException(e);
    }
  }

  /**
   * Looks for a boolean property key and return <code>true</code> in case its value is true or yes
   * and false other case.
   */
  public boolean getBooleanProperty(String key) {
    Properties properties = getOpenbravoProperties();
    if (properties == null) {
      return false;
    }

    String value = properties.getProperty(key);
    if (value == null) {
      return false;
    }
    return StringUtils.equalsIgnoreCase(value, "true") || StringUtils.equalsIgnoreCase(value, "yes");
  }

  // tries to read the properties from the openbravo development project

  private void readPropertiesFromDevelopmentProject() {
    final File propertiesFile = getFileFromDevelopmentPath("Openbravo.properties");
    if (propertiesFile == null) {
      return;
    }
    setProperties(propertiesFile.getAbsolutePath());
    OBConfigFileProvider.getInstance()
      .setFileLocation(propertiesFile.getParentFile().getAbsolutePath());
  }

  private File getFileFromDevelopmentPath(String fileName) {
    return new File(fileName);
  }

}
