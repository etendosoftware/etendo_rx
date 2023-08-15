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

package org.etendorx.base.provider;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Module;

import javax.servlet.ServletContext;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Is used to read config files from specific locations and initialize the OBProvider.
 *
 * @author Martin Taal
 */
public class OBConfigFileProvider implements OBSingleton {
  private final Logger log = LogManager.getLogger();

  private static final String CUSTOM_POSTFIX = "-" + org.etendorx.base.provider.OBProvider.CONFIG_FILE_NAME;

  private static OBConfigFileProvider instance;

  public static synchronized OBConfigFileProvider getInstance() {
    if (instance == null) {
      instance = org.etendorx.base.provider.OBProvider.getInstance()
        .get(OBConfigFileProvider.class);
    }
    return instance;
  }

  public static synchronized void setInstance(OBConfigFileProvider instance) {
    OBConfigFileProvider.instance = instance;
  }

  // the location of the main file
  private String fileLocation;
  private String classPathLocation;
  private ServletContext servletContext;

  /**
   * @return the directory containing the Openbravo.properties file, so <b>not</b> the full path
   *   including the filename (Openbravo.properties) but the path to and including the
   *   directory.
   */
  public String getFileLocation() {
    return fileLocation;
  }

  /**
   * @param fileLocation
   *   the path to the directory which contains the etendorx properties file
   *   (Openbravo.properties). The path does not include the Openbravo.properties file
   *   itself.
   *
   * @see #getFileLocation()
   */
  public void setFileLocation(String fileLocation) {
    this.fileLocation = fileLocation;
  }

  public String getClassPathLocation() {
    return classPathLocation;
  }

  public void setClassPathLocation(String classPathLocation) {
    this.classPathLocation = classPathLocation;
  }

  /**
   * This method will read the ob-provider config files (with bean specifications) and pass them to
   * the {@link org.openbravo.base.provider.OBProvider}. It reads the file from the class path or from a file location.
   * Depending on what is set: {@link #getClassPathLocation()} and/or {@link #getFileLocation()}.
   */
  public void setConfigInProvider() {
    log.debug("Reading config files for setting the provider");
    if (classPathLocation != null) {
      readModuleConfigsFromClassPath();
    }
    if (fileLocation != null) {
      readModuleConfigsFromFile();
    }
    checkClassPathRoot();

  }

  // currently searches for modules at the same location at the
  // main config file
  // TODO: add searching at the root of the classpath
  protected void readModuleConfigsFromFile() {
    log.debug("Reading from fileLocation {}", fileLocation);
    // find the parent
    try {
      File providerDir = new File(fileLocation);
      if (providerDir.exists()) {
        if (!providerDir.isDirectory()) {
          log.warn("File Location of config file should be a directory!");
          providerDir = providerDir.getParentFile();
        }
        File configFile = new File(providerDir,
          org.etendorx.base.provider.OBProvider.CONFIG_FILE_NAME);
        if (configFile.exists()) {
          final InputStream is = new FileInputStream(configFile);
          log.info("Found provider config file " + configFile.getAbsolutePath());
          org.etendorx.base.provider.OBProvider.getInstance().register("", is);
        }

        for (final Module module : ModelProvider.getInstance().getModules()) {
          if (module.getJavaPackage() == null) {
            continue;
          }
          final String fileName = module.getJavaPackage() + CUSTOM_POSTFIX;
          configFile = new File(providerDir, fileName);
          if (configFile.exists()) {
            final InputStream is = new FileInputStream(configFile);
            log.info("Found provider config file " + configFile.getAbsolutePath());
            org.etendorx.base.provider.OBProvider.getInstance()
              .register(module.getJavaPackage(), is);
          }
        }
      }
    } catch (final Exception t) {
      log.error(t.getMessage(), t);
    }
  }

  protected void readModuleConfigsFromClassPath() {
    InputStream is = null;
    try {
      if (classPathLocation.endsWith("/")) {
        log.warn("Classpathlocation of config file should not end with /");
        classPathLocation = classPathLocation.substring(0, classPathLocation.length() - 1);
      }

      is = getResourceAsStream(
        classPathLocation + "/" + org.etendorx.base.provider.OBProvider.CONFIG_FILE_NAME);
      if (is != null) {
        org.etendorx.base.provider.OBProvider.getInstance().register("", is);
      }

      for (final Module module : ModelProvider.getInstance().getModules()) {
        if (module.getJavaPackage() == null) {
          continue;
        }
        final String configLoc = classPathLocation + "/" + module.getJavaPackage() + CUSTOM_POSTFIX;
        registerConfiguration(module, configLoc);
      }
    } catch (final Exception t) {
      log.error(t.getMessage(), t);
    } finally {
      if(is != null) {
        try {
          is.close();
        } catch (final IOException e) {
          log.error(e.getMessage(), e);
        }
      }
    }
  }

  private void registerConfiguration(Module module, String configLoc) {
    try(InputStream cis = getResourceAsStream(configLoc)) {
      if (cis != null) {
        log.info("Found provider config file {}", configLoc);
        OBProvider.getInstance()
            .register(module.getJavaPackage(), cis);
      }
    } catch (final Exception e) {
      log.error(e.getMessage(), e);
    }
  }

  protected InputStream getResourceAsStream(String path) {
    if (getServletContext() != null) {
      return getServletContext().getResourceAsStream(path);
    }
    return this.getClass().getResourceAsStream(path);
  }

  private void checkClassPathRoot() {
    // and look in the root of the classpath
    for (final Module module : ModelProvider.getInstance().getModules()) {
      if (module.getJavaPackage() == null) {
        continue;
      }
      final String fileName = "/" + module.getJavaPackage() + CUSTOM_POSTFIX;
      // always use this class itself for getting the resource
      final InputStream is = getClass().getResourceAsStream(fileName);
      if (is != null) {
        log.info("Found provider config file " + fileName);
        OBProvider.getInstance().register(module.getJavaPackage(), is);
      }
    }
  }

  public ServletContext getServletContext() {
    return servletContext;
  }

  public void setServletContext(ServletContext servletContext) {
    this.servletContext = servletContext;
  }
}
