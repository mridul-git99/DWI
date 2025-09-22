package com.leucine.streem.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParameterExceptionDto implements Serializable {
  private static final long serialVersionUID = -3026728201774430753L;
  private String id;
  private String status;
  private String code;
  private String initiatorsReason;
  private String reviewersReason;
  private List<ParameterExceptionReviewerDto> reviewer = new ArrayList<>();
  private String value;
  private JsonNode choices;
  private Long createdAt;
  private UserAuditDto createdBy;
  private String jobId;
  private String taskExecutionId;
  private String reason;
  private String ruleId;
}
