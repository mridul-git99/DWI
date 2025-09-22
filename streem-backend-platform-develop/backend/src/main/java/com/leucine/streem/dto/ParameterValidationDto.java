package com.leucine.streem.dto;

import com.leucine.streem.constant.Type;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParameterValidationDto implements Serializable {
  @Serial
  private static final long serialVersionUID = -4529982386862705157L;

  private String ruleId;
  private List<ResourceParameterPropertyValidationDto> resourceParameterValidations;
  private List<ParameterRelationPropertyValidationDto> relationPropertyValidations;
  private List<CustomRelationPropertyValidationDto> customValidations;
  private List<CriteriaValidationDto> criteriaValidations = new ArrayList<>();
  private List<ParameterRelationPropertyValidationDto> propertyValidations = new ArrayList<>();
  private List<DateParameterValidationDto> dateTimeParameterValidations = new ArrayList<>(); //for Date and Date Time Parameter Validations
  private String validationType;
  private Type.ParameterExceptionApprovalType exceptionApprovalType;
}
