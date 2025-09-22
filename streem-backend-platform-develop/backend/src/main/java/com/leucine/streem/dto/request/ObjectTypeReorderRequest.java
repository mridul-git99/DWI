package com.leucine.streem.dto.request;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

  @Data
  public class ObjectTypeReorderRequest {
    private String id;
    private Map<String, Integer> propertySortOrderMap = new HashMap<>();
    private Map<String, Integer> relationSortOrderMap = new HashMap<>();
    private String reason;
  }

