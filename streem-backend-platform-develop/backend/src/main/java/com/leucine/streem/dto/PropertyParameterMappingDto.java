package com.leucine.streem.dto;

import lombok.Data;

@Data
public class PropertyParameterMappingDto {
  private String propertyId;
  private String parameterId;
  private String relationId;
  private Object defaultValue;
}
