package com.leucine.streem.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.leucine.streem.constant.Type;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TempParameterDto implements Serializable {
  private static final long serialVersionUID = -7639615260770390827L;

  private String id;
  private int orderTree;
  private boolean isMandatory;
  private String type;
  private String label;
  private JsonNode data;
  private JsonNode validations;
  private boolean isAutoInitialized;
  private JsonNode autoInitialize;
  private JsonNode rules;
  private boolean hidden;
  private Set<String> hide;
  private Set<String> show;
  private Type.VerificationType verificationType;
  private List<TempParameterValueDto> response;
}
