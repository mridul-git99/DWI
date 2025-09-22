package com.leucine.streem.controller.impl;

import com.leucine.streem.controller.ITaskDependencyController;
import com.leucine.streem.dto.TaskDependencyDetailsDto;
import com.leucine.streem.dto.TaskDependencyDto;
import com.leucine.streem.dto.request.TaskDependencyRequest;
import com.leucine.streem.dto.response.Response;
import com.leucine.streem.exception.ResourceNotFoundException;
import com.leucine.streem.exception.StreemException;
import com.leucine.streem.service.ITaskDependencyService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class TaskDependencyController implements ITaskDependencyController {


  @Autowired
  private final ITaskDependencyService taskDependencyService;

  @Override
  public Response<TaskDependencyDto> getTaskDependenciesByTaskId(Long taskId) throws ResourceNotFoundException, StreemException {
    return Response.builder().data(taskDependencyService.getTaskDependenciesByTaskId(taskId)).build();
  }

  @Override
  public Response<TaskDependencyDetailsDto> getTaskDependenciesDetailsByTaskId(Long taskId) throws ResourceNotFoundException, StreemException {
    return Response.builder().data(taskDependencyService.getTaskDependenciesDetailsByTaskId(taskId)).build();

  }

  @Override
  public Response<TaskDependencyDto> updateTaskDependency(Long taskId, TaskDependencyRequest taskDependencyRequest) {
    return Response.builder().data(taskDependencyService.updateTaskDependency(taskId, taskDependencyRequest)).build();
  }

}
