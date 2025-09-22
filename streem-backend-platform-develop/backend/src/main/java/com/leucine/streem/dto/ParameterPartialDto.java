package com.leucine.streem.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.leucine.streem.constant.Type;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParameterPartialDto implements Serializable {
  private static final long serialVersionUID = 5441645236668634120L;
  private String id;
  private String label;
  private JsonNode data;
  private Type.Parameter type;
  private String value;
  private JsonNode choices;
  private String taskId;
  private String taskExecutionId;
  private String parameterValueId;
  private boolean isHidden;
}
