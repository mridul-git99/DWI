package com.leucine.streem.dto;


import com.leucine.streem.constant.Type;
import lombok.Data;

@Data
public class AutomationActionArchiveObjectDto {
  private String referencedParameterId;
  private Type.SelectorType selector;
}
