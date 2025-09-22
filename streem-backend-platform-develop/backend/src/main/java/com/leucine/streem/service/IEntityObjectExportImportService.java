package com.leucine.streem.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.leucine.streem.dto.ImportSummary;
import com.leucine.streem.exception.ResourceNotFoundException;
import com.leucine.streem.exception.StreemException;
import org.springframework.web.multipart.MultipartFile;

public interface IEntityObjectExportImportService {
  byte[] exportToExcel(String objectTypeId) throws ResourceNotFoundException, StreemException;
  
  ImportSummary importFromExcel(String objectTypeId, MultipartFile file)
      throws ResourceNotFoundException, StreemException, JsonProcessingException;
}
