package com.leucine.streem.dto.request;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

@Data
public class ParameterCorrectionApproveRejectRequest {
  private String reviewerReason;
  private Long correctionId;
}
