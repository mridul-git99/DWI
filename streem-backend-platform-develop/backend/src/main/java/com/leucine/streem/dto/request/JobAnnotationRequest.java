package com.leucine.streem.dto.request;

import lombok.Data;

import java.util.Set;

@Data
public class JobAnnotationRequest {
  private String remarks;
  private Long jobId;
  private Set<Long> ids;
}
