package com.leucine.streem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationParameterExceptionDto implements Serializable {
  private static final long serialVersionUID = -3026758201574430753L;
  private String jobId;
  private String taskExecutionId;
  private String jobCode;
  private String parameterName;
  private String taskName;
  private String processName;
  private String initiatorReason;
  private Long organisationId;
}
