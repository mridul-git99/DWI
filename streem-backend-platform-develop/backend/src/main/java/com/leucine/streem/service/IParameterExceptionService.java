package com.leucine.streem.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.leucine.streem.dto.BulkParameterExceptionRequest;
import com.leucine.streem.dto.ParameterExceptionDto;
import com.leucine.streem.dto.request.ParameterExceptionApproveRejectRequest;
import com.leucine.streem.dto.request.ParameterExceptionAutoAcceptRequest;
import com.leucine.streem.dto.request.ParameterExceptionInitiatorRequest;
import com.leucine.streem.dto.request.ParameterExceptionRequest;
import com.leucine.streem.exception.ParameterExecutionException;
import com.leucine.streem.exception.ResourceNotFoundException;
import com.leucine.streem.exception.StreemException;

import java.io.IOException;
import java.util.List;

public interface IParameterExceptionService {
  ParameterExceptionDto initiateParameterException(Long parameterExecutionId, ParameterExceptionInitiatorRequest parameterExceptionInitiatorRequest) throws StreemException, ResourceNotFoundException, JsonProcessingException;

  ParameterExceptionDto approveParameterException(Long parameterExecutionId, ParameterExceptionApproveRejectRequest parameterExceptionApproveRejectRequest) throws ResourceNotFoundException, StreemException, IOException, ParameterExecutionException;

  ParameterExceptionDto rejectParameterException(Long parameterExecutionId, ParameterExceptionApproveRejectRequest parameterExceptionApproveRejectRequest) throws StreemException, ResourceNotFoundException;

  ParameterExceptionDto autoAcceptParameterException(Long parameterExecutionId, ParameterExceptionAutoAcceptRequest parameterExceptionAutoAcceptRequest) throws StreemException, ResourceNotFoundException, IOException, ParameterExecutionException;

  List<ParameterExceptionRequest> bulkParameterException(BulkParameterExceptionRequest bulkParameterExceptionRequest) throws Exception;
}
