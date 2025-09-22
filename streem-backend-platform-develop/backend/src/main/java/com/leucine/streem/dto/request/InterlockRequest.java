package com.leucine.streem.dto.request;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

@Data
public class InterlockRequest {
  private String id;
  private JsonNode validations;
}
