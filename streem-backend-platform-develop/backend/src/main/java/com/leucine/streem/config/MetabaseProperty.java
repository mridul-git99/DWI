package com.leucine.streem.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "metabase")
@Setter
@Getter
public class MetabaseProperty {
  private String url;
  private String secretKey;
  private Integer refreshRate;
}
