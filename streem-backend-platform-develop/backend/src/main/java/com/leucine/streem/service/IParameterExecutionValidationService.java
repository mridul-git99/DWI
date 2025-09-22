package com.leucine.streem.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.leucine.streem.dto.DateParameterValidationDto;
import com.leucine.streem.exception.ParameterExecutionException;
import com.leucine.streem.exception.ResourceNotFoundException;
import com.leucine.streem.exception.StreemException;
import com.leucine.streem.model.*;
import com.leucine.streem.model.helper.parameter.LeastCount;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

public interface IParameterExecutionValidationService {
  void validateNumberParameterValidations(Long jobId, Long parameterValueId, Long parameterId, JsonNode validations, String value) throws IOException, StreemException, ResourceNotFoundException, ParameterExecutionException;
  void validateParameterValueChoice(String objectId, String objectTypeExternalId, JsonNode validations, String parameterId, Long jobId, boolean isScheduled) throws StreemException, ResourceNotFoundException, IOException, ParameterExecutionException;

  void validateIfCorrectionCanBeInitiated(ParameterValue parameterValue) throws StreemException;

  boolean isParameterExecutedPartially(Parameter parameter, String data, boolean isExecutedForCorrection, List<ParameterValueMediaMapping> parameterValueMediaMappings,
                                       List<TempParameterValueMediaMapping> tempParameterValueMediaMapping) throws IOException;

  boolean isParameterValueIncomplete(Parameter parameter, String data, boolean isExecutedForCorrection, List<ParameterValueMediaMapping> parameterValueMediaMappings,
                                     List<TempParameterValueMediaMapping> tempParameterValueMediaMapping, List<Media> correctionMediaList, String correctionValue, JsonNode correctionChoice) throws IOException;

  void validateLeastCount(Long parameterId, LeastCount leastCount, BigDecimal inputValue, Long jobId) throws StreemException;

  void validateDateAndDateTimeParameterValidations(Long jobId,JsonNode validations, String inputValue, boolean isDateTimeParameter,String facilityTimeZone, Long parameterId) throws StreemException, IOException, ParameterExecutionException;
}
