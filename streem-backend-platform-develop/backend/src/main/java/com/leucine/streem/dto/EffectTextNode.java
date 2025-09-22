package com.leucine.streem.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EffectTextNode {
  private String mode;
  private String text;
  private String type;
  private String style;
  private int detail;
  private int format;
  private int version;
  private String trigger;
  private String value;
  private EffectDataNode data;
}
