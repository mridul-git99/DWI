package com.leucine.streem.service;

import com.leucine.streem.dto.RuleHideShowDto;
import com.leucine.streem.dto.request.ParameterExecuteRequest;
import com.leucine.streem.model.Parameter;
import com.leucine.streem.model.ParameterValue;

import java.io.IOException;
import java.util.Map;

public interface IRulesExecutionService {
  RuleHideShowDto updateRules(Long jobId, Parameter parameter, ParameterValue parameterValue) throws IOException;
  RuleHideShowDto tempExecuteRules(Map<Long, ParameterExecuteRequest> parameterExecuteRequestMap, Long checklistId) throws IOException;
}
