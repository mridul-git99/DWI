package com.leucine.streem.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.leucine.streem.constant.State;
import com.leucine.streem.constant.Type;
import com.leucine.streem.dto.*;
import com.leucine.streem.dto.mapper.IParameterExceptionMapper;
import com.leucine.streem.dto.mapper.IParameterExceptionReviewerMapper;
import com.leucine.streem.dto.mapper.IParameterMapper;
import com.leucine.streem.dto.mapper.IUserMapper;
import com.leucine.streem.dto.request.*;
import com.leucine.streem.dto.response.ErrorCode;
import com.leucine.streem.exception.ExceptionType;
import com.leucine.streem.exception.ParameterExecutionException;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.leucine.streem.constant.Type.CHOICE_PARAMETER_TYPES;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParameterExceptionService implements IParameterExceptionService {
  private final IUserRepository userRepository;
  private final IParameterValueRepository parameterValueRepository;
  private final IParameterExecutionService parameterExecutionService;
  private final ICodeService codeService;
  private final IFacilityRepository facilityRepository;
  private final IParameterExceptionRepository parameterExceptionRepository;
  private final IUserGroupRepository userGroupRepository;
  private final IParameterExceptionReviewerRepository parameterExceptionReviewerRepository;
  private final IParameterExceptionMapper parameterExceptionMapper;
  private final IParameterExceptionReviewerMapper parameterExceptionReviewerMapper;
  private final IJobAuditService jobAuditService;
  private final IJobService jobService;
  private final IParameterExecutionHandler parameterExecutionHandler;
  private final IJobLogService jobLogService;
  private final IUserMapper userMapper;
  private final INotificationService notificationService;
  private final IRulesExecutionService rulesExecutionService;
  private final IParameterMapper parameterMapper;
  private final IParameterRepository parameterRepository;

  @Override
  @Transactional(rollbackFor = Exception.class)
  public ParameterExceptionDto initiateParameterException(Long parameterExecutionId, ParameterExceptionInitiatorRequest parameterExceptionInitiatorRequest) throws StreemException, ResourceNotFoundException, JsonProcessingException {
    log.info("[initiateParameterException] Request to initiate parameter exception ,parameterExecutionId:{} parameterExceptionInitiatorRequest: {}", parameterExecutionId, parameterExceptionInitiatorRequest);
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    User principalUserEntity = userRepository.findById(principalUser.getId())
      .orElseThrow(() -> new ResourceNotFoundException(principalUser.getId(), ErrorCode.USER_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));

    ParameterValue parameterValue = parameterValueRepository.findById(parameterExecutionId)
      .orElseThrow(() -> new ResourceNotFoundException(parameterExecutionId, ErrorCode.PARAMETER_VALUE_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));
    Parameter parameter = parameterRepository.getReferenceById(parameterValue.getParameterId());

    boolean isTaskParameter = parameter.getTargetEntityType().equals(Type.ParameterTargetEntityType.TASK);
    if (isTaskParameter) {
      parameterExecutionService.validateIfUserIsAssignedToExecuteParameter(parameterValue.getTaskExecutionId(), principalUser.getId());
    }else if(parameterValue.getJob().getState() == State.Job.IN_PROGRESS){
      ValidationUtils.invalidate(parameterValue.getParameterId(), ErrorCode.PARAMETER_EXCEPTION_ALREADY_EXECUTED);
    }
    Facility facility = facilityRepository.findById(principalUser.getCurrentFacilityId()).get();


    String initiatorReason = parameterExceptionInitiatorRequest.getInitiatorReason();
    JsonNode choices = null;
    String exceptionDeviatedValue = null;
    if (CHOICE_PARAMETER_TYPES.contains(parameter.getType())) {
      choices = parameterExceptionInitiatorRequest.getChoices();
      if (Utility.isEmpty(choices)) {
        ValidationUtils.invalidate(parameterValue.getParameterId(), ErrorCode.PARAMETER_EXCEPTION_CHOICE_MISSING);
      }
    } else {
      exceptionDeviatedValue = parameterExceptionInitiatorRequest.getValue();
      if (Utility.isEmpty(exceptionDeviatedValue)) {
        ValidationUtils.invalidate(parameterValue.getParameterId(), ErrorCode.PARAMETER_EXCEPTION_VALUE);
      }
    }    if (Utility.isEmpty(initiatorReason)) {
      ValidationUtils.invalidate(parameterValue.getParameterId(), ErrorCode.PARAMETER_EXCEPTION_INITIATOR_REASON);
    }

    ParameterExceptionReviewersRequest reviewers = parameterExceptionInitiatorRequest.getReviewers();

    ParameterException parameterException = new ParameterException();
    parameterException.setCode(codeService.getCode(Type.EntityType.PARAMETER_EXCEPTION, principalUser.getOrganisationId()));
    parameterException.setParameterValue(parameterValue);
    parameterException.setTaskExecution(parameterValue.getTaskExecution());
    parameterException.setJob(parameterValue.getJob());
    parameterException.setFacility(facility);
    parameterException.setInitiatorsReason(initiatorReason);
    parameterException.setStatus(State.ParameterException.INITIATED);
    parameterException.setRuleId(parameterExceptionInitiatorRequest.getRuleId());
    parameterException.setCreatedBy(principalUserEntity);
    parameterException.setModifiedBy(principalUserEntity);
    parameterException.setCreatedAt(DateTimeUtils.now());
    parameterException.setModifiedAt(DateTimeUtils.now());
    if (CHOICE_PARAMETER_TYPES.contains(parameter.getType())) {
      parameterException.setChoices(choices);
    } else {
      parameterException.setValue(exceptionDeviatedValue);
    }
    parameterException.setPreviousState(parameterValue.getState());
    ParameterException savedParameterException = parameterExceptionRepository.save(parameterException);

    List<User> reviewerUsersList = userRepository.findAllById(reviewers.getUserId());
    Map<Long, User> reviewersUserMap = reviewerUsersList.stream().collect(Collectors.toMap(BaseEntity::getId, Function.identity()));
    List<ParameterExceptionReviewer> reviewerList = new ArrayList<>();
    for (Long userId : reviewers.getUserId()) {
      User user = reviewersUserMap.get(userId);
      ParameterExceptionReviewer reviewer = new ParameterExceptionReviewer();
      reviewer.setExceptions(savedParameterException);
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
      ParameterExceptionReviewer reviewer = new ParameterExceptionReviewer();
      reviewer.setExceptions(savedParameterException);
      reviewer.setUserGroup(userGroup);
      reviewer.setCreatedBy(principalUserEntity);
      reviewer.setModifiedBy(principalUserEntity);
      reviewer.setCreatedAt(DateTimeUtils.now());
      reviewer.setModifiedAt(
        DateTimeUtils.now()
      );
      reviewerList.add(reviewer);
    }

    parameterExceptionReviewerRepository.saveAll(reviewerList);

    parameterValue.setState(determineParameterValueState(parameterExecutionId));
    parameterValue.setHasExceptions(true);
    parameterValueRepository.save(parameterValue);
    List<ParameterExceptionReviewerDto> reviewerDtos = parameterExceptionReviewerMapper.toDto(reviewerList);
    ParameterExceptionDto parameterExceptionDto = parameterExceptionMapper.toDto(savedParameterException);
    parameterExceptionDto.setReviewer(reviewerDtos);
    NotificationParameterExceptionDto notificationParameterExceptionDto = getNotificationParameterExceptionDto(parameterValue, principalUser, initiatorReason);
    if (isTaskParameter) {
      jobAuditService.initiateParameterException(parameterValue.getJobId(), parameterValue.getTaskExecutionId(), parameterValue.getParameterId(), principalUser, savedParameterException, initiatorReason, exceptionDeviatedValue);
    }else{
      jobAuditService.initiateCjfParameterException(parameterValue.getJobId(), parameterValue.getParameterId(), principalUser, savedParameterException, initiatorReason, exceptionDeviatedValue);
    }
    notificationService.parameterExceptionRequested(notificationParameterExceptionDto, reviewerList);
    return parameterExceptionDto;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public ParameterExceptionDto approveParameterException(Long parameterExecutionId, ParameterExceptionApproveRejectRequest parameterExceptionApproveRejectRequest) throws ResourceNotFoundException, StreemException, IOException, ParameterExecutionException {
    log.info("[approveParameterException] Request to approve exception parameterExecutionId:{},parameterExceptionApproveRejectRequest,:{}", parameterExecutionId, parameterExceptionApproveRejectRequest);
    ParameterDto parameterDto;
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    User principalUserEntity = userRepository.getOne(principalUser.getId());
    ParameterValue parameterValue = parameterValueRepository.findById(parameterExecutionId)
      .orElseThrow(() -> new ResourceNotFoundException(parameterExecutionId, ErrorCode.PARAMETER_VALUE_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));
    jobService.validateIfUserIsAssignedToExecuteJob(parameterValue.getJobId(), principalUser.getId());
    String reviewerReason = parameterExceptionApproveRejectRequest.getReviewerReason();
    Long exceptionId = parameterExceptionApproveRejectRequest.getExceptionId();
    boolean isTaskParameter = parameterValue.getParameter().getTargetEntityType().equals(Type.ParameterTargetEntityType.TASK);

    if (Utility.isEmpty(reviewerReason)) {
      ValidationUtils.invalidate(parameterValue.getParameterId(), ErrorCode.PARAMETER_EXCEPTION_REVIEWER_REASON);
    }
    List<ParameterExceptionReviewer> exceptionReviewerList = parameterExceptionReviewerRepository.findByExceptionId(exceptionId);
    boolean isUserInReviewerList = exceptionReviewerList.stream()
      .map(ParameterExceptionReviewer::getUser)
      .map(User::getId)
      .anyMatch(userId -> userId.equals(principalUser.getId()));

    if (!isUserInReviewerList) {
      ValidationUtils.invalidate(parameterValue.getParameterId(), ErrorCode.PARAMETER_EXCEPTION_REVIEWER_DID_NOT_MATCH);
    }
    Parameter parameter = parameterValue.getParameter();
    ParameterException currentException = parameterExceptionRepository.findById(exceptionId)
      .orElseThrow(() -> new ResourceNotFoundException(exceptionId, ErrorCode.PARAMETER_EXCEPTION_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));
    validateExceptionStatusFlow(currentException.getStatus(), String.valueOf(parameterValue.getParameterId()), State.ParameterException.ACCEPTED);
    currentException.setReviewersReason(reviewerReason);
    currentException.setModifiedAt(DateTimeUtils.now());
    currentException.setModifiedBy(principalUserEntity);
    currentException.setStatus(State.ParameterException.ACCEPTED);
    parameterExceptionRepository.save(currentException);

    for (ParameterExceptionReviewer reviewer : exceptionReviewerList) {
      if (reviewer.getUser().getId().equals(principalUser.getId())) {
        reviewer.setActionPerformed(true);
        reviewer.setModifiedAt(DateTimeUtils.now());
        parameterExceptionReviewerRepository.save(reviewer);
        break;
      }
    }

    if (CHOICE_PARAMETER_TYPES.contains(parameter.getType())) {
      JsonNode choices = currentException.getChoices();
      boolean isResourceOrMultiResourceParameter = parameter.getType() == Type.Parameter.RESOURCE || parameter.getType() == Type.Parameter.MULTI_RESOURCE;
      if(isResourceOrMultiResourceParameter){
        if (choices instanceof ArrayNode arrayNode) {
          // Directly iterate and add the key for each object node
          arrayNode.forEach(choice -> {
            if (choice instanceof ObjectNode objectNode) {
              objectNode.put("exceptionApproved", true);
            }
          });
        }
      }
      parameterValue.setChoices(choices);
    } else {
      parameterValue.setValue(currentException.getValue());
    }
    parameterValue.setModifiedAt(currentException.getCreatedAt());
    parameterValue.setModifiedBy(currentException.getCreatedBy());
    parameterValue.setState(determineParameterValueState(parameterExecutionId));
    parameterValueRepository.save(parameterValue);

    List<ParameterExceptionReviewerDto> exceptionReviewerDtos = parameterExceptionReviewerMapper.toDto(exceptionReviewerList);
    ParameterExceptionDto parameterExceptionDto = parameterExceptionMapper.toDto(currentException);
    parameterExceptionDto.setReviewer(exceptionReviewerDtos);
    UserAuditDto userBasicInfoDto = userMapper.toUserAuditDto(principalUserEntity);
    NotificationParameterExceptionDto notificationParameterExceptionDto = getNotificationParameterExceptionDto(parameterValue, principalUser, null);

    jobLogService.updateJobLog(parameterValue.getJobId(), parameter.getId(), parameter.getType(), reviewerReason, parameter.getLabel(), Type.JobLogTriggerType.PARAMETER_VALUE, userBasicInfoDto);
    if (isTaskParameter) {
      jobAuditService.approveParameterException(parameterValue.getJobId(), parameterValue.getTaskExecutionId(), parameterValue.getParameterId(), principalUser, currentException, reviewerReason);
    }else{
      jobAuditService.approveCjfParameterException(parameterValue.getJobId(), parameterValue.getParameterId(), principalUser, currentException, reviewerReason);
    }

    approveTriggerAutoInitialize(parameterValue, parameter, parameterExecutionId, parameterValue.getJobId());
    notificationService.approveParameterException(currentException, notificationParameterExceptionDto, exceptionReviewerList);
    return parameterExceptionDto;
  }

  @Override
  public ParameterExceptionDto rejectParameterException(Long parameterExecutionId, ParameterExceptionApproveRejectRequest parameterExceptionApproveRejectRequest) throws StreemException, ResourceNotFoundException {
    log.info("[rejectParameterException] Request to reject exception parameterExecutionId:{},parameterExceptionApproveRejectRequest,:{}", parameterExecutionId, parameterExceptionApproveRejectRequest);
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    User principalUserEntity = userRepository.getOne(principalUser.getId());
    ParameterValue parameterValue = parameterValueRepository.findById(parameterExecutionId)
      .orElseThrow(() -> new ResourceNotFoundException(parameterExecutionId, ErrorCode.PARAMETER_VALUE_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));
    jobService.validateIfUserIsAssignedToExecuteJob(parameterValue.getJobId(), principalUser.getId());
    String reviewerReason = parameterExceptionApproveRejectRequest.getReviewerReason();
    Long exceptionId = parameterExceptionApproveRejectRequest.getExceptionId();
    boolean isTaskParameter = parameterValue.getParameter().getTargetEntityType().equals(Type.ParameterTargetEntityType.TASK);

    if (Utility.isEmpty(reviewerReason)) {
      ValidationUtils.invalidate(parameterValue.getParameterId(), ErrorCode.PARAMETER_EXCEPTION_REVIEWER_REASON);
    }
    List<ParameterExceptionReviewer> exceptionReviewerList = parameterExceptionReviewerRepository.findByExceptionId(exceptionId);
    boolean isUserInReviewerList = exceptionReviewerList.stream()
      .map(ParameterExceptionReviewer::getUser)
      .map(User::getId)
      .anyMatch(userId -> userId.equals(principalUser.getId()));

    if (!isUserInReviewerList) {
      ValidationUtils.invalidate(parameterValue.getParameterId(), ErrorCode.PARAMETER_EXCEPTION_REVIEWER_DID_NOT_MATCH);
    }
    ParameterException currentException = parameterExceptionRepository.findById(exceptionId)
      .orElseThrow(() -> new ResourceNotFoundException(exceptionId, ErrorCode.PARAMETER_EXCEPTION_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));
    validateExceptionStatusFlow(currentException.getStatus(), String.valueOf(parameterValue.getParameterId()), State.ParameterException.REJECTED);
    currentException.setReviewersReason(reviewerReason);
    currentException.setModifiedAt(DateTimeUtils.now());
    currentException.setModifiedBy(principalUserEntity);
    currentException.setStatus(State.ParameterException.REJECTED);
    parameterExceptionRepository.save(currentException);

    for (ParameterExceptionReviewer reviewer : exceptionReviewerList) {
      if (reviewer.getUser().getId().equals(principalUser.getId())) {
        reviewer.setActionPerformed(true);
        reviewer.setModifiedAt(DateTimeUtils.now());
        parameterExceptionReviewerRepository.save(reviewer);
        break;
      }
    }

    parameterValue.setState(determineParameterValueState(parameterExecutionId));
    parameterValueRepository.save(parameterValue);
    ParameterExceptionDto parameterExceptionDto = parameterExceptionMapper.toDto(currentException);
    List<ParameterExceptionReviewerDto> exceptionReviewerDtos = parameterExceptionReviewerMapper.toDto(exceptionReviewerList);
    parameterExceptionDto.setReviewer(exceptionReviewerDtos);
    NotificationParameterExceptionDto notificationParameterExceptionDto = getNotificationParameterExceptionDto(parameterValue, principalUser, null);

    if (isTaskParameter) {
      jobAuditService.rejectParameterException(parameterValue.getJobId(), parameterValue.getTaskExecutionId(), parameterValue.getParameterId(), principalUser, currentException, reviewerReason);
    }else{
      jobAuditService.rejectCjfParameterException(parameterValue.getJobId(), parameterValue.getParameterId(), principalUser, currentException, reviewerReason);
    }
    notificationService.rejectParameterException(currentException, notificationParameterExceptionDto, exceptionReviewerList);
    return parameterExceptionDto;
  }

  @Override
  public ParameterExceptionDto autoAcceptParameterException(Long parameterExecutionId, ParameterExceptionAutoAcceptRequest parameterExceptionAutoAcceptRequest) throws StreemException, ResourceNotFoundException, IOException, ParameterExecutionException {
    log.info("[autoAcceptParameterException] Request to autoAccept Parameter Exception parameterExecutionId:{},parameterExceptionAutoAcceptRequest,:{}", parameterExecutionId, parameterExceptionAutoAcceptRequest);
    ParameterDto parameterDto;
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    User principalUserEntity = userRepository.getOne(principalUser.getId());
    ParameterValue parameterValue = parameterValueRepository.findById(parameterExecutionId)
      .orElseThrow(() -> new ResourceNotFoundException(parameterExecutionId, ErrorCode.PARAMETER_VALUE_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));

    Parameter parameter = parameterRepository.getReferenceById(parameterValue.getParameterId());
    boolean isTaskParameter = parameter.getTargetEntityType().equals(Type.ParameterTargetEntityType.TASK);

    if(isTaskParameter){
      parameterExecutionService.validateIfUserIsAssignedToExecuteParameter(parameterValue.getTaskExecutionId(), principalUser.getId());
    }else if(parameterValue.getJob().getState() == State.Job.IN_PROGRESS){
      ValidationUtils.invalidate(parameterValue.getParameterId(), ErrorCode.PARAMETER_EXCEPTION_ALREADY_EXECUTED);
    }
    Facility facility = facilityRepository.findById(principalUser.getCurrentFacilityId()).get();

    String reason = parameterExceptionAutoAcceptRequest.getReason();
    JsonNode choices = null;
    String exceptionDeviatedValue = null;
    if (CHOICE_PARAMETER_TYPES.contains(parameter.getType())) {
      choices = parameterExceptionAutoAcceptRequest.getChoices();
      if (Utility.isEmpty(choices)) {
        ValidationUtils.invalidate(parameterValue.getParameterId(), ErrorCode.PARAMETER_EXCEPTION_CHOICE_MISSING);
      }
    } else {
      exceptionDeviatedValue = parameterExceptionAutoAcceptRequest.getValue();
      if (Utility.isEmpty(exceptionDeviatedValue)) {
        ValidationUtils.invalidate(parameterValue.getParameterId(), ErrorCode.PARAMETER_EXCEPTION_VALUE);
      }
    }

    if (Utility.isEmpty(reason)) {
      ValidationUtils.invalidate(parameterValue.getParameterId(), ErrorCode.PARAMETER_EXCEPTION_REASON);
    }

    ParameterException parameterException = new ParameterException();
    parameterException.setCode(codeService.getCode(Type.EntityType.PARAMETER_EXCEPTION, principalUser.getOrganisationId()));
    parameterException.setParameterValue(parameterValue);
    parameterException.setTaskExecution(parameterValue.getTaskExecution());
    parameterException.setJob(parameterValue.getJob());
    parameterException.setFacility(facility);
    parameterException.setReason(reason);
    parameterException.setStatus(State.ParameterException.AUTO_ACCEPTED);
    parameterException.setCreatedBy(principalUserEntity);
    parameterException.setModifiedBy(principalUserEntity);
    parameterException.setCreatedAt(DateTimeUtils.now());
    parameterException.setModifiedAt(DateTimeUtils.now());
    parameterException.setRuleId(parameterExceptionAutoAcceptRequest.getRuleId());
    if (CHOICE_PARAMETER_TYPES.contains(parameter.getType())) {
      boolean isResourceOrMultiResourceParameter = parameter.getType() == Type.Parameter.RESOURCE || parameter.getType() == Type.Parameter.MULTI_RESOURCE;
      if(isResourceOrMultiResourceParameter){
        if (choices instanceof ArrayNode arrayNode) {
          // Directly iterate and add the key for each object node
          arrayNode.forEach(choice -> {
            if (choice instanceof ObjectNode objectNode) {
              objectNode.put("exceptionApproved", true);
            }
          });
        }
      }
      parameterException.setChoices(choices);
    } else {
      parameterException.setValue(exceptionDeviatedValue);
    }
    parameterException.setPreviousState(parameterValue.getState());
    ParameterException savedParameterException = parameterExceptionRepository.save(parameterException);

    parameterValue.setHasExceptions(true);
    if (CHOICE_PARAMETER_TYPES.contains(parameter.getType())) {
      parameterValue.setChoices(savedParameterException.getChoices());
    } else {
      parameterValue.setValue(savedParameterException.getValue());
    }
    parameterValue.setModifiedAt(savedParameterException.getCreatedAt());
    parameterValue.setModifiedBy(savedParameterException.getCreatedBy());
    parameterValue.setState(determineParameterValueState(parameterExecutionId));
    parameterValueRepository.save(parameterValue);

    ParameterExceptionDto parameterExceptionDto = parameterExceptionMapper.toDto(savedParameterException);
    UserAuditDto userBasicInfoDto = userMapper.toUserAuditDto(principalUserEntity);

    jobLogService.updateJobLog(parameterValue.getJobId(), parameter.getId(), parameter.getType(), reason, parameter.getLabel(), Type.JobLogTriggerType.PARAMETER_VALUE, userBasicInfoDto);
    if(isTaskParameter){
      jobAuditService.autoAcceptParameterException(parameterValue.getJobId(), parameterValue.getTaskExecutionId(), parameterValue.getParameterId(), principalUser, savedParameterException, reason);
    }else{
      jobAuditService.autoAcceptCjfParameterException(parameterValue.getJobId(), parameterValue.getParameterId(), principalUser, savedParameterException, reason);
    }

    approveTriggerAutoInitialize(parameterValue, parameter, parameterExecutionId, parameterValue.getJobId());
    return parameterExceptionDto;
  }

  private void validateExceptionStatusFlow(State.ParameterException activeStatus, String parameterId, State.ParameterException latestStatus) throws StreemException {
    Set<State.ParameterException> validNextStates = new HashSet<>();

    switch (activeStatus) {
      case INITIATED -> {
        validNextStates.add(State.ParameterException.ACCEPTED);
        validNextStates.add(State.ParameterException.REJECTED);
      }
      case ACCEPTED, REJECTED -> validNextStates.add(State.ParameterException.INITIATED);
      default -> {
      }
    }

    if (!validNextStates.contains(latestStatus)) {
      ValidationUtils.invalidate(parameterId, ErrorCode.PARAMETER_EXCEPTION_ALREADY_EXECUTED);
    }
  }

  private void approveTriggerAutoInitialize(ParameterValue parameterValue, Parameter parameter, Long parameterExecutionId, Long jobId) throws StreemException, IOException, ResourceNotFoundException, ParameterExecutionException {
    if (parameterValue.getState() == State.ParameterExecution.EXECUTED) {
      ParameterExecuteRequest parameterExecuteRequest = new ParameterExecuteRequest();
      parameterExecuteRequest.setJobId(jobId);
      ParameterRequest parameterRequest = new ParameterRequest();
      parameterRequest.setData(parameter.getData());
      parameterRequest.setId(parameter.getId());
      parameterRequest.setLabel(parameter.getLabel());
      parameterExecuteRequest.setParameter(parameterRequest);
      parameterExecuteRequest.setReferencedParameterId(parameter.getId());
      parameterExecutionHandler.executeParameter(jobId, parameterExecutionId, parameterExecuteRequest, Type.JobLogTriggerType.PARAMETER_VALUE, true, false, false);
    }
  }

  private static NotificationParameterExceptionDto getNotificationParameterExceptionDto(ParameterValue parameterValue, PrincipalUser principalUser, String reason) {
    NotificationParameterExceptionDto notificationParameterExceptionDto = new NotificationParameterExceptionDto();
    notificationParameterExceptionDto.setParameterName(parameterValue.getParameter().getLabel());
    notificationParameterExceptionDto.setJobId(String.valueOf(parameterValue.getJobId()));
    notificationParameterExceptionDto.setJobCode(parameterValue.getJob().getCode());
    notificationParameterExceptionDto.setTaskExecutionId(String.valueOf(parameterValue.getTaskExecutionId()));
    if (!Utility.isEmpty(parameterValue.getTaskExecutionId())){
      notificationParameterExceptionDto.setTaskName(parameterValue.getParameter().getTask().getName());
    }
    notificationParameterExceptionDto.setProcessName(parameterValue.getJob().getChecklist().getName());
    notificationParameterExceptionDto.setOrganisationId(principalUser.getOrganisationId());
    notificationParameterExceptionDto.setInitiatorReason(reason);
    return notificationParameterExceptionDto;
  }

  @Override
  public List<ParameterExceptionRequest> bulkParameterException(BulkParameterExceptionRequest bulkParameterExceptionRequest) throws Exception {

    for (ParameterExceptionRequest parameterExceptionRequest : bulkParameterExceptionRequest.getExceptions()) {
      long parameterExecutionId = parameterExceptionRequest.getParameterExecutionId();

      if (!Utility.isEmpty(parameterExecutionId)) {
        for (ParameterExceptionInitiatorRequest initiatorRequest : parameterExceptionRequest.getParameterExceptionInitiatorRequest()) {
          initiateParameterException(parameterExecutionId, initiatorRequest);
        }
        for (ParameterExceptionAutoAcceptRequest autoAcceptRequest : parameterExceptionRequest.getParameterExceptionAutoAcceptRequest()) {
          autoAcceptParameterException(parameterExecutionId, autoAcceptRequest);
        }
      }
    }
    return bulkParameterExceptionRequest.getExceptions();
  }

  private State.ParameterExecution determineParameterValueState(Long parameterValueId) throws ResourceNotFoundException {
    List<ParameterException> exceptions = parameterExceptionRepository.findLatestException(parameterValueId);

    Set<State.ParameterException> exceptionStatuses = EnumSet.noneOf(State.ParameterException.class);

    for (ParameterException exception : exceptions) {
      exceptionStatuses.add(exception.getStatus());
    }

    boolean hasInitiated = exceptionStatuses.contains(State.ParameterException.INITIATED);
    boolean hasRejected = exceptionStatuses.contains(State.ParameterException.REJECTED);
    boolean areAllAcceptedOrAutoAccepted = exceptionStatuses.stream().allMatch(status -> status == State.ParameterException.ACCEPTED || status == State.ParameterException.AUTO_ACCEPTED);

    ParameterValue parameterValue = parameterValueRepository.findById(parameterValueId)
      .orElseThrow(() -> new ResourceNotFoundException(parameterValueId, ErrorCode.PARAMETER_VALUE_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));
    Parameter parameter = parameterRepository.findById(parameterValue.getParameterId())
      .orElseThrow(() -> new ResourceNotFoundException(parameterValue.getParameterId(), ErrorCode.PARAMETER_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));
    if (areAllAcceptedOrAutoAccepted) {
      parameterValue.setHasActiveException(false);
      parameterValueRepository.save(parameterValue);
      return (parameter.getVerificationType() == Type.VerificationType.NONE) ? State.ParameterExecution.EXECUTED : State.ParameterExecution.BEING_EXECUTED;
    }

    State.ParameterExecution finalState = null;
    if (hasRejected ) {
      finalState = State.ParameterExecution.BEING_EXECUTED;
    }

    if (hasInitiated) {
      finalState = State.ParameterExecution.PENDING_FOR_APPROVAL;
    }

    return finalState;
  }

  }
