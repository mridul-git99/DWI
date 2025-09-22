package com.leucine.streem.service;

import com.leucine.streem.dto.NotificationParameterValueDto;
import com.leucine.streem.dto.NotificationParameterExceptionDto;
import com.leucine.streem.dto.request.ParameterCorrectionInitiatorRequest;
import com.leucine.streem.dto.request.UserGroupUpdateRequest;
import com.leucine.streem.exception.StreemException;
import com.leucine.streem.model.*;
import com.leucine.streem.model.helper.PrincipalUser;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.Set;

public interface INotificationService {

  void notifyAssignedUsers(Set<Long> assignIds, Long jobId, Long organisationId);

  void notifyUnassignedUsers(Set<Long> unassignIds, Long organisationId);

  void notifyAllShouldBeParameterReviewersForApproval(Long jobId, Long checklistId,Long taskExecutionId, Long organisationId) throws IOException, StreemException;

  void notifyChecklistCollaborators(Set<Long> userIds, String template, String subject, Long checklistId, Long organisationId);

  void notifyAuthors(Set<Long> ids, Long checklistId, Long organisationId);

  
  @Transactional(rollbackFor = Exception.class)
  void notifyPeerVerification(Set<Long> ids, Long jobId, Long organisationId);

  
  @Transactional
  void notifyJobStartDelayed(Set<Long> ids, Long jobId, Long organisationId);

  
  @Transactional
  void notifyJobOverDue(Set<Long> ids, Long jobId, Long organisationId);

  @Transactional
  void correctionRequested(List<User> correctorUsersList, ParameterCorrectionInitiatorRequest parameterCorrectionInitiatorRequest, NotificationParameterValueDto notificationParameterValueDto, Long organisationId);

  @Transactional
  void reviewCorrection(List<Reviewer> reviewerList, List<Corrector> correctorList, NotificationParameterValueDto notificationParameterValueDto, Long organisationId);

  @Transactional
  void approveCorrection(Correction currentCorrection, NotificationParameterValueDto notificationParameterValueDto, List<Reviewer> reviewerList, Long organisationId);

  @Transactional
  void rejectCorrection(Correction currentCorrection, NotificationParameterValueDto notificationParameterValueDto, List<Reviewer> reviewerList, Long organisationId);

  @Transactional
  void addedToUserGroup(List<UserGroupMember> userGroupMembers, Long organisationId);

  @Transactional
  void archiveGroup(UserGroup userGroup, String reason, PrincipalUser principalUser);

  @Transactional
  void groupMembershipUpdate(UserGroup userGroup, UserGroupUpdateRequest userGroupUpdateRequest, List<User> users, PrincipalUser principleUser);

  @Transactional
  @Async
  void notifyIfAllPrerequisiteTasksCompleted(Long preRequisiteTaskId, Long jobId, Long organisationId);

  @Async
  @Transactional
  void parameterExceptionRequested(NotificationParameterExceptionDto notificationParameterExceptionDto, List<ParameterExceptionReviewer> reviewerUsersList);

  @Async
  @Transactional
  void approveParameterException(ParameterException currentException, NotificationParameterExceptionDto notificationParameterExceptionDto, List<ParameterExceptionReviewer> reviewerUsersList);

  @Async
  @Transactional
  void rejectParameterException(ParameterException currentException, NotificationParameterExceptionDto notificationParameterExceptionDto, List<ParameterExceptionReviewer> reviewerUsersList);

}
