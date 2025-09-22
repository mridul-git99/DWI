package com.leucine.streem.dto;

import com.leucine.streem.dto.request.ParameterExceptionRequest;
import lombok.Data;
import java.util.List;

@Data
public class BulkParameterExceptionRequest {
  private List<ParameterExceptionRequest> exceptions;
}
