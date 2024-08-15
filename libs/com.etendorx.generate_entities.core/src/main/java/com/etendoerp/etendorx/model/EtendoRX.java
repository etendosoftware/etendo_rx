package com.etendoerp.etendorx.model;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Properties;

public class EtendoRX {
  private static final Logger log = LogManager.getLogger();

  public static String currentVersion() {
    Properties properties = new Properties();
    try {
      properties.load(EtendoRX.class.getResourceAsStream("/version.properties"));
      return properties.getProperty("version");
    } catch (IOException e) {
      log.error("Error reading rx version", e);
    }
    return null;
  }
}
