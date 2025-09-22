package com.leucine.streem.dto;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.leucine.streem.constant.Type;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EffectDataNode {
  private String id;
  private String uuid;
  private String postfix;
  private String entity;
}
