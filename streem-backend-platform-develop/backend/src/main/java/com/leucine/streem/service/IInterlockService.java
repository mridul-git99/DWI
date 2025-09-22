package com.leucine.streem.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.leucine.streem.constant.Type;
import com.leucine.streem.dto.BasicDto;
import com.leucine.streem.dto.request.InterlockDto;
import com.leucine.streem.dto.request.InterlockRequest;
import com.leucine.streem.dto.response.Error;
import com.leucine.streem.exception.ResourceNotFoundException;
import com.leucine.streem.exception.StreemException;
import com.leucine.streem.model.Task;
import com.leucine.streem.model.User;

import java.io.IOException;
import java.util.List;

public interface IInterlockService {


  InterlockDto addInterlockForTask(Task task, InterlockRequest interlockRequest, User principalUserEntity) throws StreemException, ResourceNotFoundException, JsonProcessingException;

  InterlockDto getAndCreateInterlockByTaskId(Task task, User principalUserEntity) throws StreemException, ResourceNotFoundException, JsonProcessingException;

  InterlockDto getInterlockByTaskId(Task task, User principalUserEntity) throws StreemException, ResourceNotFoundException, JsonProcessingException;

  void validateInterlockForTaskExecution(Long taskId, String jobIdAsString, Type.InterlockTriggerType triggerType) throws StreemException, ResourceNotFoundException, IOException;

  InterlockDto updateInterlockForTask(Task task, InterlockRequest interlockRequest, User principalUserEntity) throws StreemException, ResourceNotFoundException, JsonProcessingException;

  BasicDto deleteInterlockByTaskId(String interlockId, Long taskId, User principalUserEntity) throws StreemException, ResourceNotFoundException, JsonProcessingException;

  void validateTaskInterlocks(Task task, List<Error> errorList) throws JsonProcessingException;
}
