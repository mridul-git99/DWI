package com.leucine.streem.dto.projection;

public interface ParameterVerificationListViewProjection {
  Long getId();

  Long getModifiedById();

  String getModifiedByEmployeeId();

  String getModifiedByFirstName();

  String getModifiedByLastName();

  Long getCreatedById();

  String getCreatedByEmployeeId();

  String getCreatedByFirstName();

  String getCreatedByLastName();

  Long getRequestedToId();

  String getRequestedToEmployeeId();

  String getRequestedToFirstName();

  String getRequestedToLastName();


  Long getModifiedAt();

  Long getCreatedAt();

  String getJobId();

  String getCode();

  Long getTaskId();

  String getTaskName();

  Long getStageId();

  Long getParameterValueId();

  String getParameterName();

  String getProcessName();

  String getVerificationType();

  String getVerificationStatus();

  String getComments();

  String getTaskExecutionId();
}
