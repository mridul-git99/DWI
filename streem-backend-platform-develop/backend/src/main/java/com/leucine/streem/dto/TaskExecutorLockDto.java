package com.leucine.streem.dto;

import com.leucine.streem.constant.Type;
import lombok.Data;

@Data
public class TaskExecutorLockDto {
  private Long taskId;
  private Long referencedTaskId;
  private Type.TaskExecutorLockType lockType;
}
