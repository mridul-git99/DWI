package com.leucine.streem.dto.request;

import lombok.Data;

@Data
public class ParameterExceptionApproveRejectRequest {
  private String reviewerReason;
  private Long exceptionId;
}
