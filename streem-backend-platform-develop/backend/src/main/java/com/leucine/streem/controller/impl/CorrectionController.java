package com.leucine.streem.controller.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.leucine.streem.controller.ICorrectionController;
import com.leucine.streem.dto.CorrectionDto;
import com.leucine.streem.dto.TaskExecutionDto;
import com.leucine.streem.dto.request.ParameterCorrectionApproveRejectRequest;
import com.leucine.streem.dto.request.ParameterCorrectionCorrectorRequest;
import com.leucine.streem.dto.request.ParameterCorrectionInitiatorRequest;
import com.leucine.streem.dto.request.ParameterCorrectionRecallRequest;
import com.leucine.streem.dto.response.Response;
import com.leucine.streem.exception.ResourceNotFoundException;
import com.leucine.streem.exception.StreemException;
import com.leucine.streem.service.ICorrectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CorrectionController implements ICorrectionController {
    private final ICorrectionService correctionService;

    @Autowired
    public CorrectionController(ICorrectionService correctionService) {
        this.correctionService = correctionService;
    }

    @Override
    public Response<TaskExecutionDto> enableCorrection(Long taskExecutionId) throws StreemException {
        return Response.builder().data(correctionService.enableCorrection(taskExecutionId)).build();
    }

    @Override
    public Response<TaskExecutionDto> cancelCorrection(Long taskExecutionId) throws StreemException {
        return Response.builder().data(correctionService.cancelCorrection(taskExecutionId)).build();
    }

  @Override
  public Response<CorrectionDto> initiateCorrection(Long parameterExecutionId, ParameterCorrectionInitiatorRequest parameterCorrectionInitiatorRequest) throws StreemException, ResourceNotFoundException {
    return Response.builder().data(correctionService.initiateCorrection(parameterExecutionId, parameterCorrectionInitiatorRequest)).build();
  }

  public Response<CorrectionDto> performCorrection(Long parameterExecutionId, ParameterCorrectionCorrectorRequest parameterCorrectionCorrectorRequest) throws StreemException, ResourceNotFoundException, IOException {
    return Response.builder().data(correctionService.performCorrection(parameterExecutionId, parameterCorrectionCorrectorRequest)).build();
  }

  public Response<CorrectionDto> approveCorrection(Long parameterExecutionId, ParameterCorrectionApproveRejectRequest parameterCorrectionApproveRejectRequest) throws StreemException, ResourceNotFoundException, IOException {
    return Response.builder().data(correctionService.approveCorrection(parameterExecutionId, parameterCorrectionApproveRejectRequest)).build();
  }

  public Response<CorrectionDto> rejectCorrection(Long parameterExecutionId, ParameterCorrectionApproveRejectRequest parameterCorrectionApproveRejectRequest) throws StreemException, ResourceNotFoundException {
    return Response.builder().data(correctionService.rejectCorrection(parameterExecutionId, parameterCorrectionApproveRejectRequest)).build();
  }

  @Override
  public Response<CorrectionDto> recallCorrection(Long parameterExecutionId, ParameterCorrectionRecallRequest parameterCorrectionRecallRequest) throws ResourceNotFoundException, StreemException {
    return Response.builder().data(correctionService.recallCorrection(parameterExecutionId, parameterCorrectionRecallRequest)).build();
  }

  public Response<Page<CorrectionDto>> getAllCorrections(Long userId, Long useCaseId, String status, String parameterName, String processName, Long jobId, Long initiatedBy, Pageable pageable) throws StreemException, ResourceNotFoundException, JsonProcessingException {
    return Response.builder().data(correctionService.getAllCorrections(userId,useCaseId,status,parameterName,processName, jobId,initiatedBy,pageable)).build();
  }

}
