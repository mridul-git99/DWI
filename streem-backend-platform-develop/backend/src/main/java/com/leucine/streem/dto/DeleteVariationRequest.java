package com.leucine.streem.dto;

import com.leucine.streem.constant.Action;
import lombok.Data;

@Data
public class DeleteVariationRequest {
  private String reason;
  private Long parameterId;
  private Long variationId;
  private Action.Variation type;
  private Long jobId;
}
