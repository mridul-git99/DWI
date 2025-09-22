package com.leucine.streem.dto;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class CreateObjectAutomationResponseDto {
  private String createdObjectExternalId;
  private String createdObjectId;
}
