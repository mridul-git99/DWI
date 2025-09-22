package com.leucine.streem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CorrectionPrintDto implements Serializable {
  private static final long serialVersionUID = -3026728201774432753L;
  private String parameterExecutionId;
  private List<CorrectionDto> corrections;
}
