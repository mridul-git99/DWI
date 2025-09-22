package com.leucine.streem.dto;

import com.leucine.streem.dto.request.ParameterExecuteRequest;
import lombok.Data;

@Data
public class BulkParameterExecuteRequest {
  private Long parameterExecutionId;
  private ParameterExecuteRequest parameterExecuteRequest;

}
