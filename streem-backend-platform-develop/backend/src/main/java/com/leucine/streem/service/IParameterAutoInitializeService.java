package com.leucine.streem.service;

import com.leucine.streem.dto.projection.AutoInitializeParameterView;
import com.leucine.streem.dto.request.ParameterExecuteRequest;
import com.leucine.streem.exception.ResourceNotFoundException;
import com.leucine.streem.exception.StreemException;

import java.io.IOException;
import java.util.List;

public interface IParameterAutoInitializeService {
  ParameterExecuteRequest getParameterExecuteRequestForParameterToAutoInitialize(Long jobId, Long autoInitializedParameterId, boolean isExecutedForCorrection, Long referencedParameterId) throws StreemException, IOException, ResourceNotFoundException;
  List<ParameterExecuteRequest> getAllParameterExecuteRequestForParameterToAutoInitialize(Long jobId, List<AutoInitializeParameterView> autoInitializedParameterView, boolean isExecutedForCorrection) throws StreemException, IOException, ResourceNotFoundException;
}
