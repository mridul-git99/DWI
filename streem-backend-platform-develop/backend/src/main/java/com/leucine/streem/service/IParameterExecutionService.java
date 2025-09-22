package com.leucine.streem.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.leucine.streem.collections.partial.PartialEntityObject;
import com.leucine.streem.constant.Type;
import com.leucine.streem.dto.*;
import com.leucine.streem.dto.request.CreateVariationRequest;
import com.leucine.streem.dto.request.ParameterExecuteRequest;
import com.leucine.streem.dto.request.ParameterPartialRequest;
import com.leucine.streem.dto.request.ParameterStateChangeRequest;
import com.leucine.streem.exception.ParameterExecutionException;
import com.leucine.streem.exception.ResourceNotFoundException;
import com.leucine.streem.exception.StreemException;
import com.leucine.streem.model.Parameter;
import com.leucine.streem.model.ParameterValue;
import com.leucine.streem.model.helper.PrincipalUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface IParameterExecutionService {
  ParameterDto executeParameter(Long jobId, ParameterExecuteRequest parameterExecuteRequest, boolean isAutoInitialized, Type.JobLogTriggerType jobLogTriggerType, PrincipalUser principalUser, boolean isCreateJobRequest, boolean isScheduled) throws StreemException, ResourceNotFoundException, IOException, ParameterExecutionException;

  TempParameterDto executeParameterForError(ParameterExecuteRequest parameterExecuteRequest) throws IOException, StreemException, ResourceNotFoundException, ParameterExecutionException;

  ParameterDto rejectParameter(Long parameterExecutionId, ParameterStateChangeRequest parameterStateChangeRequest) throws ResourceNotFoundException, StreemException;

  ParameterDto approveParameter(Long parameterExecutionId, ParameterStateChangeRequest parameterStateChangeRequest) throws ResourceNotFoundException, StreemException, IOException;

  RuleHideShowDto tempExecuteRules(Map<Long, ParameterExecuteRequest> parameterExecuteRequestMap, Long checklistId) throws IOException;

  RuleHideShowDto updateRules(Long jobId, Parameter parameter, ParameterValue parameterValue) throws Exception;

  BasicDto createVariations(CreateVariationRequest createVariationRequest) throws ResourceNotFoundException, StreemException, JsonProcessingException;

  Page<ParameterDto> getAllAllowedParametersForVariations(Long jobId, String filters, String parameterName, Pageable pageable);

  Page<VariationDto> getAllVariationsOfJob(Long jobId, String parameterName, Pageable pageable);

  BasicDto deleteVariation(DeleteVariationRequest deleteVariationRequest) throws ResourceNotFoundException, StreemException;

  List<VariationDto> getAllVariationsOfParameterExecution(Long parameterValueId);

  void validateIfUserIsAssignedToExecuteParameter(Long taskExecutionId, Long currentUserId) throws StreemException;

  Page<PartialEntityObject> getAllFilteredEntityObjects(Long parameterExecutionId, String query, String shortCode, Pageable pageable, Boolean isCjf) throws IOException, ResourceNotFoundException;

  List<ParameterDto> getParameterExecutionByParameterIdAndJobId(Long jobId, String filters) throws ResourceNotFoundException;

  List<ParameterPartialDto> getParameterPartialData(Long jobId, ParameterPartialRequest parameterPartialRequest) throws JsonProcessingException;

  List<ParameterPartialDto> getParameterPartialDataForMaster(Long jobId, ParameterPartialRequest parameterPartialRequest) throws JsonProcessingException;
}
