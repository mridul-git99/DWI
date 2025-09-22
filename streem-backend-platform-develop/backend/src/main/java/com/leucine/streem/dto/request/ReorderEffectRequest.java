package com.leucine.streem.dto.request;

import lombok.Data;

import java.util.Map;

@Data
public class ReorderEffectRequest {
  private Map<Long, Integer> effectOrder;
}
