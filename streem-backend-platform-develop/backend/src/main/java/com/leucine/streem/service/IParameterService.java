package com.leucine.streem.service;

import com.leucine.streem.dto.BasicDto;
import com.leucine.streem.dto.ParameterDto;
import com.leucine.streem.dto.ParameterInfoDto;
import com.leucine.streem.dto.request.*;
import com.leucine.streem.exception.ResourceNotFoundException;
import com.leucine.streem.exception.StreemException;
import com.leucine.streem.model.Checklist;
import com.leucine.streem.model.Parameter;
import com.leucine.streem.model.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.io.IOException;

public interface IParameterService {

  ParameterDto addParameterToTask(Long checklistId, Long stageId, Long taskId, ParameterCreateRequest parameterCreateRequest) throws ResourceNotFoundException, StreemException, IOException;

  Parameter prepareTaskParameter(Checklist checklist, Task task, ParameterCreateRequest parameterCreateRequest) throws StreemException, IOException, ResourceNotFoundException;

  ParameterDto createParameter(Long checklistId, ParameterCreateRequest parameterCreateRequest) throws ResourceNotFoundException, StreemException, IOException;

  Parameter prepareParameter(Checklist checklist, ParameterCreateRequest parameterCreateRequest) throws StreemException, IOException, ResourceNotFoundException;

  BasicDto reorderParameters(Long checklistId, Long stageId, Long taskId, ParameterReorderRequest parameterReorderRequest) throws ResourceNotFoundException, StreemException;

  ParameterDto getParameter(Long parameterId) throws ResourceNotFoundException, StreemException;

  ParameterInfoDto unmapParameter(Long parameterId) throws ResourceNotFoundException, StreemException;

  ParameterDto mapParameterToTask(Long checklistId, Long taskId, MapParameterToTaskRequest mapParameterToTaskRequest) throws ResourceNotFoundException, StreemException;

  ParameterDto updateParameter(Long parameterId, ParameterUpdateRequest parameterUpdateRequest) throws ResourceNotFoundException, StreemException, IOException;

  ParameterInfoDto archiveParameter(Long parameterId) throws ResourceNotFoundException, StreemException;

  Page<ParameterInfoDto> getAllParameters(Long checklistId, String filters, Pageable pageable);

  BasicDto updateParameterVisibility(ParameterVisibilityRequest parameterVisibilityRequest) throws StreemException;
}
