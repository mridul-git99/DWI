package com.leucine.streem.dto.request;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

@Data
public class SetTaskRecurrentRequest {
  private Integer startDateInterval;
  private JsonNode startDateDuration;
  private Integer dueDateInterval;
  private JsonNode dueDateDuration;
  private Integer positiveStartDateToleranceInterval;
  private JsonNode positiveStartDateToleranceDuration;
  private Integer negativeStartDateToleranceInterval;
  private JsonNode negativeStartDateToleranceDuration;
  private Integer positiveDueDateToleranceInterval;
  private JsonNode positiveDueDateToleranceDuration;
  private Integer negativeDueDateToleranceInterval;
  private JsonNode negativeDueDateToleranceDuration;
}
