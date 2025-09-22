package com.leucine.streem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PendingForApprovalStatusDto implements Serializable {
  private static final long serialVersionUID = -3026728201574490753L;

  private String parameterValueId;
  private String jobId;
  private String parameterName;
  private String taskName;
  private String processName;
  private Long modifiedAt;
  private String jobCode;
  private String stageId;
  private String taskId;
  private Long createdAt;
  private String taskExecutionId;
  private String parameterId;
  private UserAuditDto exceptionInitiatedBy;
  private String rulesId;
}
