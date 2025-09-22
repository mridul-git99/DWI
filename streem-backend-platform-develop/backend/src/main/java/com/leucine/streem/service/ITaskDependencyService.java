package com.leucine.streem.service;

import com.leucine.streem.dto.TaskDependencyDetailsDto;
import com.leucine.streem.dto.request.TaskDependencyRequest;
import com.leucine.streem.model.TaskDependency;

import java.util.List;

public interface ITaskDependencyService {

  List<Long> getTaskDependenciesByTaskId(Long taskId);

  List<TaskDependency> getTaskDependenciesByTaskIds(List<Long> taskIds);

  List<Long> updateTaskDependency(Long taskId, TaskDependencyRequest taskDependencyRequest);

  TaskDependencyDetailsDto getTaskDependenciesDetailsByTaskId(Long taskId);
}
