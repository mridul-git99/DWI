package com.leucine.streem.model.helper.parameter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.Map;

import static com.leucine.streem.util.Utility.MAX_PRECISION_LIMIT_UI;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CalculationParameter {
  private String expression;
  private String uom;
  private Map<String, CalculationParameterVariable> variables;
  private Integer precision = (int) MAX_PRECISION_LIMIT_UI;
}
