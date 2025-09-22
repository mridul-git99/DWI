package com.leucine.streem.util.lock;


import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

@Slf4j
public class ParameterValueLock {
  public static final Cache<ParameterValueKey, Lock> lock = Caffeine.newBuilder()
    .expireAfterAccess(1, TimeUnit.MINUTES)
    .initialCapacity(100)
    .removalListener((Object key, Lock value, RemovalCause cause) -> {
      assert key != null;
      log.info(String.format("[Lock] Key: %s was removed (%s)", ((ParameterValueKey)key).getKey(), cause));
    })
    .build();
}
