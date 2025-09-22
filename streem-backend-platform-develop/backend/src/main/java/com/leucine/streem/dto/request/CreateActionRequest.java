package com.leucine.streem.dto.request;

import com.leucine.streem.constant.ActionTriggerType;
import lombok.Data;

@Data
public class CreateActionRequest {
  private String name;
  private String description;
  private ActionTriggerType triggerType;
  private Long triggerEntityId;
  private String successMessage;
  private String failureMessage;
  private Long checklistId;
}
