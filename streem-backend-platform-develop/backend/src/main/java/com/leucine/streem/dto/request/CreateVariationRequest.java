package com.leucine.streem.dto.request;

import com.fasterxml.jackson.databind.JsonNode;
import com.leucine.streem.constant.Action;
import lombok.Data;

@Data
public class CreateVariationRequest {
  private Long parameterId;
  private Long jobId;
  private JsonNode details;
  private Action.Variation type;
  private String name;
  private String description;
  private String variationNumber;
  private VariationMediaRequest media;
}
