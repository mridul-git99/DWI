package com.leucine.streem.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.leucine.streem.collections.partial.PartialEntityObject;
import com.leucine.streem.dto.*;
import com.leucine.streem.dto.request.*;
import com.leucine.streem.dto.response.Response;
import com.leucine.streem.exception.ParameterExecutionException;
import com.leucine.streem.exception.ResourceNotFoundException;
import com.leucine.streem.exception.StreemException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/v1/parameter-executions")
public interface IParameterExecutionController {

  @PatchMapping("/{parameterExecutionId}/execute")
  @ResponseBody
  Response<ParameterDto> executeParameter(@PathVariable Long parameterExecutionId, @RequestBody ParameterExecuteRequest parameterExecuteRequest) throws IOException, StreemException, ResourceNotFoundException;

  @PatchMapping("/bulk/execute")
  Response<List<ParameterDto>> executeParameters(@RequestBody List<BulkParameterExecuteRequest> bulkParameterExecuteRequests) throws Exception;

  @PatchMapping("/{parameterExecutionId}/error-correction")
  @ResponseBody
  Response<TempParameterDto> fixError(@PathVariable Long parameterExecutionId, @RequestBody ParameterExecuteRequest parameterExecuteRequest) throws IOException, StreemException, ResourceNotFoundException;

  @PatchMapping("/{parameterExecutionId}/reject")
  @ResponseBody
  Response<ParameterDto> rejectParameter(@PathVariable Long parameterExecutionId, @RequestBody ParameterStateChangeRequest parameterStateChangeRequest) throws ResourceNotFoundException, StreemException;

  @PatchMapping("/{parameterExecutionId}/approve")
  @ResponseBody
  Response<ParameterDto> approveParameter(@PathVariable Long parameterExecutionId, @RequestBody ParameterStateChangeRequest parameterStateChangeRequest) throws StreemException, IOException, ResourceNotFoundException, ParameterExecutionException;

  @PatchMapping("/execute/temporary")
  Response<RuleHideShowDto> executeTemporary(@RequestBody ParameterTemporaryExecuteRequest parameterTemporaryExecuteRequest) throws IOException;

  @PostMapping("/variations")
  Response<BasicDto> addVariations(@RequestBody CreateVariationRequest createVariationRequest) throws IOException, ResourceNotFoundException, StreemException;

  @GetMapping("/variations/{jobId}/allowed")
  Response<Page<ParameterDto>> getAllParametersAvailableForVariations(@PathVariable Long jobId, @RequestParam(value = "filters", required = false) String filters, @RequestParam(value = "parameterName", required = false, defaultValue = "") String parameterName, Pageable pageable) throws ResourceNotFoundException;

  @GetMapping("/variations/{jobId}")
  Response<Page<VariationDto>> getAllVariationsOfJob(@PathVariable Long jobId, @RequestParam(value = "parameterName", required = false) String parameterName, Pageable pageable) throws ResourceNotFoundException;

  @DeleteMapping("/variations")
  Response<BasicDto> deleteVariation(@RequestBody DeleteVariationRequest deleteVariationRequest) throws ResourceNotFoundException, StreemException;

  @GetMapping("/variations/parameter/{parameterExecutionId}")
  Response<List<VariationDto>> getAllVariationsOfParameterExecution(@PathVariable Long parameterExecutionId);

  @GetMapping("/jobs/{jobId}")
  Response<List<ParameterDto>>getParameterExecutionByParameterIdAndJobId(@PathVariable Long jobId,@RequestParam(name = "filters", defaultValue = "") String filters) throws ResourceNotFoundException;

  @PatchMapping("/jobs/{jobId}/partial-latest")
  Response<List<ParameterPartialDto>> getParameterPartialData(@PathVariable Long jobId , @RequestBody ParameterPartialRequest parameterPartialRequest) throws JsonProcessingException;


  @GetMapping("/parameter/{parameterExecutionId}/filter")
  Response<Page<PartialEntityObject>> getAllFilteredEntityObjects(@PathVariable Long parameterExecutionId, @RequestParam(value = "query", required = false) String query, @RequestParam(value = "shortCode", required = false) String shortCode, Pageable pageable) throws IOException, ResourceNotFoundException;

  @PatchMapping("/jobs/{jobId}/partial-master")
  Response<List<ParameterPartialDto>> getParameterPartialDataForMaster(@PathVariable Long jobId , @RequestBody ParameterPartialRequest parameterPartialRequest) throws JsonProcessingException;



}

