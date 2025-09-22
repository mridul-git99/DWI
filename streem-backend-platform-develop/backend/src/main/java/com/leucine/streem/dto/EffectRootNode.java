package com.leucine.streem.dto;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EffectRootNode {
  private String type;
  private String format;
  private int indent;
  private int version;
  private List<EffectChildNode> children = new ArrayList<>();
  private String direction;
  private String textStyle;
  private int textFormat;
}
