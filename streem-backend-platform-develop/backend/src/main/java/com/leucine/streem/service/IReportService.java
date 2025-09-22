package com.leucine.streem.service;

import com.leucine.streem.dto.BasicDto;
import com.leucine.streem.dto.ReportDto;
import com.leucine.streem.dto.ReportURIDto;
import com.leucine.streem.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IReportService {
  ReportURIDto getReportURI(String id, String useCaseId) throws ResourceNotFoundException;

  Page<ReportDto> getAllReports(String filters, Pageable pageable);


  BasicDto generateQSConsoleUrl();

  BasicDto generateQSDashboardUrl(String dashboardId);

  BasicDto generateQSDashboardIds();

  BasicDto reportsEditorOrViewer();
}
