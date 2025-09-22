package com.leucine.streem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StageLiteDto implements Serializable {
  private static final long serialVersionUID = -334742739641428191L;
  private String id;
  private String name;
  private int orderTree;
  private List<TaskLiteDto> tasks;
  private boolean hidden;
}
