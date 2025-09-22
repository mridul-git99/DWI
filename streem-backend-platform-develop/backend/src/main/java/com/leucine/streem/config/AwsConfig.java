package com.leucine.streem.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
@Setter
public class AwsConfig {
  @Value("${aws.access.key}")
  private String accessKey;

  @Value("${aws.secret.key}")
  private String secretKey;

  @Value("${aws.account.id}")
  private String accountId;

  @Value("${aws.region}")
  private String region;

  @Value("${aws.arn}")
  private String arn;

  @Value("${aws.allowed.domains}")
  private String[] allowedDomains;

  @Value("${aws.sqs.queue.name}")
  private String queueName;
}
