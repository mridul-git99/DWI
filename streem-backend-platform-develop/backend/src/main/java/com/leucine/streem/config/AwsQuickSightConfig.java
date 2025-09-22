package com.leucine.streem.config;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.quicksight.AmazonQuickSight;
import com.amazonaws.services.quicksight.AmazonQuickSightClientBuilder;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@RequiredArgsConstructor
@Configuration
public class AwsQuickSightConfig {
  private final AwsConfig awsConfig;

  @Bean
  public AmazonQuickSight amazonQuickSight() {
    AWSCredentialsProvider credentialsProvider = new AWSCredentialsProvider() {
      public AWSCredentials getCredentials() {
        return new BasicAWSCredentials(awsConfig.getAccessKey(), awsConfig.getSecretKey());
      }

      public void refresh() {
      }
    };

    return AmazonQuickSightClientBuilder.standard()
      .withRegion(awsConfig.getRegion())
      .withCredentials(credentialsProvider)
      .build();
  }
}
