package com.leucine.streem.model.helper.parameter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.leucine.streem.constant.Type;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LeastCount extends ValueParameterBase {
  private String value;
  private String referencedParameterId;
  private Type.SelectorType selector;
}
