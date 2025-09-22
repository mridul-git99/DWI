package com.leucine.streem.service;

import com.leucine.streem.collections.partial.PartialEntityObject;
import com.leucine.streem.constant.Type;
import com.leucine.streem.dto.BulkParameterExecuteRequest;
import com.leucine.streem.dto.ParameterDto;
import com.leucine.streem.dto.TempParameterDto;
import com.leucine.streem.dto.request.ParameterExecuteRequest;
import com.leucine.streem.exception.ParameterExecutionException;
import com.leucine.streem.exception.ResourceNotFoundException;
import com.leucine.streem.exception.StreemException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.io.IOException;
import java.util.List;

public interface IParameterExecutionHandler {
  ParameterDto executeParameter(Long jobId, Long parameterExecutionId, ParameterExecuteRequest parameterExecuteRequest, Type.JobLogTriggerType jobLogTriggerType, boolean ignoreRootExecution, boolean isCreateJobRequest, boolean isScheduled) throws StreemException, IOException, ResourceNotFoundException, ParameterExecutionException;

  TempParameterDto executeParameterForError(ParameterExecuteRequest parameterExecuteRequest) throws IOException, StreemException, ResourceNotFoundException, ParameterExecutionException;


  List<ParameterDto> executeParameters(List<BulkParameterExecuteRequest> bulkParameterExecuteRequests) throws StreemException, IOException, ResourceNotFoundException;

  Page<PartialEntityObject> getAllFilteredEntityObjects(Long parameterExecutionId, String query, String shortCode, Pageable pageable) throws IOException, ResourceNotFoundException;



}
