package com.leucine.streem.dto.request;

import com.leucine.streem.collections.PropertyOption;
import com.leucine.streem.constant.CollectionMisc;
import com.leucine.streem.constant.Type;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InterlockResourcePropertyValidationDto implements Serializable {
  private String id;
  private String label;
  private Type.InterlockTriggerType triggerType;
  private String parameterId;
  private String value;
  private String propertyId;
  private String propertyExternalId;
  private String propertyDisplayName;
  private CollectionMisc.PropertyValidationConstraint constraint;
  private CollectionMisc.PropertyType propertyInputType;
  private CollectionMisc.DateUnit dateUnit;
  private List<PropertyOption> choices;
  private String errorMessage;
}
