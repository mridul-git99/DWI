package com.leucine.streem.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.leucine.streem.dto.request.InterlockDto;
import com.leucine.streem.dto.request.SetTaskRecurrentRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class TaskDto implements Serializable, IChecklistElementDto {
  @Serial
  private static final long serialVersionUID = -4597062794980467226L;

  private String id;
  private int orderTree;
  private String name;
  private Long maxPeriod;
  private Long minPeriod;
  private String timerOperator;
  private boolean hasStop;
  private boolean isSoloTask;
  private boolean isTimed;
  private boolean isMandatory;
  private boolean enableRecurrence;
  private boolean enableScheduling;
  private boolean hasPrerequisites;
  private boolean hasDependents;
  private List<Long> prerequisiteTaskIds;
  private InterlockDto interlocks;
  private List<ParameterDto> parameters;
  private List<MediaDto> medias;
  private List<AutomationDto> automations;
  private List<TaskExecutionDto> taskExecutions;
  private TaskSchedulesRequest taskSchedules;
  private SetTaskRecurrentRequest taskRecurrence;
  private boolean hasExecutorLock;
  private boolean isReferencedTaskExecutorLock;
  private TaskExecutorLockRequest taskExecutorLock;
  private boolean hasBulkVerification;
  private boolean hasInterlocks;
}
