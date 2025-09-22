package com.leucine.streem.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.leucine.streem.constant.Type;
import com.leucine.streem.dto.response.Error;
import com.leucine.streem.model.Parameter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public final class ParameterDto implements Serializable, IChecklistElementDto {
  @Serial
  private static final long serialVersionUID = 5441644236668634120L;

  private String id;
  private int orderTree;
  private boolean isMandatory;
  private String type;
  private Type.ParameterTargetEntityType targetEntityType;
  private String label;
  private String description;
  private JsonNode data;
  private JsonNode validations;
  private List<ParameterValueDto> response;
  private boolean isAutoInitialized;
  private JsonNode autoInitialize;
  private JsonNode rules;
  private boolean hidden;
  private Set<String> hide;
  private Set<String> show;
  private Type.VerificationType verificationType;
  private Type.ParameterExceptionApprovalType exceptionApprovalType;
  private JsonNode metadata;
  private List<Error> softErrors;
}
