package com.leucine.streem.dto.request;


import lombok.Data;

@Data
public class CreateObjectAutomationRequest {
  private Long automationId;
  private EntityObjectValueRequest entityObjectValueRequest;
}
