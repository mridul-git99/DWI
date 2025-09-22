package com.leucine.streem.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.leucine.streem.dto.BulkParameterExceptionRequest;
import com.leucine.streem.dto.CorrectionDto;
import com.leucine.streem.dto.ParameterExceptionDto;
import com.leucine.streem.dto.request.ParameterExceptionApproveRejectRequest;
import com.leucine.streem.dto.request.ParameterExceptionAutoAcceptRequest;
import com.leucine.streem.dto.request.ParameterExceptionInitiatorRequest;
import com.leucine.streem.dto.response.Response;
import com.leucine.streem.exception.ParameterExecutionException;
import com.leucine.streem.exception.ResourceNotFoundException;
import com.leucine.streem.exception.StreemException;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/v1/exceptions")
public interface IParameterExceptionController {
  @ResponseBody
  @PostMapping("/parameter-executions/{parameterExecutionId}/initiate")
  Response<ParameterExceptionDto> initiateParameterException(@PathVariable Long parameterExecutionId, @RequestBody ParameterExceptionInitiatorRequest parameterExceptionInitiatorRequest) throws StreemException, ResourceNotFoundException, JsonProcessingException;

  @ResponseBody
  @PatchMapping("/parameter-executions/{parameterExecutionId}/approve")
  Response<ParameterExceptionDto> approveParameterException(@PathVariable Long parameterExecutionId, @RequestBody ParameterExceptionApproveRejectRequest parameterExceptionApproveRejectRequest) throws Exception;

  @ResponseBody
  @PatchMapping("/parameter-executions/{parameterExecutionId}/reject")
  Response<ParameterExceptionDto> rejectParameterException(@PathVariable Long parameterExecutionId, @RequestBody ParameterExceptionApproveRejectRequest parameterExceptionApproveRejectRequest) throws StreemException, ResourceNotFoundException;

  @ResponseBody
  @PostMapping("/parameter-executions/{parameterExecutionId}/auto-accept")
  Response<ParameterExceptionDto> autoAcceptParameterException(@PathVariable Long parameterExecutionId, @RequestBody ParameterExceptionAutoAcceptRequest parameterExceptionAutoAcceptRequest) throws Exception;

  @ResponseBody
  @PostMapping("/parameter-executions/bulk")
  Response<BulkParameterExceptionRequest> bulkParameterException(@RequestBody BulkParameterExceptionRequest bulkParameterExceptionRequest) throws Exception;
}
