package com.leucine.streem.controller;

import com.leucine.streem.dto.ReportDto;
import com.leucine.streem.dto.ReportURIDto;
import com.leucine.streem.dto.response.Response;
import com.leucine.streem.exception.ResourceNotFoundException;
import com.leucine.streem.exception.StreemException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/reports")
public interface IReportController {
  @GetMapping("/{id}")
  Response<ReportURIDto> getReportURI(@PathVariable String id, @RequestParam("useCaseId") String useCaseId) throws ResourceNotFoundException;

  @GetMapping
  Response<Page<ReportDto>> getAllReports(@RequestParam(value = "filters", required = false) String filters, Pageable pageable);

  @GetMapping("/qs/url/console")
  Response<Object> generateQSConsoleUrl() throws ResourceNotFoundException, StreemException;

  @GetMapping("/role")
  Response<Object> reportsEditorOrViewer() throws ResourceNotFoundException, StreemException;


  @GetMapping("/qs/url/dashboard/{dashboardId}")
  Response<Object> generateQSDashboardUrl(@PathVariable String dashboardId) throws ResourceNotFoundException, StreemException;


  @GetMapping("/qs/dashboards")
  Response<Object> generateQSDashboardIds() throws ResourceNotFoundException, StreemException;
}
