package com.leucine.streem.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.leucine.streem.constant.EffectType;
import lombok.Data;

@Data
public class EffectDto {
  private String id;
  private String name;
  private String description;
  private Integer orderTree;
  private JsonNode query;
  private EffectType effectType;
  private JsonNode apiEndpoint;
  private String apiMethod;
  private JsonNode apiHeaders;
  private JsonNode apiPayload;
  private boolean archived;
  private String actionsId;
  private boolean javascriptEnabled = false;
}
