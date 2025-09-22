package com.leucine.streem.dto.projection;

public interface TaskExecutorLockView {
  String getTaskId();

  String getTaskName();

  Integer getTaskOrderTree();

  String getStageOrderTree();

  String getCondition();

}
