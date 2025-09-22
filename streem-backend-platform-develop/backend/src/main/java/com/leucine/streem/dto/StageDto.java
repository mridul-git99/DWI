package com.leucine.streem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public final class StageDto implements Serializable, IChecklistElementDto {
  @Serial
  private static final long serialVersionUID = -334742639641428191L;

  private String id;
  private String name;
  private int orderTree;
  private List<TaskDto> tasks;
}
