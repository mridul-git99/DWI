package com.leucine.streem.dto.request;

import lombok.Data;

@Data
public class ParameterCorrectionInitiatorRequest {
  private String initiatorReason;
  private CorrectionCorrectorsReviewersRequest correctors;
  private CorrectionCorrectorsReviewersRequest reviewers;
}
