package com.leucine.streem.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class TempParameterValueDto extends BaseParameterValueDto implements Serializable {
  @Serial
  private static final long serialVersionUID = -7154539329446214542L;

  private List<ParameterVerificationDto> parameterVerifications = new ArrayList<>();
}
