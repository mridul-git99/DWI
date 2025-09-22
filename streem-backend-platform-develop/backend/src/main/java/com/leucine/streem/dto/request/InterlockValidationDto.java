package com.leucine.streem.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InterlockValidationDto {
  private static final long serialVersionUID = -4529982386862705157L;

  private List<InterlockResourcePropertyValidationDto> resourceParameterValidations;
}
