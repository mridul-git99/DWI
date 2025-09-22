package com.leucine.streem.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.leucine.streem.constant.ErrorMessage;
import com.leucine.streem.constant.State;
import com.leucine.streem.constant.Type;
import com.leucine.streem.dto.*;
import com.leucine.streem.dto.mapper.*;
import com.leucine.streem.dto.projection.CorrectionListViewProjection;
import com.leucine.streem.dto.projection.TaskExecutionView;
import com.leucine.streem.dto.request.*;
import com.leucine.streem.dto.response.Error;
import com.leucine.streem.dto.response.ErrorCode;
import com.leucine.streem.exception.ExceptionType;
import com.leucine.streem.exception.ResourceNotFoundException;
import com.leucine.streem.exception.StreemException;
import com.leucine.streem.model.*;
import com.leucine.streem.model.helper.BaseEntity;
import com.leucine.streem.model.helper.PrincipalUser;
import com.leucine.streem.repository.*;
import com.leucine.streem.service.*;
import com.leucine.streem.util.DateTimeUtils;
import com.leucine.streem.util.Utility;
import com.leucine.streem.util.ValidationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CorrectionService implements ICorrectionService {
  private final IUserRepository userRepository;
  private final ITaskExecutionRepository taskExecutionRepository;
  private final ITaskExecutionMapper taskExecutionMapper;
  private final IParameterValueRepository parameterValueRepository;
  private final ICodeService codeService;
  private final IFacilityRepository facilityRepository;
  private final ICorrectionRepository correctionRepository;
  private final IUserGroupRepository userGroupRepository;
  private final ICorrectorRepository correctorRepository;
  private final IReviewerRepository reviewerRepository;
  private final ICorrectorMapper correctorMapper;
  private final IReviewerMapper reviewerMapper;
  private final ICorrectionMapper correctionMapper;
  private final IJobAuditService jobAuditService;
  private final IParameterValueMapper parameterValueMapper;
  private final IParameterMapper parameterMapper;
  private final IParameterExecutionValidationService parameterExecutionValidationService;
  private final IUserMapper userMapper;
  private final IJobLogService jobLogService;
  private final ICorrectionMediaMappingRepository correctionMediaMappingRepository;
  private final IMediaRepository mediaRepository;
  private final IMediaMapper mediaMapper;
  private final IParameterValueMediaRepository parameterValueMediaRepository;
  private final IJobService jobService;
  private final INotificationService notificationService;

  @Override
  @Transactional(rollbackFor = Exception.class)
  public TaskExecutionDto enableCorrection(Long taskExecutionId) throws StreemException {
    log.info("[enableCorrection] Request to enable correction for task, taskExecutionId: {}", taskExecutionId);
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    TaskExecution taskExecution = taskExecutionRepository.getReferenceById(taskExecutionId);

    jobService.validateIfUserIsAssignedToExecuteJob(taskExecution.getJobId(), principalUser.getId());
    // We are allowing error correction if a repeated instance's task is not started
    validateIfAllTaskExecutionsAreInCompletedStateOrNotStarted(taskExecution.getTaskId(), taskExecution.getJobId());
    if (taskExecution.isCorrectionEnabled()) {
      ValidationUtils.invalidate(taskExecution.getId(), ErrorCode.TASK_ALREADY_ENABLED_FOR_CORRECTION);
    }
    Job job = taskExecution.getJob();
    if (State.JOB_COMPLETED_STATES.contains(job.getState())) {
      ValidationUtils.invalidate(job.getId(), ErrorCode.JOB_ALREADY_COMPLETED);
    }
    TaskExecutionDto taskExecutionDto = taskExecutionMapper.toDto(taskExecution);
    taskExecution.setCorrectionEnabled(true);
    taskExecutionDto.setCorrectionEnabled(true);
    taskExecutionRepository.save(taskExecution);
    jobAuditService.enableCorrection(taskExecution.getJobId(), taskExecutionId, principalUser);
    return taskExecutionDto;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public TaskExecutionDto cancelCorrection(Long taskExecutionId) throws StreemException {
    log.info("[cancelCorrection] Request to cancel correction, taskExecutionId: {}", taskExecutionId);
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    TaskExecution taskExecution = taskExecutionRepository.getReferenceById(taskExecutionId);

    if (!taskExecution.isCorrectionEnabled()) {
      ValidationUtils.invalidate(taskExecution.getId(), ErrorCode.TASK_NOT_ENABLED_FOR_CORRECTION);
    }
    Job job = taskExecution.getJob();
    if (State.JOB_COMPLETED_STATES.contains(job.getState())) {
      ValidationUtils.invalidate(job.getId(), ErrorCode.JOB_ALREADY_COMPLETED);
    }
    validateIfAllParametersInTaskAreInCompletedState(taskExecutionId);
    jobService.validateIfUserIsAssignedToExecuteJob(taskExecution.getJobId(), principalUser.getId());
    TaskExecutionDto taskExecutionDto = taskExecutionMapper.toDto(taskExecution);
    taskExecutionDto.setCorrectionEnabled(false);
    taskExecution.setCorrectionEnabled(false);
    taskExecutionRepository.save(taskExecution);
    jobAuditService.disableCorrection(taskExecution.getJobId(), taskExecutionId, principalUser);
    return taskExecutionDto;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public CorrectionDto initiateCorrection(Long parameterExecutionId, ParameterCorrectionInitiatorRequest parameterCorrectionInitiatorRequest) throws StreemException, ResourceNotFoundException {
    log.info("[executeParameterForError] Request to initiate correction in parameter,parameterExecutionId:{} parameterExecuteRequest: {}", parameterExecutionId, parameterCorrectionInitiatorRequest);
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    User principalUserEntity = userRepository.findById(principalUser.getId())
      .orElseThrow(() -> new ResourceNotFoundException(principalUser.getId(), ErrorCode.USER_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));

    ParameterValue parameterValue = parameterValueRepository.findById(parameterExecutionId)
      .orElseThrow(() -> new ResourceNotFoundException(parameterExecutionId, ErrorCode.PARAMETER_VALUE_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));
    jobService.validateIfUserIsAssignedToExecuteJob(parameterValue.getJobId(), principalUser.getId());
    parameterExecutionValidationService.validateIfCorrectionCanBeInitiated(parameterValue);
    Facility facility = facilityRepository.findById(principalUser.getCurrentFacilityId()).get();
    Parameter parameter = parameterValue.getParameter();
    String initiatorReason = parameterCorrectionInitiatorRequest.getInitiatorReason();

    if (Utility.isEmpty(initiatorReason)) {
      ValidationUtils.invalidate(parameterValue.getParameterId(), ErrorCode.CORRECTION_INITIATOR_REASON);
    }

    CorrectionCorrectorsReviewersRequest correctors = parameterCorrectionInitiatorRequest.getCorrectors();
    CorrectionCorrectorsReviewersRequest reviewers = parameterCorrectionInitiatorRequest.getReviewers();

    validateCorrectorReviewerAtInitiate(correctors, reviewers, parameterValue);

    Correction correction = new Correction();
    correction.setCode(codeService.getCode(Type.EntityType.CORRECTION, principalUser.getOrganisationId()));
    correction.setParameterValue(parameterValue);
    correction.setTaskExecution(parameterValue.getTaskExecution());
    correction.setJob(parameterValue.getJob());
    correction.setFacility(facility);
    correction.setInitiatorsReason(initiatorReason);
    correction.setStatus(State.Correction.INITIATED);
    correction.setCreatedBy(principalUserEntity);
    correction.setModifiedBy(principalUserEntity);
    correction.setCreatedAt(DateTimeUtils.now());
    correction.setModifiedAt(DateTimeUtils.now());
    correction.setPreviousState(parameterValue.getState());
    setCorrectionValuesByParameterType(correction, parameterValue, parameter, principalUserEntity, parameterValue.getValue(), parameterValue.getChoices(), false, new ArrayList<>());
    Correction savedCorrection = correctionRepository.save(correction);

    List<User> correctorUsersList = userRepository.findAllById(correctors.getUserId());
    Map<Long, User> correctorUserMap = correctorUsersList.stream().collect(Collectors.toMap(BaseEntity::getId, Function.identity()));
    List<Corrector> correctorList = new ArrayList<Corrector>();
    for (Long userId : correctors.getUserId()) {
      User user = correctorUserMap.get(userId);
      Corrector corrector = new Corrector();
      corrector.setCorrection(savedCorrection);
      corrector.setUser(user);
      corrector.setCreatedBy(principalUserEntity);
      corrector.setModifiedBy(principalUserEntity);
      corrector.setCreatedAt(DateTimeUtils.now());
      corrector.setModifiedAt(
        DateTimeUtils.now()
      );
      correctorList.add(corrector);
    }

    List<UserGroup> correctorUserGroupsList = userGroupRepository.findAllById(correctors.getUserId());
    Map<Long, UserGroup> correctorUserGroupsMap = correctorUserGroupsList.stream().collect(Collectors.toMap(BaseEntity::getId, Function.identity()));
    for (Long userGroupId : correctors.getUserGroupId()) {
      UserGroup userGroup = correctorUserGroupsMap.get(userGroupId);
      Corrector corrector = new Corrector();
      corrector.setCorrection(savedCorrection);
      corrector.setUserGroup(userGroup);
      corrector.setCreatedBy(principalUserEntity);
      corrector.setModifiedBy(principalUserEntity);
      corrector.setCreatedAt(DateTimeUtils.now());
      corrector.setModifiedAt(
        DateTimeUtils.now()
      );
      correctorList.add(corrector);
    }

    correctorRepository.saveAll(correctorList);

    List<User> reviewerUsersList = userRepository.findAllById(reviewers.getUserId());
    Map<Long, User> reviewersUserMap = reviewerUsersList.stream().collect(Collectors.toMap(BaseEntity::getId, Function.identity()));
    List<Reviewer> reviewerList = new ArrayList<>();
    for (Long userId : reviewers.getUserId()) {
      User user = reviewersUserMap.get(userId);
      Reviewer reviewer = new Reviewer();
      reviewer.setCorrection(savedCorrection);
      reviewer.setUser(user);
      reviewer.setCreatedBy(principalUserEntity);
      reviewer.setModifiedBy(principalUserEntity);
      reviewer.setCreatedAt(DateTimeUtils.now());
      reviewer.setModifiedAt(
        DateTimeUtils.now()
      );
      reviewerList.add(reviewer);
    }

    List<UserGroup> reviewerUserGroupsList = userGroupRepository.findAllById(reviewers.getUserId());
    Map<Long, UserGroup> reviewerUserGroupsMap = reviewerUserGroupsList.stream().collect(Collectors.toMap(BaseEntity::getId, Function.identity()));
    for (Long userGroupId : reviewers.getUserGroupId()) {
      UserGroup userGroup = reviewerUserGroupsMap.get(userGroupId);
      Reviewer reviewer = new Reviewer();
      reviewer.setCorrection(savedCorrection);
      reviewer.setUserGroup(userGroup);
      reviewer.setCreatedBy(principalUserEntity);
      reviewer.setModifiedBy(principalUserEntity);
      reviewer.setCreatedAt(DateTimeUtils.now());
      reviewer.setModifiedAt(
        DateTimeUtils.now()
      );
      reviewerList.add(reviewer);
    }

    reviewerRepository.saveAll(reviewerList);

    parameterValue.setState(State.ParameterExecution.APPROVAL_PENDING);
    parameterValue.setHasCorrections(true);
    parameterValueRepository.save(parameterValue);

    List<CorrectorDto> correctorDtos = correctorMapper.toDto(correctorList);
    List<ReviewerDto> reviewerDtos = reviewerMapper.toDto(reviewerList);

    CorrectionDto correctionDto = correctionMapper.toDto(savedCorrection);
    correctionDto.setCorrector(correctorDtos);
    correctionDto.setReviewer(reviewerDtos);

    Long organisationId = principalUser.getOrganisationId();
    NotificationParameterValueDto notificationParameterValueDto = parameterValueToNotificationDto(parameterValue, organisationId);

    jobAuditService.initiateCorrection(parameterValue.getJobId(), parameterValue.getTaskExecutionId(), parameterValue.getParameterId(), principalUser, savedCorrection, initiatorReason);
    notificationService.correctionRequested(correctorUsersList, parameterCorrectionInitiatorRequest, notificationParameterValueDto, principalUser.getOrganisationId());
    return correctionDto;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public CorrectionDto performCorrection(Long parameterExecutionId, ParameterCorrectionCorrectorRequest parameterCorrectionCorrectorRequest) throws ResourceNotFoundException, StreemException, IOException {
    log.info("[performCorrection] Request to perform correction parameterExecutionId:{}, parameterCorrectionCorrectorRequest,:{}", parameterExecutionId, parameterCorrectionCorrectorRequest);
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    User principalUserEntity = userRepository.getOne(principalUser.getId());
    ParameterValue parameterValue = parameterValueRepository.findById(parameterExecutionId)
      .orElseThrow(() -> new ResourceNotFoundException(parameterExecutionId, ErrorCode.PARAMETER_VALUE_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));
    jobService.validateIfUserIsAssignedToExecuteJob(parameterValue.getJobId(), principalUser.getId());
    Parameter parameter = parameterValue.getParameter();
    String correctorRemark = parameterCorrectionCorrectorRequest.getCorrectorReason();
    String newValue = parameterCorrectionCorrectorRequest.getNewValue();
    JsonNode newChoice = parameterCorrectionCorrectorRequest.getNewChoice();
    List<Media> mediasList = mediaMapper.toEntity(parameterCorrectionCorrectorRequest.getMedias());
    Long correctionId = parameterCorrectionCorrectorRequest.getCorrectionId();

    if (Utility.isEmpty(correctorRemark)) {
      ValidationUtils.invalidate(parameterValue.getParameterId(), ErrorCode.CORRECTION_CORRECTOR_REASON);
    }
    if (parameter.isMandatory()) {
      boolean isParameterIncomplete = parameterExecutionValidationService.isParameterValueIncomplete(parameter, null, true, parameterValue.getMedias(), null, mediasList, newValue, newChoice);
      if (isParameterIncomplete) {
        ValidationUtils.invalidate(parameter.getId(), ErrorCode.CORRECTION_CORRECTOR_VALUE_MISSING);
      }
    }
    List<Corrector> correctorList = correctorRepository.findByCorrectionId(correctionId);
    List<Reviewer> reviewerList = reviewerRepository.findByCorrectionId(correctionId);

    boolean isUserInCorrectorList = correctorList.stream()
      .map(Corrector::getUser)
      .map(User::getId)
      .anyMatch(userId -> userId.equals(principalUser.getId()));
    if (!isUserInCorrectorList) {
      ValidationUtils.invalidate(parameterValue.getParameterId(), ErrorCode.CORRECTION_CORRECTOR_DID_NOT_MATCH);
    }

    Correction currentCorrection = correctionRepository.findById(correctionId).orElseThrow(() -> new ResourceNotFoundException(correctionId, ErrorCode.CORRECTION_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));
    validateCorrectionStatusFlow(currentCorrection.getStatus(), String.valueOf(parameterValue.getParameterId()), State.Correction.CORRECTED);
    currentCorrection.setCorrectorsReason(correctorRemark);
    setCorrectionValuesByParameterType(currentCorrection, parameterValue, parameter, principalUserEntity, newValue, newChoice, true, mediasList);
    currentCorrection.setModifiedAt(DateTimeUtils.now());
    currentCorrection.setModifiedBy(principalUserEntity);
    currentCorrection.setStatus(State.Correction.CORRECTED);
    correctionRepository.save(currentCorrection);

    for (Corrector corrector : correctorList) {
      if (corrector.getUser().getId().equals(principalUser.getId())) {
        corrector.setActionPerformed(true);
        corrector.setModifiedAt(DateTimeUtils.now());
        correctorRepository.save(corrector);
        break;
      }
    }

    parameterValue.setState(State.ParameterExecution.VERIFICATION_PENDING);
    parameterValueRepository.save(parameterValue);

    ParameterValueDto parameterValueDto = parameterValueMapper.toDto(parameterValue);
    List<MediaDto> oldMedias = parameterMapper.getMedias(parameterValue.getMedias());

    CorrectionDto correctionDto = correctionMapper.toDto(currentCorrection);
    List<CorrectorDto> correctorDtos = correctorMapper.toDto(correctorList);
    List<ReviewerDto> reviewerDtos = reviewerMapper.toDto(reviewerList);
    correctionDto.setCorrector(correctorDtos);
    correctionDto.setReviewer(reviewerDtos);
    jobAuditService.executedParameter(parameterValue.getJobId(), parameterValue.getId(), parameterValue.getParameterId(), parameterValueDto, oldMedias, parameterValue.getParameter().getType(), true, parameterValue.getReason(), correctorRemark, principalUser, null, false);
    NotificationParameterValueDto notificationParameterValueDto = parameterValueToNotificationDto(parameterValue, principalUser.getOrganisationId());
    notificationService.reviewCorrection(reviewerList, correctorList, notificationParameterValueDto, principalUser.getOrganisationId());
    return correctionDto;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public CorrectionDto approveCorrection(Long parameterExecutionId, ParameterCorrectionApproveRejectRequest parameterCorrectionApproveRejectRequest) throws ResourceNotFoundException, StreemException, IOException {
    log.info("[approveCorrection] Request to approve correction parameterExecutionId:{},parameterCorrectionApproveRejectRequest,:{}", parameterExecutionId, parameterCorrectionApproveRejectRequest);
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    User principalUserEntity = userRepository.getOne(principalUser.getId());
    ParameterValue parameterValue = parameterValueRepository.findById(parameterExecutionId)
      .orElseThrow(() -> new ResourceNotFoundException(parameterExecutionId, ErrorCode.PARAMETER_VALUE_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));
    jobService.validateIfUserIsAssignedToExecuteJob(parameterValue.getJobId(), principalUser.getId());
    String reviewerReason = parameterCorrectionApproveRejectRequest.getReviewerReason();
    Long correctionId = parameterCorrectionApproveRejectRequest.getCorrectionId();
    Parameter parameter = parameterValue.getParameter();
    if (Utility.isEmpty(reviewerReason)) {
      ValidationUtils.invalidate(parameterValue.getParameterId(), ErrorCode.CORRECTION_REVIEWER_REASON);
    }
    List<Corrector> correctorList = correctorRepository.findByCorrectionId(correctionId);
    List<Reviewer> reviewerList = reviewerRepository.findByCorrectionId(correctionId);

    boolean isUserInReviewerList = reviewerList.stream()
      .map(Reviewer::getUser)
      .map(User::getId)
      .anyMatch(userId -> userId.equals(principalUser.getId()));

    if (!isUserInReviewerList) {
      ValidationUtils.invalidate(parameterValue.getParameterId(), ErrorCode.CORRECTION_REVIEWER_DID_NOT_MATCH);
    }

    Correction currentCorrection = correctionRepository.findById(correctionId).orElseThrow(() -> new ResourceNotFoundException(correctionId, ErrorCode.CORRECTION_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));
    validateCorrectionStatusFlow(currentCorrection.getStatus(), String.valueOf(parameterValue.getParameterId()), State.Correction.ACCEPTED);
    currentCorrection.setReviewersReason(reviewerReason);
    currentCorrection.setModifiedAt(DateTimeUtils.now());
    currentCorrection.setModifiedBy(principalUserEntity);
    currentCorrection.setStatus(State.Correction.ACCEPTED);
    correctionRepository.save(currentCorrection);

    for (Reviewer reviewer : reviewerList) {
      if (reviewer.getUser().getId().equals(principalUser.getId())) {
        reviewer.setActionPerformed(true);
        reviewer.setModifiedAt(DateTimeUtils.now());
        reviewerRepository.save(reviewer);
        break;
      }
    }

    setParameterValueOnApprovalCorrection(parameter.getType(), currentCorrection, parameterValue, principalUserEntity);

    User correctedByUser = userRepository.getUserWhoCorrectedByCorrectionId(correctionId);
    parameterValue.setModifiedAt(DateTimeUtils.now());
    parameterValue.setModifiedBy(correctedByUser);
    parameterValue.setState(State.ParameterExecution.EXECUTED);
    parameterValueRepository.save(parameterValue);

    CorrectionDto correctionDto = correctionMapper.toDto(currentCorrection);
    List<CorrectorDto> correctorDtos = correctorMapper.toDto(correctorList);
    List<ReviewerDto> reviewerDtos = reviewerMapper.toDto(reviewerList);
    correctionDto.setCorrector(correctorDtos);
    correctionDto.setReviewer(reviewerDtos);

    UserAuditDto userBasicInfoDto = userMapper.toUserAuditDto(correctedByUser);
    jobLogService.updateJobLog(parameterValue.getJobId(), parameter.getId(), parameter.getType(), reviewerReason, parameter.getLabel(), Type.JobLogTriggerType.PARAMETER_VALUE, userBasicInfoDto);
    jobAuditService.approveCorrection(parameterValue.getJobId(), parameterValue.getTaskExecutionId(), parameterValue.getParameterId(), principalUser, currentCorrection, reviewerReason);
    NotificationParameterValueDto notificationParameterValueDto = parameterValueToNotificationDto(parameterValue, principalUser.getOrganisationId());
    notificationService.approveCorrection(currentCorrection, notificationParameterValueDto, reviewerList, principalUser.getOrganisationId());
    return correctionDto;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public CorrectionDto rejectCorrection(Long parameterExecutionId, ParameterCorrectionApproveRejectRequest parameterCorrectionApproveRejectRequest) throws ResourceNotFoundException, StreemException {
    log.info("[rejectCorrection] Request to reject correction parameterExecutionId:{},parameterCorrectionApproveRejectRequest,:{}", parameterExecutionId, parameterCorrectionApproveRejectRequest);
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    User principalUserEntity = userRepository.getOne(principalUser.getId());
    ParameterValue parameterValue = parameterValueRepository.findById(parameterExecutionId)
      .orElseThrow(() -> new ResourceNotFoundException(parameterExecutionId, ErrorCode.PARAMETER_VALUE_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));
    jobService.validateIfUserIsAssignedToExecuteJob(parameterValue.getJobId(), principalUser.getId());
    String reviewerReason = parameterCorrectionApproveRejectRequest.getReviewerReason();
    Long correctionId = parameterCorrectionApproveRejectRequest.getCorrectionId();

    if (Utility.isEmpty(reviewerReason)) {
      ValidationUtils.invalidate(parameterValue.getParameterId(), ErrorCode.CORRECTION_REVIEWER_REASON);
    }
    List<Corrector> correctorList = correctorRepository.findByCorrectionId(correctionId);
    List<Reviewer> reviewerList = reviewerRepository.findByCorrectionId(correctionId);

    boolean isUserInReviewerList = reviewerList.stream()
      .map(Reviewer::getUser)
      .map(User::getId)
      .anyMatch(userId -> userId.equals(principalUser.getId()));

    if (!isUserInReviewerList) {
      ValidationUtils.invalidate(parameterValue.getParameterId(), ErrorCode.CORRECTION_REVIEWER_DID_NOT_MATCH);
    }

    Correction currentCorrection = correctionRepository.findById(correctionId).orElseThrow(() -> new ResourceNotFoundException(correctionId, ErrorCode.CORRECTION_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));
    validateCorrectionStatusFlow(currentCorrection.getStatus(), String.valueOf(parameterValue.getParameterId()), State.Correction.REJECTED);
    currentCorrection.setReviewersReason(reviewerReason);
    currentCorrection.setModifiedAt(DateTimeUtils.now());
    currentCorrection.setModifiedBy(principalUserEntity);
    currentCorrection.setStatus(State.Correction.REJECTED);
    correctionRepository.save(currentCorrection);

    for (Reviewer reviewer : reviewerList) {
      if (reviewer.getUser().getId().equals(principalUser.getId())) {
        reviewer.setActionPerformed(true);
        reviewer.setModifiedAt(DateTimeUtils.now());
        reviewerRepository.save(reviewer);
        break;
      }
    }


    parameterValue.setState(Objects.requireNonNullElse(currentCorrection.getPreviousState(), State.ParameterExecution.EXECUTED));
    parameterValueRepository.save(parameterValue);

    CorrectionDto correctionDto = correctionMapper.toDto(currentCorrection);
    List<CorrectorDto> correctorDtos = correctorMapper.toDto(correctorList);
    List<ReviewerDto> reviewerDtos = reviewerMapper.toDto(reviewerList);
    correctionDto.setCorrector(correctorDtos);
    correctionDto.setReviewer(reviewerDtos);
    NotificationParameterValueDto notificationParameterValueDto = parameterValueToNotificationDto(parameterValue, principalUser.getOrganisationId());
    jobAuditService.rejectCorrection(parameterValue.getJobId(), parameterValue.getTaskExecutionId(), parameterValue.getParameterId(), principalUser, currentCorrection, reviewerReason);
    notificationService.rejectCorrection(currentCorrection, notificationParameterValueDto, reviewerList, principalUser.getOrganisationId());
    return correctionDto;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public CorrectionDto recallCorrection(Long parameterExecutionId, ParameterCorrectionRecallRequest parameterCorrectionRecallRequest) throws ResourceNotFoundException, StreemException {
    log.info("[recallCorrection] Request to recall the correction for parameterExecutionId: {},  parameterCorrectionRecallRequest: {}", parameterExecutionId, parameterCorrectionRecallRequest);
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    User principalUserEntity = userRepository.getReferenceById(principalUser.getId());
    ParameterValue parameterValue = parameterValueRepository.findById(parameterExecutionId)
      .orElseThrow(() -> new ResourceNotFoundException(parameterExecutionId, ErrorCode.PARAMETER_VALUE_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));

    Set<State.ParameterExecution> allowedParameterExecutionStateForRecall = Set.of(State.ParameterExecution.APPROVAL_PENDING, State.ParameterExecution.VERIFICATION_PENDING);
    if (!allowedParameterExecutionStateForRecall.contains(parameterValue.getState())) {
      ValidationUtils.invalidate(parameterExecutionId, ErrorCode.CORRECTION_NOT_FOUND);
    }


    Correction correction = correctionRepository.getReferenceById(parameterCorrectionRecallRequest.getCorrectionId());
    if (!Utility.isEmpty(correction.getPreviousState())) {
      parameterValue.setState(correction.getPreviousState());
    }
    parameterValue.setModifiedBy(principalUserEntity);
    parameterValue.setHasCorrections(false);
    parameterValueRepository.save(parameterValue);
    correction.setStatus(State.Correction.RECALLED);
    Correction savedCorrection = correctionRepository.save(correction);
    jobAuditService.recallCorrection(parameterValue.getJobId(), parameterValue.getTaskExecutionId(), parameterValue.getParameterId(), principalUser, savedCorrection, parameterCorrectionRecallRequest.getReason());
    return correctionMapper.toDto(correction);
  }

  @Override
  public Page<CorrectionDto> getAllCorrections(Long userId, Long useCaseId, String status, String parameterName, String processName, Long jobId, Long initiatedBy, Pageable pageable) throws JsonProcessingException {
    log.info("[getAllCorrections] Request to get all corrections for status: {}, jobId: {}", status, jobId);
    int limit = pageable.getPageSize();
    int offset = (int) pageable.getOffset();
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    Long facilityId = principalUser.getCurrentFacilityId();
    List<CorrectionListViewProjection> corrections = correctionRepository.getAllCorrections(userId, facilityId, useCaseId, status, parameterName, processName, jobId, initiatedBy, limit, offset);
    List<CorrectionMediaMapping> oldCorrectionMediaMappingList = correctionMediaMappingRepository.findAllByCorrectionIdInAndIsOldMedia(corrections.stream().map(c -> Long.parseLong(c.getId())).collect(Collectors.toSet()), true);
    List<CorrectionMediaMapping> newCorrectionMediaMappingList = correctionMediaMappingRepository.findAllByCorrectionIdInAndIsOldMediaAndArchived(corrections.stream().map(c -> Long.parseLong(c.getId())).collect(Collectors.toSet()), false, false);
    Map<Long, List<Media>> oldCorrectionIdMediaListMap = oldCorrectionMediaMappingList.stream().collect(Collectors.groupingBy(CorrectionMediaMapping::getCorrectionId, Collectors.mapping(CorrectionMediaMapping::getMedia, Collectors.toList())));
    Map<Long, List<Media>> newCorrectionIdMediaListMap = newCorrectionMediaMappingList.stream().collect(Collectors.groupingBy(CorrectionMediaMapping::getCorrectionId, Collectors.mapping(CorrectionMediaMapping::getMedia, Collectors.toList())));
    Set<Long> correctionIdSet = corrections.stream().map(correctionListViewProjection -> Long.parseLong(correctionListViewProjection.getId()))
      .collect(Collectors.toSet());
    List<Corrector> correctorList = correctorRepository.findAllByCorrectionIdIn(correctionIdSet);
    Map<Long, List<CorrectorDto>> correctorIdMap = correctorList.stream()
      .collect(Collectors.groupingBy(Corrector::getCorrectionId, Collectors.mapping(correctorMapper::toDto, Collectors.toList())));
    List<Reviewer> reviewerList = reviewerRepository.findAllByCorrectionIdIn(correctionIdSet);
    Map<Long, List<ReviewerDto>> reviewerIdMap = reviewerList.stream()
      .collect(Collectors.groupingBy(Reviewer::getCorrectionId, Collectors.mapping(reviewerMapper::toDto, Collectors.toList())));
    Map<Long, CorrectionListViewProjection> correctionListViewProjectionMap = corrections.stream()
      .collect(Collectors.toMap(correctionListViewProjection -> Long.parseLong(correctionListViewProjection.getId()), correctionListViewProjection -> correctionListViewProjection));
    List<CorrectionDto> correctionDtoList = correctionMapper.toDtoList(correctionListViewProjectionMap, correctorIdMap, reviewerIdMap, oldCorrectionIdMediaListMap, newCorrectionIdMediaListMap);
    long resultCount = correctionRepository.getAllCorrectionsCount(userId, facilityId, useCaseId, status, parameterName, processName, jobId, initiatedBy);
    return new PageImpl<>(correctionDtoList, PageRequest.of(offset / limit, limit), resultCount);
  }


  //  TODO: handle for repeated initiation at the same time
  private void validateCorrectionStatusFlow(State.Correction activeStatus, String parameterId, State.Correction latestStatus) throws StreemException {
    Set<State.Correction> validNextStates = new HashSet<>();

    switch (activeStatus) {
      case INITIATED -> validNextStates.add(State.Correction.CORRECTED);
      case CORRECTED -> {
        validNextStates.add(State.Correction.ACCEPTED);
        validNextStates.add(State.Correction.REJECTED);
      }
      case ACCEPTED, REJECTED -> validNextStates.add(State.Correction.INITIATED);
      default -> {
      }
    }

    if (!validNextStates.contains(latestStatus)) {
      ValidationUtils.invalidate(parameterId, ErrorCode.CORRECTION_ALREADY_EXECUTED);
    }
  }

  private void initiateMediaParameterCorrection(Correction correction, ParameterValue parameterValue, User principalUser) {
    List<Media> parameterValueOldMedias = parameterValue.getMedias().stream()
      .filter(parameterValueMediaMapping -> !parameterValueMediaMapping.isArchived())
      .map(ParameterValueMediaMapping::getMedia)
      .toList();
    List<CorrectionMediaMapping> correctionMediaMappingList = new ArrayList<>();
    for (Media media : parameterValueOldMedias) {
      CorrectionMediaMapping correctionMediaMapping = new CorrectionMediaMapping();
      correctionMediaMapping.setCorrection(correction);
      correctionMediaMapping.setCorrectionId(correction.getId());
      correctionMediaMapping.setOldMedia(true);
      correctionMediaMapping.setParameterValue(parameterValue);
      correctionMediaMapping.setMedia(media);
      correctionMediaMapping.setCreatedAt(DateTimeUtils.now());
      correctionMediaMapping.setCreatedBy(principalUser);
      correctionMediaMapping.setModifiedAt(DateTimeUtils.now());
      correctionMediaMapping.setModifiedBy(principalUser);
      correctionMediaMappingList.add(correctionMediaMapping);
    }
    correctionMediaMappingRepository.saveAll(correctionMediaMappingList);
  }

  private void setCorrectionValuesByParameterType(Correction correction, ParameterValue parameterValue, Parameter parameter, User principalUser, String value, JsonNode choice, boolean isSetNewValues, List<Media> correctedMediaList) {
    Type.Parameter parameterType = parameter.getType();
    switch (parameterType) {
      case DATE, DATE_TIME, NUMBER, SHOULD_BE, SINGLE_LINE, MULTI_LINE -> {
        if (isSetNewValues) {
          correction.setNewValue(value);
        } else {
          correction.setOldValue(value);
        }
      }
      case MEDIA, FILE_UPLOAD, SIGNATURE -> {
        if (!isSetNewValues) {
          initiateMediaParameterCorrection(correction, parameterValue, principalUser);
        } else {
          //Media Name and Description are null update that first.
          List<Long> mediaIdsToRemove = new ArrayList<>();
          for (Media correctedMedia : correctedMediaList) {
            mediaRepository.updateByMediaId(correctedMedia.getId(), correctedMedia.getName(), correctedMedia.getDescription());
            if (correctedMedia.isArchived()) {
              correctionMediaMappingRepository.updateArchiveStatusByMediaIdAndCorrectionId(correction.getId(), correctedMedia.getId(), true);
              mediaIdsToRemove.add(correctedMedia.getId());
            }
          }
          correctedMediaList.removeIf(media -> mediaIdsToRemove.contains(media.getId()));
          Set<Long> mediaIds = correctedMediaList.stream()
            .map(Media::getId)
            .collect(Collectors.toSet());
          List<Media> mediaList = mediaRepository.findAll(mediaIds);
          List<CorrectionMediaMapping> correctionMediaMappingList = new ArrayList<>();

          for (Media media : mediaList) {
            CorrectionMediaMapping correctionMediaMapping = new CorrectionMediaMapping();
            correctionMediaMapping.setCorrection(correction);
            correctionMediaMapping.setCorrectionId(correction.getId());
            correctionMediaMapping.setParameterValue(parameterValue);
            correctionMediaMapping.setMedia(media);
            correctionMediaMapping.setCreatedAt(DateTimeUtils.now());
            correctionMediaMapping.setCreatedBy(principalUser);
            correctionMediaMapping.setModifiedAt(DateTimeUtils.now());
            correctionMediaMapping.setModifiedBy(principalUser);
            correctionMediaMappingList.add(correctionMediaMapping);
          }
          correctionMediaMappingRepository.saveAll(correctionMediaMappingList);
        }
      }
      case MULTISELECT, RESOURCE, MULTI_RESOURCE, SINGLE_SELECT, YES_NO -> {
        if (isSetNewValues) {
          correction.setNewChoices(choice);
        } else {
          correction.setOldChoices(choice);
        }
      }
      default -> {
      }
    }
  }

  private void setParameterValueOnApprovalCorrection(Type.Parameter parameterType, Correction correction, ParameterValue parameterValue, User principalUser) {
    switch (parameterType) {
      case DATE, DATE_TIME, NUMBER, SHOULD_BE, SINGLE_LINE, MULTI_LINE -> {
        parameterValue.setValue(correction.getNewValue());
      }
      case MEDIA, FILE_UPLOAD, SIGNATURE -> {
        List<CorrectionMediaMapping> CorrectionNewMediaMappingList = correctionMediaMappingRepository.findByCorrectionIdAndIsOldMedia(correction.getId(), false);
        List<CorrectionMediaMapping> CorrecitonOldMediaMappingList = correctionMediaMappingRepository.findByCorrectionIdAndIsOldMedia(correction.getId(), true);
        //For Old Medias Which has Been archived
        for (CorrectionMediaMapping correctionMedia : CorrecitonOldMediaMappingList) {
          if (correctionMedia.isArchived()) {
            ParameterValueMediaMapping parameterValueMediaMapping = parameterValueMediaRepository.findMediaByParameterValueIdAndMediaId(parameterValue.getId(), correctionMedia.getMedia().getId());
            parameterValueMediaMapping.setArchived(correctionMedia.isArchived());
            parameterValueMediaMapping.setModifiedAt(DateTimeUtils.now());
            parameterValueMediaMapping.setModifiedBy(principalUser);
            parameterValueMediaRepository.save(parameterValueMediaMapping);
          }
        }
        for (CorrectionMediaMapping correctionMedia : CorrectionNewMediaMappingList) {
          parameterValue.addMedia(correctionMedia.getMedia(), principalUser);
        }
      }
      case MULTISELECT, RESOURCE, MULTI_RESOURCE, SINGLE_SELECT, YES_NO -> {
        parameterValue.setChoices(correction.getNewChoices());
      }
      default -> {
      }
    }
  }

  private void validateIfAllTaskExecutionsAreInCompletedStateOrNotStarted(Long taskId, Long jobId) throws StreemException {
    List<TaskExecutionView> taskExecutionViews = taskExecutionRepository.findAllTaskExecutionsNotInCompletedOrNotInStartedStatedByTaskIdAndJobId(taskId, jobId);
    if (!Utility.isEmpty(taskExecutionViews)) {
      List<Error> errorList = new ArrayList<>();
      for (TaskExecutionView taskExecutionView : taskExecutionViews) {
        errorList.add(Error.builder()
          .id(taskExecutionView.getId().toString())
          .code(ErrorCode.TASK_EXECUTION_NOT_IN_COMPLETED_STATE.getCode())
          .message(ErrorCode.TASK_EXECUTION_NOT_IN_COMPLETED_STATE.getDescription())
          .build());
      }
      ValidationUtils.invalidate(ErrorMessage.TASK_EXECUTION__IN_STARTED_STATE, errorList);
    }
  }

  private void validateIfAllParametersInTaskAreInCompletedState(Long taskExecutionId) throws StreemException {
    boolean isCorrectionPending = parameterValueRepository.areAllInitiatedParametersCompletedWithCorrection(taskExecutionId);
    if (isCorrectionPending) {
      ValidationUtils.invalidate(taskExecutionId, ErrorCode.CORRECTION_CANNOT_DISABLE);
    }
  }

  private void validateCorrectorReviewerAtInitiate(CorrectionCorrectorsReviewersRequest correctors, CorrectionCorrectorsReviewersRequest reviewers, ParameterValue parameterValue) throws StreemException {
    Set<Long> correctorUserIds = new HashSet<>(correctors.getUserId());
    Set<Long> correctorUserGroupIds = new HashSet<>(correctors.getUserGroupId());
    Set<Long> reviewerUserIds = new HashSet<>(reviewers.getUserId());
    Set<Long> reviewerUserGroupIds = new HashSet<>(reviewers.getUserGroupId());

    boolean userIdMatch = !Collections.disjoint(correctorUserIds, reviewerUserIds);
    boolean userGroupIdMatch = !Collections.disjoint(correctorUserGroupIds, reviewerUserGroupIds);

    if (userIdMatch) {
      ValidationUtils.invalidate(parameterValue.getParameterId(), ErrorCode.CORRECTION_CORRECTOR_REVIEWER_MATCHED);
    }

    if (userGroupIdMatch) {
      ValidationUtils.invalidate(parameterValue.getParameterId(), ErrorCode.CORRECTION_CORRECTOR_REVIEWER_MATCHED);
    }

    if (!Utility.isEmpty(correctors)) {
      Set<Long> userIds = correctors.getUserId();
      Set<Long> userGroupIds = correctors.getUserGroupId();

      if (userIds.isEmpty() && userGroupIds.isEmpty()) {
        ValidationUtils.invalidate(parameterValue.getParameterId(), ErrorCode.CORRECTION_CORRECTOR_REVIEWER_USER_LIST);
      }
    }

    if (!Utility.isEmpty(reviewers)) {
      Set<Long> userIds = reviewers.getUserId();
      Set<Long> userGroupIds = reviewers.getUserGroupId();

      if (userIds.isEmpty() && userGroupIds.isEmpty()) {
        ValidationUtils.invalidate(parameterValue.getParameterId(), ErrorCode.CORRECTION_CORRECTOR_REVIEWER_USER_LIST);
      }
    }
  }

  private NotificationParameterValueDto parameterValueToNotificationDto(ParameterValue parameterValue, Long organisationId) {
    NotificationParameterValueDto notificationParameterValueDto = new NotificationParameterValueDto();
    notificationParameterValueDto.setJobId(parameterValue.getJobId());
    notificationParameterValueDto.setParameterId(parameterValue.getParameterId());
    notificationParameterValueDto.setTaskExecutionId(parameterValue.getTaskExecutionId().toString());
    notificationParameterValueDto.setTaskName(parameterValue.getParameter().getTask().getName());
    notificationParameterValueDto.setJobCode(parameterValue.getJob().getCode());
    notificationParameterValueDto.setParameterLabel(parameterValue.getParameter().getLabel());
    notificationParameterValueDto.setCheckListName(parameterValue.getJob().getChecklist().getName());
    notificationParameterValueDto.setOrganizationId(organisationId);
    return notificationParameterValueDto;
  }
}
