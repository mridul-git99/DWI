package com.leucine.streem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskExecutorLockRequest {
  private Long hasToBeExecutorId;
  private Set<Long> cannotBeExecutorIds = new HashSet<>();
}
