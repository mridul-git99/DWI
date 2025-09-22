package com.leucine.streem.controller.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.leucine.streem.controller.ITaskController;
import com.leucine.streem.dto.*;
import com.leucine.streem.dto.projection.TaskExecutorLockView;
import com.leucine.streem.dto.request.*;
import com.leucine.streem.dto.response.Response;
import com.leucine.streem.exception.ResourceNotFoundException;
import com.leucine.streem.exception.StreemException;
import com.leucine.streem.service.ITaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TaskController implements ITaskController {
  private final ITaskService taskService;

  @Autowired
  public TaskController(ITaskService taskService) {
    this.taskService = taskService;
  }

  @Override
  public Response<TaskDto> createTask(Long checklistId, Long stageId, TaskRequest taskRequest) throws ResourceNotFoundException, StreemException {
    return Response.builder().data(taskService.createTask(checklistId, stageId, taskRequest)).build();
  }

  @Override
  public Response<TaskDto> getTask(Long taskId) throws ResourceNotFoundException, StreemException {
    return Response.builder().data(taskService.getTask(taskId)).build();
  }

  @Override
  public Response<TaskDto> updateTask(Long taskId, TaskRequest taskRequest) throws ResourceNotFoundException, StreemException {
    return Response.builder().data(taskService.updateTask(taskId, taskRequest)).build();
  }

  @Override
  public Response<TaskRecurrenceDto> getTaskRecurrence(Long taskId) throws ResourceNotFoundException {
    return Response.builder().data(taskService.getTaskRecurrence(taskId)).build();
  }

  @Override
  public Response<TaskDto> setTaskRecurrence(Long taskId, SetTaskRecurrentRequest setTaskRecurrentRequest) throws StreemException, ResourceNotFoundException, JsonProcessingException {
    return Response.builder().data(taskService.setTaskRecurrence(taskId, setTaskRecurrentRequest)).build();
  }

  @Override
  public Response<TaskDto> unsetTaskRecurrence(Long taskId) throws StreemException, ResourceNotFoundException {
    return Response.builder().data(taskService.unsetTaskRecurrence(taskId)).build();
  }

  @Override
  public Response<BasicDto> reorderTasks(TaskReorderRequest taskReorderRequest) throws StreemException, ResourceNotFoundException {
    return Response.builder().data(taskService.reorderTasks(taskReorderRequest)).build();
  }

  @Override
  public Response<TaskDto> setTimer(Long taskId, TimerRequest timerRequest) throws ResourceNotFoundException, StreemException {
    return Response.builder().data(taskService.setTimer(taskId, timerRequest)).build();
  }

  @Override
  public Response<TaskDto> unsetTimer(Long taskId) throws ResourceNotFoundException, StreemException {
    return Response.builder().data(taskService.unsetTimer(taskId)).build();
  }

  @Override
  public Response<TaskDto> addStop(Long taskId) throws ResourceNotFoundException, StreemException {
    return Response.builder().data(taskService.addStop(taskId)).build();
  }

  @Override
  public Response<TaskDto> removeStop(Long taskId) throws ResourceNotFoundException, StreemException {
    return Response.builder().data(taskService.removeStop(taskId)).build();
  }

  @Override
  public Response<TaskDto> addMedia(Long taskId, MediaRequest mediaRequest) throws ResourceNotFoundException, StreemException {
    return Response.builder().data(taskService.addMedia(taskId, mediaRequest)).build();
  }

  @Override
  public Response<MediaDto> updateMedia(Long taskId, Long mediaId, MediaRequest mediaRequest) throws ResourceNotFoundException, StreemException {
    return Response.builder().data(taskService.updateMedia(taskId, mediaId, mediaRequest)).build();
  }

  @Override
  public Response<TaskDto> deleteMedia(Long taskId, Long mediaId) {
    return Response.builder().data(taskService.deleteMedia(taskId, mediaId)).build();
  }

  @Override
  public Response<TaskDto> archiveTask(Long taskId) throws ResourceNotFoundException, StreemException {
    return Response.builder().data(taskService.archiveTask(taskId)).build();
  }

  @Override
  public Response<TaskDto> addAutomation(Long taskId, AutomationRequest automationRequest) throws StreemException, ResourceNotFoundException, JsonProcessingException {
    return Response.builder().data(taskService.addAutomation(taskId, automationRequest)).build();
  }

  @Override
  public Response<TaskDto> deleteAutomation(Long taskId, Long automationId) throws StreemException, ResourceNotFoundException {
    return Response.builder().data(taskService.deleteAutomation(taskId, automationId)).build();
  }

  @Override
  public Response<TaskSchedulesDto> getTaskSchedule(Long taskId) throws ResourceNotFoundException {
    return Response.builder().data(taskService.getTaskSchedule(taskId)).build();
  }

  @Override
  public Response<TaskDto> unsetBulkVerification(Long taskId) {
    return Response.builder().data(taskService.unsetBulkVerification(taskId)).build();
  }

  @Override
  public Response<TaskDto> setBulkVerification(Long taskId) {
    return Response.builder().data(taskService.setBulkVerification(taskId)).build();
  }

  @Override
  public Response<List<TaskExecutorLockView>> getTaskExecutorLockView(Long taskId) {
    return Response.builder().data(taskService.getTaskExecutorLockView(taskId)).build();
  }

  @Override
  public Response<TaskDto> removeExecutor(Long taskId) {
    return Response.builder().data(taskService.unsetExecutor(taskId)).build();
  }

  @Override
  public Response<BasicDto> addExecutor(Long taskId, TaskExecutorLockRequest taskExecutorLockRequest) throws ResourceNotFoundException, StreemException {
    return Response.builder().data(taskService.addTaskExecutorLock(taskId, taskExecutorLockRequest)).build();
  }

  @Override
  public Response<TaskDto> unsetTaskSchedules(Long taskId) throws ResourceNotFoundException, StreemException {
    return Response.builder().data(taskService.unsetTaskSchedules(taskId)).build();
  }

  @Override
  public Response<TaskDto> setTaskSchedules(Long taskId, TaskSchedulesRequest taskSchedulesRequest) throws ResourceNotFoundException, StreemException, JsonProcessingException {
    return Response.builder().data(taskService.setTaskSchedules(taskId, taskSchedulesRequest)).build();
  }

  @Override
  public Response<TaskDto> updateAutomation(Long taskId, Long automationId, AutomationRequest automationRequest) throws StreemException, ResourceNotFoundException, JsonProcessingException {
    return Response.builder().data(taskService.updateAutomation(taskId, automationId, automationRequest)).build();
  }

  @Override
  public Response<InterlockDto> getInterlockByTaskId(String taskId) throws StreemException, ResourceNotFoundException, JsonProcessingException {
    return Response.builder().data(taskService.getInterlockByTaskId(taskId)).build();
  }

  @Override
  public Response<InterlockDto> addInterlockForTask(String taskId, InterlockRequest interlockRequest) throws StreemException, ResourceNotFoundException, JsonProcessingException {
    return Response.builder().data(taskService.addInterlockForTask(taskId, interlockRequest)).build();
  }

  @Override
  public Response<InterlockDto> updateInterlockForTask(String taskId, InterlockRequest interlockRequest) throws StreemException, ResourceNotFoundException, JsonProcessingException {
    return Response.builder().data(taskService.updateInterlockForTask(taskId, interlockRequest)).build();
  }

  @Override
  public Response<BasicDto> deleteInterlockByTaskId(String interlockId, Long taskId) throws StreemException, ResourceNotFoundException, JsonProcessingException {
    return Response.builder().data(taskService.deleteInterlockByTaskId(interlockId, taskId)).build();
  }

  @Override
  public Response<TaskDto> addSoloTaskLock(Long taskId) throws ResourceNotFoundException, StreemException {
    return Response.builder().data(taskService.addSoloTaskLock(taskId)).build();
  }

  @Override
  public Response<TaskDto> removeSoloTaskLock(Long taskId) throws ResourceNotFoundException, StreemException {
    return Response.builder().data(taskService.removeSoloTaskLock(taskId)).build();
  }
}
