package com.leucine.streem.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.leucine.streem.constant.Type;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class TaskSchedulesDto implements Serializable {
  @Serial
  private static final long serialVersionUID = 9132211057740097023L;

  private Type.ScheduledTaskType type;
  private String referencedTaskId;
  private Type.ScheduledTaskCondition condition;
  private JsonNode startDateDuration;
  private Integer startDateInterval;
  private JsonNode dueDateDuration;
  private Integer dueDateInterval;
}
