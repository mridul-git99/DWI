package com.leucine.streem.service;

import com.leucine.streem.dto.ActionDto;
import com.leucine.streem.dto.BasicDto;
import com.leucine.streem.dto.request.CreateActionRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IActionService {
  BasicDto createAction(CreateActionRequest createActionRequest);

  Page<ActionDto> getActions(Long checklistId, Pageable pageable);

  ActionDto updateAction(Long actionId, ActionDto actionDto);

  BasicDto archiveAction(Long actionId);
}
