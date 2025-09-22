package com.leucine.streem.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import com.leucine.streem.model.ParameterValue;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.SortDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/v1/jobs")
public interface IJobController {
  @GetMapping
  @ResponseBody
  Response<Page<JobPartialDto>> getAll(@RequestParam(name = "objectId", defaultValue = "") String objectId, @RequestParam(name = "filters", defaultValue = "") String filters, Pageable pageable);

  @GetMapping("/count")
  @ResponseBody
  Response<Page<JobPartialDto>> getAllCount(@RequestParam(name = "objectId", defaultValue = "") String objectId, @RequestParam(name = "filters", defaultValue = "") String filters, Pageable pageable);

  @GetMapping("/assignee/me")
  @ResponseBody
  Response<Page<JobPartialDto>> getJobsAssignedToMe(@RequestParam(name = "objectId", defaultValue = "") String objectId, @RequestParam(name = "showPendingOnly", defaultValue = "false") Boolean showPendingOnly, @RequestParam(name = "filters", defaultValue = "") String filters, Pageable pageable) throws StreemException;

  @GetMapping("/assignee/me/autosuggest")
  @ResponseBody
  Response<Page<JobAutoSuggestDto>> getJobsAssignedToMeAutoSuggest(@RequestParam(name = "objectId", defaultValue = "") String objectId, @RequestParam(name = "showPendingOnly", defaultValue = "false") Boolean showPendingOnly, @RequestParam(name = "filters", defaultValue = "") String filters, Pageable pageable) throws StreemException;

  @GetMapping("/assignee/me/count")
  @ResponseBody
  Response<CountDto> getJobsAssignedToMeCount(@RequestParam(name = "objectId", defaultValue = "") String objectId, @RequestParam(name = "filters", defaultValue = "") String filters, @RequestParam(name = "showPendingOnly", defaultValue = "false") Boolean showPendingOnly) throws StreemException;

  @GetMapping("/{jobId}")
  @ResponseBody
  Response<JobDto> getJob(@PathVariable Long jobId) throws ResourceNotFoundException, JsonProcessingException;

  @GetMapping("/{jobId}/cwe-details")
  @ResponseBody
  Response<JobCweDto> getJobCweDetail(@PathVariable Long jobId) throws ResourceNotFoundException;

  @PostMapping
  @ResponseBody
  Response<JobDto> createJob(@RequestBody CreateJobRequest createJobRequest, @RequestParam(name = "validateUserRole") Boolean validateUserRole) throws StreemException, IOException, ResourceNotFoundException, MultiStatusException;

  @PatchMapping("/{jobId}")
  @ResponseBody
  Response<BasicDto> updateJob(@PathVariable Long jobId, @RequestBody UpdateJobRequest updateJobRequest) throws StreemException, ResourceNotFoundException;

  @PatchMapping("/{jobId}/start")
  Response<JobInfoDto> startJob(@PathVariable("jobId") Long jobId) throws ResourceNotFoundException, StreemException, IOException, ParameterExecutionException;

  @PatchMapping("/{jobId}/complete")
  Response<JobInfoDto> completeJob(@PathVariable("jobId") Long jobId) throws ResourceNotFoundException, StreemException;

  @PatchMapping("/{jobId}/complete-with-exception")
  Response<JobInfoDto> completeJobWithException(@PathVariable("jobId") Long jobId, @Valid @RequestBody JobCweDetailRequest jobCweDetailRequest) throws ResourceNotFoundException, StreemException;

  @GetMapping("/{jobId}/state")
  Response<JobStateDto> getJobState(@PathVariable("jobId") Long jobId) throws ResourceNotFoundException;

  @GetMapping("/{jobId}/stages/state")
  Response<StageDetailsDto> pollStageData(@PathVariable("jobId") Long jobId, @RequestParam(name = "stageId") Long stageId) throws ResourceNotFoundException, JsonProcessingException;

  @GetMapping("/{jobId}/task-executions/state")
  Response<TaskDetailsDto> pollTaskData(@PathVariable("jobId") Long jobId, @RequestParam(name = "taskExecutionId") Long taskExecutionId) throws ResourceNotFoundException, JsonProcessingException;

  @PatchMapping("/{jobId}/assignments")
  Response<BasicDto> bulkAssign(@PathVariable(name = "jobId") Long jobId, @RequestBody TaskExecutionAssignmentRequest taskExecutionAssignmentRequest,
                                @RequestParam(required = false, defaultValue = "false") boolean notify) throws ResourceNotFoundException, StreemException, MultiStatusException;

  @GetMapping("/{jobId}/assignments")
  Response<List<TaskExecutionAssigneeDetailsView>> getAssignees(@PathVariable(name = "jobId") Long jobId);

  @GetMapping("/{jobId}/reports")
  Response<JobReportDto> getJobReport(@PathVariable("jobId") Long jobId) throws ResourceNotFoundException, JsonProcessingException;

  @GetMapping("/{jobId}/reports/print")
  Response<JobReportDto> printJobReport(@PathVariable("jobId") Long jobId) throws ResourceNotFoundException, JsonProcessingException;

  @GetMapping("/{jobId}/print")
  ResponseEntity<byte[]> printJob(@PathVariable("jobId") Long jobId) throws ResourceNotFoundException, IOException;

  @GetMapping("/{jobId}/correction-print")
  Response<List<CorrectionPrintDto>> printJobCorrections(@PathVariable("jobId") Long jobId) throws ResourceNotFoundException, JsonProcessingException;

  @GetMapping("/by/resource/{objectId}")
  Response<Page<JobPartialDto>> getAllByResource(@PathVariable("objectId") String objectId, @RequestParam("filters") String filters, Pageable pageable);

  @GetMapping("/{jobId}/info")
  Response<JobInformationDto> getJobInformation(@PathVariable("jobId") Long jobId) throws ResourceNotFoundException;

  // TODO better we could have /v1/jobs/parameter-executions?status=PENDING_FOR_APPROVAL
  // make this change in repeat task
  @GetMapping("/approvals")
  Response<Page<PendingForApprovalStatusDto>> getPendingForApprovalParameters(@RequestParam(value = "processName", required = false, defaultValue = "") String processName,
                                                                              @RequestParam(value = "parameterName", required = false, defaultValue = "") String parameterName,
                                                                              @RequestParam(value = "objectId", required = false) String objectId,
                                                                              @RequestParam(value = "jobId", required = false) String jobId,
                                                                              @RequestParam(value = "userId", required = false) String userId,
                                                                              @RequestParam(value = "useCaseId", required = true) String useCaseId,
                                                                              @RequestParam(value = "showAllException", required = false, defaultValue = "false") boolean showAllException,
                                                                              @RequestParam(required = false) Long requestedBy,
                                                                              @SortDefault(sort = ParameterValue.DEFAULT_SORT, direction = Sort.Direction.DESC) Pageable pageable);

  @GetMapping("/{jobId}/activity/print")
  ResponseEntity<byte[]> printJobActivity(
      @PathVariable("jobId") Long jobId,
      @RequestParam(name = "filters", defaultValue = "") String filters) 
      throws ResourceNotFoundException, IOException;

  @GetMapping("/{jobId}/assignees")
  Response<List<JobAssigneeView>> getAllJobAssignees(@PathVariable("jobId") Long jobId, @RequestParam(value = "query", required = false) String query, @RequestParam(value = "roles", required = false) List<String> roles, Pageable pageable);

  @GetMapping("/{jobId}/assigned")
  Response<JobAssigneeDto> isCurrentUserAssignedToJob(@PathVariable("jobId") Long jobId) throws ResourceNotFoundException;

  @GetMapping("/{jobId}/job-lite")
  @ResponseBody
  Response<JobLiteDto> getJobLite(@PathVariable Long jobId) throws ResourceNotFoundException, JsonProcessingException;

  @GetMapping("/download")
  void downloadJobsExcel(
      @RequestParam(name = "filters", required = false) String filters,
      @RequestParam(name = "objectId", required = false) String objectId,
      HttpServletResponse response
  ) throws IOException, ResourceNotFoundException;
}
