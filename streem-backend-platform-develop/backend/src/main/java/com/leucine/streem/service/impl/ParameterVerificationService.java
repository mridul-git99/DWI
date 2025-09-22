package com.leucine.streem.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.leucine.streem.constant.Misc;
import com.leucine.streem.constant.State;
import com.leucine.streem.constant.Type;
import com.leucine.streem.dto.*;
import com.leucine.streem.dto.mapper.IParameterMapper;
import com.leucine.streem.dto.mapper.IParameterVerificationMapper;
import com.leucine.streem.dto.mapper.ITempParameterVerificationMapper;
import com.leucine.streem.dto.mapper.IUserMapper;
import com.leucine.streem.dto.projection.JobAssigneeView;
import com.leucine.streem.dto.projection.ParameterVerificationListViewProjection;
import com.leucine.streem.dto.projection.UserGroupView;
import com.leucine.streem.dto.request.*;
import com.leucine.streem.dto.response.ErrorCode;
import com.leucine.streem.dto.response.Response;
import com.leucine.streem.exception.ExceptionType;
import com.leucine.streem.exception.ParameterExecutionException;
import com.leucine.streem.exception.ResourceNotFoundException;
import com.leucine.streem.exception.StreemException;
import com.leucine.streem.handler.IParameterVerificationHandler;
import com.leucine.streem.handler.ParameterVerificationHandler;
import com.leucine.streem.model.*;
import com.leucine.streem.model.helper.PrincipalUser;
import com.leucine.streem.repository.*;
import com.leucine.streem.service.*;
import com.leucine.streem.util.DateTimeUtils;
import com.leucine.streem.util.JsonUtils;
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
import java.util.stream.Collectors;

import static com.leucine.streem.constant.Type.JobLogTriggerType.PARAMETER_VALUE;

//TODO: check if branching rules related changes are required
//TODO: After one cycle of self verify and peer verify is complete, wwe will create new entry"
@Slf4j
@Service
public class ParameterVerificationService implements IParameterVerificationService {
  private final IUserRepository userRepository;
  private final IJobRepository jobRepository;
  private final IParameterVerificationRepository parameterVerificationRepository;
  private final IParameterValueRepository parameterValueRepository;
  private final IParameterVerificationHandler parameterVerificationHandler;

  private final IJobAuditService jobAuditService;
  private final IParameterVerificationMapper parameterVerificationMapper;
  private final Map<State.ParameterVerification, Set<State.ParameterVerification>> stateMapSelf;
  private final Map<State.ParameterVerification, Set<State.ParameterVerification>> stateMapPeer;
  private final ITaskExecutionAssigneeRepository taskExecutionAssigneeRepository;
  private final IUserService userService;
  private final IParameterRepository parameterRepository;
  private final IJobLogService jobLogService;
  private final IUserMapper userMapper;
  private final IParameterExecutionValidationService parameterExecutionValidationService;
  private final ITempParameterValueRepository tempParameterValueRepository;
  private final ITempParameterVerificationRepository tempParameterVerificationRepository;
  private final ITempParameterVerificationMapper tempParameterVerificationMapper;
  private final INotificationService notificationService;
  private final ITaskExecutionRepository taskExecutionRepository;
  private final IUserGroupMemberRepository userGroupMemberRepository;
  private final ParameterExecutionHandler parameterExecutionHandler;
  private final IRulesExecutionService rulesExecutionService;
  private final IParameterMapper parameterMapper;
  private final IJwtService jwtService;



  public ParameterVerificationService(IUserRepository userRepository, IJobRepository jobRepository, IParameterVerificationRepository parameterVerificationRepository,
                                      IParameterValueRepository parameterValueRepository, ParameterVerificationHandler parameterVerificationHandler,
                                      IJobAuditService jobAuditService, IParameterVerificationMapper parameterVerificationMapper,
                                      ITaskExecutionAssigneeRepository taskExecutionAssigneeRepository,
                                      IUserService userService, IParameterRepository parameterRepository,
                                      IParameterExecutionValidationService parameterExecutionValidationService,
                                      ITempParameterValueRepository tempParameterValueRepository,
                                      ITempParameterVerificationRepository tempParameterVerificationRepository,
                                      ITempParameterVerificationMapper tempParameterVerificationMapper,
                                      IJobLogService jobLogService,
                                      IUserMapper userMapper, INotificationService notificationService, ITaskExecutionRepository taskExecutionRepository, IUserGroupMemberRepository userGroupMemberRepository, ParameterExecutionHandler parameterExecutionHandler, IRulesExecutionService rulesExecutionService, IParameterMapper parameterMapper, IJwtService jwtService) {
    this.userRepository = userRepository;
    this.jobRepository = jobRepository;
    this.parameterVerificationRepository = parameterVerificationRepository;
    this.parameterValueRepository = parameterValueRepository;
    this.parameterVerificationHandler = parameterVerificationHandler;
    this.jobAuditService = jobAuditService;
    this.parameterVerificationMapper = parameterVerificationMapper;
    this.taskExecutionAssigneeRepository = taskExecutionAssigneeRepository;
    this.userService = userService;
    this.parameterRepository = parameterRepository;
    this.jobLogService = jobLogService;
    this.userMapper = userMapper;
    this.parameterExecutionValidationService = parameterExecutionValidationService;
    this.tempParameterValueRepository = tempParameterValueRepository;
    this.tempParameterVerificationRepository = tempParameterVerificationRepository;
    this.tempParameterVerificationMapper = tempParameterVerificationMapper;
    this.notificationService = notificationService;
    this.taskExecutionRepository = taskExecutionRepository;
    this.parameterExecutionHandler = parameterExecutionHandler;
    this.userGroupMemberRepository = userGroupMemberRepository;
    this.rulesExecutionService = rulesExecutionService;
    this.parameterMapper = parameterMapper;
    this.jwtService = jwtService;
    this.stateMapSelf = new HashMap<>();
    this.stateMapPeer = new HashMap<>();
    this.init();
  }

  private void init() {
    // middle transition states
    stateMapSelf.put(State.ParameterVerification.PENDING, Set.of(State.ParameterVerification.ACCEPTED, State.ParameterVerification.RECALLED));
    stateMapSelf.put(State.ParameterVerification.ACCEPTED, Set.of(State.ParameterVerification.PENDING)); // peer initiated
    stateMapSelf.put(State.ParameterVerification.RECALLED, Set.of(State.ParameterVerification.PENDING));

    // starting from here, we will not allow any state transition
    stateMapSelf.put(null, Set.of(State.ParameterVerification.PENDING));


    stateMapPeer.put(State.ParameterVerification.PENDING, Set.of(State.ParameterVerification.RECALLED, State.ParameterVerification.ACCEPTED, State.ParameterVerification.REJECTED));
    stateMapPeer.put(State.ParameterVerification.REJECTED, Set.of(State.ParameterVerification.PENDING));
    stateMapPeer.put(State.ParameterVerification.RECALLED, Set.of(State.ParameterVerification.PENDING));
    stateMapPeer.put(State.ParameterVerification.ACCEPTED, Set.of(State.ParameterVerification.PENDING));

    // starting from here, we will not allow any state transition
    stateMapPeer.put(null, Set.of(State.ParameterVerification.PENDING));
  }

  /**
   * User executing the job will fill values in parameter and initiate self verification, entry is created in PV table with initiated SV status
   * we will take a lock on parameter values entry of parameter value table, so that it cannot be further modified
   */
  @Override
  @Transactional
  public ParameterVerificationDto initiateSelfVerification(Long parameterExecutionId, boolean isBulk) throws ResourceNotFoundException, StreemException, IOException {
    log.info("[initiateSelfVerification] Request to initiate self verification for parameter, parameterExecutionId: {}", parameterExecutionId);
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    User principalUserEntity = userRepository.findById(principalUser.getId())
      .orElseThrow(() -> new ResourceNotFoundException(principalUser.getId(), ErrorCode.USER_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));

    Type.VerificationType verificationType = Type.VerificationType.SELF;
    State.ParameterVerification expectedState = State.ParameterVerification.PENDING;
    ParameterValue parameterValue = parameterValueRepository.findById(parameterExecutionId)
      .orElseThrow(() -> new ResourceNotFoundException(parameterExecutionId, ErrorCode.PARAMETER_VALUE_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));

    validateIfUserIsTaskAssignee(parameterValue.getTaskExecutionId(), principalUser.getId());

    boolean isVerifiedForCorrection = parameterValue.getTaskExecution().isCorrectionEnabled();

    if (!isVerifiedForCorrection) {
      validateInitiateRequest(parameterValue);
    }

    Long parameterId = parameterValue.getParameterId();
    Long jobId = parameterValue.getJobId();


    // validate partially executed parameters, such parameters cannot be signed until executed completely
    Parameter parameter = parameterRepository.getReferenceById(parameterId);

    boolean isParameterExecutedPartially;
    ParameterValueBase parameterValueBase;
    TempParameterValue tempParameterValue = null;
    String data;
    if (isVerifiedForCorrection) {
      tempParameterValue = tempParameterValueRepository.findByParameterIdAndTaskExecutionId(parameterValue.getParameterId(), parameterValue.getTaskExecutionId()).get();
      data = Type.CHOICE_PARAMETER_TYPES.contains(parameter.getType()) ? tempParameterValue.getChoices().toString() : tempParameterValue.getValue();
      isParameterExecutedPartially = parameterExecutionValidationService.isParameterValueIncomplete(parameter, data, true, null, tempParameterValue.getMedias(), null, null, null);
      parameterValueBase = tempParameterValue;
    } else {
      data = Type.CHOICE_PARAMETER_TYPES.contains(parameter.getType()) ? parameterValue.getChoices().toString() : parameterValue.getValue();
      isParameterExecutedPartially = parameterExecutionValidationService.isParameterValueIncomplete(parameter, data, false, parameterValue.getMedias(), null, null, null, null);
      parameterValueBase = parameterValue;
    }

    if (isParameterExecutedPartially) {
      ValidationUtils.invalidate(parameterId, ErrorCode.MANDATORY_PARAMETER_PENDING);
    }

    validateSelfStateTransfer(jobId, parameterValueBase.getId(), expectedState, isVerifiedForCorrection, principalUser.getId());
    parameterVerificationHandler.canInitiateSelfVerification(principalUserEntity, parameterValueBase);
    Job job = jobRepository.getReferenceById(jobId);

    ParameterVerification parameterVerification = null;
    TempParameterVerification tempParameterVerification = null;

    if (!isVerifiedForCorrection) {
      parameterVerification = createParameterVerification(
        DateTimeUtils.now(),
        principalUserEntity,
        verificationType,
        expectedState,
        job,
        (ParameterValue) parameterValueBase,
        isBulk,
        DateTimeUtils.now()
      );
    } else {
      tempParameterVerification = createTempParameterVerification(
        DateTimeUtils.now(),
        principalUserEntity,
        verificationType,
        expectedState,
        job,
        tempParameterValue,
        isBulk
      );
    }

    parameterValueBase.setVerified(false);
    parameterValueBase.setState(State.ParameterExecution.APPROVAL_PENDING);

    try {
      if (!isVerifiedForCorrection) {
        parameterValueRepository.save((ParameterValue) parameterValueBase);
        parameterVerificationRepository.save(parameterVerification);
      } else {
        tempParameterValueRepository.save((TempParameterValue) parameterValueBase);
        tempParameterVerificationRepository.save(tempParameterVerification);
      }
    } catch (Exception e) {
      log.error("[initiateSelfVerification] Error while initiating self verification for parameter, jobId: {}, parameterId: {}", jobId, parameterId, e);
      ValidationUtils.invalidate(parameterId, ErrorCode.PARAMETER_VERIFICATION_INITIATION_FAILED);
    }
    if(!isBulk) {
      jobAuditService.initiateSelfVerification(jobId, isVerifiedForCorrection ? tempParameterVerification : parameterVerification, isVerifiedForCorrection, principalUser);
    }
    return isVerifiedForCorrection ? tempParameterVerificationMapper.toDto(tempParameterVerification) : parameterVerificationMapper.toDto(parameterVerification);
  }

  /**
   * After initiate verification data of parameter is locked and user click on sign option enter password and credential verification call goes to jaas,
   * once successfully sign is done, this api call is made to complete the verification and unlock the data of parameter value table
   **/
  @Override
  @Transactional
  public ParameterVerificationDto acceptSelfVerification(Long parameterExecutionId, boolean isBulk, Long checkedAt) throws ResourceNotFoundException, StreemException, IOException {
    log.info("[completeSelfVerification] Request to complete self verification for parameter, parameterExecutionId: {}", parameterExecutionId);
    ParameterDto parameterDto;
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    User principalUserEntity = userRepository.findById(principalUser.getId())
      .orElseThrow(() -> new ResourceNotFoundException(principalUser.getId(), ErrorCode.USER_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));


    Type.VerificationType verificationType = Type.VerificationType.SELF;
    State.ParameterVerification expectedState = State.ParameterVerification.ACCEPTED;
    ParameterValue parameterValue = parameterValueRepository.findById(parameterExecutionId)
      .orElseThrow(() -> new ResourceNotFoundException(parameterExecutionId, ErrorCode.PARAMETER_VALUE_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));

    validateIfUserIsTaskAssignee(parameterValue.getTaskExecutionId(), principalUser.getId());

    Parameter parameter = parameterValue.getParameter();
    Long parameterId = parameterValue.getParameterId();
    Long jobId = parameterValue.getJobId();
    boolean isVerifiedForCorrection = parameterValue.getTaskExecution().isCorrectionEnabled();

    ParameterValueBase parameterValueBase;
    TempParameterValue tempParameterValue = null;
    if (isVerifiedForCorrection) {
      tempParameterValue = tempParameterValueRepository.findByParameterIdAndTaskExecutionId(parameterValue.getParameterId(), parameterValue.getTaskExecutionId()).get();
      parameterValueBase = tempParameterValue;
    } else {
      parameterValueBase = parameterValue;
    }

    long createdAt = DateTimeUtils.now();

    VerificationBase lastParameterVerification = validateSelfStateTransfer(jobId, parameterValueBase.getId(), expectedState, isVerifiedForCorrection, principalUser.getId());
    if (!Utility.isEmpty(lastParameterVerification)) {
      createdAt = lastParameterVerification.getCreatedAt();
    }
    parameterVerificationHandler.canCompleteSelfVerification(principalUserEntity, parameterId, lastParameterVerification);
    Job job = jobRepository.getReferenceById(jobId);

    ParameterVerification parameterVerification = null;
    TempParameterVerification tempParameterVerification = null;
    checkedAt = (checkedAt == null) ? DateTimeUtils.now() : checkedAt;
    if (!isVerifiedForCorrection) {
      parameterVerification = createParameterVerification(
        createdAt,
        principalUserEntity,
        verificationType,
        expectedState,
        job,
        parameterValue,
        isBulk,
        checkedAt);
    } else {
      tempParameterVerification = createTempParameterVerification(
        createdAt,
        principalUserEntity,
        verificationType,
        expectedState,
        job,
        tempParameterValue,
        isBulk);
    }
    boolean verified = parameterValueBase.getParameter().getVerificationType().equals(Type.VerificationType.SELF);
    parameterValueBase.setVerified(verified);
    if (parameterValueBase.getParameter().getVerificationType().equals(Type.VerificationType.BOTH)) {
      parameterValueBase.setState(State.ParameterExecution.VERIFICATION_PENDING);
    } else {
      parameterValueBase.setState(State.ParameterExecution.EXECUTED);
    }

    try {
      if (!isVerifiedForCorrection) {
        parameterVerificationRepository.save(parameterVerification);
        parameterVerificationRepository.deleteStaleEntriesByParameterValueIdAndVerificationType(parameterValueBase.getId(), Type.VerificationType.SELF.toString(), State.ParameterVerification.PENDING.toString());
        parameterValueRepository.save(parameterValue);
      } else {
        tempParameterVerificationRepository.save(tempParameterVerification);
        tempParameterVerificationRepository.deleteStaleEntriesByTempParameterValueIdAndVerificationType(parameterValueBase.getId(), Type.VerificationType.SELF.toString(), State.ParameterVerification.PENDING.toString());

        tempParameterValueRepository.save(tempParameterValue);
      }
    } catch (Exception e) {
      log.error("[completeSelfVerification] Error while completing self verification for parameter, jobId: {}, parameterId: {}", jobId, parameterId, e);
      ValidationUtils.invalidate(parameterId, ErrorCode.PARAMETER_VERIFICATION_COMPLETION_FAILED);
    }

    if (!isVerifiedForCorrection) {
      jobLogService.recordJobLogTrigger(String.valueOf(jobId), String.valueOf(parameterId), Type.JobLogTriggerType.PARAMETER_SELF_VERIFIED_BY, parameter.getLabel(), null, Utility.getFullNameAndEmployeeIdFromPrincipalUser(principalUser), principalUserEntity.getIdAsString(), userMapper.toUserAuditDto(principalUserEntity));
      jobLogService.recordJobLogTrigger(String.valueOf(jobId), String.valueOf(parameterId), Type.JobLogTriggerType.PARAMETER_SELF_VERIFIED_AT, parameter.getLabel(), null, String.valueOf(DateTimeUtils.now()), String.valueOf(DateTimeUtils.now()), userMapper.toUserAuditDto(principalUserEntity));
    }
    
    // Audit verification FIRST
    if(!isBulk) {
      jobAuditService.completeSelfVerification(jobId, isVerifiedForCorrection ? tempParameterVerification : parameterVerification, isVerifiedForCorrection, principalUser);
    }

    // THEN execute parameter (which will audit the execution)
    if (parameterValueBase.getState() == State.ParameterExecution.EXECUTED) {
      ParameterExecuteRequest parameterExecuteRequest = new ParameterExecuteRequest();
      parameterExecuteRequest.setJobId(jobId);
      ParameterRequest parameterRequest = new ParameterRequest();
      parameterRequest.setData(parameter.getData());
      parameterRequest.setId(parameter.getId());
      parameterRequest.setLabel(parameter.getLabel());
      parameterExecuteRequest.setParameter(parameterRequest);
      parameterExecuteRequest.setReferencedParameterId(parameter.getId());
      parameterExecutionHandler.executeParameter(jobId, parameterExecutionId, parameterExecuteRequest, PARAMETER_VALUE, true, false, false);
    }

    return isVerifiedForCorrection ? tempParameterVerificationMapper.toDto(tempParameterVerification) : parameterVerificationMapper.toDto(parameterVerification);
  }

  @Override
  @Transactional
  public List<ParameterVerificationDto> sendForPeerVerification(Long parameterExecutionId, PeerAssignRequest peerAssignRequest, boolean isBulk) throws ResourceNotFoundException, StreemException, IOException {
    log.info("[sendForPeerVerification] Request to send for peer verification for jobId: {}, peerAssignRequest: {}", parameterExecutionId, peerAssignRequest);

    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    User principalUserEntity = userRepository.findById(principalUser.getId())
      .orElseThrow(() -> new ResourceNotFoundException(principalUser.getId(), ErrorCode.USER_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));
    ParameterValue parameterValue = parameterValueRepository.findById(parameterExecutionId)
      .orElseThrow(() -> new ResourceNotFoundException(parameterExecutionId, ErrorCode.PARAMETER_VALUE_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));
    Long parameterId = parameterValue.getParameterId();
    Long jobId = parameterValue.getJobId();

    validateIfUserIsTaskAssignee(parameterValue.getTaskExecutionId(), principalUser.getId());

    if (!Utility.isEmpty(peerAssignRequest.getUserGroupId())) {
      Set<Long> filteredUserIds = getFilteredUserIds(peerAssignRequest, jobId, principalUser.getId());
      if (Utility.isEmpty(filteredUserIds)) {
        ValidationUtils.invalidate(parameterId, ErrorCode.NO_VALID_USER_FOUND_FOR_PEER_VERIFICATION);
      }
      peerAssignRequest.setUserId(filteredUserIds);
    }

    boolean isVerifiedForCorrection = parameterValue.getTaskExecution().isCorrectionEnabled();

    // validate partially executed parameters, such parameters cannot be signed until executed completely
    Parameter parameter = parameterRepository.getReferenceById(parameterId);

    if (!isVerifiedForCorrection) {
      validateInitiateRequest(parameterValue);
      if (parameter.getVerificationType() == Type.VerificationType.BOTH) {
        validateIfSelfParameterVerificationIsCompleted(parameterExecutionId, jobId, principalUser, parameterId);
      }
    }
    String data = Type.CHOICE_PARAMETER_TYPES.contains(parameter.getType()) ? parameterValue.getChoices().toString() : parameterValue.getValue();
    boolean isParameterExecutedPartially = parameterExecutionValidationService.isParameterValueIncomplete(parameter, data, false, parameterValue.getMedias(), null, null, null, null);
    if (isParameterExecutedPartially) {
      ValidationUtils.invalidate(parameterId, ErrorCode.MANDATORY_PARAMETER_PENDING);
    }

    ParameterValueBase parameterValueBase;
    TempParameterValue tempParameterValue = null;

    if (isVerifiedForCorrection) {
      tempParameterValue = tempParameterValueRepository.findByParameterIdAndTaskExecutionId(parameterValue.getParameterId(), parameterValue.getTaskExecutionId()).get();
      parameterValueBase = tempParameterValue;
    } else {
      parameterValueBase = parameterValue;
    }

    Type.VerificationType verificationType = Type.VerificationType.PEER;
    State.ParameterVerification expectedState = State.ParameterVerification.PENDING;
    validatePeerStateTransfer(jobId, parameterValueBase.getId(), expectedState, isVerifiedForCorrection, principalUser.getId());

    Job job = jobRepository.getReferenceById(jobId);

    List<ParameterVerification> parameterVerifications = new ArrayList<>();
    List<TempParameterVerification> tempParameterVerifications = new ArrayList<>();

    if (isVerifiedForCorrection) {
      for (Long userId : peerAssignRequest.getUserId()) {
        TempParameterVerification tempParameterVerification;
        tempParameterVerification = createTempParameterVerification(
          DateTimeUtils.now(),
          principalUserEntity,
          verificationType,
          expectedState,
          job,
          tempParameterValue,
          isBulk);
        tempParameterVerification.setUser(userRepository.getReferenceById(userId));
        tempParameterVerifications.add(tempParameterVerification);
      }
    } else {
      for (Long userId : peerAssignRequest.getUserId()) {
        ParameterVerification parameterVerification;
        parameterVerification = createParameterVerification(
          DateTimeUtils.now(),
          principalUserEntity,
          verificationType,
          expectedState,
          job,
          parameterValue,
          isBulk,
          DateTimeUtils.now());
        parameterVerification.setUser(userRepository.getReferenceById(userId));
        parameterVerifications.add(parameterVerification);
      }
    }
    for (Long userId : peerAssignRequest.getUserId()) {
      notificationService.notifyPeerVerification(Collections.singleton(userId), jobId, principalUser.getOrganisationId());
    }

    parameterValueBase.setState(State.ParameterExecution.APPROVAL_PENDING);

    try {
      if (!isVerifiedForCorrection) {
        parameterValueRepository.save(parameterValue);
        parameterVerificationRepository.deleteStaleEntriesByParameterValueIdAndVerificationType(parameterValueBase.getId(), Type.VerificationType.PEER.toString(), State.ParameterVerification.PENDING.toString());
        parameterVerificationRepository.deleteStaleEntriesByParameterValueIdAndVerificationType(parameterValueBase.getId(), Type.VerificationType.PEER.toString(), State.ParameterVerification.RECALLED.toString());
        parameterVerificationRepository.saveAll(parameterVerifications);
      } else {
        tempParameterValueRepository.save(tempParameterValue);
        tempParameterVerificationRepository.deleteStaleEntriesByTempParameterValueIdAndVerificationType(parameterValueBase.getId(), Type.VerificationType.PEER.toString(), State.ParameterVerification.PENDING.toString());
        tempParameterVerificationRepository.deleteStaleEntriesByTempParameterValueIdAndVerificationType(parameterValueBase.getId(), Type.VerificationType.PEER.toString(), State.ParameterVerification.RECALLED.toString());
        tempParameterVerificationRepository.saveAll(tempParameterVerifications);
      }
    } catch (Exception e) {
      log.error("[sendForPeerVerification] Error while sending verification for parameter, parameterId: {}",
        parameterId, e);
      ValidationUtils.invalidate(parameterId, ErrorCode.CANNOT_SEND_PARAMETER_FOR_PEER_VERIFICATION);
    }
    if (!isVerifiedForCorrection) {
      for (ParameterVerification parameterVerification : parameterVerifications) {
        if (!isBulk) {
          jobAuditService.sendForPeerVerification(jobId, parameterVerification, false, principalUser);
        }
      }
    } else {
      for (TempParameterVerification tempParameterVerification : tempParameterVerifications) {
        if (!isBulk) {
          jobAuditService.sendForPeerVerification(jobId, tempParameterVerification, true, principalUser);
        }
      }
    }
    //TODO: refactor
    if (!isVerifiedForCorrection) {
      List<ParameterVerificationDto> parameterVerificationDtos = new ArrayList<>();
      for (ParameterVerification parameterVerification : parameterVerifications) {
        parameterVerificationDtos.add(parameterVerificationMapper.toDto(parameterVerification));
      }
      return parameterVerificationDtos;
    } else {
      List<ParameterVerificationDto> tempParameterVerificationDtos = new ArrayList<>();
      for (TempParameterVerification tempParameterVerification : tempParameterVerifications) {
        tempParameterVerificationDtos.add(tempParameterVerificationMapper.toDto(tempParameterVerification));
      }
      return tempParameterVerificationDtos;
    }
  }

  private Set<Long> getFilteredUserIds(PeerAssignRequest peerAssignRequest, Long jobId, Long currentUserId) {
    Set<Long> userIds = peerAssignRequest.getUserId();
    Set<Long> userGroupIds = peerAssignRequest.getUserGroupId();
    Set<Long> allUserIds = userGroupMemberRepository.findAllUsersByUserGroupIds(userGroupIds);

    userIds.addAll(allUserIds);
    userIds.remove(currentUserId);

    if (Utility.isEmpty(userIds)) {
      return new HashSet<>();
    }

    Set<Long> filteredUserIds = new HashSet<>();
    FilterAssigneeRequest filterAssigneeRequest = new FilterAssigneeRequest(userIds.stream().map(String::valueOf).collect(Collectors.toSet()));
    Response<Object> allByRoles = userService.getAllByRoles(Misc.ASSIGNEE_ROLES, "", true, filterAssigneeRequest, PageRequest.of(0, Integer.MAX_VALUE));
    JsonNode rootNode = JsonUtils.valueToNode(allByRoles);
    JsonNode dataArray = rootNode.path("data");
    if (dataArray.isArray()) {
      for (JsonNode dataNode : dataArray) {
        long id = dataNode.path("id").asLong();
        filteredUserIds.add(id);
      }
    }
    return filteredUserIds;
  }

  @Override
  @Transactional
  public ParameterVerificationDto recallPeerVerification(Long parameterExecutionId) throws ResourceNotFoundException, StreemException {
    log.info("[recallPeerVerification] Request to recall  verification for jobId: {}", parameterExecutionId);

    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    User principalUserEntity = userRepository.findById(principalUser.getId())
      .orElseThrow(() -> new ResourceNotFoundException(principalUser.getId(), ErrorCode.USER_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));

    State.ParameterVerification expectedState = State.ParameterVerification.RECALLED;
    Type.VerificationType verificationType = Type.VerificationType.PEER;
    ParameterValue parameterValue = parameterValueRepository.findById(parameterExecutionId)
      .orElseThrow(() -> new ResourceNotFoundException(parameterExecutionId, ErrorCode.PARAMETER_VALUE_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));

    validateIfUserIsTaskAssignee(parameterValue.getTaskExecutionId(), principalUser.getId());

    Long parameterId = parameterValue.getParameterId();
    Long jobId = parameterValue.getJobId();
    boolean isVerifiedForCorrection = parameterValue.getTaskExecution().isCorrectionEnabled();


    ParameterValueBase parameterValueBase;
    TempParameterValue tempParameterValue = null;

    if (isVerifiedForCorrection) {
      tempParameterValue = tempParameterValueRepository.findByParameterIdAndTaskExecutionId(parameterId, parameterValue.getTaskExecutionId())
        .orElseThrow(() -> new ResourceNotFoundException(parameterId, ErrorCode.PARAMETER_VALUE_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));
      parameterValueBase = tempParameterValue;
    } else {
      parameterValueBase = parameterValue;
    }

    long createdAt = DateTimeUtils.now();
    VerificationBase lastParameterVerification = validatePeerStateTransfer(jobId, parameterValueBase.getId(), expectedState, isVerifiedForCorrection, null);
    if (!Utility.isEmpty(lastParameterVerification)) {
      createdAt = lastParameterVerification.getCreatedAt();
    }

    Job job = jobRepository.getReferenceById(jobId);

    ParameterVerification parameterVerification = null;
    TempParameterVerification tempParameterVerification = null;

    if (!isVerifiedForCorrection) {
      parameterVerification = createParameterVerification(
        createdAt,
        principalUserEntity,
        verificationType,
        expectedState,
        job,
        parameterValue,
        false,
        DateTimeUtils.now());
    } else {
      tempParameterVerification = createTempParameterVerification(
        createdAt,
        principalUserEntity,
        verificationType,
        expectedState,
        job,
        tempParameterValue,
        false);
    }


    parameterValueBase.setState(State.ParameterExecution.VERIFICATION_PENDING);

    try {
      if (!isVerifiedForCorrection) {
        parameterValueRepository.save(parameterValue);
        parameterVerificationRepository.deleteStaleEntriesByParameterValueIdAndVerificationType(parameterValueBase.getId(), Type.VerificationType.PEER.toString(), State.ParameterVerification.PENDING.toString());
        parameterVerificationRepository.save(parameterVerification);
      } else {
        tempParameterValueRepository.save(tempParameterValue);
        tempParameterVerificationRepository.deleteStaleEntriesByTempParameterValueIdAndVerificationType(parameterValueBase.getId(), Type.VerificationType.PEER.toString(), State.ParameterVerification.PENDING.toString());

        tempParameterVerificationRepository.save(tempParameterVerification);
      }
    } catch (Exception e) {
      log.error("[recallPeerVerification] Error while recalling verification for parameter, parameterId: {}",
        parameterId, e);
      ValidationUtils.invalidate(parameterId, ErrorCode.PARAMETER_VERIFICATION_RECALL_FAILED);
    }

    jobAuditService.recallVerification(jobId, isVerifiedForCorrection ? tempParameterVerification : parameterVerification, isVerifiedForCorrection, principalUser);
    return isVerifiedForCorrection ? tempParameterVerificationMapper.toDto(tempParameterVerification) : parameterVerificationMapper.toDto(parameterVerification);
  }

  @Override
  @Transactional
  public ParameterVerificationDto recallSelfVerification(Long parameterExecutionId) throws ResourceNotFoundException, StreemException {
    log.info("[recallSelfVerification] Request to recall  verification for parameterExecutionId: {}", parameterExecutionId);

    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    User principalUserEntity = userRepository.findById(principalUser.getId())
      .orElseThrow(() -> new ResourceNotFoundException(principalUser.getId(), ErrorCode.USER_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));

    State.ParameterVerification expectedState = State.ParameterVerification.RECALLED;
    Type.VerificationType verificationType = Type.VerificationType.SELF;
    ParameterValue parameterValue = parameterValueRepository.findById(parameterExecutionId)
      .orElseThrow(() -> new ResourceNotFoundException(parameterExecutionId, ErrorCode.PARAMETER_VALUE_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));

    validateIfUserIsTaskAssignee(parameterValue.getTaskExecutionId(), principalUser.getId());

    Long parameterId = parameterValue.getParameterId();
    Long jobId = parameterValue.getJobId();
    boolean isVerifiedForCorrection = parameterValue.getTaskExecution().isCorrectionEnabled();

    ParameterValueBase parameterValueBase;
    TempParameterValue tempParameterValue = null;
    if (isVerifiedForCorrection) {
      tempParameterValue = tempParameterValueRepository.findByParameterIdAndTaskExecutionId(parameterId, parameterValue.getTaskExecutionId())
        .orElseThrow(() -> new ResourceNotFoundException(parameterId, ErrorCode.PARAMETER_VALUE_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));
      parameterValueBase = tempParameterValue;
    } else {
      parameterValueBase = parameterValue;
    }

    long createdAt = DateTimeUtils.now();
    VerificationBase lastParameterVerification = validateSelfStateTransfer(jobId, parameterValueBase.getId(), expectedState, isVerifiedForCorrection, principalUser.getId());
    if (!Utility.isEmpty(lastParameterVerification)) {
      createdAt = lastParameterVerification.getCreatedAt();
    }

    Job job = jobRepository.getReferenceById(jobId);

    ParameterVerification parameterVerification = null;
    TempParameterVerification tempParameterVerification = null;
    if (!isVerifiedForCorrection) {
      parameterVerification = createParameterVerification(
        createdAt,
        principalUserEntity,
        verificationType,
        expectedState,
        job,
        parameterValue,
        false,
        DateTimeUtils.now());
    } else {
      tempParameterVerification = createTempParameterVerification(
        createdAt,
        principalUserEntity,
        verificationType,
        expectedState,
        job,
        tempParameterValue,
        false);
    }

    parameterValueBase.setState(State.ParameterExecution.BEING_EXECUTED);


    try {
      if (!isVerifiedForCorrection) {
        parameterValueRepository.save(parameterValue);
        parameterVerificationRepository.deleteStaleEntriesByParameterValueIdAndVerificationType(parameterValueBase.getId(), Type.VerificationType.SELF.toString(), State.ParameterVerification.PENDING.toString());
        parameterVerificationRepository.save(parameterVerification);
      } else {
        tempParameterValueRepository.save(tempParameterValue);
        tempParameterVerificationRepository.deleteStaleEntriesByTempParameterValueIdAndVerificationType(parameterValueBase.getId(), Type.VerificationType.SELF.toString(), State.ParameterVerification.PENDING.toString());
        tempParameterVerificationRepository.save(tempParameterVerification);
      }
    } catch (Exception e) {
      log.error("[recallSelfVerification] Error while recalling verification for parameter, parameterId: {}", parameterId, e);
      ValidationUtils.invalidate(parameterId, ErrorCode.PARAMETER_VERIFICATION_RECALL_FAILED);
    }

    jobAuditService.recallVerification(jobId, isVerifiedForCorrection ? tempParameterVerification : parameterVerification, isVerifiedForCorrection, principalUser);
    return isVerifiedForCorrection ? tempParameterVerificationMapper.toDto(tempParameterVerification) : parameterVerificationMapper.toDto(parameterVerification);
  }

  @Override
  @Transactional
  public ParameterVerificationDto acceptPeerVerification(Long parameterExecutionId, boolean isBulk, Long checkedAt,ParameterVerificationRequest parameterVerificationRequest) throws ResourceNotFoundException, StreemException, IOException {
    log.info("[acceptPeerVerification] Request to accept peer verification for parameter, parameterExecutionId: {}, parameterVerificationRequest: {}", parameterExecutionId, parameterVerificationRequest);
    ParameterDto parameterDto;
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    User principalUserEntity = userRepository.findById(principalUser.getId())
      .orElseThrow(() -> new ResourceNotFoundException(principalUser.getId(), ErrorCode.USER_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));

    Type.VerificationType verificationType = Type.VerificationType.PEER;
    State.ParameterVerification expectedState = State.ParameterVerification.ACCEPTED;
    ParameterValue parameterValue = parameterValueRepository.findById(parameterExecutionId)
      .orElseThrow(() -> new ResourceNotFoundException(parameterExecutionId, ErrorCode.PARAMETER_VALUE_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));

    validateIfUserIsJobAssignee(parameterValue.getJobId(), principalUser.getId());

    Parameter parameter = parameterValue.getParameter();
    Long parameterId = parameterValue.getParameterId();
    Long jobId = parameterValue.getJobId();
    boolean isVerifiedForCorrection = parameterValue.getTaskExecution().isCorrectionEnabled();

    ParameterValueBase parameterValueBase;
    TempParameterValue tempParameterValue = null;
    if (isVerifiedForCorrection) {
      tempParameterValue = tempParameterValueRepository.findByParameterIdAndTaskExecutionId(parameterId, parameterValue.getTaskExecutionId())
        .orElseThrow(() -> new ResourceNotFoundException(parameterId, ErrorCode.PARAMETER_VALUE_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));
      parameterValueBase = tempParameterValue;
    } else {
      parameterValueBase = parameterValue;
    }

    long createdAt = DateTimeUtils.now();
    VerificationBase lastParameterVerification = validatePeerStateTransfer(jobId, parameterValueBase.getId(), expectedState, isVerifiedForCorrection, principalUser.getId());
    if (Utility.isEmpty(lastParameterVerification.getUser())) {
      ValidationUtils.invalidate(parameterValue.getId(), ErrorCode.PARAMETER_VERIFICATION_NOT_ALLOWED);
    }
    if (!Utility.isEmpty(lastParameterVerification)) {
      createdAt = lastParameterVerification.getCreatedAt();
    }
    parameterVerificationHandler.canCompletePeerVerification(principalUserEntity, lastParameterVerification);
    Job job = jobRepository.getReferenceById(jobId);

    ParameterVerification parameterVerification = null;
    TempParameterVerification tempParameterVerification = null;
    checkedAt = (checkedAt == null) ? DateTimeUtils.now() : checkedAt;
    if (!isVerifiedForCorrection) {
      parameterVerification = createParameterVerification(
        createdAt,
        lastParameterVerification.getUser(),
        verificationType,
        expectedState,
        job,
        parameterValue,
        isBulk,
        checkedAt);
      parameterVerification.setCreatedBy(lastParameterVerification.getCreatedBy());
    } else {
      tempParameterVerification = createTempParameterVerification(
        createdAt,
        lastParameterVerification.getUser(),
        verificationType,
        expectedState,
        job,
        tempParameterValue,
        isBulk);
      tempParameterVerification.setCreatedBy(lastParameterVerification.getCreatedBy());
    }

    parameterValueBase.setVerified(true);
    parameterValueBase.setState(State.ParameterExecution.EXECUTED);

    try {
      if (!isVerifiedForCorrection) {
        parameterVerificationRepository.save(parameterVerification);
        parameterVerificationRepository.deleteStaleEntriesByParameterValueIdAndVerificationType(parameterValueBase.getId(), Type.VerificationType.PEER.toString(), State.ParameterVerification.PENDING.toString());
        parameterValueRepository.save(parameterValue);
      } else {
        tempParameterVerificationRepository.save(tempParameterVerification);
        tempParameterVerificationRepository.deleteStaleEntriesByTempParameterValueIdAndVerificationType(parameterValueBase.getId(), Type.VerificationType.PEER.toString(), State.ParameterVerification.PENDING.toString());
        tempParameterValueRepository.save(tempParameterValue);
      }
    } catch (Exception e) {
      log.error("[completePeerVerification] Error while completing peer verification for parameter, jobId: {}, parameterId: {}", jobId, parameterId, e);
      ValidationUtils.invalidate(parameterId, ErrorCode.PARAMETER_VERIFICATION_COMPLETION_FAILED);
    }

    if (!isVerifiedForCorrection) {
      jobLogService.recordJobLogTrigger(String.valueOf(jobId), String.valueOf(parameterId), Type.JobLogTriggerType.PARAMETER_PEER_VERIFIED_BY, parameterValue.getParameter().getLabel(), null, Utility.getFullNameAndEmployeeIdFromPrincipalUser(principalUser), principalUser.getIdAsString(), userMapper.toUserAuditDto(principalUserEntity));
      jobLogService.recordJobLogTrigger(String.valueOf(jobId), String.valueOf(parameterId), Type.JobLogTriggerType.PARAMETER_PEER_STATUS, parameter.getLabel(), null, parameterVerification.getVerificationStatus().name(), principalUserEntity.getIdAsString(), userMapper.toUserAuditDto(principalUserEntity));
      jobLogService.recordJobLogTrigger(String.valueOf(jobId), String.valueOf(parameterId), Type.JobLogTriggerType.PARAMETER_PEER_VERIFIED_AT, parameterValue.getParameter().getLabel(), null, String.valueOf(DateTimeUtils.now()), String.valueOf(DateTimeUtils.now()), userMapper.toUserAuditDto(principalUserEntity));
    }

    // Check if this is a same session verification
    boolean isSameSession = parameterVerificationRequest != null && Boolean.TRUE.equals(parameterVerificationRequest.getSameSession());
    
    // Audit verification FIRST
    if(!isBulk && !isSameSession) {
      jobAuditService.acceptPeerVerification(jobId, isVerifiedForCorrection ? tempParameterVerification : parameterVerification, isVerifiedForCorrection, principalUser);
    }
    if (isSameSession && !isBulk) {
        // Parse initiator JWT and get initiator user context
        PrincipalUser initiatorUser = jwtService.parseAndValidate(parameterVerificationRequest.getInitiatorJwtToken());
        // Use same session audit method with dual user context
        jobAuditService.acceptSameSessionVerification(jobId, isVerifiedForCorrection ? tempParameterVerification : parameterVerification, isVerifiedForCorrection, principalUser, initiatorUser);
    }

    // THEN execute parameter (which will audit the execution)
    ParameterExecuteRequest parameterExecuteRequest = new ParameterExecuteRequest();
    parameterExecuteRequest.setJobId(jobId);
    ParameterRequest parameterRequest = new ParameterRequest();
    parameterRequest.setData(parameter.getData());
    parameterRequest.setId(parameter.getId());
    parameterRequest.setLabel(parameter.getLabel());
    parameterExecuteRequest.setParameter(parameterRequest);
    parameterExecuteRequest.setReferencedParameterId(parameter.getId());

    ParameterVerificationDto parameterVerificationDto = isVerifiedForCorrection ? tempParameterVerificationMapper.toDto(tempParameterVerification) : parameterVerificationMapper.toDto(parameterVerification);
    parameterExecutionHandler.executeParameter(jobId, parameterExecutionId, parameterExecuteRequest, PARAMETER_VALUE, true, false, false);

    return parameterVerificationDto;
  }

  @Override
  @Transactional
  public ParameterVerificationDto rejectPeerVerification(Long parameterExecutionId, ParameterVerificationRequest parameterVerificationRequest) throws ResourceNotFoundException, StreemException {
    log.info("[rejectPeerVerification] Request to reject peer verification for parameter, parameterExecutionId: {}, parameterVerificationRequest: {}", parameterExecutionId, parameterVerificationRequest);
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    User principalUserEntity = userRepository.findById(principalUser.getId())
      .orElseThrow(() -> new ResourceNotFoundException(principalUser.getId(), ErrorCode.USER_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));

    Type.VerificationType verificationType = Type.VerificationType.PEER;
    State.ParameterVerification expectedState = State.ParameterVerification.REJECTED;
    ParameterValue parameterValue = parameterValueRepository.findById(parameterExecutionId)
      .orElseThrow(() -> new ResourceNotFoundException(principalUser.getId(), ErrorCode.PARAMETER_VALUE_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));

    validateIfUserIsJobAssignee(parameterValue.getJobId(), principalUser.getId());

    Parameter parameter = parameterValue.getParameter();
    Long parameterId = parameterValue.getParameterId();
    Long jobId = parameterValue.getJobId();
    boolean isVerifiedForCorrection = parameterValue.getTaskExecution().isCorrectionEnabled();

    ParameterValueBase parameterValueBase;
    TempParameterValue tempParameterValue = null;
    if (isVerifiedForCorrection) {
      tempParameterValue = tempParameterValueRepository.findByParameterIdAndTaskExecutionId(parameterId, parameterValue.getTaskExecutionId())
        .orElseThrow(() -> new ResourceNotFoundException(parameterId, ErrorCode.PARAMETER_VALUE_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));
      parameterValueBase = tempParameterValue;
    } else {
      parameterValueBase = parameterValue;
    }

    long createdAt = DateTimeUtils.now();
    VerificationBase lastParameterVerification = validatePeerStateTransfer(jobId, parameterValueBase.getId(), expectedState, isVerifiedForCorrection, principalUser.getId());

    if (Utility.isEmpty(lastParameterVerification.getUser())) {
      ValidationUtils.invalidate(parameterValue.getId(), ErrorCode.PARAMETER_VERIFICATION_NOT_ALLOWED);
    }
    if (!Utility.isEmpty(lastParameterVerification)) {
      createdAt = lastParameterVerification.getCreatedAt();
    }
    parameterVerificationHandler.canCompletePeerVerification(principalUserEntity, lastParameterVerification);
    Job job = jobRepository.getReferenceById(jobId);

    ParameterVerification parameterVerification = null;
    TempParameterVerification tempParameterVerification = null;

    if (!isVerifiedForCorrection) {
      parameterVerification = createParameterVerification(
        createdAt,
        lastParameterVerification.getUser(),
        verificationType,
        expectedState,
        job,
        parameterValue,
        false,
        DateTimeUtils.now());
      parameterVerification.setCreatedBy(lastParameterVerification.getCreatedBy());
      parameterVerification.setComments(parameterVerificationRequest.getComments());
    } else {
      tempParameterVerification = createTempParameterVerification(
        createdAt,
        lastParameterVerification.getUser(),
        verificationType,
        expectedState,
        job,
        tempParameterValue,
        false);
      tempParameterVerification.setCreatedBy(lastParameterVerification.getCreatedBy());
      tempParameterVerification.setComments(parameterVerificationRequest.getComments());
    }
    parameterValueBase.setState(State.ParameterExecution.VERIFICATION_PENDING);

    try {
      if (!isVerifiedForCorrection) {
        parameterVerificationRepository.save(parameterVerification);
        parameterVerificationRepository.deleteStaleEntriesByParameterValueIdAndVerificationType(parameterValueBase.getId(), Type.VerificationType.PEER.toString(), State.ParameterVerification.PENDING.toString());
        parameterValueRepository.save(parameterValue);
      } else {
        tempParameterVerificationRepository.save(tempParameterVerification);
        tempParameterVerificationRepository.deleteStaleEntriesByTempParameterValueIdAndVerificationType(parameterValueBase.getId(), Type.VerificationType.PEER.toString(), State.ParameterVerification.PENDING.toString());
        tempParameterValueRepository.save(tempParameterValue);
      }
    } catch (Exception ex) {
      log.error("[rejectPeerVerification] Error while completing peer verification for parameter, jobId: {}, parameterId: {}", jobId, parameterId);
      ValidationUtils.invalidate(parameterId, ErrorCode.PARAMETER_VERIFICATION_COMPLETION_FAILED);
    }

    if (!isVerifiedForCorrection) {
      jobLogService.recordJobLogTrigger(String.valueOf(jobId), String.valueOf(parameterId), Type.JobLogTriggerType.PARAMETER_PEER_STATUS, parameter.getLabel(), null, parameterVerification.getVerificationStatus().name(), principalUserEntity.getIdAsString(), userMapper.toUserAuditDto(principalUserEntity));
      jobLogService.recordJobLogTrigger(String.valueOf(jobId), String.valueOf(parameterId), Type.JobLogTriggerType.PARAMETER_PEER_VERIFIED_BY, parameterValue.getParameter().getLabel(), null, Utility.getFullNameAndEmployeeIdFromPrincipalUser(principalUser), principalUser.getIdAsString(), userMapper.toUserAuditDto(principalUserEntity));
      jobLogService.recordJobLogTrigger(String.valueOf(jobId), String.valueOf(parameterId), Type.JobLogTriggerType.PARAMETER_PEER_VERIFIED_AT, parameterValue.getParameter().getLabel(), null, String.valueOf(DateTimeUtils.now()), String.valueOf(DateTimeUtils.now()), userMapper.toUserAuditDto(principalUserEntity));

    }

    // Check if this is a same session verification
    boolean isSameSession = Boolean.TRUE.equals(parameterVerificationRequest.getSameSession());
    
    if (isSameSession) {
      try {
        // Parse initiator JWT and get initiator user context
        PrincipalUser initiatorUser = jwtService.parseAndValidate(parameterVerificationRequest.getInitiatorJwtToken());
        log.info("[rejectPeerVerification] Same session verification - Initiator: {}, Verifier: {}", initiatorUser.getId(), principalUser.getId());
        
        // Use same session audit method with dual user context
        jobAuditService.rejectSameSessionVerification(jobId, isVerifiedForCorrection ? tempParameterVerification : parameterVerification, isVerifiedForCorrection, principalUser, initiatorUser);
      } catch (StreemException e) {
        log.error("[rejectPeerVerification] Failed to parse initiator JWT token for same session verification: {}", e.getMessage());
        throw e;
      }
    } else {
      jobAuditService.rejectPeerVerification(jobId, isVerifiedForCorrection ? tempParameterVerification : parameterVerification, isVerifiedForCorrection, principalUser);
    }
    
    return isVerifiedForCorrection ? tempParameterVerificationMapper.toDto(tempParameterVerification) : parameterVerificationMapper.toDto(parameterVerification);
  }

  @Override
  public List<UserGroupView> getUserGroupAssignees(Long jobId, String query) {
    log.info("[getUserGroupAssignees] Request to get user group assignees for jobId: {}", jobId);
    return taskExecutionAssigneeRepository.getUserGroupAssignees(jobId, query);
  }

  @Override
  public Response<Object> getAssignees(Long jobId, String filters) {
    log.info("[getAssignees] Request to get assignees for jobId: {}", jobId);
    Set<String> userIds = getJobAssignees(jobId);
    if (!userIds.isEmpty()) {
      FilterAssigneeRequest filterAssigneeRequest = new FilterAssigneeRequest(userIds);
      return userService.getAllByRoles(Misc.ASSIGNEE_ROLES, filters, true, filterAssigneeRequest, PageRequest.of(0, Integer.MAX_VALUE));
    }
    else {
      return new Response<>();
    }

  }

  private Set<String> getJobAssignees(Long jobId) {
    List<JobAssigneeView> jobAssignees = taskExecutionAssigneeRepository.getJobAssignees(Set.of(jobId));
    return jobAssignees.stream().map(JobAssigneeView::getId).collect(Collectors.toSet());
  }

  @Override
  public Page<ParameterVerificationListViewDto> getAllVerifications(String status, Long jobId, Long requestedTo, Long requestedBy, String parameterName, String processName, String objectId, Long useCaseId, Pageable pageable) {
    log.info("[getUserAssignedAndRequestedVerifications] Request to get user parameter verifications for status: {}, jobId: {},  requestedTo: {}, requestedBy: {}, parameterName: {}", status, jobId, requestedTo, requestedBy, parameterName);

    int limit = pageable.getPageSize();
    int offset = (int) pageable.getOffset();
    objectId = Utility.isEmpty(objectId) ? null : objectId;
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    Long facilityId = principalUser.getCurrentFacilityId();

    List<ParameterVerificationListViewProjection> parameterVerifications = parameterVerificationRepository
      .getVerificationFilterView(status, jobId, requestedTo, requestedBy, parameterName, processName, objectId, limit, offset, facilityId, useCaseId);

    long resultCount = parameterVerificationRepository
      .getVerificationFilterViewCount(status, jobId, requestedTo, requestedBy, parameterName,processName, objectId, facilityId, useCaseId);

    return new PageImpl<>(parameterVerificationMapper.toParameterListViewDto(parameterVerifications), PageRequest.of(offset / limit, limit), resultCount);

  }

  /**
   * here we have created map of parameter value id and list of verifications with latest status of self and peer and through this map we have created map of parameterId and list of verifications
   */
  @Override
  public Map<Long, List<ParameterVerification>> getParameterVerificationsDataForAJob(Long jobId) {
    // This query fetches the latest self and peer verification for each parameter value in a job. After many round of
    // verifications we only care about last self and peer verification statuses hence this query
    //TODO: return only ids
    List<ParameterVerification> parameterVerifications = parameterVerificationRepository.findLatestSelfAndPeerVerificationOfParametersInJob(jobId);
    List<ParameterVerification> updatedParameterVerification = filterVerifications(parameterVerifications);
    return updatedParameterVerification.stream().collect(Collectors.groupingBy(ParameterVerification::getParameterValueId));
  }

  @Override
  public Map<Long, List<TempParameterVerification>> getTempParameterVerificationsDataForAJob(Long jobId) {
    // This query fetches the latest self and peer verification for each parameter value in a job. After many round of
    // verifications we only care about last self and peer verification statuses hence this query
    //TODO: return only ids
    List<TempParameterVerification> tempParameterVerifications = tempParameterVerificationRepository.findLatestSelfAndPeerVerificationOfParametersInJob(jobId);
    List<TempParameterVerification> updatedTempParameterVerifications = filterTempVerifications(tempParameterVerifications);
    return updatedTempParameterVerifications.stream().collect(Collectors.groupingBy(TempParameterVerification::getTempParameterValueId));
  }

  private VerificationBase validateSelfStateTransfer(long jobId, long parameterValueId, State.ParameterVerification expectedState, boolean isVerifiedForCorrection, Long userId) throws StreemException {
    State.ParameterVerification currentState = null;
    ParameterVerification parameterVerification = null;
    TempParameterVerification tempParameterVerification = null;
    if (!isVerifiedForCorrection) {
      parameterVerification = parameterVerificationRepository.findByJobIdAndParameterValueIdAndVerificationTypeAndUserId(jobId, parameterValueId, Type.VerificationType.SELF.toString(), userId);
      if (!Utility.isEmpty(parameterVerification)) {
        currentState = parameterVerification.getVerificationStatus();
      }
    } else {
      tempParameterVerification = tempParameterVerificationRepository.findByJobIdAndParameterValueIdAndVerificationTypeAndUserId(jobId, parameterValueId, Type.VerificationType.SELF.toString(), userId);
      if (!Utility.isEmpty(tempParameterVerification)) {
        currentState = tempParameterVerification.getVerificationStatus();
      }
    }


    if (!this.stateMapSelf.containsKey(currentState) || !this.stateMapSelf.get(currentState).contains(expectedState)) {
      ValidationUtils.invalidate(parameterValueId, ErrorCode.PARAMETER_VERIFICATION_NOT_ALLOWED);
    }

    return isVerifiedForCorrection ? tempParameterVerification : parameterVerification;
  }

  private VerificationBase validatePeerStateTransfer(long jobId, long parameterValueId, State.ParameterVerification expectedState, boolean isVerifiedForCorrection, Long userId) throws StreemException {
    State.ParameterVerification currentState = null;
    ParameterVerification parameterVerification = null;
    TempParameterVerification tempParameterVerification = null;
    if (!isVerifiedForCorrection) {
      parameterVerification = parameterVerificationRepository.findByJobIdAndParameterValueIdAndVerificationTypeAndUserId(jobId, parameterValueId, Type.VerificationType.PEER.toString(), userId);
      if (!Utility.isEmpty(parameterVerification)) {
        currentState = parameterVerification.getVerificationStatus();
      }
    } else {
      tempParameterVerification = tempParameterVerificationRepository.findByJobIdAndParameterValueIdAndVerificationTypeAndUserId(jobId, parameterValueId, Type.VerificationType.PEER.toString(), userId);
      if (!Utility.isEmpty(tempParameterVerification)) {
        currentState = tempParameterVerification.getVerificationStatus();
      }
    }

    if (!this.stateMapPeer.containsKey(currentState) || !this.stateMapPeer.get(currentState).contains(expectedState)) {
      ValidationUtils.invalidate(parameterValueId, ErrorCode.PARAMETER_VERIFICATION_NOT_ALLOWED);
    }

    return isVerifiedForCorrection ? tempParameterVerification : parameterVerification;
  }

  private static ParameterVerification createParameterVerification(
    long createdAt,
    User userEntity,
    Type.VerificationType verificationType,
    State.ParameterVerification expectedState,
    Job job,
    ParameterValue parameterValue,
    boolean isBulk,
    Long checkedAt) {
    ParameterVerification parameterVerification = new ParameterVerification();
    parameterVerification.setJob(job);
    parameterVerification.setParameterValue(parameterValue);
    parameterVerification.setVerificationType(verificationType);
    parameterVerification.setVerificationStatus(expectedState);
    parameterVerification.setCreatedBy(userEntity);
    parameterVerification.setModifiedBy(userEntity);
    parameterVerification.setUser(userEntity);
    parameterVerification.setCreatedAt(createdAt);
    parameterVerification.setModifiedAt(checkedAt);
    parameterVerification.setBulk(isBulk);

    return parameterVerification;
  }

  private TempParameterVerification createTempParameterVerification(
    long createdAt,
    User userEntity,
    Type.VerificationType verificationType,
    State.ParameterVerification expectedState,
    Job job,
    TempParameterValue tempParameterValue, boolean isBulk) {

    TempParameterVerification tempParameterVerification = new TempParameterVerification();
    tempParameterVerification.setJob(job);
    tempParameterVerification.setTempParameterValue(tempParameterValue);
    tempParameterVerification.setVerificationType(verificationType);
    tempParameterVerification.setVerificationStatus(expectedState);
    tempParameterVerification.setCreatedBy(userEntity);
    tempParameterVerification.setModifiedBy(userEntity);
    tempParameterVerification.setUser(userEntity);
    tempParameterVerification.setCreatedAt(createdAt);
    tempParameterVerification.setModifiedAt(DateTimeUtils.now());
    tempParameterVerification.setBulk(isBulk);

    return tempParameterVerification;
  }

  private void validateInitiateRequest(ParameterValue parameterValue) throws IOException, StreemException {
    Parameter parameter = parameterValue.getParameter();
    boolean isParameterValueOfMasterTask = parameterValueRepository.countParameterValueByParameterIdAndJobId(parameter.getId(), parameterValue.getJobId()) == 1;

    if (!isParameterValueOfMasterTask && (parameter.getType() == Type.Parameter.RESOURCE || parameter.getType() == Type.Parameter.MULTI_RESOURCE)) {
      // This case will fail if the master task's resource parameter is optional and not executed
      TaskExecution masterTaskExecution = taskExecutionRepository.findByTaskIdAndJobIdAndType(parameter.getTask().getId(), parameterValue.getJobId(), Type.TaskExecutionType.MASTER);
      ParameterValue masterTaskParameterValue = parameterValueRepository.findByTaskExecutionIdAndParameterId(masterTaskExecution.getId(), parameter.getId());

      if (!Utility.isEmpty(masterTaskParameterValue) && !Utility.isEmpty(masterTaskParameterValue.getChoices())) {
        List<ResourceParameterChoiceDto> masterTaskResourceParameterChoices = JsonUtils.jsonToCollectionType(JsonUtils.writeValueAsString(masterTaskParameterValue.getChoices()), List.class, ResourceParameterChoiceDto.class);
        List<ResourceParameterChoiceDto> latestExecutedResourceParameterChoices = JsonUtils.jsonToCollectionType(JsonUtils.writeValueAsString(parameterValue.getChoices()), List.class, ResourceParameterChoiceDto.class);

        validateResourceParameterChoices(parameter.getType(), latestExecutedResourceParameterChoices, masterTaskResourceParameterChoices, parameterValue.getState(), parameterValue.getId());

      }
    }
  }

  private void validateResourceParameterChoices(Type.Parameter type, List<ResourceParameterChoiceDto> latestExecutedResourceParameterChoices,
                                                List<ResourceParameterChoiceDto> masterTaskExecutedResourceParameterChoices,
                                                State.ParameterExecution parameterExecutionState, Long currentParameterValueId) throws StreemException {
    // If it's empty, then any resource can be selected
    // (keeping in mind if resource parameter is optional it's first execution can occur in any repeated task)
    if (!Utility.isEmpty(latestExecutedResourceParameterChoices)) {
      Set<ResourceParameterChoiceDto> choicesSet = new HashSet<>(latestExecutedResourceParameterChoices);
      switch (type) {
        case RESOURCE -> {
          // If it's not empty, then the latest-executed resource parameter choices should be equal of the current resource parameter choices
          if (!choicesSet.containsAll(masterTaskExecutedResourceParameterChoices)) {
            ValidationUtils.invalidate(currentParameterValueId, ErrorCode.RESOURCE_PARAMETER_INVALID_SELECTION);
          }

        }
        case MULTI_RESOURCE -> {
          // If it's not empty,
          // then the latest-executed resource parameter choices should be a subset
          // or equal of the current resource parameter choices

          //Checking if the current executed resource parameter choices have number of selections as the latest executed resource parameter
          boolean isPotentiallyExecuted = masterTaskExecutedResourceParameterChoices.size() == latestExecutedResourceParameterChoices.size();
          if (isPotentiallyExecuted) {
            for (ResourceParameterChoiceDto resourceParameterChoiceDto : masterTaskExecutedResourceParameterChoices) {
              if (!latestExecutedResourceParameterChoices.contains(resourceParameterChoiceDto)) {
                ValidationUtils.invalidate(currentParameterValueId, ErrorCode.MULTI_RESOURCE_PARAMETER_INVALID_SELECTION);
              }

            }
          } else {
            ValidationUtils.invalidate(currentParameterValueId, ErrorCode.MULTI_RESOURCE_PARAMETER_INVALID_SELECTION);
          }
        }

      }
    }
  }

  private void validateIfUserIsTaskAssignee(Long taskExecutionId, Long currentUserId) throws StreemException {
    boolean isAllowedUser = taskExecutionAssigneeRepository.existsByTaskExecutionIdAndUserId(taskExecutionId, currentUserId);
    if (!isAllowedUser) {
      ValidationUtils.invalidate(taskExecutionId, ErrorCode.USER_NOT_ASSIGNED_TO_EXECUTE_TASK);
    }
  }

  private void validateIfUserIsJobAssignee(Long jobId, Long userId) throws StreemException {
    if (!taskExecutionAssigneeRepository.isUserAssignedToAnyTask(jobId, userId)) {
      ValidationUtils.invalidate(jobId, ErrorCode.USER_NOT_ASSIGNED_TO_EXECUTE_JOB);
    }
  }

  private void validateIfSelfParameterVerificationIsCompleted(Long parameterExecutionId, Long jobId, PrincipalUser principalUser, Long parameterId) throws StreemException {
    ParameterVerification lastSelfParameterVerification = parameterVerificationRepository.findByJobIdAndParameterValueIdAndVerificationTypeAndUserId(jobId, parameterExecutionId, Type.VerificationType.SELF.toString(), principalUser.getId());
    if (lastSelfParameterVerification.getVerificationStatus() == State.ParameterVerification.PENDING) {
      ValidationUtils.invalidate(parameterId, ErrorCode.SELF_VERIFICATION_NOT_COMPLETED);
    }
  }

  //  Method to filter the parameter verifications
  private List<ParameterVerification> filterVerifications(List<ParameterVerification> parameterVerifications) {
    Map<Long, List<ParameterVerification>> verificationMap = parameterVerifications.stream()
      .collect(Collectors.groupingBy(ParameterVerification::getParameterValueId));

    List<ParameterVerification> result = new ArrayList<>();

    for (List<ParameterVerification> verifications : verificationMap.values()) {
      // For SELF verifications
      List<ParameterVerification> selfVerifications = verifications.stream()
        .filter(pv -> pv.getVerificationType() == Type.VerificationType.SELF)
        .collect(Collectors.toList());

      if (!selfVerifications.isEmpty()) {
        long maxModifiedAt = selfVerifications.stream()
          .mapToLong(ParameterVerification::getModifiedAt)
          .max()
          .orElse(Long.MIN_VALUE);

        List<ParameterVerification> latestSelfVerifications = selfVerifications.stream()
          .filter(pv -> pv.getModifiedAt() == maxModifiedAt)
          .collect(Collectors.toList());

        result.addAll(latestSelfVerifications);
      }

      // For PEER verifications
      List<ParameterVerification> peerVerifications = verifications.stream()
        .filter(pv -> pv.getVerificationType() == Type.VerificationType.PEER)
        .collect(Collectors.toList());

      if (!peerVerifications.isEmpty()) {
        long maxModifiedAt = peerVerifications.stream()
          .mapToLong(ParameterVerification::getModifiedAt)
          .max()
          .orElse(Long.MIN_VALUE);

        List<ParameterVerification> latestPeerVerifications = peerVerifications.stream()
          .filter(pv -> pv.getModifiedAt() == maxModifiedAt)
          .collect(Collectors.toList());

        result.addAll(latestPeerVerifications);
      }
    }

    return result;
  }


  private List<TempParameterVerification> filterTempVerifications(List<TempParameterVerification> parameterVerifications) {
    Map<Long, List<TempParameterVerification>> verificationMap = parameterVerifications.stream()
      .collect(Collectors.groupingBy(TempParameterVerification::getTempParameterValueId));

    List<TempParameterVerification> result = new ArrayList<>();

    for (List<TempParameterVerification> verifications : verificationMap.values()) {
      // For SELF verifications
      List<TempParameterVerification> selfVerifications = verifications.stream()
        .filter(pv -> pv.getVerificationType() == Type.VerificationType.SELF)
        .collect(Collectors.toList());

      if (!selfVerifications.isEmpty()) {
        long maxModifiedAt = selfVerifications.stream()
          .mapToLong(TempParameterVerification::getModifiedAt)
          .max()
          .orElse(Long.MIN_VALUE);

        List<TempParameterVerification> latestSelfVerifications = selfVerifications.stream()
          .filter(pv -> pv.getModifiedAt() == maxModifiedAt)
          .collect(Collectors.toList());

        result.addAll(latestSelfVerifications);
      }

      // For PEER verifications
      List<TempParameterVerification> peerVerifications = verifications.stream()
        .filter(pv -> pv.getVerificationType() == Type.VerificationType.PEER)
        .collect(Collectors.toList());

      if (!peerVerifications.isEmpty()) {
        long maxModifiedAt = peerVerifications.stream()
          .mapToLong(TempParameterVerification::getModifiedAt)
          .max()
          .orElse(Long.MIN_VALUE);

        List<TempParameterVerification> latestPeerVerifications = peerVerifications.stream()
          .filter(pv -> pv.getModifiedAt() == maxModifiedAt)
          .collect(Collectors.toList());

        result.addAll(latestPeerVerifications);
      }
    }
    return result;
  }

}
