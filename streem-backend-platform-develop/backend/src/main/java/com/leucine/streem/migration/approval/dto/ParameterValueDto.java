package com.leucine.streem.migration.approval.dto;

import lombok.Data;

@Data
public class ParameterValueDto {
  private Long id;
  private Long jobId;
  private String value;
  private Long taskExecutionId;
  private Long facilityId;
  private String reason;
  private Long createdAt;
  private Long createdBy;
  private Long modifiedAt;
  private Long modifiedBy;
  private Long organisationId;
}
