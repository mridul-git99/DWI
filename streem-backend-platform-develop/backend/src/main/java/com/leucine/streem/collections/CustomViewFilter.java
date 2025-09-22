package com.leucine.streem.collections;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomViewFilter {
  private String constraint;
  private String displayName;
  private String key;
  private List<Object> value;
  @Field("id")
  @JsonProperty("id")
  private String id;
}
