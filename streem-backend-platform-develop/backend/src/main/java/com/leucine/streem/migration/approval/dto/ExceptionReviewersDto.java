package com.leucine.streem.migration.approval.dto;

import lombok.Data;

@Data
public class ExceptionReviewersDto {
  private Long id;
  private Long usersId;
  private Long exceptionsId;
  private boolean actionPerformed;
  private Long createdBy;
  private Long createdAt;
  private Long modifiedBy;
  private Long modifiedAt;
}
