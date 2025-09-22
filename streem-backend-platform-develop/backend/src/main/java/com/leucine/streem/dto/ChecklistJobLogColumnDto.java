package com.leucine.streem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChecklistJobLogColumnDto implements Serializable {
  private String id;
  private String type;
  private String displayName;
  private String triggerType;
}
