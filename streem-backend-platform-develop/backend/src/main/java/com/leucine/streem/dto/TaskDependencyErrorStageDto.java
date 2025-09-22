package com.leucine.streem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskDependencyErrorStageDto {
  private String id;
  private Integer orderTree;
  private List<TaskDependencyErrorTaskDto> tasks;
}
