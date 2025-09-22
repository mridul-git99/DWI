package com.leucine.streem.dto.request;

import lombok.Data;

import java.util.Set;
@Data
public class ParameterExceptionReviewersRequest {
  private Set<Long> userId;
  private Set<Long> userGroupId;
}
