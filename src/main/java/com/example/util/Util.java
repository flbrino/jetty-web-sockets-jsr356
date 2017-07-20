package com.example.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Util {
  private static final Logger log = Logger.getLogger(Util.class.getName());

  public static Properties getProperties(String filePath) {
    Properties properties = new Properties();
    InputStream input = null;

    try {
      input = new FileInputStream(filePath);
      properties.load(input);
    } catch (IOException exception) {
      log.log(Level.SEVERE, exception.getMessage());
      throw new RuntimeException("No configuration details...");
    }
    return properties;
  }
}
