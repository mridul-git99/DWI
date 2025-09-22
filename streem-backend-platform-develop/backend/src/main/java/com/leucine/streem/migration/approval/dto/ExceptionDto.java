package com.leucine.streem.migration.approval.dto;

import lombok.Data;

@Data
public class ExceptionDto {
  private Long id;
  private String code;
  private String value;
  private Long parameterValueId;
  private Long taskExecutionId;
  private Long facilityId;
  private Long jobId;
  private String status;
  private String initiatorsReason;
  private String reviewersReason;
  private String previousState;
  private Long createdAt;
  private Long createdBy;
  private Long modifiedAt;
  private Long modifiedBy;
}
