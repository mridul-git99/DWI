package com.leucine.streem.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.leucine.streem.exception.StreemException;

import java.io.IOException;

public interface IParameterValidationService {
  void validateIfParameterCanBeArchived(Long parameterId, Long checklistId, boolean isUnmap) throws StreemException;

  void validateIfParameterCanBeUpdated(Long parameterId, Long checklistId, JsonNode parameterUpdateRequest) throws StreemException, IOException;
}
