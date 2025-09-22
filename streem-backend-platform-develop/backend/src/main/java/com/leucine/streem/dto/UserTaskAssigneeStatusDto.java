package com.leucine.streem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserTaskAssigneeStatusDto {
  private boolean isAssigned;
  private String taskExecutionId;
  private String userId;
}
