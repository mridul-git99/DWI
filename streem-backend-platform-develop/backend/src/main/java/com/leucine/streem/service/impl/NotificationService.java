package com.leucine.streem.service.impl;

import com.leucine.streem.config.AppUrl;
import com.leucine.streem.constant.Email;
import com.leucine.streem.constant.Misc;
import com.leucine.streem.dto.NotificationParameterValueDto;
import com.leucine.streem.dto.NotificationParameterExceptionDto;
import com.leucine.streem.dto.PartialUserDto;
import com.leucine.streem.dto.TrainedUsersDto;
import com.leucine.streem.dto.projection.TaskExecutionAssigneeBasicView;
import com.leucine.streem.dto.projection.TaskExecutionView;
import com.leucine.streem.dto.request.ParameterCorrectionInitiatorRequest;
import com.leucine.streem.dto.request.UserGroupUpdateRequest;
import com.leucine.streem.email.dto.EmailRequest;
import com.leucine.streem.exception.StreemException;
import com.leucine.streem.model.*;
import com.leucine.streem.model.helper.PrincipalUser;
import com.leucine.streem.repository.*;
import com.leucine.streem.service.IChecklistTrainedUserService;
import com.leucine.streem.service.IEmailService;
import com.leucine.streem.service.INotificationService;
import com.leucine.streem.service.IUserService;
import com.leucine.streem.util.JsonUtils;
import com.leucine.streem.util.Utility;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService implements INotificationService {
  private final AppUrl appUrl;
  private final IEmailService emailService;
  private final IOrganisationRepository organisationRepository;
  private final IUserRepository userRepository;
  private final IUserService userService;
  private final IChecklistTrainedUserService checklistTrainedUserService;
  private final IUserGroupMemberRepository userGroupMemberRepository;
  private final ITaskExecutionRepository taskExecutionRepository;
  private final ITaskExecutionAssigneeRepository taskExecutionAssigneeRepository;

  @Override
  
  @Transactional(rollbackFor = Exception.class)
  public void notifyAssignedUsers(Set<Long> assignIds, Long jobId, Long organisationId) {
    if (!Utility.isEmpty(assignIds)) {
      Organisation organisation = organisationRepository.getReferenceById(organisationId);
      String fqdn = organisation.getFqdn();
      if (!"/".equals(fqdn.substring(fqdn.length() - 1))) {
        fqdn = fqdn + "/";
      }
      List<User> users = userRepository.findAllByIdInAndArchivedFalse(assignIds);

      Set<String> toEmailIds = users.stream().map(User::getEmail).collect(Collectors.toSet());
      Map<String, String> attributes = new HashMap<>();
      attributes.put(Email.ATTRIBUTE_LOGIN_URL, fqdn + appUrl.getLoginPath());
      attributes.put(Email.ATTRIBUTE_JOB, fqdn + appUrl.getJobPath(jobId));

      EmailRequest emailRequest = EmailRequest.builder()
        .to(toEmailIds)
        .templateName(Email.TEMPLATE_USER_ASSIGNED_TO_JOB)
        .subject(Email.SUBJECT_USER_ASSIGNED_TO_JOB)
        .attributes(attributes)
        .build();
      emailService.sendEmail(emailRequest);
    }
  }

  @Override
  
  @Transactional
  public void notifyUnassignedUsers(Set<Long> unassignIds, Long organisationId) {
    if (!Utility.isEmpty(unassignIds)) {
      Organisation organisation = organisationRepository.getOne(organisationId);
      String fqdn = organisation.getFqdn();
      if (!"/".equals(fqdn.substring(fqdn.length() - 1))) {
        fqdn = fqdn + "/";
      }
      List<User> users = userRepository.findAllByIdInAndArchivedFalse(unassignIds);

      Set<String> toEmailIds = users.stream().map(User::getEmail).collect(Collectors.toSet());
      Map<String, String> attributes = new HashMap<>();
      attributes.put(Email.ATTRIBUTE_LOGIN_URL, fqdn + appUrl.getLoginPath());

      EmailRequest emailRequest = EmailRequest.builder()
        .to(toEmailIds)
        .templateName(Email.TEMPLATE_USER_UNASSIGNED_FROM_JOB)
        .subject(Email.SUBJECT_USER_UNASSIGNED_FROM_JOB)
        .attributes(attributes).build();
      emailService.sendEmail(emailRequest);
    }
  }

  @Override
  
  @Transactional
  public void notifyAllShouldBeParameterReviewersForApproval(Long jobId, Long checklistId, Long taskExecutionId, Long organisationId) throws IOException, StreemException {
    Organisation organisation = organisationRepository.getOne(organisationId);
    String fqdn = organisation.getFqdn();
    if (!"/".equals(fqdn.substring(fqdn.length() - 1))) {
      fqdn = fqdn + "/";
    }
    Object users = userService.getAllByRoles(Misc.SHOULD_BE_PARAMETER_REVIEWER).getData();
    List<PartialUserDto> reviewers = JsonUtils.jsonToCollectionType(users, List.class, PartialUserDto.class);
    Set<String> defaultUserIds = checklistTrainedUserService.getTrainedUsers(checklistId, true, false, null, Pageable.ofSize(100000)).stream()
      .map(TrainedUsersDto::getUserId).collect(Collectors.toSet());
    Set<String> defaultUserGroupIdStrings = checklistTrainedUserService.
      getTrainedUsers(checklistId, false, true, null, Pageable.ofSize(100000)).stream()
      .map(TrainedUsersDto::getUserGroupId).collect(Collectors.toSet());

    Set<Long> defaultUserGroupIds = defaultUserGroupIdStrings.stream()
      .map(Long::parseLong) // Convert each string to a long
      .collect(Collectors.toSet());


    Set<Long> userGroupMemberIdsAsLong = userGroupMemberRepository.findAllUsersByUserGroupIds(defaultUserGroupIds);
    Set<String> userGroupMemberIds = userGroupMemberIdsAsLong.stream()
      .map(String::valueOf)
      .collect(Collectors.toSet());
    defaultUserIds.addAll(userGroupMemberIds);
    List<PartialUserDto> filteredReviewers = reviewers.stream()
      .filter(reviewer -> defaultUserIds.contains(reviewer.getId()))
      .toList();

    Set<String> emailIds = filteredReviewers.stream().map(PartialUserDto::getEmail).collect(Collectors.toSet());
    Map<String, String> attributes = new HashMap<>();
    attributes.put(Email.ATTRIBUTE_LOGIN_URL, fqdn + appUrl.getLoginPath());
    attributes.put(Email.ATTRIBUTE_JOB, fqdn + appUrl.getJobPath(jobId) + "?taskId=" + taskExecutionId);

    EmailRequest emailRequest = EmailRequest.builder().to(emailIds)
      .templateName(Email.TEMPLATE_PARAMETER_APPROVAL_REQUEST).subject(Email.SUBJECT_PARAMETER_APPROVAL_REQUEST).attributes(attributes).build();
    emailService.sendEmail(emailRequest);
  }

  @Override
  
  @Transactional
  public void notifyChecklistCollaborators(Set<Long> userIds, String template, String subject, Long checklistId, Long organisationId) {
    if (!Utility.isEmpty(userIds)) {
      Organisation organisation = organisationRepository.getOne(organisationId);
      String fqdn = organisation.getFqdn();
      if (!"/".equals(fqdn.substring(fqdn.length() - 1))) {
        fqdn = fqdn + "/";
      }
      List<User> users = userRepository.findAllByIdInAndArchivedFalse(userIds);
      Set<String> emailIds = users.stream().map(User::getEmail).collect(Collectors.toSet());
      Map<String, String> attributes = new HashMap<>();
      attributes.put(Email.ATTRIBUTE_LOGIN_URL, fqdn + appUrl.getLoginPath());
      attributes.put(Email.ATTRIBUTE_CHECKLIST, fqdn + appUrl.getChecklistPath(checklistId));
      EmailRequest emailRequest = EmailRequest.builder().to(emailIds)
        .templateName(template).subject(subject).attributes(attributes).build();
      emailService.sendEmail(emailRequest);
    }
  }

  @Override
  
  @Transactional
  public void notifyAuthors(Set<Long> ids, Long checklistId, Long organisationId) {
    if (!Utility.isEmpty(ids)) {
      PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
      Organisation organisation = organisationRepository.getOne(principalUser.getOrganisationId());
      String fqdn = organisation.getFqdn();
      if (!"/".equals(fqdn.substring(fqdn.length() - 1))) {
        fqdn = fqdn + "/";
      }
      List<User> users = userRepository.findAllByIdInAndArchivedFalse(ids);

      Set<String> emailIds = users.stream().map(User::getEmail).collect(Collectors.toSet());
      Map<String, String> attributes = new HashMap<>();
      attributes.put(Email.ATTRIBUTE_LOGIN_URL, fqdn + appUrl.getLoginPath());
      attributes.put(Email.ATTRIBUTE_CHECKLIST, fqdn + appUrl.getChecklistPath(checklistId));

      EmailRequest emailRequest = EmailRequest.builder().to(emailIds)
        .templateName(Email.TEMPLATE_AUTHOR_CHECKLIST_CONFIGURATION).subject(Email.SUBJECT_AUTHOR_CHECKLIST_CONFIGURATION).attributes(attributes).build();
      emailService.sendEmail(emailRequest);
    }
  }

  @Override
  
  @Transactional
  public void notifyPeerVerification(Set<Long> ids, Long jobId, Long organisationId) {
    if (!Utility.isEmpty(ids)) {
      Organisation organisation = organisationRepository.getOne(organisationId);
      String fqdn = organisation.getFqdn();
      if (!"/".equals(fqdn.substring(fqdn.length() - 1))) {
        fqdn = fqdn + "/";
      }
      List<User> users = userRepository.findAllByIdInAndArchivedFalse(ids);

      Set<String> emailIds = users.stream().map(User::getEmail).collect(Collectors.toSet());
      Map<String, String> attributes = new HashMap<>();
      attributes.put(Email.ATTRIBUTE_LOGIN_URL, fqdn + appUrl.getLoginPath());
      attributes.put(Email.ATTRIBUTE_JOB, fqdn + appUrl.getJobPath(jobId));

      EmailRequest emailRequest = EmailRequest.builder().to(emailIds)
        //TODO: Replace correct Templates and Subjects once the doc is populated
        .templateName(Email.TEMPLATE_PEER_VERIFICATION).subject(Email.SUBJECT_PEER_VERIFICATION).attributes(attributes).build();
      emailService.sendEmail(emailRequest);
    }
  }

  @Override
  
  @Transactional
  public void notifyJobStartDelayed(Set<Long> ids, Long jobId, Long organisationId) {
    if (!Utility.isEmpty(ids)) {
      Organisation organisation = organisationRepository.getOne(organisationId);
      String fqdn = organisation.getFqdn();
      if (!"/".equals(fqdn.substring(fqdn.length() - 1))) {
        fqdn = fqdn + "/";
      }
      List<User> users = userRepository.findAllByIdInAndArchivedFalse(ids);

      Set<String> emailIds = users.stream().map(User::getEmail).collect(Collectors.toSet());
      Map<String, String> attributes = new HashMap<>();
      attributes.put(Email.ATTRIBUTE_LOGIN_URL, fqdn + appUrl.getLoginPath());
      attributes.put(Email.ATTRIBUTE_JOB, fqdn + appUrl.getJobPath(jobId));

      EmailRequest emailRequest = EmailRequest.builder().to(emailIds)
        .templateName(Email.TEMPLATE_JOB_START_DUE).subject(Email.SUBJECT_TEMPLATE_JOB_START_DUE).attributes(attributes).build();
      emailService.sendEmail(emailRequest);
    }
  }

  @Override
  
  @Transactional
  public void notifyJobOverDue(Set<Long> ids, Long jobId, Long organisationId) {
    if (!Utility.isEmpty(ids)) {
      Organisation organisation = organisationRepository.getOne(organisationId);
      String fqdn = organisation.getFqdn();
      if (!"/".equals(fqdn.substring(fqdn.length() - 1))) {
        fqdn = fqdn + "/";
      }
      List<User> users = userRepository.findAllByIdInAndArchivedFalse(ids);

      Set<String> emailIds = users.stream().map(User::getEmail).collect(Collectors.toSet());
      Map<String, String> attributes = new HashMap<>();
      attributes.put(Email.ATTRIBUTE_LOGIN_URL, fqdn + appUrl.getLoginPath());
      attributes.put(Email.ATTRIBUTE_JOB, fqdn + appUrl.getJobPath(jobId));

      EmailRequest emailRequest = EmailRequest.builder().to(emailIds)
        .templateName(Email.TEMPLATE_JOB_OVER_DUE).subject(Email.SUBJECT_TEMPLATE_JOB_OVER_DUE).attributes(attributes).build();
      emailService.sendEmail(emailRequest);
    }
  }

  @Override
  
  @Transactional
  public void correctionRequested(List<User> correctorUsersList, ParameterCorrectionInitiatorRequest parameterCorrectionInitiatorRequest, NotificationParameterValueDto notificationParameterValueDto, Long organisationId) {
    Organisation organisation = organisationRepository.getOne(organisationId);
    String fqdn = organisation.getFqdn();
    if (!"/".equals(fqdn.substring(fqdn.length() - 1))) {
      fqdn = fqdn + "/";
    }
    Set<String> emailIds = correctorUsersList.stream().filter(user -> !user.isArchived()).map(User::getEmail).collect(Collectors.toSet());
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    String initiatorDescription = parameterCorrectionInitiatorRequest.getInitiatorReason();
    String taskLink = fqdn + "/inbox/" + notificationParameterValueDto.getJobId() + "?taskId=" + notificationParameterValueDto.getTaskExecutionId();
    String jobLink = fqdn + "/inbox/" + notificationParameterValueDto.getJobId();
    Map<String, String> attributes = new HashMap<>();
    attributes.put(Email.ATTRIBUTE_LOGIN_URL, fqdn + appUrl.getLoginPath());
    attributes.put(Email.ATTRIBUTE_INITIATOR_NAME, principalUser.getFirstName());
    attributes.put(Email.ATTRIBUTE_INITIATOR_DESCRIPTION, initiatorDescription);
    attributes.put(Email.ATTRIBUTE_JOB_ID, notificationParameterValueDto.getJobCode());
    attributes.put(Email.ATTRIBUTE_JOB_LINK, jobLink);
    attributes.put(Email.ATTRIBUTE_PARAMETER, notificationParameterValueDto.getParameterLabel());
    attributes.put(Email.ATTRIBUTE_TASK_NAME, notificationParameterValueDto.getTaskName());
    attributes.put(Email.ATTRIBUTE_PROCESS_NAME, notificationParameterValueDto.getCheckListName());
    attributes.put(Email.ATTRIBUTE_TASK_LINK, taskLink);

    EmailRequest emailRequest = EmailRequest.builder().to(emailIds)
      .templateName(Email.TEMPLATE_CORRECTION_REQUESTED).subject(Email.SUBJECT_CORRECTION_REQUESTED).attributes(attributes).build();
    emailService.sendEmail(emailRequest);

  }

  @Override
  
  @Transactional
  public void reviewCorrection(List<Reviewer> reviewerList, List<Corrector> correctorList, NotificationParameterValueDto notificationParameterValueDto, Long organisationId) {
    Organisation organisation = organisationRepository.getOne(organisationId);
    String fqdn = organisation.getFqdn();
    if (!"/".equals(fqdn.substring(fqdn.length() - 1))) {
      fqdn = fqdn + "/";
    }
    String taskLink = fqdn + "/inbox/" + notificationParameterValueDto.getJobId() + "?taskId=" + notificationParameterValueDto.getTaskExecutionId();
    String jobLink = fqdn + "/inbox/" + notificationParameterValueDto.getJobId();
    String correctorName = "";
    String correctorDescription = "";
    for (Corrector corrector1 : correctorList) {
      if (corrector1.isActionPerformed()) {
        correctorName = corrector1.getUser().getFirstName();
        correctorDescription = corrector1.getCorrection().getCorrectorsReason();
      }
    }


    for (Reviewer reviewer : reviewerList) {

      Map<String, String> attributes = new HashMap<>();
      attributes.put(Email.ATTRIBUTE_LOGIN_URL, fqdn + appUrl.getLoginPath());
      attributes.put(Email.ATTRIBUTE_USER_NAME, reviewer.getUser().getFirstName());
      attributes.put(Email.ATTRIBUTE_CORRECTOR_NAME, correctorName);
      attributes.put(Email.ATTRIBUTE_CORRECTOR_DESCRIPTION, correctorDescription);
      attributes.put(Email.ATTRIBUTE_JOB_ID, notificationParameterValueDto.getJobCode());
      attributes.put(Email.ATTRIBUTE_JOB_LINK, jobLink);
      attributes.put(Email.ATTRIBUTE_PARAMETER, notificationParameterValueDto.getParameterLabel());
      attributes.put(Email.ATTRIBUTE_TASK_NAME, notificationParameterValueDto.getTaskName());
      attributes.put(Email.ATTRIBUTE_PROCESS_NAME, notificationParameterValueDto.getCheckListName());
      attributes.put(Email.ATTRIBUTE_TASK_LINK, taskLink);
      Set<String> emailIds = Collections.singleton(reviewer.getUser().getEmail());

      EmailRequest emailRequest = EmailRequest.builder().to(emailIds)
        .templateName(Email.TEMPLATE_APPROVAL_REQUESTED).subject(Email.SUBJECT_APPROVAL_REQUESTED).attributes(attributes).build();
      emailService.sendEmail(emailRequest);
    }


  }

  @Override
  
  @Transactional
  public void approveCorrection(Correction currentCorrection, NotificationParameterValueDto notificationParameterValueDto, List<Reviewer> reviewerList, Long organisationId) {
    Organisation organisation = organisationRepository.getOne(organisationId);
    String fqdn = organisation.getFqdn();
    if (!"/".equals(fqdn.substring(fqdn.length() - 1))) {
      fqdn = fqdn + "/";
    }
    String taskLink = fqdn + "/inbox/" + notificationParameterValueDto.getJobId() + "?taskId=" + notificationParameterValueDto.getTaskExecutionId();
    String jobLink = fqdn + "/inbox/" + notificationParameterValueDto.getJobId();
    String reviewerName = "";
    String reviewerDescription = "";
    for (Reviewer reviewer : reviewerList) {
      if (reviewer.isActionPerformed()) {
        reviewerName = reviewer.getUser().getFirstName();
        reviewerDescription = reviewer.getCorrection().getReviewersReason();
      }
    }
    Set<String> emailIds = Collections.singleton(currentCorrection.getCreatedBy().getEmail());
    Map<String, String> attributes = new HashMap<>();
    attributes.put(Email.ATTRIBUTE_LOGIN_URL, fqdn + appUrl.getLoginPath());
    attributes.put(Email.ATTRIBUTE_USER_NAME, currentCorrection.getCreatedBy().getFirstName());
    attributes.put(Email.ATTRIBUTE_REVIEWER_NAME, reviewerName);
    attributes.put(Email.ATTRIBUTE_REVIEWER_DESCRIPTION, reviewerDescription);
    attributes.put(Email.ATTRIBUTE_JOB_ID, notificationParameterValueDto.getJobCode());
    attributes.put(Email.ATTRIBUTE_JOB_LINK, jobLink);
    attributes.put(Email.ATTRIBUTE_PARAMETER, notificationParameterValueDto.getParameterLabel());
    attributes.put(Email.ATTRIBUTE_TASK_NAME, notificationParameterValueDto.getTaskName());
    attributes.put(Email.ATTRIBUTE_PROCESS_NAME, notificationParameterValueDto.getCheckListName());
    attributes.put(Email.ATTRIBUTE_TASK_LINK, taskLink);
    EmailRequest emailRequest = EmailRequest.builder().to(emailIds)
      .templateName(Email.TEMPLATE_CORRECTION_APPROVED).subject(Email.SUBJECT_CORRECTION_APPROVED).attributes(attributes).build();
    emailService.sendEmail(emailRequest);


  }


  @Override
  
  @Transactional
  public void rejectCorrection(Correction currentCorrection, NotificationParameterValueDto notificationParameterValueDto, List<Reviewer> reviewerList, Long organisationId) {
    Organisation organisation = organisationRepository.getOne(organisationId);
    String fqdn = organisation.getFqdn();
    if (!"/".equals(fqdn.substring(fqdn.length() - 1))) {
      fqdn = fqdn + "/";
    }
    String taskLink = fqdn + "/inbox/" + notificationParameterValueDto.getJobId() + "?taskId=" + notificationParameterValueDto.getTaskExecutionId();
    String jobLink = fqdn + "/inbox/" + notificationParameterValueDto.getJobId();
    String reviewerName = "";
    String reviewerDescription = "";
    for (Reviewer reviewer : reviewerList) {
      if (reviewer.isActionPerformed()) {
        reviewerName = reviewer.getUser().getFirstName();
        reviewerDescription = reviewer.getCorrection().getReviewersReason();
      }
    }
    Set<String> emailIds = Collections.singleton(currentCorrection.getCreatedBy().getEmail());
    Map<String, String> attributes = new HashMap<>();
    attributes.put(Email.ATTRIBUTE_LOGIN_URL, fqdn + appUrl.getLoginPath());
    attributes.put(Email.ATTRIBUTE_USER_NAME, currentCorrection.getCreatedBy().getFirstName());
    attributes.put(Email.ATTRIBUTE_REVIEWER_NAME, reviewerName);
    attributes.put(Email.ATTRIBUTE_REVIEWER_DESCRIPTION, reviewerDescription);
    attributes.put(Email.ATTRIBUTE_JOB_ID, notificationParameterValueDto.getJobCode());
    attributes.put(Email.ATTRIBUTE_JOB_LINK, jobLink);
    attributes.put(Email.ATTRIBUTE_PARAMETER, notificationParameterValueDto.getParameterLabel());
    attributes.put(Email.ATTRIBUTE_TASK_NAME, notificationParameterValueDto.getTaskName());
    attributes.put(Email.ATTRIBUTE_PROCESS_NAME, notificationParameterValueDto.getCheckListName());
    attributes.put(Email.ATTRIBUTE_TASK_LINK, taskLink);
    EmailRequest emailRequest = EmailRequest.builder().to(emailIds)
      .templateName(Email.TEMPLATE_CORRECTION_REJECTED).subject(Email.SUBJECT_CORRECTION_REJECTED).attributes(attributes).build();
    emailService.sendEmail(emailRequest);


  }

  @Override
  
  @Transactional
  public void addedToUserGroup(List<UserGroupMember> userGroupMembers, Long organisationId) {
    Organisation organisation = organisationRepository.getOne(organisationId);
    String fqdn = organisation.getFqdn();
    if (!"/".equals(fqdn.substring(fqdn.length() - 1))) {
      fqdn = fqdn + "/";
    }

    for (UserGroupMember userGroupMember : userGroupMembers) {
      String GroupLink = fqdn + "users/group/edit/" + userGroupMember.getUserGroup().getId() + "?readOnly=true";
      Map<String, String> attributes = new HashMap<>();
      attributes.put(Email.ATTRIBUTE_LOGIN_URL, fqdn + appUrl.getLoginPath());
      attributes.put(Email.ATTRIBUTE_USER_NAME, userGroupMember.getUser().getFirstName());
      attributes.put(Email.ATTRIBUTE_GROUP_LINK, GroupLink);
      attributes.put(Email.ATTRIBUTE_USER_GROUP_NAME, userGroupMember.getUserGroup().getName());
      Set<String> emailIds = Collections.singleton(userGroupMember.getUser().getEmail());

      EmailRequest emailRequest = EmailRequest.builder().to(emailIds)
        .templateName(Email.TEMPLATE_ADDED_TO_USER_GROUP).subject(Email.SUBJECT_ADDED_TO_USER_GROUP).attributes(attributes).build();
      emailService.sendEmail(emailRequest);
    }
  }

  @Override
  
  @Transactional
  public void archiveGroup(UserGroup userGroup, String reason, PrincipalUser principalUser) {
    Organisation organisation = organisationRepository.getOne(principalUser.getOrganisationId());
    String fqdn = organisation.getFqdn();
    if (!"/".equals(fqdn.substring(fqdn.length() - 1))) {
      fqdn = fqdn + "/";
    }
    List<UserGroupMember> userGroupMembers = userGroup.getUserGroupMembers();
    String subject = Email.SUBJECT_USER_GROUP_ARCHIVED.replace("[User Group Name]", userGroup.getName());
    for (UserGroupMember userGroupMember : userGroupMembers) {
      String GroupLink = fqdn + "users/group/edit/" + userGroupMember.getUserGroup().getId() + "?readOnly=true";
      Map<String, String> attributes = new HashMap<>();
      attributes.put(Email.ATTRIBUTE_LOGIN_URL, fqdn + appUrl.getLoginPath());
      attributes.put(Email.ATTRIBUTE_USER_NAME, userGroupMember.getUser().getFirstName());
      attributes.put(Email.ATTRIBUTE_USER_GROUP_NAME, userGroup.getName());
      attributes.put(Email.ATTRIBUTE_USER_GROUP_ARCHIVAL_REASON, reason);
      attributes.put(Email.ATTRIBUTE_SYSTEM_ADMIN, principalUser.getFirstName());
      attributes.put(Email.ATTRIBUTE_GROUP_LINK, GroupLink);
      Set<String> emailIds = Collections.singleton(userGroupMember.getUser().getEmail());

      EmailRequest emailRequest = EmailRequest.builder().to(emailIds)
        .templateName(Email.TEMPLATE_USER_GROUP_ARCHIVED).subject(subject).attributes(attributes).build();
      emailService.sendEmail(emailRequest);
    }
  }

  @Override
  
  @Transactional
  public void groupMembershipUpdate(UserGroup userGroup, UserGroupUpdateRequest userGroupUpdateRequest, List<User> users, PrincipalUser principleUser) {
    Organisation organisation = organisationRepository.getOne(principleUser.getOrganisationId());
    String fqdn = organisation.getFqdn();
    if (!"/".equals(fqdn.substring(fqdn.length() - 1))) {
      fqdn = fqdn + "/";
    }
    for (User removeUser : users) {
      Map<String, String> attributes = new HashMap<>();
      attributes.put(Email.ATTRIBUTE_LOGIN_URL, fqdn + appUrl.getLoginPath());
      attributes.put(Email.ATTRIBUTE_USER_NAME, removeUser.getFirstName());
      attributes.put(Email.ATTRIBUTE_SINGLE_USER_REMOVAL_REASON, userGroupUpdateRequest.getReason());
      attributes.put(Email.ATTRIBUTE_SYSTEM_ADMIN, principleUser.getFirstName());
      attributes.put(Email.ATTRIBUTE_USER_GROUP_NAME, userGroup.getName());
      Set<String> emailIds = Collections.singleton(removeUser.getEmail());

      EmailRequest emailRequest = EmailRequest.builder().to(emailIds)
        .templateName(Email.TEMPLATE_REMOVED_FROM_USER_GROUP).subject(Email.SUBJECT_REMOVED_FROM_USER_GROUP).attributes(attributes).build();
      emailService.sendEmail(emailRequest);
    }
  }

  @Override
  @Async
  @Transactional
  public void parameterExceptionRequested(NotificationParameterExceptionDto notificationParameterExceptionDto, List<ParameterExceptionReviewer> reviewerUsersList) {
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    User initiatorUser = userRepository.getOne(principalUser.getId());
    Organisation organisation = organisationRepository.getOne(notificationParameterExceptionDto.getOrganisationId());
    String fqdn = organisation.getFqdn();
    if (!"/".equals(fqdn.substring(fqdn.length() - 1))) {
      fqdn = fqdn + "/";
    }
    String taskLink = fqdn + "/inbox/" + notificationParameterExceptionDto.getJobId() + "?taskId=" + notificationParameterExceptionDto.getTaskExecutionId();
    String jobLink = fqdn + "/inbox/" + notificationParameterExceptionDto.getJobId();

    for (ParameterExceptionReviewer reviewer : reviewerUsersList) {
      Map<String, String> attributes = new HashMap<>();
      attributes.put(Email.ATTRIBUTE_LOGIN_URL, fqdn + appUrl.getLoginPath());
      attributes.put(Email.ATTRIBUTE_USER_NAME, reviewer.getUser().getFirstName());
      attributes.put(Email.ATTRIBUTE_INITIATOR_NAME, initiatorUser.getFirstName());
      attributes.put(Email.ATTRIBUTE_INITIATOR_DESCRIPTION, notificationParameterExceptionDto.getInitiatorReason());
      attributes.put(Email.ATTRIBUTE_JOB_ID, notificationParameterExceptionDto.getJobCode());
      attributes.put(Email.ATTRIBUTE_JOB_LINK, jobLink);
      attributes.put(Email.ATTRIBUTE_PARAMETER, notificationParameterExceptionDto.getParameterName());
      attributes.put(Email.ATTRIBUTE_TASK_NAME, notificationParameterExceptionDto.getTaskName());
      attributes.put(Email.ATTRIBUTE_PROCESS_NAME, notificationParameterExceptionDto.getProcessName());
      attributes.put(Email.ATTRIBUTE_TASK_LINK, taskLink);
      Set<String> emailIds = Collections.singleton(reviewer.getUser().getEmail());

      EmailRequest emailRequest = EmailRequest.builder().to(emailIds)
        .templateName(Email.TEMPLATE_EXCEPTION_REQUESTED).subject(Email.SUBJECT_EXCEPTION_APPROVAL_REQUESTED).attributes(attributes).build();
      emailService.sendEmail(emailRequest);
    }
  }

  @Override
  @Async
  @Transactional
  public void approveParameterException(ParameterException currentException, NotificationParameterExceptionDto notificationParameterExceptionDto, List<ParameterExceptionReviewer> reviewerUsersList) {
    Organisation organisation = organisationRepository.getOne(notificationParameterExceptionDto.getOrganisationId());
    String fqdn = organisation.getFqdn();
    if (!"/".equals(fqdn.substring(fqdn.length() - 1))) {
      fqdn = fqdn + "/";
    }
    String taskLink = fqdn + "/inbox/" + notificationParameterExceptionDto.getJobId() + "?taskId=" + notificationParameterExceptionDto.getTaskExecutionId();
    String jobLink = fqdn + "/inbox/" + notificationParameterExceptionDto.getJobId();
    String reviewerName = "";
    String reviewerDescription = "";
    for (ParameterExceptionReviewer reviewer : reviewerUsersList) {
      if (reviewer.isActionPerformed()) {
        reviewerName = reviewer.getUser().getFirstName();
        reviewerDescription = reviewer.getExceptions().getReviewersReason();
      }
    }
    Set<String> emailIds = Collections.singleton(currentException.getCreatedBy().getEmail());
    Map<String, String> attributes = new HashMap<>();
    attributes.put(Email.ATTRIBUTE_LOGIN_URL, fqdn + appUrl.getLoginPath());
    attributes.put(Email.ATTRIBUTE_USER_NAME, currentException.getCreatedBy().getFirstName());
    attributes.put(Email.ATTRIBUTE_REVIEWER_NAME, reviewerName);
    attributes.put(Email.ATTRIBUTE_REVIEWER_DESCRIPTION, reviewerDescription);
    attributes.put(Email.ATTRIBUTE_JOB_ID, notificationParameterExceptionDto.getJobCode());
    attributes.put(Email.ATTRIBUTE_JOB_LINK, jobLink);
    attributes.put(Email.ATTRIBUTE_PARAMETER, notificationParameterExceptionDto.getParameterName());
    attributes.put(Email.ATTRIBUTE_TASK_NAME, notificationParameterExceptionDto.getTaskName());
    attributes.put(Email.ATTRIBUTE_PROCESS_NAME, notificationParameterExceptionDto.getProcessName());
    attributes.put(Email.ATTRIBUTE_TASK_LINK, taskLink);
    EmailRequest emailRequest = EmailRequest.builder().to(emailIds)
      .templateName(Email.TEMPLATE_EXCEPTION_APPROVED).subject(Email.SUBJECT_EXCEPTION_APPROVED).attributes(attributes).build();
    emailService.sendEmail(emailRequest);
  }

  @Override
  @Async
  @Transactional
  public void rejectParameterException(ParameterException currentException, NotificationParameterExceptionDto notificationParameterExceptionDto, List<ParameterExceptionReviewer> reviewerUsersList) {
    Organisation organisation = organisationRepository.getOne(notificationParameterExceptionDto.getOrganisationId());
    String fqdn = organisation.getFqdn();
    if (!"/".equals(fqdn.substring(fqdn.length() - 1))) {
      fqdn = fqdn + "/";
    }
    String taskLink = fqdn + "/inbox/" + notificationParameterExceptionDto.getJobId() + "?taskId=" + notificationParameterExceptionDto.getTaskExecutionId();
    String jobLink = fqdn + "/inbox/" + notificationParameterExceptionDto.getJobId();
    String reviewerName = "";
    String reviewerDescription = "";
    for (ParameterExceptionReviewer reviewer : reviewerUsersList) {
      if (reviewer.isActionPerformed()) {
        reviewerName = reviewer.getUser().getFirstName();
        reviewerDescription = reviewer.getExceptions().getReviewersReason();
      }
    }
    Set<String> emailIds = Collections.singleton(currentException.getCreatedBy().getEmail());
    Map<String, String> attributes = new HashMap<>();
    attributes.put(Email.ATTRIBUTE_LOGIN_URL, fqdn + appUrl.getLoginPath());
    attributes.put(Email.ATTRIBUTE_USER_NAME, currentException.getCreatedBy().getFirstName());
    attributes.put(Email.ATTRIBUTE_REVIEWER_NAME, reviewerName);
    attributes.put(Email.ATTRIBUTE_REVIEWER_DESCRIPTION, reviewerDescription);
    attributes.put(Email.ATTRIBUTE_JOB_ID, notificationParameterExceptionDto.getJobCode());
    attributes.put(Email.ATTRIBUTE_JOB_LINK, jobLink);
    attributes.put(Email.ATTRIBUTE_PARAMETER, notificationParameterExceptionDto.getParameterName());
    attributes.put(Email.ATTRIBUTE_TASK_NAME, notificationParameterExceptionDto.getTaskName());
    attributes.put(Email.ATTRIBUTE_PROCESS_NAME, notificationParameterExceptionDto.getProcessName());
    attributes.put(Email.ATTRIBUTE_TASK_LINK, taskLink);
    EmailRequest emailRequest = EmailRequest.builder().to(emailIds)
      .templateName(Email.TEMPLATE_EXCEPTION_REJECTED).subject(Email.SUBJECT_EXCEPTION_REJECTED).attributes(attributes).build();
    emailService.sendEmail(emailRequest);

  }

  @Override
  @Async
  public void notifyIfAllPrerequisiteTasksCompleted(Long preRequisiteTaskId, Long jobId, Long organisationId) {
    Organisation organisation = organisationRepository.getReferenceById(organisationId);
    String fqdn = organisation.getFqdn();
    if (!"/".equals(fqdn.substring(fqdn.length() - 1))) {
      fqdn = fqdn + "/";
    }

    List<TaskExecutionView> dependantTaskExecutionView = taskExecutionRepository.getAllLatestDependantTaskExecutionIdsHavingPrerequisiteTaskId(preRequisiteTaskId, jobId);
    for (TaskExecutionView dependantTaskExecution: dependantTaskExecutionView) {
      List<TaskExecution> incompleteDependencies = taskExecutionRepository.findIncompleteDependencies(dependantTaskExecution.getTaskId(), jobId);
      if (Utility.isEmpty(incompleteDependencies)) {
        List<TaskExecutionAssigneeBasicView> assignees = taskExecutionAssigneeRepository.findAllByTaskExecutionId(dependantTaskExecution.getId());
        String preRequisiteTaskNames = taskExecutionRepository.getAllCompletedPreRequisiteTaskDetails(dependantTaskExecution.getTaskId(), jobId).stream()
          .map(TaskExecutionView::getTaskName)
          .collect(Collectors.joining(","));
        for (TaskExecutionAssigneeBasicView assignee: assignees) {
          Map<String, String> attributes = new HashMap<>();
          attributes.put(Email.ATTRIBUTE_LOGIN_URL, fqdn + appUrl.getLoginPath());
          attributes.put(Email.ATTRIBUTE_USER_NAME, assignee.getUserName());
          attributes.put(Email.ATTRIBUTE_TASK_NAME, dependantTaskExecution.getTaskName());
          attributes.put(Email.ATTRIBUTE_PROCESS_NAME, dependantTaskExecution.getChecklistName());
          attributes.put(Email.ATTRIBUTE_JOB_LINK, fqdn + appUrl.getJobPath(jobId));
          attributes.put(Email.ATTRIBUTE_TASK_LINK, fqdn + appUrl.getTaskExecutionPath(jobId, dependantTaskExecution.getId()));
          attributes.put(Email.ATTRIBUTE_PREREQUISITE_TASKS, preRequisiteTaskNames);
          attributes.put(Email.ATTRIBUTE_JOB_CODE, dependantTaskExecution.getJobCode());


          EmailRequest emailRequest = EmailRequest.builder().to(Collections.singleton(assignee.getEmail()))
            .templateName(Email.TEMPLATE_DEPENDANT_TASK_READY_TO_START).subject(Email.PREREQUISITE_TASKS_COMPLETED).attributes(attributes).build();
          emailService.sendEmail(emailRequest);
        }
      }
    }
  }
}



