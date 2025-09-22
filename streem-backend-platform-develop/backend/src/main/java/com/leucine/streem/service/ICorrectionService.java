package com.leucine.streem.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.leucine.streem.dto.CorrectionDto;
import com.leucine.streem.dto.ParameterVerificationListViewDto;
import com.leucine.streem.dto.TaskExecutionDto;
import com.leucine.streem.dto.projection.CorrectionListViewProjection;
import com.leucine.streem.dto.request.ParameterCorrectionApproveRejectRequest;
import com.leucine.streem.dto.request.ParameterCorrectionCorrectorRequest;
import com.leucine.streem.dto.request.ParameterCorrectionInitiatorRequest;
import com.leucine.streem.dto.request.ParameterCorrectionRecallRequest;
import com.leucine.streem.exception.ResourceNotFoundException;
import com.leucine.streem.exception.StreemException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.io.IOException;

public interface ICorrectionService {
  TaskExecutionDto enableCorrection(Long taskExecutionId) throws StreemException;

  TaskExecutionDto cancelCorrection(Long taskExecutionId) throws StreemException;

  CorrectionDto initiateCorrection(Long parameterExecutionId, ParameterCorrectionInitiatorRequest parameterCorrectionInitiatorRequest) throws StreemException, ResourceNotFoundException;

  CorrectionDto performCorrection(Long parameterExecutionId, ParameterCorrectionCorrectorRequest parameterCorrectionCorrectorRequest) throws ResourceNotFoundException, StreemException, IOException;

  CorrectionDto approveCorrection(Long parameterExecutionId, ParameterCorrectionApproveRejectRequest parameterCorrectionApproveRejectRequest) throws ResourceNotFoundException, StreemException, IOException;

  CorrectionDto rejectCorrection(Long parameterExecutionId, ParameterCorrectionApproveRejectRequest parameterCorrectionApproveRejectRequest) throws ResourceNotFoundException, StreemException;

  Page<CorrectionDto> getAllCorrections(Long userId,Long useCaseId,String status, String parameterName, String processName,Long jobId,Long initiatedBy, Pageable pageable) throws ResourceNotFoundException, StreemException, JsonProcessingException;

  CorrectionDto recallCorrection(Long parameterExecutionId, ParameterCorrectionRecallRequest parameterCorrectionRecallRequest) throws ResourceNotFoundException, StreemException;
}
