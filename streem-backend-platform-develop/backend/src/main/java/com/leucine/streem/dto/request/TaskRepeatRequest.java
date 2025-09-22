package com.leucine.streem.dto.request;

import lombok.Data;

@Data
public class TaskRepeatRequest {
  private Long jobId;
  private Long taskId;
}
