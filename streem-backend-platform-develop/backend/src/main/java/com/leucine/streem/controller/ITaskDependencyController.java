package com.leucine.streem.controller;

import com.leucine.streem.dto.TaskDependencyDetailsDto;
import com.leucine.streem.dto.TaskDependencyDto;
import com.leucine.streem.dto.request.TaskDependencyRequest;
import com.leucine.streem.dto.response.Response;
import com.leucine.streem.exception.ResourceNotFoundException;
import com.leucine.streem.exception.StreemException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/tasks")
public interface ITaskDependencyController {

  @GetMapping("/{taskId}/dependencies")
  @ResponseBody
  Response<TaskDependencyDto> getTaskDependenciesByTaskId(@PathVariable Long taskId) throws ResourceNotFoundException, StreemException;

  @GetMapping("/{taskId}/dependencies/details")
  @ResponseBody
  Response<TaskDependencyDetailsDto> getTaskDependenciesDetailsByTaskId(@PathVariable Long taskId) throws ResourceNotFoundException, StreemException;

  @PatchMapping("/{taskId}/dependencies")
  @ResponseBody
  Response<TaskDependencyDto> updateTaskDependency(@PathVariable Long taskId, @RequestBody TaskDependencyRequest taskDependencyRequest);

}
