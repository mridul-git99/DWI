package com.leucine.streem.service;


import com.leucine.streem.dto.CopyChecklistElementRequest;
import com.leucine.streem.dto.IChecklistElementDto;
import com.leucine.streem.exception.ResourceNotFoundException;
import com.leucine.streem.exception.StreemException;

public interface IElementCopyService {
  IChecklistElementDto copyChecklistElements(Long checklistId, CopyChecklistElementRequest copyChecklistRequest) throws ResourceNotFoundException, StreemException;
}
