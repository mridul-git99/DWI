package com.leucine.streem.dto.projection;

public interface JobLogTaskExecutionView {
  Long getTaskId();

  Long getStartedAt();

  Long getEndedAt();

  String getTaskStartedByFirstName();

  String getTaskStartedByLastName();

  String getTaskStartedByEmployeeId();

  String getTaskModifiedByFirstName();

  String getTaskModifiedByLastName();

  String getTaskModifiedByEmployeeId();

  String getName();

  Long getJobId();

  String getTaskStartedById();
  String getTaskModifiedById();
}
