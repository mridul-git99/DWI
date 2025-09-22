package com.leucine.streem.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TaskRecurrenceDto implements Serializable {
  private static final long serialVersionUID = -4597062794980467226L;

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
