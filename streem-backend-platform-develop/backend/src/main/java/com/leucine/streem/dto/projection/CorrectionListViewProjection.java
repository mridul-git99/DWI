package com.leucine.streem.dto.projection;


public interface CorrectionListViewProjection {
  String getId();
  Long getCreatedAt();
  String getTaskExecutionId();
  String getJobId();
  String getCode();
  String getStatus();
  String getJobCode();
  String getProcessName();
  String getParameterName();

  String getInitiatorFirstName();
  String getInitiatorLastName();
  String getInitiatorEmployeeId();
  String getInitiatorUserId();
  String getInitiatorsReason();
  String getCorrectorsReason();
  String getReviewersReason();
  String getOldValue();
  String getNewValue();
  String getTaskName();
  String getParameterId();
  String getOldChoices();
  String getNewChoices();
}
