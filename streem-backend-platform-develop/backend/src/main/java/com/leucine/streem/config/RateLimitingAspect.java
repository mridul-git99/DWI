package com.leucine.streem.config;

import com.leucine.streem.exception.RateLimitExceededException;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class RateLimitingAspect {
  private final Bucket rateLimitedBucket;

  public RateLimitingAspect(Bucket rateLimitedBucket) {
    this.rateLimitedBucket = rateLimitedBucket;
  }

  @Around("@annotation(com.leucine.streem.config.RateLimited)")
  public Object rateLimit(ProceedingJoinPoint joinPoint) throws Throwable {
    ConsumptionProbe probe = rateLimitedBucket.tryConsumeAndReturnRemaining(1);
    if (probe.isConsumed()) {
      return joinPoint.proceed();
    } else {
      throw new RateLimitExceededException("Rate limit exceeded");
    }
  }
}
