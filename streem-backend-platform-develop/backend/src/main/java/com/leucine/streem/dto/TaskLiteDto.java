package com.leucine.streem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskLiteDto implements Serializable {
  private static final long serialVersionUID = -4597062794990467226L;
  private String id;
  private int orderTree;
  private String name;
  private List<TaskExecutionLiteDto> taskExecutions;
  private boolean hidden;
}
