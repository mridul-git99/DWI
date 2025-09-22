package com.leucine.streem.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.leucine.streem.constant.Type;
import com.leucine.streem.dto.AutomationResponseDto;
import com.leucine.streem.dto.TaskDto;
import com.leucine.streem.dto.request.AutomationRequest;
import com.leucine.streem.dto.request.CreateObjectAutomationRequest;
import com.leucine.streem.exception.ResourceNotFoundException;
import com.leucine.streem.exception.StreemException;

import java.io.IOException;
import java.util.List;

public interface ITaskAutomationService {
  TaskDto addTaskAutomation(Long taskId, AutomationRequest automationRequest) throws ResourceNotFoundException, JsonProcessingException, StreemException;

  TaskDto updateAutomation(Long taskId, Long automationId, AutomationRequest automationRequest) throws ResourceNotFoundException, StreemException, JsonProcessingException;

  TaskDto deleteTaskAutomation(Long taskId, Long automationId) throws ResourceNotFoundException;

  List<AutomationResponseDto> completeTaskAutomations(Long taskId, Long jobId, List<CreateObjectAutomationRequest> createObjectAutomationRequests,
                                                      String automationReason, Type.AutomationTriggerType automationTriggerType) throws IOException, ResourceNotFoundException, StreemException;
}
