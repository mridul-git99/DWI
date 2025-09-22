package com.leucine.streem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskDependencyTaskDetailsDto {
  private String id;
  private String name;
  private Integer orderTree;
}
