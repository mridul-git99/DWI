package com.leucine.streem.dto;

import com.leucine.streem.constant.CollectionMisc;
import com.leucine.streem.constant.Type;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DateParameterValidationDto {
  private String id;
  private String referencedParameterId;
  private Type.SelectorType selector;
  private String value;
  private CollectionMisc.DateUnit dateUnit;
  private CollectionMisc.PropertyValidationConstraint constraint;
  private String errorMessage;
}
