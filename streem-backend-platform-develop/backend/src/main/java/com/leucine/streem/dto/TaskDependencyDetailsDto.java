package com.leucine.streem.dto;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@RequiredArgsConstructor
public class TaskDependencyDetailsDto {
  List<TaskDependencyStageDetailsDto> stages = new ArrayList<>();
}
