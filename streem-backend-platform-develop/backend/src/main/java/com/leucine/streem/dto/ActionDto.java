package com.leucine.streem.dto;

import com.leucine.streem.constant.ActionTriggerType;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ActionDto {
  private String id;
  private String name;
  private String description;
  private ActionTriggerType triggerType;
  private String triggerEntityId;
  private String successMessage;
  private String failureMessage;
  private List<EffectDto> effects = new ArrayList<>();
}
