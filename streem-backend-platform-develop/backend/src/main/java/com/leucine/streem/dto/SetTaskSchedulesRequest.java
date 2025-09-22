package com.leucine.streem.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

@Data
public class SetTaskSchedulesRequest {
  private Integer startDateInterval;
  private JsonNode startDateDuration;
  private Integer dueDateInterval;
  private JsonNode dueDateDuration;
}
