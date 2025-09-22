package com.leucine.streem.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.leucine.streem.dto.*;
import com.leucine.streem.dto.projection.TaskExecutorLockView;
import com.leucine.streem.dto.request.*;
import com.leucine.streem.exception.ResourceNotFoundException;
import com.leucine.streem.exception.StreemException;

import java.util.List;

public interface ITaskService {
  TaskDto getTask(Long taskId) throws ResourceNotFoundException, StreemException;

  TaskDto createTask(Long checklistId, Long stageId, TaskRequest taskRequest) throws ResourceNotFoundException, StreemException;

  TaskDto updateTask(Long taskId, TaskRequest taskRequest) throws ResourceNotFoundException, StreemException;

  TaskRecurrenceDto getTaskRecurrence(Long taskId) throws ResourceNotFoundException;

  TaskDto setTaskRecurrence(Long taskId, SetTaskRecurrentRequest setTaskRecurrentRequest) throws ResourceNotFoundException, StreemException, JsonProcessingException;

  TaskDto unsetTaskRecurrence(Long taskId) throws ResourceNotFoundException, StreemException;

  TaskDto addStop(Long taskId) throws ResourceNotFoundException, StreemException;

  TaskDto removeStop(Long taskId) throws ResourceNotFoundException, StreemException;

  TaskDto addMedia(Long taskId, MediaRequest mediaRequest) throws ResourceNotFoundException, StreemException;

  MediaDto updateMedia(Long taskId, Long mediaId, MediaRequest mediaRequest) throws ResourceNotFoundException, StreemException;

  TaskDto deleteMedia(Long taskId, Long mediaId);

  TaskDto setTimer(Long taskId, TimerRequest timerRequest) throws StreemException, ResourceNotFoundException;

  TaskDto unsetTimer(Long taskId) throws StreemException, ResourceNotFoundException;

  TaskDto archiveTask(Long taskId) throws StreemException, ResourceNotFoundException;

  BasicDto reorderTasks(TaskReorderRequest taskReorderRequest) throws ResourceNotFoundException, StreemException;

  TaskDto addAutomation(Long taskId, AutomationRequest automationRequest) throws ResourceNotFoundException, StreemException, JsonProcessingException;

  TaskDto deleteAutomation(Long taskId, Long automationId) throws ResourceNotFoundException, StreemException;

  TaskDto updateAutomation(Long taskId, Long automationId, AutomationRequest automationRequest) throws ResourceNotFoundException, StreemException, JsonProcessingException;

  InterlockDto addInterlockForTask(String taskId, InterlockRequest interlockRequest) throws StreemException, ResourceNotFoundException, JsonProcessingException;

  InterlockDto getInterlockByTaskId(String taskId) throws StreemException, ResourceNotFoundException, JsonProcessingException;

  InterlockDto updateInterlockForTask(String taskId, InterlockRequest interlockRequest) throws StreemException, ResourceNotFoundException, JsonProcessingException;

  BasicDto deleteInterlockByTaskId(String interlockId, Long taskId) throws StreemException, ResourceNotFoundException, JsonProcessingException;

  TaskSchedulesDto getTaskSchedule(Long taskId) throws ResourceNotFoundException;

  TaskDto setTaskSchedules(Long taskId, TaskSchedulesRequest taskSchedulesRequest) throws ResourceNotFoundException, StreemException, JsonProcessingException;

  TaskDto unsetTaskSchedules(Long taskId) throws ResourceNotFoundException, StreemException;

  TaskDto addSoloTaskLock(Long taskId) throws ResourceNotFoundException, StreemException;

  TaskDto removeSoloTaskLock(Long taskId) throws ResourceNotFoundException, StreemException;
  BasicDto addTaskExecutorLock(Long taskId, TaskExecutorLockRequest taskExecutorLockRequest) throws ResourceNotFoundException, StreemException;

  BasicDto unsetExecutor(Long taskId);

  List<TaskExecutorLockView> getTaskExecutorLockView(Long taskId);

  TaskDto setBulkVerification(Long taskId);

  TaskDto unsetBulkVerification(Long taskId);
}
