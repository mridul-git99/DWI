package com.leucine.streem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChecklistJobLiteDto implements Serializable {
  private static final long serialVersionUID = 1305472982372612136L;
  private String id;
  private String name;
  private String code;
  private List<StageLiteDto> stages = new ArrayList<>();
}
