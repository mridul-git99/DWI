package com.leucine.streem.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.leucine.streem.collections.EntityObject;
import com.leucine.streem.collections.partial.PartialEntityObject;
import com.leucine.streem.constant.Type;
import com.leucine.streem.dto.*;
import com.leucine.streem.dto.request.*;
import com.leucine.streem.exception.ResourceNotFoundException;
import com.leucine.streem.exception.StreemException;
import com.leucine.streem.model.*;
import com.leucine.streem.model.helper.PrincipalUser;
import com.mongodb.lang.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface IJobAuditService {
  Page<JobAuditDto> getAuditsByJobId(Long jobId, String filters, Pageable pageable) throws StreemException;

  void createJob(String jobId, PrincipalUser principalUser);

  void startJob(JobInfoDto jobDto, PrincipalUser principalUser);

  void completeJob(JobInfoDto jobDto, PrincipalUser principalUser);

  void completeJobWithException(Long jobId, JobCweDetailRequest jobCweDetailRequest, PrincipalUser principalUser);

  void printJob(JobPrintDto jobPrintDto, PrincipalUser principalUser);

  void deleteJobAnnotation(Long jobId, String reason, PrincipalUser principalUser);

  void printJobReport(JobReportDto jobReportDto, PrincipalUser principalUser);

  void startTask(Long jobId, Long taskId, TaskExecutionRequest taskExecutionRequest, PrincipalUser principalUser);

  void completeTask(Long jobId, Long taskId, TaskCompletionRequest taskCompletionRequest, Integer orderTree, PrincipalUser principalUser);

  void completeTaskWithException(Long jobId, Long taskId, TaskCompletionRequest taskCompletionRequest, Integer orderTree, PrincipalUser principalUser);

  void skipTask(Long jobId, Long taskId, TaskExecutionRequest taskExecutionRequest, PrincipalUser principalUser);

  void enableTaskForCorrection(Long jobId, Long taskExecutionId, TaskExecutionRequest taskExecutionRequest, PrincipalUser principalUser);

  void cancelCorrection(Long jobId, Long taskExecutionId, TaskExecutionRequest taskExecutionRequest, PrincipalUser principalUser);

  void completeCorrection(Long jobId, Long taskExecutionId, TaskExecutionRequest taskExecutionRequest, PrincipalUser principalUser);

  void bulkAssignUsersToJob(Long jobId, boolean areUsersAssigned, boolean areUsersUnassigned, boolean areUserGroupsAssigned, boolean areUserGroupsUnassigned, PrincipalUser principalUser);

  <T extends BaseParameterValueDto> void executedParameter(Long jobId, Long parameterValueId, Long parameterId, @Nullable T oldValue, List<MediaDto> mediaDtoList, Type.Parameter parameterType,
                                                           boolean isExecutedForCorrection, String reason,String correctorRemark, PrincipalUser principalUser, ParameterExecuteRequest parameterExecuteRequest, Boolean isCJF) throws IOException, ResourceNotFoundException, StreemException;

  void signedOffTasks(TaskSignOffRequest taskSignOffRequest, PrincipalUser principalUser);

  void approveParameter(Long jobId, ParameterDto parameterDto, Long parameterId, PrincipalUser principalUser);

  void rejectParameter(Long jobId, ParameterDto parameterDto, Long parameterId, PrincipalUser principalUser);

  void initiateSelfVerification(Long jobId, VerificationBase verificationBase, boolean isVerifiedForCorrection, PrincipalUser principalUser);

  void completeSelfVerification(Long jobId, VerificationBase verificationBase, boolean isVerifiedForCorrection, PrincipalUser principalUser);

  void recallVerification(Long jobId, VerificationBase verificationBase, boolean isVerifiedForCorrection, PrincipalUser principalUser);

  void sendForPeerVerification(Long jobId, VerificationBase verificationBase, boolean isVerifiedForCorrection, PrincipalUser principalUser);

  void acceptPeerVerification(Long jobId, VerificationBase verificationBase, boolean isVerifiedForCorrection, PrincipalUser principalUser);

  void rejectPeerVerification(Long jobId, VerificationBase verificationBase, boolean isVerifiedForCorrection, PrincipalUser principalUser);

  void acceptSameSessionVerification(Long jobId, VerificationBase verificationBase, boolean isVerifiedForCorrection, PrincipalUser verifierUser, PrincipalUser initiatorUser);

  void rejectSameSessionVerification(Long jobId, VerificationBase verificationBase, boolean isVerifiedForCorrection, PrincipalUser verifierUser, PrincipalUser initiatorUser);

  void pauseTask(Long taskId, TaskPauseOrResumeRequest taskPauseOrResumeRequest, PrincipalUser principalUser);

  void resumeTask(Long taskId, TaskPauseOrResumeRequest taskPauseOrResumeRequest, PrincipalUser principalUser);

  void repeatTask(TaskRepeatRequest taskRepeatRequest, PrincipalUser principalUser, Long previousTaskExecutionId, Long latestTaskExecutionId);

  void recurrenceTask(Long jobId, Long previousTaskRecurrenceId, Long latestTaskRecurrenceId, Long taskId, boolean continueRecurrence, PrincipalUser principalUser);

  void printJobActivity(JobPrintDto jobPrintDto, PrincipalUser principalUser);

  void saveJobAnnotation(JobAnnotationDto jobAnnotationDto, PrincipalUser principalUser);

  void createVariation(CreateVariationRequest createVariationRequest, PrincipalUser principalUser);

  void deleteVariation(DeleteVariationRequest deleteVariationRequest, PrincipalUser principalUser);

  void increaseOrDecreasePropertyAutomation(Long taskId, Long jobId, Type.AutomationActionType automationActionType, String valueToUpdate, Parameter referencedParameter, Parameter parameter, AutomationActionForResourceParameterDto resourceParameterAction, PrincipalUser principalUser, String displayName);

  void skipIncreaseOrDecreaseAutomation(Long taskId, Long jobId, Type.AutomationActionType automationActionType, Parameter referencedParameter, Parameter parameter, AutomationActionForResourceParameterDto resourceParameterAction, PrincipalUser principalUser, String displayName);

  void setPropertyAutomation(Long dateTimeValue, Long taskId, Long jobId, AutomationActionDateTimeDto automationActionDateTimeDto, AutomationActionSetPropertyDto automationSetProperty, Parameter parameter, PrincipalUser principalUser, String displayName);

  void skipSetPropertyAutomation(Long taskId, Long jobId, AutomationActionDateTimeDto automationActionDateTimeDto, AutomationActionSetPropertyDto automationSetProperty, Parameter parameter, PrincipalUser principalUser, String displayName);

  void setRelationAutomation(Long taskId, Long jobId, AutomationActionMappedRelationDto automationActionMappedRelationDto, EntityObject entityObject, String resourceParameterChoices, Parameter referencedParameter, Parameter parameter, PrincipalUser principalUser, String displayName);

  void skipSetRelationAutomation(Long taskId, Long jobId, AutomationActionMappedRelationDto automationActionMappedRelationDto, Map<String, List<PartialEntityObject>> partialEntityObjectMap, Parameter referencedParameter, Parameter parameter, PrincipalUser principalUser, String displayName);

  void createObjectAutomation(Long taskId, Long jobId, EntityObject entityObject, AutomationObjectCreationActionDto automationObjectCreationActionDto, PrincipalUser principalUser, String displayName);

  void archiveObjectAutomation(Long taskId, Long jobId, ResourceParameterChoiceDto resourceParameterChoiceDto, Parameter parameter, AutomationActionArchiveObjectDto automationActionArchiveObjectDto, PrincipalUser principalUser, String displayName) throws JsonProcessingException;

  void skipArchiveObjectAutomation(Long taskId, Long jobId, Parameter parameter, AutomationActionArchiveObjectDto automationActionArchiveObjectDto, PrincipalUser principalUser, String displayName) throws JsonProcessingException;

  void removeTask(Long taskId, Long jobId, Integer orderTree, PrincipalUser principalUser);

  void scheduleTask(Long jobId, Long taskId, PrincipalUser principalUser, boolean isScheduledAtTaskComplete,boolean isScheduledAtStartJob, Set<String> taskScheduledIds);

  void initiateCorrection(Long jobId, Long taskExecutionId, Long parameterId, PrincipalUser principalUser, Correction savedCorrection, String initiatorReason);
  void approveCorrection(Long jobId, Long taskExecutionId, Long parameterId, PrincipalUser principalUser, Correction savedCorrection, String initiatorReason);
  void rejectCorrection(Long jobId, Long taskExecutionId, Long parameterId, PrincipalUser principalUser, Correction savedCorrection, String initiatorReason);

  void handleSoloTaskLock(Task task, Long jobId, PrincipalUser principalUser, PrincipalUser systemUser);
  void enableCorrection(Long jobId, Long taskExecutionId, PrincipalUser principalUser);
  void disableCorrection(Long jobId, Long taskExecutionId, PrincipalUser principalUser);
  void initiateParameterException(Long jobId, Long taskExecutionId, Long parameterId, PrincipalUser principalUser, ParameterException parameterException, String initiatorReason, String exceptionDeviatedValue) throws JsonProcessingException;
  void initiateCjfParameterException(Long jobId, Long parameterId, PrincipalUser principalUser, ParameterException parameterException, String initiatorReason, String exceptionDeviatedValue) throws JsonProcessingException;
  void approveParameterException(Long jobId, Long taskExecutionId, Long parameterId, PrincipalUser principalUser, ParameterException parameterException, String reviewerReason);
  void rejectParameterException(Long jobId, Long taskExecutionId, Long parameterId, PrincipalUser principalUser, ParameterException parameterException, String reviewerReason);
  void approveCjfParameterException(Long jobId, Long parameterId, PrincipalUser principalUser, ParameterException currentException, String reviewerReason);
  void rejectCjfParameterException(Long jobId, Long parameterId, PrincipalUser principalUser, ParameterException currentException, String reviewerReason);
  void autoAcceptParameterException(Long jobId, Long taskExecutionId, Long parameterId, PrincipalUser principalUser, ParameterException parameterException, String reason) throws ResourceNotFoundException, JsonProcessingException;
  void autoAcceptCjfParameterException(Long jobId, Long parameterId, PrincipalUser principalUser, ParameterException parameterException, String reason) throws ResourceNotFoundException, JsonProcessingException;
  void checkVerification(Long parameterExecutionId, Long checkedAt, PrincipalUser principalUser);

  void initiateBulkSelfVerification(Long jobId,Long checkedAt, Task task, PrincipalUser principalUser);
  void logBulkSelfParameterExamination(Long parameterExecutionId, Long checkedAt, PrincipalUser principalUser);
  void approveBulkSelfVerification(Long jobId, Task task,Long checkedAt, PrincipalUser principalUser);
  void acceptBulkPeerVerification(Long jobId, Task task, PrincipalUser principalUser);
  void initiateBulkPeerVerification(Long jobId, Task task, PrincipalUser principalUser);


  void userUnassignmentLogsForJobs(Set<Long> jobIds, String reason, PrincipalUser systemUser,PrincipalUser archivedUser, PrincipalUser archivedByUser);

  void recallCorrection(Long jobId, Long taskExecutionId, Long parameterId, PrincipalUser principalUser, Correction savedCorrection, String initiatorReason);

  void startJobEarly(JobInfoDto jobDto, PrincipalUser principalUser, String expectedStartStr);
}
