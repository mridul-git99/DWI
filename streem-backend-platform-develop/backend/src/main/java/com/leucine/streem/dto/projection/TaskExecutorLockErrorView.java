package com.leucine.streem.dto.projection;

public interface TaskExecutorLockErrorView {
  boolean getIsValidUser();

  String getLockType();

  String getTaskExecutionId();

  String getTaskName();

  String getTaskOrderTree();

  String getStageOrderTree();

  String getTaskExecutionOrderTree();
  boolean getIsValidTaskState();
}
