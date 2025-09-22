package com.leucine.streem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StagePendingDto {
  private String id;
  private String name;
  private Integer orderTree;
  private List<TaskPendingDto> tasks;
}
