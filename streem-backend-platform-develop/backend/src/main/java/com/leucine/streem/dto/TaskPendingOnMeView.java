package com.leucine.streem.dto;


public interface TaskPendingOnMeView {
  String getUserId();

  String getJobId();

  String getTaskExecutionId();

  String getTaskExecutionOrderTree();

  String getTaskExecutionState();

  String getTaskId();

  String getTaskName();

  String getTaskOrderTree();

  String getStageId();

  String getStageName();

  String getStageOrderTree();
}
