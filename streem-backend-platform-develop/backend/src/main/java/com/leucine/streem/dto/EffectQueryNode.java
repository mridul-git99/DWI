package com.leucine.streem.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EffectQueryNode {
  private String type;
  private String format;
  private int indent;
  private int version;
  private List<EffectChildNode> children;
  private String direction;
  private String textStyle;
  private int textFormat;
}
