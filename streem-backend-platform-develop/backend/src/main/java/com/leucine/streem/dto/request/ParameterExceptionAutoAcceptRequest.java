package com.leucine.streem.dto.request;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

@Data
public class ParameterExceptionAutoAcceptRequest {
  private String value;
  private String reason;
  private JsonNode choices;
  private String ruleId;
}
