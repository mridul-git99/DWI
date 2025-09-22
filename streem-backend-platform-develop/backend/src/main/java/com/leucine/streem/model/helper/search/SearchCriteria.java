package com.leucine.streem.model.helper.search;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class SearchCriteria {
  private String field;
  private String op;
  
  // This will be used for deserialization of both 'values' and 'value' fields
  private List<Object> values = new ArrayList<>();
  
  // This is used to handle the 'value' field in the JSON
  @JsonProperty("value")
  private void setValue(Object value) {
    if (value != null) {
      // If value is a complex object with 'value' field, extract that
      if (value instanceof java.util.Map) {
        java.util.Map<?, ?> map = (java.util.Map<?, ?>) value;
        if (map.containsKey("value")) {
          value = map.get("value");
        }
      }
      
      // Add the value to the values list
      if (this.values == null) {
        this.values = new ArrayList<>();
      }
      this.values.add(value);
    }
  }
  
  // Helper method to get the first value
  @JsonIgnore
  public Object getValue() {
    return values != null && !values.isEmpty() ? values.get(0) : null;
  }
}
