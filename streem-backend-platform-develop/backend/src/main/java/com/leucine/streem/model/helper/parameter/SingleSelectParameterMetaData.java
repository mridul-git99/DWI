package com.leucine.streem.model.helper.parameter;

import lombok.Data;

@Data
public class SingleSelectParameterMetaData {
  private String objectTypeId;
  private String objectTypeDisplayName;
  private String objectTypeExternalId;
  private String collection;
  private String propertyId;
  private String propertyDisplayName;
  private String propertyExternalId;
}
