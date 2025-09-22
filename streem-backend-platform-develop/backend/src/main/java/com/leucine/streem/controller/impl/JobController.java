package com.leucine.streem.controller.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.leucine.streem.config.RateLimited;
import com.leucine.streem.controller.IJobController;
import com.leucine.streem.dto.*;
import com.leucine.streem.dto.projection.JobAssigneeView;
import com.leucine.streem.dto.projection.PendingForApprovalStatusView;
import com.leucine.streem.dto.projection.TaskExecutionAssigneeDetailsView;
import com.leucine.streem.dto.request.CreateJobRequest;
import com.leucine.streem.dto.request.JobCweDetailRequest;
import com.leucine.streem.dto.request.TaskExecutionAssignmentRequest;
import com.leucine.streem.dto.request.UpdateJobRequest;
import com.leucine.streem.dto.response.Response;
import com.leucine.streem.exception.MultiStatusException;
import com.leucine.streem.exception.ParameterExecutionException;
import com.leucine.streem.exception.ResourceNotFoundException;
import com.leucine.streem.exception.StreemException;
import com.leucine.streem.service.IJobCweService;
import com.leucine.streem.service.IJobService;
import com.leucine.streem.util.DateTimeUtils;
import java.util.UUID;
import org.apache.commons.compress.utils.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

@Component
public class JobController implements IJobController {
  private final IJobService jobService;
  private final IJobCweService jobCweService;

  @Autowired
  public JobController(IJobService jobService, IJobCweService jobCweService) {
    this.jobService = jobService;
    this.jobCweService = jobCweService;
  }

  @Override
  public Response<Page<JobPartialDto>> getAll(String objectId, String filters, Pageable pageable) {
    return Response.builder().data(jobService.getAllJobs(objectId, filters, pageable)).build();
  }

  @Override
  public Response<Page<JobPartialDto>> getAllCount(String objectId, String filters, Pageable pageable) {
    return Response.builder().data(jobService.getAllJobsCount(objectId, filters, pageable)).build();
  }

  @Override
  public Response<Page<JobPartialDto>> getJobsAssignedToMe(String objectId, Boolean showPendingOnly, String filters, Pageable pageable) throws StreemException {
    return Response.builder().data(jobService.getJobsAssignedToMe(objectId, filters, pageable, showPendingOnly)).build();
  }

  @Override
  public Response<Page<JobAutoSuggestDto>> getJobsAssignedToMeAutoSuggest(String objectId, Boolean showPendingOnly, String filters, Pageable pageable) throws StreemException {
    return Response.builder().data(jobService.getJobsAssignedToMeAutoSuggest(objectId, filters, pageable, showPendingOnly)).build();
  }

  @Override
  public Response<CountDto> getJobsAssignedToMeCount(String objectId, String filters, Boolean showPendingOnly) throws StreemException {
    return Response.builder().data(jobService.getJobsAssignedToMeCount(objectId, filters, showPendingOnly)).build();
  }


  @Override
  public Response<JobDto> getJob(Long jobId) throws ResourceNotFoundException, JsonProcessingException {
    return Response.builder().data(jobService.getJobById(jobId)).build();
  }

  @Override
  public Response<JobCweDto> getJobCweDetail(Long jobId) throws ResourceNotFoundException {
    return Response.builder().data(jobCweService.getJobCweDetail(jobId)).build();
  }

  @Override
  public Response<JobDto> createJob(CreateJobRequest createJobRequest, Boolean validateUserRole) throws StreemException, IOException, ResourceNotFoundException, MultiStatusException {
    return Response.builder().data(jobService.createJob(createJobRequest, validateUserRole)).build();
  }

  @Override
  public Response<BasicDto> updateJob(Long jobId, UpdateJobRequest updateJobRequest) throws StreemException, ResourceNotFoundException {
    return Response.builder().data(jobService.updateJob(jobId, updateJobRequest)).build();
  }

  @Override
  public Response<JobInfoDto> startJob(Long jobId) throws ResourceNotFoundException, StreemException, IOException, ParameterExecutionException {
    return Response.builder().data(jobService.startJob(jobId)).build();
  }

  @Override
  public Response<JobInfoDto> completeJob(Long jobId) throws ResourceNotFoundException, StreemException {
    return Response.builder().data(jobService.completeJob(jobId)).build();
  }

  @Override
  public Response<JobInfoDto> completeJobWithException(Long jobId, JobCweDetailRequest jobCweDetailRequest) throws ResourceNotFoundException, StreemException {
    return Response.builder().data(jobService.completeJobWithException(jobId, jobCweDetailRequest)).build();
  }

  @Override
  public Response<JobStateDto> getJobState(Long jobId) throws ResourceNotFoundException {
    return Response.builder().data(jobService.getJobState(jobId)).build();
  }

  @Override
  public Response<TaskDetailsDto> pollTaskData(Long jobId, Long taskExecutionId) throws ResourceNotFoundException, JsonProcessingException {
    return Response.builder().data(jobService.getTaskData(jobId, taskExecutionId)).build();
  }

  @Override
  public Response<StageDetailsDto> pollStageData(Long jobId, Long stageId) throws ResourceNotFoundException, JsonProcessingException {
    return Response.builder().data(jobService.getStageData(jobId, stageId)).build();
  }

  @Override
  @RateLimited
  public Response<BasicDto> bulkAssign(Long jobId, TaskExecutionAssignmentRequest taskExecutionAssignmentRequest, boolean notify) throws ResourceNotFoundException, StreemException, MultiStatusException {
    return Response.builder().data(jobService.bulkAssign(jobId, taskExecutionAssignmentRequest, notify)).build();
  }

  @Override
  public Response<List<TaskExecutionAssigneeDetailsView>> getAssignees(Long jobId) {
    return Response.builder().data(jobService.getAssignees(jobId)).build();
  }

  @Override
  public Response<JobReportDto> getJobReport(Long jobId) throws ResourceNotFoundException, JsonProcessingException {
    return Response.builder().data(jobService.getJobReport(jobId)).build();
  }

  @Override
  public Response<JobReportDto> printJobReport(Long jobId) throws ResourceNotFoundException, JsonProcessingException {
    return Response.builder().data(jobService.printJobReport(jobId)).build();
  }

  @Override
  public ResponseEntity<byte[]> printJob(Long jobId) throws ResourceNotFoundException, IOException {
//    return Response.builder().data(jobService.printJob(jobId)).build();
    byte[] pdf = jobService.printJob(jobId);

    String filename = UUID.randomUUID().toString() + ".pdf";

    return ResponseEntity.ok()
      .contentType(MediaType.APPLICATION_PDF)
      .header(HttpHeaders.CONTENT_DISPOSITION,
        "attachment; filename=\"" + filename + "\"")
      .body(pdf);
  }

  @Override
  public Response<List<CorrectionPrintDto>> printJobCorrections(Long jobId) throws ResourceNotFoundException, JsonProcessingException {
    return Response.builder().data(jobService.printJobCorrections(jobId)).build();
  }

  @Override
  public Response<Page<JobPartialDto>> getAllByResource(String objectId, String filters, Pageable pageable) {
    return Response.builder().data(jobService.getAllByResource(objectId, filters, pageable)).build();
  }

  @Override
  public Response<JobInformationDto> getJobInformation(Long jobId) throws ResourceNotFoundException {
    return Response.builder().data(jobService.getJobInformation(jobId)).build();
  }

  @Override
  public Response<Page<PendingForApprovalStatusDto>> getPendingForApprovalParameters(String processName, String parameterName, String objectId, String jobId, String userId,String useCaseId, boolean showAllException,Long requestedBy, Pageable pageable) {
    return Response.builder().data(jobService.getPendingForApprovalParameters(processName, parameterName, objectId, jobId, userId,useCaseId, showAllException,requestedBy, pageable)).build();
  }

  @Override
  public Response<JobAssigneeDto> isCurrentUserAssignedToJob(Long jobId) throws ResourceNotFoundException {
    return Response.builder().data(jobService.isCurrentUserAssignedToJob(jobId)).build();
  }

  @Override
  public Response<List<JobAssigneeView>> getAllJobAssignees(Long jobId, String query,List<String> roles, Pageable pageable) {
    return Response.builder().data(jobService.getAllJobAssignees(jobId, query, roles, pageable)).build();
  }

  @Override
  public ResponseEntity<byte[]> printJobActivity(Long jobId,String filters)
      throws ResourceNotFoundException, IOException {
    byte[] pdf = jobService.printJobActivity(jobId, filters);
    
    String filename = UUID.randomUUID().toString() + ".pdf";
    
    return ResponseEntity.ok()
      .contentType(MediaType.APPLICATION_PDF)
      .header(HttpHeaders.CONTENT_DISPOSITION,
        "attachment; filename=\"" + filename + "\"")
      .body(pdf);
  }
  public Response<JobLiteDto> getJobLite(Long jobId) throws ResourceNotFoundException, JsonProcessingException {
    return Response.builder().data(jobService.getJobLiteById(jobId)).build();
  }

  @Override
  public void downloadJobsExcel(String filters, String objectId, HttpServletResponse httpServletResponse) 
      throws IOException, ResourceNotFoundException {
    
    // Use proper Excel MIME type instead of generic octet-stream
    httpServletResponse.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    httpServletResponse.setHeader("Content-Disposition", String.format("attachment; filename=%s.xlsx", DateTimeUtils.now()));
    
    // Add proper cache control headers
    httpServletResponse.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
    httpServletResponse.setHeader("Pragma", "no-cache");
    httpServletResponse.setHeader("Expires", "0");
    
    httpServletResponse.setStatus(HttpServletResponse.SC_OK);
    ByteArrayInputStream jobExcelStream = jobService.generateJobsExcel(filters, objectId);
    IOUtils.copy(jobExcelStream, httpServletResponse.getOutputStream());
  }
}
