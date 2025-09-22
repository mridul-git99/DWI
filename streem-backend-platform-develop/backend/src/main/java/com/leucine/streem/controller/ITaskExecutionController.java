package com.leucine.streem.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.leucine.streem.dto.*;
import com.leucine.streem.dto.projection.TaskExecutionAssigneeView;
import com.leucine.streem.dto.request.*;
import com.leucine.streem.dto.response.Response;
import com.leucine.streem.exception.ParameterExecutionException;
import com.leucine.streem.exception.ResourceNotFoundException;
import com.leucine.streem.exception.StreemException;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/v1/task-executions")
public interface ITaskExecutionController {
  @GetMapping("/{taskExecutionId}")
  Response<TaskDto> getTask(@PathVariable Long taskExecutionId) throws ResourceNotFoundException;

  // TODO fix apis when you move to repeat, dynamic task
  @GetMapping("/{taskExecutionId}/validate")
  Response<TaskDto> validateTask(@PathVariable Long taskExecutionId) throws ResourceNotFoundException, StreemException;

  @PatchMapping("/{taskExecutionId}/start")
  Response<TaskExecutionDto> startTask(@PathVariable Long taskExecutionId, @RequestBody TaskExecutionRequest taskExecutionRequest) throws ResourceNotFoundException, StreemException, IOException, ParameterExecutionException;

  @PatchMapping("/{taskExecutionId}/complete")
  Response<TaskExecutionDto> completeTask(@PathVariable Long taskExecutionId, @RequestBody TaskCompletionRequest taskCompletionRequest) throws ResourceNotFoundException, StreemException, IOException, ParameterExecutionException;

  @PostMapping("/repeat")
  Response<TaskExecutionDto> repeatTask(@RequestBody TaskRepeatRequest taskRepeatRequest) throws ResourceNotFoundException, StreemException, IOException;

  @PatchMapping("/{taskExecutionId}/complete-with-exception")
  Response<TaskExecutionDto> completeTaskWithException(@PathVariable Long taskExecutionId, @RequestBody TaskCompletionRequest taskCompletionRequest) throws ResourceNotFoundException, StreemException, IOException;

  @PatchMapping("/sign-off")
  Response<BasicDto> signOffTask(@RequestBody TaskSignOffRequest taskSignOffRequest) throws StreemException;

  @PostMapping("/assignments")
  Response<List<TaskExecutionAssigneeView>> getTaskExecutionAssignees( @RequestBody TaskAssigneeDto taskAssigneeDto);

  @PatchMapping("/{taskExecutionId}/skip")
  Response<TaskExecutionDto> skipTask(@PathVariable Long taskExecutionId, @RequestBody TaskExecutionRequest taskExecutionRequest) throws ResourceNotFoundException, StreemException, IOException;

  @PatchMapping("/{taskExecutionId}/correction/start")
  Response<TaskExecutionDto> enableCorrection(@PathVariable Long taskExecutionId, @RequestBody TaskExecutionRequest taskExecutionRequest) throws StreemException;

  @PatchMapping("/{taskExecutionId}/correction/cancel")
  Response<TaskExecutionDto> cancelCorrection(@PathVariable Long taskExecutionId, @RequestBody TaskExecutionRequest taskExecutionRequest) throws ResourceNotFoundException, StreemException;

  @PatchMapping("/{taskExecutionId}/correction/complete")
  Response<TaskExecutionDto> completeCorrection(@PathVariable Long taskExecutionId, @RequestBody TaskExecutionRequest taskExecutionRequest) throws ResourceNotFoundException, StreemException, IOException;

  @PostMapping("/{taskExecutionId}/pause")
  Response<TaskExecutionDto> pauseTask(@PathVariable Long taskExecutionId, @RequestBody TaskPauseOrResumeRequest taskPauseOrResumeRequest) throws StreemException;

  @PatchMapping("/{taskExecutionId}/resume")
  Response<TaskExecutionDto> resumeTask(@PathVariable Long taskExecutionId, @RequestBody TaskPauseOrResumeRequest taskPauseOrResumeRequest) throws StreemException;

  @DeleteMapping("/{taskExecutionId}/remove")
  Response<BasicDto> removeTask(@PathVariable Long taskExecutionId) throws StreemException, ResourceNotFoundException;

  @PatchMapping("/{taskExecutionId}/stop-recurring")
  Response<BasicDto> stopTaskExecutionRecurring(@PathVariable Long taskExecutionId) throws StreemException, ResourceNotFoundException;

  @GetMapping("/{taskExecutionId}/check/assignee")
  Response<UserTaskAssigneeStatusDto> checkTaskAssignee(@PathVariable Long taskExecutionId) throws ResourceNotFoundException, StreemException;

  @GetMapping("/{taskExecutionId}/state")
  Response<TaskDetailsDto> pollTaskData(@PathVariable Long taskExecutionId) throws ResourceNotFoundException, JsonProcessingException;

}
