package com.leucine.streem.dto;

import lombok.Data;

@Data
public class NotificationParameterValueDto {
  private Long jobId;
  private Long parameterId;
  private String taskExecutionId;
  private String taskName;
  private String jobCode;
  private String parameterLabel;
  private String checkListName;
  private Long organizationId;
}
