package com.leucine.streem.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.leucine.streem.dto.*;
import com.leucine.streem.dto.projection.JobAssigneeView;
import com.leucine.streem.dto.projection.PendingForApprovalStatusView;
import com.leucine.streem.dto.projection.TaskExecutionAssigneeDetailsView;
import com.leucine.streem.dto.request.CreateJobRequest;
import com.leucine.streem.dto.request.JobCweDetailRequest;
import com.leucine.streem.dto.request.TaskExecutionAssignmentRequest;
import com.leucine.streem.dto.request.UpdateJobRequest;
import com.leucine.streem.exception.MultiStatusException;
import com.leucine.streem.exception.ParameterExecutionException;
import com.leucine.streem.exception.ResourceNotFoundException;
import com.leucine.streem.exception.StreemException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

public interface IJobService {
  JobDto getJobById(Long jobId) throws ResourceNotFoundException, JsonProcessingException;

  Page<JobPartialDto> getAllJobs(String objectId, String filters, Pageable pageable);

  Page<JobPartialDto> getAllJobsCount(String objectId, String filters, Pageable pageable);

  Page<JobPartialDto> getJobsAssignedToMe(String objectId, String filters, Pageable pageable, Boolean showPendingOnly) throws StreemException;

  Page<JobAutoSuggestDto> getJobsAssignedToMeAutoSuggest(String objectId, String filters, Pageable pageable, Boolean showPendingOnly);

  CountDto getJobsAssignedToMeCount(String objectId, String filters, Boolean showPendingOnly) throws StreemException;

  JobDto createJob(CreateJobRequest createJobRequest, Boolean validateUserRole) throws StreemException, IOException, ResourceNotFoundException, MultiStatusException;

  BasicDto updateJob(Long jobId, UpdateJobRequest updateJobRequest) throws ResourceNotFoundException, StreemException;

  JobInfoDto startJob(Long jobId) throws ResourceNotFoundException, StreemException, IOException, ParameterExecutionException;

  JobInfoDto completeJob(Long jobId) throws ResourceNotFoundException, StreemException;

  JobInfoDto completeJobWithException(Long jobId, JobCweDetailRequest jobCweDetailRequest) throws ResourceNotFoundException, StreemException;

  BasicDto bulkAssign(Long jobId, TaskExecutionAssignmentRequest taskExecutionAssignmentRequest, boolean notify) throws ResourceNotFoundException, StreemException, MultiStatusException;

  List<TaskExecutionAssigneeDetailsView> getAssignees(Long jobId);

  JobStateDto getJobState(Long jobId) throws ResourceNotFoundException;

  Page<JobPartialDto> getAllByResource(String objectId, String filters, Pageable pageable);

  StageDetailsDto getStageData(Long jobId, Long stageId) throws ResourceNotFoundException, JsonProcessingException;

  JobReportDto getJobReport(Long jobId) throws ResourceNotFoundException, JsonProcessingException;

  JobInformationDto getJobInformation(Long jobId) throws ResourceNotFoundException;

  JobReportDto printJobReport(Long jobId) throws ResourceNotFoundException, JsonProcessingException;

  byte[] printJob(Long jobId) throws ResourceNotFoundException, IOException;

  List<CorrectionPrintDto> printJobCorrections(Long jobId) throws ResourceNotFoundException, JsonProcessingException;

  /**
   * Generates a PDF report of job activities with optional filtering.
   *
   * @param jobId   The ID of the job
   * @param filters JSON string containing filter criteria
   * @return Byte array containing the generated PDF
   * @throws ResourceNotFoundException If the job is not found
   * @throws IOException              If there's an error generating the PDF
   */
  byte[] printJobActivity(Long jobId, String filters) throws ResourceNotFoundException, IOException;

  boolean isJobExistsBySchedulerIdAndDateGreaterThanOrEqualToExpectedStartDate(Long schedulerId, Long epochDateTime);

  Page<PendingForApprovalStatusDto> getPendingForApprovalParameters(String processName, String parameterName,
                                                                     String objectId, String jobId, String userId, String useCaseId,
                                                                     boolean showAllException, Long requestedBy, Pageable pageable);

  List<JobAssigneeView> getAllJobAssignees(Long jobId, String query, List<String> roles, Pageable pageable);
  void validateIfUserIsAssignedToExecuteJob(Long jobId, Long userId) throws StreemException;

  JobAssigneeDto isCurrentUserAssignedToJob(Long jobId);

  TaskDetailsDto getTaskData(Long jobId, Long taskExecutionId) throws ResourceNotFoundException;

  JobLiteDto getJobLiteById(Long jobId) throws ResourceNotFoundException, JsonProcessingException;


  ByteArrayInputStream generateJobsExcel(String filters, String objectId) 
      throws IOException, ResourceNotFoundException;

}
