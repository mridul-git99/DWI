package com.leucine.streem.dto.request;

import com.leucine.streem.dto.TaskExecutorLockRequest;
import com.leucine.streem.dto.TaskSchedulesRequest;
import lombok.Data;

import java.util.List;

@Data
public class ImportTaskRequest extends TaskRequest {
  private String id;
  private String timerOperator;
  private boolean hasStop;
  private boolean hasBulkVerification;
  private boolean isTimed;
  private boolean isSoloTask;
  private Long maxPeriod;
  private Long minPeriod;
  private boolean isMandatory;
  private InterlockDto interlocks;
  private List<Long> prerequisiteTaskIds;
  private List<ImportParameterRequest> parameterRequests;
  private List<AutomationRequest> automationRequests;
  private List<ImportMediaRequest> mediaRequests;
  private TaskSchedulesRequest taskSchedules;
  private SetTaskRecurrentRequest taskRecurrence;
  private TaskExecutorLockRequest taskExecutorLock;
}
