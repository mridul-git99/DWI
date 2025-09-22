package com.leucine.streem.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.leucine.streem.dto.BasicDto;
import com.leucine.streem.dto.ChecklistDto;
import com.leucine.streem.exception.ResourceNotFoundException;
import com.leucine.streem.exception.StreemException;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

public interface IChecklistRevisionService {
  ChecklistDto createChecklistRevision(Long checklistId) throws ResourceNotFoundException, StreemException, IOException;

  BasicDto validateIfCurrentUserCanReviseChecklist(Long checklistId) throws ResourceNotFoundException, StreemException;

}
