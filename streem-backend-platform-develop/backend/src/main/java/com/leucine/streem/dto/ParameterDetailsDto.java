package com.leucine.streem.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.leucine.streem.constant.Type;
import lombok.Data;

@Data
public class ParameterDetailsDto {
  private String ruleId;
  private Type.Parameter type;
  private Type.ParameterTargetEntityType targetEntityType;
  private String parameterExecutionId;
  private String parameterId;
  private Type.ParameterExceptionApprovalType exceptionApprovalType;
  private JsonNode ruleDetails;
  //TODO: find better way to handle current parameter information
  private ParameterDto currentParameterDto;
}
