package com.leucine.streem.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.leucine.streem.dto.CorrectionDto;
import com.leucine.streem.dto.ParameterVerificationListViewDto;
import com.leucine.streem.dto.TaskExecutionDto;
import com.leucine.streem.dto.projection.CorrectionListViewProjection;
import com.leucine.streem.dto.request.ParameterCorrectionApproveRejectRequest;
import com.leucine.streem.dto.request.ParameterCorrectionCorrectorRequest;
import com.leucine.streem.dto.request.ParameterCorrectionInitiatorRequest;
import com.leucine.streem.dto.request.ParameterCorrectionRecallRequest;
import com.leucine.streem.dto.response.Response;
import com.leucine.streem.exception.ResourceNotFoundException;
import com.leucine.streem.exception.StreemException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/v1/corrections")
public interface ICorrectionController {

  @ResponseBody
  @PostMapping("/task-executions/{taskExecutionId}/enable")
  Response<TaskExecutionDto> enableCorrection(@PathVariable Long taskExecutionId) throws StreemException;

  @ResponseBody
  @PatchMapping("/task-executions/{taskExecutionId}/cancel")
  Response<TaskExecutionDto> cancelCorrection(@PathVariable Long taskExecutionId) throws StreemException;

  @ResponseBody
  @PostMapping("/parameter-executions/{parameterExecutionId}/initiate")
  Response<CorrectionDto> initiateCorrection(@PathVariable Long parameterExecutionId, @RequestBody ParameterCorrectionInitiatorRequest parameterCorrectionInitiatorRequest) throws StreemException, ResourceNotFoundException;

  @ResponseBody
  @PatchMapping("/parameter-executions/{parameterExecutionId}/correct")
  Response<CorrectionDto> performCorrection(@PathVariable Long parameterExecutionId, @RequestBody ParameterCorrectionCorrectorRequest parameterCorrectionCorrectorRequest) throws StreemException, ResourceNotFoundException, IOException;

  @ResponseBody
  @PatchMapping("/parameter-executions/{parameterExecutionId}/approve")
  Response<CorrectionDto> approveCorrection(@PathVariable Long parameterExecutionId, @RequestBody ParameterCorrectionApproveRejectRequest parameterCorrectionApproveRejectRequest) throws StreemException, ResourceNotFoundException, IOException;

  @ResponseBody
  @PatchMapping("/parameter-executions/{parameterExecutionId}/reject")
  Response<CorrectionDto> rejectCorrection(@PathVariable Long parameterExecutionId, @RequestBody ParameterCorrectionApproveRejectRequest parameterCorrectionApproveRejectRequest) throws StreemException, ResourceNotFoundException;

  @ResponseBody
  @GetMapping
  Response<Page<CorrectionDto>> getAllCorrections(@RequestParam Long userId,@RequestParam Long useCaseId,@RequestParam(required = false) String status, @RequestParam(required = false) String parameterName, @RequestParam(required = false) String processName, @RequestParam(required = false) Long jobId,@RequestParam(required = false) Long initiatedBy, Pageable pageable) throws StreemException, ResourceNotFoundException, JsonProcessingException;

  @ResponseBody
  @PatchMapping("/parameter-executions/{parameterExecutionId}/recall")
  Response<CorrectionDto> recallCorrection(@PathVariable Long parameterExecutionId, @RequestBody ParameterCorrectionRecallRequest parameterCorrectionRecallRequest) throws ResourceNotFoundException, StreemException;


}
