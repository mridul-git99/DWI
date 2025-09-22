package com.leucine.streem.dto.request;

import com.fasterxml.jackson.databind.JsonNode;
import com.leucine.streem.constant.EffectType;
import lombok.Data;

@Data
public class UpdateEffectRequest {
  private String name;
  private String description;
  private Integer orderTree;
  private JsonNode query;
  private EffectType effectType;
  private JsonNode apiEndpoint;
  private JsonNode apiPayload;
  private String apiMethod;
  private JsonNode apiHeaders;
  private Boolean archived;
  private Long actionId;
  private boolean javascriptEnabled = false;

}
