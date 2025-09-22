package com.leucine.streem.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.leucine.streem.dto.BasicDto;
import com.leucine.streem.dto.request.ImportChecklistRequest;
import com.leucine.streem.exception.ResourceNotFoundException;
import com.leucine.streem.exception.StreemException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface IimportExportChecklistService {
  BasicDto importChecklists(Long useCaseId, MultipartFile file) throws StreemException, ResourceNotFoundException, IOException;
  List<ImportChecklistRequest> exportChecklists(List<Long> checklistIds) throws ResourceNotFoundException, IOException;
  List<MultipartFile> getAllMediaMultiPart(List<Long> checklistIds) throws IOException;
  void populateMissingMediasDetails(List<Long> ids) throws JsonProcessingException, ResourceNotFoundException;
}

