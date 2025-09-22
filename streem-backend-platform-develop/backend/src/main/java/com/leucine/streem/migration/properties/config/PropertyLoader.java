package com.leucine.streem.migration.properties.config;

import com.leucine.streem.StreemApplication;
import lombok.extern.slf4j.Slf4j;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;

@Slf4j
public class PropertyLoader {

  private final Properties properties;

  public PropertyLoader() {
    String filename = "application.properties";
    boolean isJar = Objects.requireNonNull(PropertyLoader.class.getResource("")).toString().startsWith("jar:");
    this.properties = new Properties();
    InputStream input = null;
    try {
      if (isJar) {
        input = new FileInputStream(StreemApplication.applicationPropertiesPath + filename);
      } else {
        input = getClass().getClassLoader().getResourceAsStream(filename);
      }
      properties.load(input);
    } catch (IOException ioException) {
      log.error("Error running the migration", ioException);
    }
  }

  public String getProperty(String key) {
    return this.properties.getProperty(key);
  }
}
