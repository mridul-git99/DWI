package com.leucine.streem.dto;

import com.leucine.streem.constant.Type;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CriteriaValidationDto implements Serializable {
  private String id;
  private String uom;
  private String operator;
  private String type;
  private String lowerValue;
  private String upperValue;
  private String value;
  private Type.SelectorType criteriaType;
  private String valueParameterId;
  private String lowerValueParameterId;
  private String upperValueParameterId;
  private String errorMessage;
}
