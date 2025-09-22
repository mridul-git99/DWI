package com.leucine.streem.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class ParameterPartialRequest {
  private List<String> parameterIds;
}
