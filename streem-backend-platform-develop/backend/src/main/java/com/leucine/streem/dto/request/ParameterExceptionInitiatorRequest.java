package com.leucine.streem.dto.request;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

@Data
public class ParameterExceptionInitiatorRequest {
  private String initiatorReason;
  private ParameterExceptionReviewersRequest reviewers;
  private String value;
  private JsonNode choices;
  private String ruleId;
}
