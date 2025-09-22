package com.leucine.streem.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class TaskCompletionRequest {
  private List<ParameterCompletionRequest> parameters;
  private String correctionReason;
  private String reason;
  private String automationReason;
  private List<CreateObjectAutomationRequest> createObjectAutomations;
  private boolean continueRecurrence = false;
  private String recurringOverdueCompletionReason;
  private String scheduleOverdueCompletionReason;
}
