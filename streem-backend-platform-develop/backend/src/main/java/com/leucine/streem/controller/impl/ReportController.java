package com.leucine.streem.controller.impl;

import com.leucine.streem.controller.IReportController;
import com.leucine.streem.dto.ReportDto;
import com.leucine.streem.dto.ReportURIDto;
import com.leucine.streem.dto.response.Response;
import com.leucine.streem.exception.ResourceNotFoundException;
import com.leucine.streem.service.IReportService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class ReportController implements IReportController {

  private final IReportService reportService;

  @Override
  public Response<Page<ReportDto>> getAllReports(String filters, Pageable pageable) {
    return Response.builder().data(reportService.getAllReports(filters, pageable)).build();
  }

  @Override
  public Response<ReportURIDto> getReportURI(String id, String useCaseId) throws ResourceNotFoundException {
    return Response.builder().data(reportService.getReportURI(id, useCaseId)).build();
  }


  @Override
  public Response<Object> generateQSConsoleUrl() {
    return Response.builder().data(reportService.generateQSConsoleUrl()).build();
  }

  @Override
  public Response<Object> reportsEditorOrViewer() {
    return Response.builder().data(reportService.reportsEditorOrViewer()).build();
  }

  @Override
  public Response<Object> generateQSDashboardUrl(String dashboardId) {
    return Response.builder().data( reportService.generateQSDashboardUrl(dashboardId)).build();
  }

  @Override
  public Response<Object> generateQSDashboardIds() {
    return Response.builder().data( reportService.generateQSDashboardIds()).build();
  }

}
