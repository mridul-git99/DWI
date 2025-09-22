package com.leucine.streem.dto;


import com.leucine.streem.collections.PropertyOption;
import com.leucine.streem.constant.Type;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AutomationActionSetPropertyDto extends AutomationSetPropertyBaseDto implements Serializable {
  private static final long serialVersionUID = -2658687507103816888L;
  private String value;
  private List<PropertyOption> choices;
  private String referencedParameterId; // Resource parameter ID
  private Type.SelectorType selector;
  private String parameterId; // Number/Parameter/Calculation Parameter having the value, which then is used for automation
}
