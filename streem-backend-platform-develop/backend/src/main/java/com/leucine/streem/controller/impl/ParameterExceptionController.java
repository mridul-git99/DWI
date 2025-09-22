package com.leucine.streem.controller.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.leucine.streem.controller.IParameterExceptionController;
import com.leucine.streem.dto.BulkParameterExceptionRequest;
import com.leucine.streem.dto.ParameterExceptionDto;
import com.leucine.streem.dto.request.ParameterExceptionApproveRejectRequest;
import com.leucine.streem.dto.request.ParameterExceptionAutoAcceptRequest;
import com.leucine.streem.dto.request.ParameterExceptionInitiatorRequest;
import com.leucine.streem.dto.request.ParameterExceptionRequest;
import com.leucine.streem.dto.response.Response;
import com.leucine.streem.exception.ParameterExecutionException;
import com.leucine.streem.exception.ResourceNotFoundException;
import com.leucine.streem.exception.StreemException;
import com.leucine.streem.service.IParameterExceptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class ParameterExceptionController implements IParameterExceptionController {
  private final IParameterExceptionService parameterExceptionService;

  @Autowired
  public ParameterExceptionController(IParameterExceptionService parameterExceptionService) {
    this.parameterExceptionService = parameterExceptionService;
  }


  @Override
  public Response<ParameterExceptionDto> initiateParameterException(Long parameterExecutionId, ParameterExceptionInitiatorRequest parameterExceptionInitiatorRequest) throws StreemException, ResourceNotFoundException, JsonProcessingException {
    return Response.builder().data(parameterExceptionService.initiateParameterException(parameterExecutionId, parameterExceptionInitiatorRequest)).build();
  }

  @Override
  public Response<ParameterExceptionDto> approveParameterException(Long parameterExecutionId, ParameterExceptionApproveRejectRequest parameterExceptionApproveRejectRequest) throws Exception {
    return Response.builder().data(parameterExceptionService.approveParameterException(parameterExecutionId, parameterExceptionApproveRejectRequest)).build();
  }

  @Override
  public Response<ParameterExceptionDto> rejectParameterException(Long parameterExecutionId, ParameterExceptionApproveRejectRequest parameterExceptionApproveRejectRequest) throws StreemException, ResourceNotFoundException {
    return Response.builder().data(parameterExceptionService.rejectParameterException(parameterExecutionId, parameterExceptionApproveRejectRequest)).build();
  }

  @Override
  public Response<ParameterExceptionDto> autoAcceptParameterException(Long parameterExecutionId, ParameterExceptionAutoAcceptRequest parameterExceptionAutoAcceptRequest) throws Exception {
    return Response.builder().data(parameterExceptionService.autoAcceptParameterException(parameterExecutionId, parameterExceptionAutoAcceptRequest)).build();
  }

  @Override
  public Response<BulkParameterExceptionRequest> bulkParameterException(BulkParameterExceptionRequest bulkParameterExceptionRequest) throws Exception {
    return Response.builder().data(parameterExceptionService.bulkParameterException(bulkParameterExceptionRequest)).build();
  }
}
