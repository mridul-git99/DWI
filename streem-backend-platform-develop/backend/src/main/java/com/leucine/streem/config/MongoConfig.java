package com.leucine.streem.config;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "mongodb")
public class MongoConfig {
  private String host;
  private int port;
  private String authenticationDatabase;
  private String database;
  private String username;
  private String password;
  private String replicaSet;
  public String buildUri() {
    return "mongodb://" + host + ":" + port + "/" + database + "?authSource=" + authenticationDatabase;
  }
}
