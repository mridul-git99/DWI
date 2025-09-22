package com.leucine.streem.controller;

import com.leucine.streem.collections.JobLog;
import com.leucine.streem.constant.Type;
import com.leucine.streem.dto.response.Response;
import com.leucine.streem.exception.ResourceNotFoundException;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/v1/job-logs")
public interface IJobLogController {
  @GetMapping
  Response<List<JobLog>> getAllJobLogs(@RequestParam(name = "filters", defaultValue = "") String filters, @RequestParam(value = "customViewId", required = false) String customViewId, Pageable pageable) throws ResourceNotFoundException;

  @GetMapping("/checklists/{checklistId}/jobs/{jobId}")
  Response<JobLog> getJobLog(@PathVariable Long checklistId, @PathVariable Long jobId);

  @GetMapping("/download")
  void downloadJobLogs(@RequestParam(name = "customViewId", required = false) String customViewId,
                       @RequestParam(name = "filters", required = false) String filters,
                       @RequestParam(name = "type", required = false) Type.ReportType reportType,
                       @RequestParam(name = "jobLogType", required = false) Type.JobLogType jobLogType,
                       @RequestParam(name = "pdfMetaData", required = false) String pdfMetaData,
                       HttpServletResponse response) throws IOException, ResourceNotFoundException;

}
