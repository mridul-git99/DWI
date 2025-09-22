package com.leucine.streem.dto.projection;

public interface PendingForApprovalStatusView {

  String getParameterValueId();

  String getJobId();

  String getParameterName();

  String getTaskName();

  String getProcessName();

  Long getModifiedAt();

  String getJobCode();

  String getStageId();

  String getTaskId();

  Long getCreatedAt();

  String getTaskExecutionId();

  String getParameterId();

  Long getExceptionInitiatedBy();

  String getRulesId();

}
