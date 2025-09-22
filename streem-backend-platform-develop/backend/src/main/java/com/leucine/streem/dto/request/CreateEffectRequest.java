package com.leucine.streem.dto.request;

import com.fasterxml.jackson.databind.JsonNode;
import com.leucine.streem.constant.EffectType;
import lombok.Data;

@Data
public class CreateEffectRequest {
    private String name;
    private String description;
    private Integer orderTree;
    private JsonNode query;
    private EffectType effectType;
    private JsonNode apiEndpoint;
    private String apiMethod;
    private JsonNode apiHeaders;
    private boolean archived;
    private Long actionId;
    private JsonNode apiPayload;
    private boolean javascriptEnabled;
}
