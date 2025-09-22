package com.leucine.streem.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.leucine.streem.dto.*;
import com.leucine.streem.dto.projection.TaskExecutorLockView;
import com.leucine.streem.dto.request.*;
import com.leucine.streem.dto.response.Response;
import com.leucine.streem.exception.ResourceNotFoundException;
import com.leucine.streem.exception.StreemException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public interface ITaskController {

  @PostMapping("/v1/checklists/{checklistId}/stages/{stageId}/tasks")
  Response<TaskDto> createTask(@PathVariable Long checklistId, @PathVariable Long stageId, @RequestBody TaskRequest taskRequest) throws ResourceNotFoundException, StreemException;

  @GetMapping("/v1/tasks/{taskId}")
  Response<TaskDto> getTask(@PathVariable Long taskId) throws ResourceNotFoundException, StreemException;

  @PatchMapping("/v1/tasks/{taskId}")
  Response<TaskDto> updateTask(@PathVariable Long taskId, @RequestBody TaskRequest taskRequest) throws ResourceNotFoundException, StreemException;

  @GetMapping("/v1/tasks/{taskId}/recurrence")
  Response<TaskRecurrenceDto> getTaskRecurrence(@PathVariable Long taskId) throws ResourceNotFoundException;

  @PatchMapping("/v1/tasks/{taskId}/recurrence/set")
  Response<TaskDto> setTaskRecurrence(@PathVariable Long taskId, @RequestBody SetTaskRecurrentRequest setTaskRecurrentRequest) throws StreemException, ResourceNotFoundException, JsonProcessingException;

  @PatchMapping("/v1/tasks/{taskId}/recurrence/unset")
  Response<TaskDto> unsetTaskRecurrence(@PathVariable Long taskId) throws StreemException, ResourceNotFoundException;

  @PatchMapping("/v1/tasks/reorder")
  Response<BasicDto> reorderTasks(@RequestBody TaskReorderRequest taskReorderRequest) throws StreemException, ResourceNotFoundException;

  @PatchMapping("/v1/tasks/{taskId}/timer/set")
  Response<TaskDto> setTimer(@PathVariable Long taskId, @RequestBody TimerRequest timerRequest) throws ResourceNotFoundException, StreemException;

  @PatchMapping("/v1/tasks/{taskId}/timer/unset")
  Response<TaskDto> unsetTimer(@PathVariable Long taskId) throws ResourceNotFoundException, StreemException;

  @PatchMapping("/v1/tasks/{taskId}/stop/add")
  Response<TaskDto> addStop(@PathVariable Long taskId) throws ResourceNotFoundException, StreemException;

  @PatchMapping("/v1/tasks/{taskId}/stop/remove")
  Response<TaskDto> removeStop(@PathVariable Long taskId) throws ResourceNotFoundException, StreemException;

  @PostMapping("/v1/tasks/{taskId}/medias")
  Response<TaskDto> addMedia(@PathVariable Long taskId, @RequestBody MediaRequest mediaRequest) throws ResourceNotFoundException, StreemException;

  @PatchMapping("/v1/tasks/{taskId}/medias/{mediaId}")
  Response<MediaDto> updateMedia(@PathVariable Long taskId, @PathVariable Long mediaId, @RequestBody MediaRequest mediaRequest) throws ResourceNotFoundException, StreemException;

  @DeleteMapping("/v1/tasks/{taskId}/medias/{mediaId}")
  Response<TaskDto> deleteMedia(@PathVariable Long taskId, @PathVariable Long mediaId);

  @PatchMapping("/v1/tasks/{taskId}/archive")
  Response<TaskDto> archiveTask(@PathVariable Long taskId) throws ResourceNotFoundException, StreemException;

  @PostMapping("/v1/tasks/{taskId}/automations")
  Response<TaskDto> addAutomation(@PathVariable Long taskId, @RequestBody AutomationRequest automationRequest) throws StreemException, ResourceNotFoundException, JsonProcessingException;

  @DeleteMapping("/v1/tasks/{taskId}/automations/{automationId}")
  Response<TaskDto> deleteAutomation(@PathVariable Long taskId, @PathVariable Long automationId) throws StreemException, ResourceNotFoundException;

  @PatchMapping("/v1/tasks/{taskId}/automations/{automationId}")
  Response<TaskDto> updateAutomation(@PathVariable Long taskId, @PathVariable Long automationId, @RequestBody AutomationRequest automationRequest) throws StreemException, ResourceNotFoundException, JsonProcessingException;

  @GetMapping("/v1/tasks/{taskId}/interlocks")
  Response<InterlockDto> getInterlockByTaskId(@PathVariable String taskId) throws StreemException, ResourceNotFoundException, JsonProcessingException;

  @PostMapping("/v1/tasks/{taskId}/interlocks")
  Response<InterlockDto> addInterlockForTask(@PathVariable String taskId, @RequestBody InterlockRequest interlockRequest) throws StreemException, ResourceNotFoundException, JsonProcessingException;

  @PatchMapping("/v1/tasks/{taskId}/interlocks")
  Response<InterlockDto> updateInterlockForTask(@PathVariable String taskId, @RequestBody InterlockRequest interlockRequest) throws StreemException, ResourceNotFoundException, JsonProcessingException;

  @DeleteMapping("/v1/tasks/{taskId}/interlocks/{interlockId}")
  Response<BasicDto> deleteInterlockByTaskId(@PathVariable String interlockId, @PathVariable Long taskId) throws StreemException, ResourceNotFoundException, JsonProcessingException;

  @GetMapping("/v1/tasks/{taskId}/schedules")
  Response<TaskSchedulesDto> getTaskSchedule(@PathVariable Long taskId) throws ResourceNotFoundException;

  @PatchMapping("/v1/tasks/{taskId}/schedules")
  Response<TaskDto> setTaskSchedules(@PathVariable Long taskId, @RequestBody TaskSchedulesRequest taskSchedulesRequest) throws ResourceNotFoundException, StreemException, JsonProcessingException;

  @PatchMapping("/v1/tasks/{taskId}/schedules/unset")
  Response<TaskDto> unsetTaskSchedules(@PathVariable Long taskId) throws ResourceNotFoundException, StreemException;

  @PatchMapping("/v1/tasks/{taskId}/solo/set")
  Response<TaskDto> addSoloTaskLock(@PathVariable Long taskId) throws ResourceNotFoundException, StreemException;

  @PatchMapping("/v1/tasks/{taskId}/solo/unset")
  Response<TaskDto> removeSoloTaskLock(@PathVariable Long taskId) throws ResourceNotFoundException, StreemException;

  @PostMapping("/v1/tasks/{taskId}/executors")
  Response<BasicDto> addExecutor(@PathVariable Long taskId, @RequestBody TaskExecutorLockRequest taskExecutorLockRequest) throws ResourceNotFoundException, StreemException;

  @PatchMapping("/v1/tasks/{taskId}/executors/unset")
  Response<TaskDto> removeExecutor(@PathVariable Long taskId) throws ResourceNotFoundException, StreemException;


  @GetMapping("/v1/tasks/{taskId}/executors")
  Response<List<TaskExecutorLockView>> getTaskExecutorLockView(@PathVariable Long taskId);

  @PatchMapping("/v1/tasks/{taskId}/bulk-verifications/set")
  Response<TaskDto> setBulkVerification(@PathVariable Long taskId) throws ResourceNotFoundException, StreemException;

  @PatchMapping("/v1/tasks/{taskId}/bulk-verifications/unset")
  Response<TaskDto> unsetBulkVerification(@PathVariable Long taskId);


}

