package com.leucine.streem.dto.request;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data

public class ParameterExceptionRequest {
  private long parameterExecutionId;
  private List<ParameterExceptionInitiatorRequest> parameterExceptionInitiatorRequest = new ArrayList<>();
  private List<ParameterExceptionAutoAcceptRequest> parameterExceptionAutoAcceptRequest= new ArrayList<>();

}



