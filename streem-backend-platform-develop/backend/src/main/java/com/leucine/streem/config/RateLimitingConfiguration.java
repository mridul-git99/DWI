package com.leucine.streem.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class RateLimitingConfiguration {

  @Value("${app.rate-limit.capacity:5}")
  private Integer capacity;

  @Value("${app.rate-limit.refill-tokens-per-min:5}")
  private Integer refillTokens;

  @Bean
  public Bucket rateLimitedBucket() {
    Bandwidth rateLimited = Bandwidth.classic(capacity, Refill.greedy(refillTokens, Duration.ofMinutes(1)));
    return Bucket4j.builder()
      .addLimit(rateLimited)
      .build();
  }

}
