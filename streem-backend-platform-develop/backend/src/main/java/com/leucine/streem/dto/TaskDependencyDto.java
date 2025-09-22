package com.leucine.streem.dto;

import lombok.Data;

import java.util.List;

@Data
public class TaskDependencyDto {
  private List<Long> prerequisiteTaskIds;
}
