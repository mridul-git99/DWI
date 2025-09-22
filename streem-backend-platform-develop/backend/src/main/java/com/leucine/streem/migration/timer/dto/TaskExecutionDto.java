package com.leucine.streem.migration.timer.dto;

import lombok.Data;

@Data
public class TaskExecutionDto {
  private Long id;
  private Long endedAt;
  private Long endedBy;
}
