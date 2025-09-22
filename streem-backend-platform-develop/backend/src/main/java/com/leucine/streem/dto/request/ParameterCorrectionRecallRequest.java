package com.leucine.streem.dto.request;

import lombok.Data;

@Data
public class ParameterCorrectionRecallRequest {
  private String reason;
  private Long correctionId;
}
