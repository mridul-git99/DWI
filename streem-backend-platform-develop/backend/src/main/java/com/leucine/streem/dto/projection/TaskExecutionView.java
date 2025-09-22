package com.leucine.streem.dto.projection;

public interface TaskExecutionView {
  Long getId();
  Long getTaskId();
  String getTaskName();
  String getChecklistName();
  String getJobCode();
}
