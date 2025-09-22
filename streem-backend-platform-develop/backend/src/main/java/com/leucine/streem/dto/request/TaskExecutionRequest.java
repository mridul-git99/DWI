package com.leucine.streem.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class TaskExecutionRequest {
  private String correctionReason;
  private String reason;
  private String recurringPrematureStartReason;
  private String automationReason;
  private List<CreateObjectAutomationRequest> createObjectAutomations;
  private String schedulePrematureStartReason;
  private String recurringOverdueCompletionReason;
  private boolean continueRecurrence = false;
}
