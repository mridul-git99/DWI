package com.leucine.streem.config;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import lombok.Data;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
public class AwsSQSConfig {
  private final AwsConfig awsConfig;

  @Bean
  public AmazonSQS amazonSQS() {
    AWSCredentialsProvider credentialsProvider = new AWSCredentialsProvider() {
      public AWSCredentials getCredentials() {
        return new BasicAWSCredentials(awsConfig.getAccessKey(), awsConfig.getSecretKey());
      }

      public void refresh() {
      }
    };

    return AmazonSQSClientBuilder.standard()
      .withRegion(awsConfig.getRegion())
      .withCredentials(credentialsProvider)
      .build();
  }
}
