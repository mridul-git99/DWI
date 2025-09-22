package com.leucine.streem.dto;

import com.leucine.streem.constant.Type;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Builder
@Data
public class AutomationResponseDto {
  private String id;
  private Type.AutomationActionType actionType;
  private List<CreateObjectAutomationResponseDto> createObjectAutomationResponseDto;
  private String propertyDisplayName;
}
