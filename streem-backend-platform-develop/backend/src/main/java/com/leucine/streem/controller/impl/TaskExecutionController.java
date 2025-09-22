package com.leucine.streem.controller.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.leucine.streem.controller.ITaskExecutionController;
import com.leucine.streem.dto.*;
import com.leucine.streem.dto.projection.TaskExecutionAssigneeView;
import com.leucine.streem.dto.request.*;
import com.leucine.streem.dto.response.Response;
import com.leucine.streem.exception.ParameterExecutionException;
import com.leucine.streem.exception.ResourceNotFoundException;
import com.leucine.streem.exception.StreemException;
import com.leucine.streem.service.ITaskExecutionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;

import java.io.IOException;
import java.util.List;

@Component
public class TaskExecutionController implements ITaskExecutionController {
  private final ITaskExecutionService taskExecutionService;

  @Autowired
  public TaskExecutionController(ITaskExecutionService taskExecutionService) {
    this.taskExecutionService = taskExecutionService;
  }

  @Override
  public Response<TaskDto> getTask(Long taskExecutionId) throws ResourceNotFoundException {
    return Response.builder().data(taskExecutionService.getTask(taskExecutionId)).build();
  }

  @Override
  public Response<TaskDto> validateTask(Long taskExecutionId) throws ResourceNotFoundException, StreemException {
    return Response.builder().data(taskExecutionService.validateTask(taskExecutionId)).build();
  }

  @Override
  public Response<TaskExecutionDto> startTask(Long taskExecutionId, TaskExecutionRequest taskExecutionRequest) throws ResourceNotFoundException, StreemException, IOException, ParameterExecutionException {
    return Response.builder().data(taskExecutionService.startTask(taskExecutionId, taskExecutionRequest)).build();
  }

  @Override
  public Response<TaskExecutionDto> completeTask(Long taskExecutionId, TaskCompletionRequest taskCompletionRequest) throws ResourceNotFoundException, StreemException, IOException, ParameterExecutionException {
    return Response.builder().data(taskExecutionService.completeTask(taskExecutionId, taskCompletionRequest)).build();
  }

  @Override
  public Response<TaskExecutionDto> repeatTask(TaskRepeatRequest taskRepeatRequest) throws ResourceNotFoundException, StreemException, IOException {
    return Response.builder().data(taskExecutionService.repeatTaskExecution(taskRepeatRequest)).build();
  }

  @Override
  public Response<TaskExecutionDto> completeTaskWithException(Long taskExecutionId, TaskCompletionRequest taskCompletionRequest) throws ResourceNotFoundException, StreemException, IOException {
    return Response.builder().data(taskExecutionService.completeWithException(taskExecutionId, taskCompletionRequest)).build();
  }

  @Override
  public Response<BasicDto> signOffTask(TaskSignOffRequest taskSignOffRequest) throws StreemException {
    return Response.builder().data(taskExecutionService.signOff(taskSignOffRequest)).build();
  }

  @Override
  public Response<List<TaskExecutionAssigneeView>> getTaskExecutionAssignees(@RequestBody TaskAssigneeDto taskAssigneeDto) {
    return Response.builder().data(taskExecutionService.getTaskExecutionAssignees(taskAssigneeDto.getTask(), taskAssigneeDto.isUsers(), taskAssigneeDto.isUserGroups())).build();
  }

  @Override
  public Response<TaskExecutionDto> skipTask(Long taskExecutionId, TaskExecutionRequest taskExecutionRequest) throws ResourceNotFoundException, StreemException, IOException {
    return Response.builder().data(taskExecutionService.skipTask(taskExecutionId, taskExecutionRequest)).build();
  }

  @Override
  public Response<TaskExecutionDto> enableCorrection(Long taskExecutionId, TaskExecutionRequest taskExecutionRequest) throws StreemException {
    return Response.builder().data(taskExecutionService.enableCorrection(taskExecutionId, taskExecutionRequest)).build();
  }

  @Override
  public Response<TaskExecutionDto> cancelCorrection(Long taskExecutionId, TaskExecutionRequest taskExecutionRequest) throws ResourceNotFoundException, StreemException {
    return Response.builder().data(taskExecutionService.cancelCorrection(taskExecutionId, taskExecutionRequest)).build();
  }

  @Override
  public Response<TaskExecutionDto> completeCorrection(Long taskExecutionId, TaskExecutionRequest taskExecutionRequest) throws ResourceNotFoundException, StreemException, IOException {
    return Response.builder().data(taskExecutionService.completeCorrection(taskExecutionId, taskExecutionRequest)).build();
  }

  @Override
  public Response<TaskExecutionDto> pauseTask(Long taskExecutionId, TaskPauseOrResumeRequest taskPauseOrResumeRequest) throws StreemException {
    return Response.builder().data(taskExecutionService.pauseTask(taskExecutionId, taskPauseOrResumeRequest)).build();
  }

  @Override
  public Response<TaskDetailsDto> pollTaskData(Long taskExecutionId) throws ResourceNotFoundException, JsonProcessingException {
    return Response.builder().data(taskExecutionService.getTaskData(taskExecutionId)).build();
  }

  @Override
  public Response<UserTaskAssigneeStatusDto> checkTaskAssignee(Long taskExecutionId) throws ResourceNotFoundException, StreemException {
    return Response.builder().data(taskExecutionService.checkTaskAssignee(taskExecutionId)).build();
  }

  @Override
  public Response<BasicDto> stopTaskExecutionRecurring(Long taskExecutionId) throws StreemException, ResourceNotFoundException {
    return Response.builder().data(taskExecutionService.stopTaskExecutionRecurring(taskExecutionId)).build();
  }

  @Override
  public Response<BasicDto> removeTask(Long taskExecutionId) throws StreemException, ResourceNotFoundException {
    return Response.builder().data(taskExecutionService.removeTaskExecution(taskExecutionId)).build();
  }

  @Override
  public Response<TaskExecutionDto> resumeTask(Long taskExecutionId, TaskPauseOrResumeRequest taskPauseOrResumeRequest) throws StreemException {
    return Response.builder().data(taskExecutionService.resumeTask(taskExecutionId, taskPauseOrResumeRequest)).build();
  }
}
