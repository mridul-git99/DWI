package com.leucine.streem.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leucine.streem.collections.partial.PartialEntityObject;
import com.leucine.streem.constant.*;
import com.leucine.streem.constant.Action;
import com.leucine.streem.dto.*;
import com.leucine.streem.dto.mapper.*;
import com.leucine.streem.dto.projection.ParameterValueView;
import com.leucine.streem.dto.projection.TaskExecutorLockErrorView;
import com.leucine.streem.dto.projection.VariationView;
import com.leucine.streem.dto.request.*;
import com.leucine.streem.dto.response.Error;
import com.leucine.streem.dto.response.ErrorCode;
import com.leucine.streem.exception.ExceptionType;
import com.leucine.streem.exception.ParameterExecutionException;
import com.leucine.streem.exception.ResourceNotFoundException;
import com.leucine.streem.exception.StreemException;
import com.leucine.streem.model.*;
import com.leucine.streem.model.helper.BaseEntity;
import com.leucine.streem.model.helper.ParameterSpecificationBuilder;
import com.leucine.streem.model.helper.PrincipalUser;
import com.leucine.streem.model.helper.parameter.*;
import com.leucine.streem.model.helper.search.SearchFilter;
import com.leucine.streem.model.helper.search.SearchOperator;
import com.leucine.streem.model.helper.search.Selector;
import com.leucine.streem.repository.*;
import com.leucine.streem.service.*;
import com.leucine.streem.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParameterExecutionService implements IParameterExecutionService {
  private final IParameterValueRepository parameterValueRepository;
  private final IParameterMapper parameterMapper;
  private final IParameterValueMapper parameterValueMapper;
  private final IUserRepository userRepository;
  private final IMediaRepository mediaRepository;
  private final ITempParameterValueRepository tempParameterValueRepository;
  private final IParameterValueApprovalRepository iParameterValueApprovalRepository;
  private final ITempParameterValueMapper tempParameterValueMapper;
  private final IUserMapper userMapper;
  private final IJobAuditService jobAuditService;
  private final ITempParameterValueMediaMapper tempParameterValueMediaMapper;
  private final IJobLogService jobLogService;
  private final IParameterRepository parameterRepository;
  private final INotificationService notificationService;
  private final ITaskExecutionRepository taskExecutionRepository;
  private final IParameterExecutionValidationService parameterExecutionValidationService;
  private final ITaskExecutionAssigneeRepository taskExecutionAssigneeRepository;
  private final IVariationRepository variationRepository;
  private final IVariationMapper variationMapper;
  private final IVariationMediaMappingRepository variationMediaMappingRepository;
  private final IRulesExecutionService rulesExecutionService;
  private final IParameterVerificationRepository parameterVerificationRepository;
  private final IParameterVerificationMapper parameterVerificationMapper;
  private final ITempParameterVerificationMapper tempParameterVerificationMapper;
  private final ITempParameterVerificationRepository tempParameterVerificationRepository;
  private final ITaskExecutorLockRepository taskExecutorLockRepository;
  private final ITaskRepository taskRepository;
  private final IFacilityRepository facilityRepository;
  private final IEntityObjectService entityObjectService;
  private final IParameterExceptionRepository parameterExceptionRepository;

  @Override
  @Transactional(rollbackFor = Exception.class, noRollbackFor = ParameterExecutionException.class)
  public ParameterDto executeParameter(Long jobId, ParameterExecuteRequest parameterExecuteRequest, boolean isAutoInitialized, Type.JobLogTriggerType jobLogTriggerType, PrincipalUser principalUser, boolean isCreateJobRequest, boolean isScheduled) throws StreemException, ResourceNotFoundException, IOException, ParameterExecutionException {
    long x = System.currentTimeMillis();
    ParameterDto parameterDto;
    User principalUserEntity = userRepository.getReferenceById(principalUser.getId());
    ParameterRequest parameterRequest = parameterExecuteRequest.getParameter();
    ParameterValue parameterValue = parameterValueRepository.findLatestByJobIdAndParameterId(jobId, parameterRequest.getId());
    State.ParameterExecution previousState = parameterValue.getState();
    Parameter parameter = parameterValue.getParameter();
    if (parameter.getTargetEntityType() == Type.ParameterTargetEntityType.PROCESS && !isCreateJobRequest) {
      validateJobState(jobId, Action.Parameter.CJF_EXECUTE, parameterValue.getJob().getState());
    }
    TaskExecution taskExecution = parameterValue.getTaskExecution();
    Long recentTimestamp = parameterValue.getClientEpoch();
    Long clientEpoch = parameterExecuteRequest.getClientEpoch();
    clientEpoch = clientEpoch == null ? DateTimeUtils.nowInMillis() : clientEpoch;
    if (!Utility.isEmpty(recentTimestamp) && recentTimestamp > clientEpoch) {
      parameterDto = parameterMapper.toDto(parameter);
      parameterDto.setResponse(List.of(parameterValueMapper.toDto(parameterValue)));
    } else {
      parameterValue.setClientEpoch(clientEpoch);

      boolean isCJFParameter = parameter.getTargetEntityType() == Type.ParameterTargetEntityType.PROCESS;
      if (!isCJFParameter) {
        Task task = taskExecution.getTask();
        if (!isAutoInitialized) {
          validateIfUserIsAssignedToExecuteParameter(parameterValue.getTaskExecutionId(), principalUser.getId());
        }
        if (task.isHasExecutorLock()) {
          validateIfCurrentExecutorIsAllowedToInteractTask(taskExecution.getTaskId(), taskExecution.getJobId(), principalUser.getId());
        }
      }

      if (!isCJFParameter && parameter.getTask().isSoloTask() && !Objects.equals(taskExecution.getStartedBy().getId(), principalUser.getId()) && !isAutoInitialized) {
        String errorMessage = String.format("%s %s %s (ID: %s)", ErrorCode.SOLO_TASK_LOCKED.getDescription(), taskExecution.getStartedBy().getFirstName(), taskExecution.getStartedBy().getLastName(), taskExecution.getStartedBy().getEmployeeId());
        ValidationUtils.invalidate(String.valueOf(taskExecution.getId()), ErrorCode.SOLO_TASK_LOCKED, errorMessage);
      }

      boolean isCJFParameterOrTaskExecutionStateInProgress = isCJFParameter || Misc.TASK_EXECUTION_EXECUTABLE_STATES.contains(parameterValue.getTaskExecution().getState());
      // If a cjf parameter is executed once it cannot be re-executed. But if a task is paused or in progress state we allow execution
      if (isCJFParameterOrTaskExecutionStateInProgress) {
        if (parameterValue.getState().equals(State.ParameterExecution.APPROVAL_PENDING)) {
          ValidationUtils.invalidate(parameterValue.getParameterId(), ErrorCode.CANNOT_EXECUTE_VERIFICATION_PENDING_PARAMETER);
        }

        if (!isAutoInitialized && Type.ParameterTargetEntityType.TASK.equals(parameter.getTargetEntityType())) {
          validateJobState(parameterValue.getJobId(), Action.Parameter.EXECUTE, parameterValue.getJob().getState());

          validateTaskState(taskExecution.getState(), Action.Parameter.EXECUTE, parameter.getId());
          List<TaskExecutionUserMapping> taskExecutionUserMapping = taskExecutionAssigneeRepository.findByTaskExecutionAndUser(taskExecution.getId(), principalUserEntity.getId())
            .orElseThrow(() -> new ResourceNotFoundException(parameter.getTaskId(), ErrorCode.USER_NOT_ASSIGNED_TO_EXECUTE_TASK, ExceptionType.ENTITY_NOT_FOUND));

          updateUserAction(taskExecutionUserMapping);
        }
        ParameterValueDto oldValueDto = parameterValueMapper.toDto(parameterValue);
        List<MediaDto> oldMedias = parameterMapper.getMedias(parameterValue.getMedias());


        if (parameter.isAutoInitialized() || parameter.getType().equals(Type.Parameter.CALCULATION)) {
          principalUserEntity = userRepository.findById(Long.valueOf(Misc.SYSTEM_USER_ID)).get();
        }
        Long taskExecutionId = parameter.getTargetEntityType() == Type.ParameterTargetEntityType.PROCESS ? null : taskExecution.getId();

        //TODO accepting a response due to strange behaviour observed in
        // single select parameter not updating 'state' inspite of which
        // when debugging state seems to be updated
        List<Error> errors = new ArrayList<>();
        try {
          parameterValue = setParameterResponse(taskExecutionId, parameter, parameterValue, parameterRequest.getData().toString(), parameterExecuteRequest.getReason(),
            jobId, false, null,
            parameterValue.getMedias(), null, principalUserEntity, parameterExecuteRequest.getReferencedParameterId(), isScheduled);
          if (parameterValue.getState() == State.ParameterExecution.EXECUTED) {
            parameterValue.setHasActiveException(false);
          }
        } catch (ParameterExecutionException e) {
          parameterValue.setHasActiveException(true);
          errors.addAll(e.getErrorList());
        }

        parameterValue.setModifiedBy(principalUserEntity);
        parameterValue.setModifiedAt(DateTimeUtils.now());

  /* we are handling this here because for some parameters like checklist if all the items are not se
  ected we set their state to BEING_EXECUTED.
  This state belongs to partially executed parameters as well as parameters that have verifications and are not executed*/

        if (!parameter.getVerificationType().equals(Type.VerificationType.NONE) && parameterValue.getState().equals(State.ParameterExecution.EXECUTED)) {
          parameterValue.setState(State.ParameterExecution.BEING_EXECUTED);
        }

        parameterDto = parameterMapper.toDto(parameter);
        parameterValue = parameterValueRepository.save(parameterValue);
        List<VariationDto> variationDtoList = variationMapper.toDtoList(parameterValue.getVariations());
        ParameterValueDto parameterValueDto = parameterValueMapper.toDto(parameterValue);
        parameterValueDto.setVariations(variationDtoList);
        // Adding this logic because attach parameter verifications is only required for the primarily executed entity
        if (!isAutoInitialized) {
          attachParameterVerifications(parameter, jobId, parameterValue, parameterValueDto);
        }
        parameterValueDto.setParameterVerifications(null);
        List<ParameterValueDto> responses = new ArrayList<>();
        responses.add(parameterValueDto);
        parameterDto.setResponse(responses);


        if (parameterValue.getState() == State.ParameterExecution.EXECUTED || (parameterValue.getState() == State.ParameterExecution.BEING_EXECUTED && parameter.getVerificationType() == Type.VerificationType.NONE && !parameterValue.isHasActiveException())) {
          RuleHideShowDto ruleHideShowDto = updateRules(jobId, parameter, parameterValue);
          if (!Utility.isEmpty(ruleHideShowDto)) {
            parameterDto.setShow(ruleHideShowDto.getShow());
            parameterDto.setHide(ruleHideShowDto.getHide());
          }
        }
        // TODO Update log parameter logic to read from parameter value, but also remember executions are done fast ? make use of queues ? the case is same with job logs
        UserAuditDto userBasicInfoDto = userMapper.toUserAuditDto(principalUserEntity);

        jobLogService.updateJobLog(jobId, parameter.getId(), parameter.getType(), parameterExecuteRequest.getReason(), parameter.getLabel(), jobLogTriggerType, userBasicInfoDto);

        PrincipalUser triggeredBy = principalUser;
        if (parameter.isAutoInitialized() || parameter.getType().equals(Type.Parameter.CALCULATION)) {
          triggeredBy = userMapper.clone(principalUser);
          triggeredBy.setId(Long.valueOf(Misc.SYSTEM_USER_ID)).setFirstName(Misc.SYSTEM_USER_FIRST_NAME).setLastName(null).setEmployeeId(Misc.SYSTEM_USER_EMPLOYEE_ID);
        }
        jobAuditService.executedParameter(jobId, parameterValue.getId(), parameter.getId(), oldValueDto, oldMedias, parameter.getType(), false, parameterExecuteRequest.getReason(), null, triggeredBy, parameterExecuteRequest, isCJFParameter);

        if (!Utility.isEmpty(errors)) {
          errors.forEach(error -> {
            ParameterDetailsDto parameterDetailsDto = (ParameterDetailsDto) error.getErrorInfo();
            parameterDetailsDto.setCurrentParameterDto(parameterDto);
          });
          throw new ParameterExecutionException(errors);
        }
        return parameterDto;
      } else {
        parameterDto = parameterMapper.toDto(parameter);
        ParameterValueDto parameterValueDto = parameterValueMapper.toDto(parameterValueRepository.save(parameterValue));
        parameterDto.setResponse(List.of(parameterValueDto));
        // Frontend show up the task if hide and show are null. So we are setting it to empty set for now
        parameterDto.setHide(new HashSet<>());
        parameterDto.setShow(new HashSet<>());
        return parameterDto;
      }
    }
    long y = System.currentTimeMillis();
    System.out.printf("Total time take for parameter execution: %s", (y - x));
    return parameterDto;
  }

  public void validateIfUserIsAssignedToExecuteParameter(Long taskExecutionId, Long currentUserId) throws StreemException {
    boolean isAllowedUser = taskExecutionAssigneeRepository.existsByTaskExecutionIdAndUserId(taskExecutionId, currentUserId);
    if (!isAllowedUser) {
      ValidationUtils.invalidate(taskExecutionId, ErrorCode.USER_NOT_ASSIGNED_TO_EXECUTE_TASK);
    }
  }

  private void validateIfUserIsAssignedToExecuteJob(Long jobId, Long userId) throws StreemException {
    if (!taskExecutionAssigneeRepository.isUserAssignedToAnyTask(jobId, userId)) {
      ValidationUtils.invalidate(jobId, ErrorCode.USER_NOT_ASSIGNED_TO_EXECUTE_JOB);
    }
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public TempParameterDto executeParameterForError(ParameterExecuteRequest parameterExecuteRequest) throws IOException, StreemException, ResourceNotFoundException, ParameterExecutionException {
    log.info("[executeParameterForError] Request to fix error in parameter, parameterExecuteRequest: {}", parameterExecuteRequest);
    TempParameterDto tempParameterDto = null;
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    User principalUserEntity = userRepository.getReferenceById(principalUser.getId());
    ParameterRequest parameterRequest = parameterExecuteRequest.getParameter();
    Long jobId = parameterExecuteRequest.getJobId();
    Long parameterExecutionId = parameterValueRepository.findByJobIdAndParameterIdWithCorrectionEnabled(jobId, parameterRequest.getId());
    ParameterValue parameterValue = parameterValueRepository.findById(parameterExecutionId)
      .orElseThrow(() -> new ResourceNotFoundException(parameterExecutionId, ErrorCode.PARAMETER_VALUE_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));
    Parameter parameter = parameterValue.getParameter();
    TaskExecution taskExecution = parameterValue.getTaskExecution();
    TempParameterValue tempParameterValue = tempParameterValueRepository.findByParameterIdAndTaskExecutionId(parameter.getId(), taskExecution.getId()).get();
    validateIfUserIsAssignedToExecuteParameter(parameterValue.getTaskExecutionId(), principalUser.getId());
    Long recentTimestamp = parameterValue.getClientEpoch();
    Long clientEpoch = parameterExecuteRequest.getClientEpoch();
    clientEpoch = clientEpoch == null ? DateTimeUtils.nowInMillis() : clientEpoch;
    if (!Utility.isEmpty(recentTimestamp) && recentTimestamp > clientEpoch) {
      tempParameterDto = parameterMapper.toTempParameterDto(parameter);
      tempParameterDto.setResponse(List.of(tempParameterValueMapper.toDto(tempParameterValue)));
    } else {
      tempParameterValue.setClientEpoch(clientEpoch);

      //TODO temp fix, find the right approach
      UserAuditDto modifiedBy = userMapper.toUserAuditDto(parameterValue.getModifiedBy());

      // TODO: this complete logic till throwing error is a workaround for task execution not having correction enabled on the task where auto initialize parameter is present
      if (!taskExecution.isCorrectionEnabled()) {
        ValidationUtils.invalidate(parameter.getTaskId(), ErrorCode.DEPENDENT_TASK_NOT_ENABLED_FOR_CORRECTION);
      }

      validateJobState(parameterValue.getJobId(), Action.Parameter.EXECUTE, parameterValue.getJob().getState());
      tempParameterDto = parameterMapper.toTempParameterDto(parameter);

      TempParameterValueDto oldValueDto = tempParameterValueMapper.toDto(tempParameterValue);
      List<MediaDto> oldMedias = tempParameterValueMediaMapper.toDto(tempParameterValue.getMedias());
      TempParameterValueDto tempParameterValueDto = new TempParameterValueDto();

      //set responses

      tempParameterValue = setParameterResponse(taskExecution.getId(), parameter, tempParameterValue, parameterRequest.getData().toString(), parameterExecuteRequest.getReason(),
        parameterExecuteRequest.getJobId(), true, tempParameterValueDto, null, tempParameterValue.getMedias(), principalUserEntity, parameterExecuteRequest.getReferencedParameterId(), false);

      //This updates medias
      if (Type.PARAMETER_MEDIA_TYPES.contains(parameter.getType())) {
        tempParameterValueDto.setMedias(tempParameterValueMediaMapper.toDto(tempParameterValue.getMedias()));
      }

      if (!parameter.getVerificationType().equals(Type.VerificationType.NONE) && tempParameterValue.getState().equals(State.ParameterExecution.EXECUTED)) {
        tempParameterValue.setState(State.ParameterExecution.BEING_EXECUTED);
        tempParameterValueDto.setState(State.ParameterExecution.BEING_EXECUTED);
        tempParameterValueRepository.updateTempParameterValueByStateAndId(State.ParameterExecution.BEING_EXECUTED.toString(), tempParameterValue.getId());
      }
      tempParameterValue = tempParameterValueRepository.save(tempParameterValue);

      // Adding this logic because attach parameter verifications are only required for the primarily executed entity
      if (!parameter.isAutoInitialized()) {
        attachTempParameterVerifications(parameter, parameterExecuteRequest.getJobId(), tempParameterValue, tempParameterValueDto);
      }
      PartialAuditDto partialAuditDto = new PartialAuditDto();
      partialAuditDto.setModifiedBy(modifiedBy);
      partialAuditDto.setModifiedAt(parameterValue.getModifiedAt());
      tempParameterValueDto.setAudit(partialAuditDto);
      tempParameterValueDto.setParameterVerifications(null);
      tempParameterDto.setResponse(List.of(tempParameterValueDto));

      jobAuditService.executedParameter(parameterExecuteRequest.getJobId(), tempParameterValue.getId(), parameter.getId(), oldValueDto, oldMedias, parameter.getType(), true, parameterExecuteRequest.getReason(), null, principalUser, null, false);
    }
    return tempParameterDto;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public ParameterDto rejectParameter(Long parameterExecutionId, ParameterStateChangeRequest parameterStateChangeRequest) throws ResourceNotFoundException, StreemException {
    log.info("[rejectParameter] Request to reject parameter, parameterStateChangeRequest: {}", parameterStateChangeRequest);
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    User principalUserEntity = userRepository.getOne(principalUser.getId());

    ParameterValue parameterValue = parameterValueRepository.findById(parameterExecutionId)
      .orElseThrow(() -> new ResourceNotFoundException(parameterExecutionId, ErrorCode.PARAMETER_VALUE_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));
    validateReviewerRoleForParameterApproval(parameterStateChangeRequest.getParameterId());

    TaskExecution taskExecution = parameterValue.getTaskExecution();
    Task task = taskRepository.findById(taskExecution.getTaskId())
      .orElseThrow(() -> new ResourceNotFoundException(taskExecution.getId(), ErrorCode.TASK_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));

    if (task.isSoloTask()) {
      validateIfUserIsAssignedToExecuteJob(parameterValue.getJobId(), principalUser.getId());
    } else {
      validateIfUserIsAssignedToExecuteParameter(parameterValue.getTaskExecutionId(), principalUser.getId());
    }
    Parameter parameter = parameterValue.getParameter();

    validateJobState(parameterValue.getJob().getId(), Action.Parameter.REJECTED, parameterValue.getJob().getState());
    validateTaskState(taskExecution.getState(), Action.Parameter.REJECTED, parameter.getId());

    ParameterDto parameterDto = updateParameterState(parameterValue, State.ParameterExecution.BEING_EXECUTED_AFTER_REJECTED, principalUserEntity, State.ParameterValue.REJECTED);

    jobAuditService.rejectParameter(parameterStateChangeRequest.getJobId(), parameterDto, parameterStateChangeRequest.getParameterId(), principalUser);
    return parameterDto;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public ParameterDto approveParameter(Long parameterExecutionId, ParameterStateChangeRequest parameterStateChangeRequest) throws ResourceNotFoundException, StreemException, IOException {
    log.info("[approveParameter] Request to approve parameter, parameterStateChangeRequest: {}", parameterStateChangeRequest);
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    User principalUserEntity = userRepository.getOne(principalUser.getId());

    ParameterValue parameterValue = parameterValueRepository.findById(parameterExecutionId)
      .orElseThrow(() -> new ResourceNotFoundException(parameterExecutionId, ErrorCode.PARAMETER_VALUE_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));
    validateReviewerRoleForParameterApproval(parameterStateChangeRequest.getParameterId());

    TaskExecution taskExecution = parameterValue.getTaskExecution();
    Task task = taskRepository.findById(taskExecution.getTaskId())
      .orElseThrow(() -> new ResourceNotFoundException(taskExecution.getId(), ErrorCode.TASK_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));

    if (task.isSoloTask()) {
      validateIfUserIsAssignedToExecuteJob(parameterValue.getJobId(), principalUser.getId());
    } else {
      validateIfUserIsAssignedToExecuteParameter(parameterValue.getTaskExecutionId(), principalUser.getId());
    }

    Parameter parameter = parameterValue.getParameter();

    validateJobState(parameterValue.getJob().getId(), Action.Parameter.APPROVED, parameterValue.getJob().getState());
    validateTaskState(taskExecution.getState(), Action.Parameter.APPROVED, parameter.getId());

    State.ParameterExecution executionState = State.ParameterExecution.EXECUTED;
    if (!Type.VerificationType.NONE.equals(parameter.getVerificationType())) {
      executionState = State.ParameterExecution.BEING_EXECUTED;
    }

    ParameterDto parameterDto = updateParameterState(parameterValue, executionState, principalUserEntity, State.ParameterValue.APPROVED);
    if (parameterValue.getState() == State.ParameterExecution.EXECUTED) {
      ParameterExecuteRequest parameterExecuteRequest = new ParameterExecuteRequest();
      parameterExecuteRequest.setJobId(parameterValue.getJobId());
      ParameterRequest parameterRequest = new ParameterRequest();
      parameterRequest.setData(parameter.getData());
      parameterRequest.setId(parameter.getId());
      parameterRequest.setLabel(parameter.getLabel());
      parameterExecuteRequest.setParameter(parameterRequest);
      parameterExecuteRequest.setReferencedParameterId(parameter.getId());
    }

    jobAuditService.approveParameter(parameterStateChangeRequest.getJobId(), parameterDto, parameterStateChangeRequest.getParameterId(), principalUser);
    return parameterDto;
  }

  //TODO validations to state management ?

  private void validateJobState(Long id, Action.Parameter action, State.Job state) throws StreemException {
    if (Action.Parameter.EXECUTE.equals(action) && State.JOB_COMPLETED_STATES.contains(state)) {
      ValidationUtils.invalidate(id, ErrorCode.JOB_ALREADY_COMPLETED);
    }
    if (!Action.PARAMETER_APROVAL_ACTIONS.contains(action) && State.Job.BLOCKED.equals(state)) {
      ValidationUtils.invalidate(id, ErrorCode.JOB_IS_BLOCKED);
    }
    if (Action.Parameter.CJF_EXECUTE.equals(action) && !State.JOB_NOT_STARTED_STATES.contains(state)) {
      ValidationUtils.invalidate(id, ErrorCode.CANNOT_EXECUTE_PARAMETER_ON_A_STARTED_JOB);
    }
  }

  private void validateTaskState(State.TaskExecution taskExecutionState, Action.Parameter action, Long id) throws StreemException {
    if (Action.Parameter.EXECUTE.equals(action) && (State.TASK_COMPLETED_STATES.contains(taskExecutionState))) {
      ValidationUtils.invalidate(id, ErrorCode.CANNOT_EXECUTE_PARAMETER_ON_A_COMPLETED_TASK);
    }
    if (Action.Parameter.EXECUTE.equals(action) && State.TaskExecution.NOT_STARTED.equals(taskExecutionState)) {
      ValidationUtils.invalidate(id, ErrorCode.CANNOT_EXECUTE_PARAMETER_ON_A_NONSTARTED_TASK);
    }
  }

  private <T extends ParameterValueBase> T setParameterResponse(Long taskExecutionId, Parameter parameter, T parameterValue, String data,
                                                                String reason, Long jobId, boolean isExecutedForCorrection, TempParameterValueDto tempParameterValueDto,
                                                                List<ParameterValueMediaMapping> parameterValueMediaMappings, List<TempParameterValueMediaMapping> tempParameterValueMediaMappings, User principalUserEntity, Long referencedParameterId, boolean isScheduled) throws IOException, StreemException, ResourceNotFoundException, ParameterExecutionException {
    Type.Parameter parameterType = parameter.getType();
    return switch (parameterType) {
      case CALCULATION ->
        executeCalculationParameter(parameterValue, data, isExecutedForCorrection, tempParameterValueDto, principalUserEntity, referencedParameterId);
      case CHECKLIST ->
        executeChecklistParameter(parameterValue, parameter, data, reason, isExecutedForCorrection, tempParameterValueDto, principalUserEntity);
      case DATE, DATE_TIME ->
        executeDateParameter(parameterValue, data, tempParameterValueDto, isExecutedForCorrection, parameter, principalUserEntity);
      case MEDIA, FILE_UPLOAD ->
        executeMediaParameter(parameterValue, parameter, data, parameterValueMediaMappings, tempParameterValueMediaMappings, principalUserEntity, isExecutedForCorrection);
      case MULTISELECT ->
        executeMultiSelectParameter(parameterValue, parameter, data, reason, isExecutedForCorrection, tempParameterValueDto, principalUserEntity);
      case NUMBER ->
        executeNumberParameter(parameterValue, data, tempParameterValueDto, isExecutedForCorrection, parameter, jobId, principalUserEntity);
      case SHOULD_BE ->
        executeShouldBeParameter(parameterValue, parameter, data, reason, isExecutedForCorrection, tempParameterValueDto, principalUserEntity);
      case RESOURCE, MULTI_RESOURCE ->
        executeResourceParameter(parameterValue, parameter, jobId, data, isExecutedForCorrection, tempParameterValueDto, principalUserEntity, isScheduled);
      case SINGLE_SELECT ->
        executeSingleSelectParameter(parameterValue, parameter, data, isExecutedForCorrection, tempParameterValueDto, principalUserEntity);
      case SIGNATURE ->
        executeSignatureParameter(parameterValue, parameter, data, isExecutedForCorrection, parameterValueMediaMappings, tempParameterValueMediaMappings, principalUserEntity);
      case MULTI_LINE, SINGLE_LINE ->
        executeTextParameter(parameterValue, data, tempParameterValueDto, isExecutedForCorrection, parameter, principalUserEntity);
      case YES_NO ->
        executeYesNoParameter(parameterValue, parameter, jobId, data, reason, isExecutedForCorrection, tempParameterValueDto, principalUserEntity);
      default -> parameterValue;
    };
  }

  private <T extends ParameterValueBase> T executeMediaParameter(T parameterValue, Parameter parameter, String data,
                                                                 List<ParameterValueMediaMapping> parameterValueMediaMappings,
                                                                 List<TempParameterValueMediaMapping> tempParameterValueMediaMappings,
                                                                 User principalUserEntity, boolean isExecutedForCorrection) throws IOException, StreemException {
    MediaParameterBase mediaParameterBase = (MediaParameterBase) JsonUtils.readValue(data, ParameterUtils.getClassForParameter(Type.Parameter.MEDIA));
    List<ExecuteMediaPrameterRequest> mediaRequestList = mediaParameterBase.getMedias();

    if (Utility.isEmpty(mediaRequestList)) {
      ValidationUtils.invalidate(parameter.getId(), ErrorCode.PARAMETER_EXECUTION_MEDIA_CANNOT_BE_EMPTY);
    }

    Map<Long, ExecuteMediaPrameterRequest> mediaRequestMap = mediaRequestList.stream().collect(Collectors.toMap(m -> Long.valueOf(m.getMediaId()), Function.identity()));
    Set<Long> mediaIds = mediaRequestList.stream().map(m -> Long.valueOf(m.getMediaId())).collect(Collectors.toSet());

    List<Media> mediaList = mediaRepository.findAll(mediaIds);

    for (Media media : mediaList) {
      if (Utility.trimAndCheckIfEmpty(mediaRequestMap.get(media.getId()).getName())) {
        ValidationUtils.invalidate(parameter.getId(), ErrorCode.PARAMETER_EXECUTION_MEDIA_NAME_CANNOT_BE_EMPTY);
      }
      media.setName(mediaRequestMap.get(media.getId()).getName());
      media.setDescription(mediaRequestMap.get(media.getId()).getDescription());
    }

    mediaRepository.saveAll(mediaList);
    Set<Long> existingMediaIds;
    if (isExecutedForCorrection) {
      existingMediaIds = tempParameterValueMediaMappings.stream().map(pvm -> pvm.getParameterValueMediaId().getMediaId()).collect(Collectors.toSet());
    } else {
      existingMediaIds = parameterValueMediaMappings.stream().map(pvm -> pvm.getParameterValueMediaId().getMediaId()).collect(Collectors.toSet());
    }

    List<Media> archivedMedias = new ArrayList<>();

    for (Media media : mediaList) {
      if (existingMediaIds.contains(media.getId())) {
        ExecuteMediaPrameterRequest mediaParameterRequest = mediaRequestMap.get(media.getId());
        if (mediaParameterRequest.getArchived()) {
          if (Utility.isEmpty(mediaParameterRequest.getReason())) {
            ValidationUtils.invalidate(parameter.getId(), ErrorCode.PARAMETER_EXECUTION_MEDIA_ARCHIVED_REASON_CANNOT_BE_EMPTY);
          }
          archivedMedias.add(media);
          parameterValue.setModifiedBy(principalUserEntity);
          parameterValue.setModifiedAt(DateTimeUtils.now());
        }
      } else {
        parameterValue.addMedia(media, principalUserEntity);
        parameterValue.setModifiedBy(principalUserEntity);
        parameterValue.setModifiedAt(DateTimeUtils.now());
      }
    }

    for (Media media : archivedMedias) {
      parameterValue.archiveMedia(media, principalUserEntity);
    }

    State.ParameterExecution parameterExecutionState = parameterExecutionValidationService.isParameterExecutedPartially(parameter, data, isExecutedForCorrection, parameterValueMediaMappings, tempParameterValueMediaMappings) ? State.ParameterExecution.BEING_EXECUTED : State.ParameterExecution.EXECUTED;
    parameterValue.setState(parameterExecutionState);


    return parameterValue;
  }

  private <T extends ParameterValueBase> T executeSignatureParameter(T parameterValue, Parameter parameter, String data, boolean isExecutedForCorrection,
                                                                     List<ParameterValueMediaMapping> parameterValueMediaMappings,
                                                                     List<TempParameterValueMediaMapping> tempParameterValueMediaMappings,
                                                                     User principalUserEntity) throws JsonProcessingException, StreemException {
    MediaParameter mediaParameter = JsonUtils.readValue(data, MediaParameter.class);
    List<ExecuteMediaPrameterRequest> mediaDtoList = mediaParameter.getMedias();

    if (Utility.isEmpty(mediaDtoList)) {
      ValidationUtils.invalidate(parameter.getId(), ErrorCode.PARAMETER_EXECUTION_MEDIA_CANNOT_BE_EMPTY);
    }

    Media media = mediaRepository.findById(Long.valueOf(mediaDtoList.get(0).getMediaId())).get();
    parameterValue.setState(State.ParameterExecution.EXECUTED);

    if (!isExecutedForCorrection) {
      if (null != parameterValueMediaMappings) {
        for (ParameterValueMediaMapping parameterValueMediaMapping : parameterValueMediaMappings) {
          parameterValue.archiveMedia(parameterValueMediaMapping.getMedia(), principalUserEntity);
        }
      }
    } else {
      if (null != tempParameterValueMediaMappings) {
        for (TempParameterValueMediaMapping tempParameterValueMediaMapping : tempParameterValueMediaMappings) {
          parameterValue.archiveMedia(tempParameterValueMediaMapping.getMedia(), principalUserEntity);
        }
        parameterValue.setModifiedAt(DateTimeUtils.now());
        parameterValue.setModifiedBy(principalUserEntity);
      }
    }

    parameterValue.addMedia(media, principalUserEntity);
    return parameterValue;
  }

  private <T extends ParameterValueBase> T executeTextParameter(T parameterValue, String data, TempParameterValueDto tempParameterValueDto, boolean isExecutedForCorrection, Parameter parameter, User principalUserEntity) throws IOException {
    TextParameter multiLineParameter = JsonUtils.readValue(data, TextParameter.class);
    State.ParameterExecution parameterExecutionState = parameterExecutionValidationService.isParameterExecutedPartially(parameter, data, isExecutedForCorrection, null, null) ? State.ParameterExecution.BEING_EXECUTED : State.ParameterExecution.EXECUTED;

    if (isExecutedForCorrection) {
      tempParameterValueDto.setValue(multiLineParameter.getInput());
    }

    parameterValue.setValue(multiLineParameter.getInput());
    parameterValue.setState(parameterExecutionState);
    parameterValue.setModifiedAt(DateTimeUtils.now());
    parameterValue.setModifiedBy(principalUserEntity);


    return parameterValue;
  }

  private <T extends ParameterValueBase> T executeYesNoParameter(T parameterValue, Parameter parameter, Long jobId, String data, String reason,
                                                                 boolean isExecutedForCorrection, TempParameterValueDto tempParameterValueDto, User principalUserEntity) throws IOException, StreemException {
    Map<String, String> parameterChoices = new HashMap<>();
    List<YesNoParameter> yesNoParameters = JsonUtils.jsonToCollectionType(data, List.class, YesNoParameter.class);
    for (YesNoParameter baseParameter : yesNoParameters) {
      String id = baseParameter.getId();
      String state = baseParameter.getState();
      if (State.Selection.SELECTED.equals(State.Selection.valueOf(state))) {
        parameterChoices.put(id, State.Selection.SELECTED.name());
        //TODO remove the types - the checklist data already has lower case
        if (baseParameter.getType().equals("no")) {
          ValidationUtils.validateNotEmpty(reason, parameter.getId(), ErrorCode.PROVIDE_REASON_FOR_YES_NO_PARAMETER);
        } else {
          reason = "";
        }
      } else {
        parameterChoices.put(id, State.Selection.NOT_SELECTED.name());
      }
    }

    if (isExecutedForCorrection) {
      JsonNode jsonNode = JsonUtils.valueToNode(parameterChoices);
      tempParameterValueDto.setChoices(jsonNode);
    }
    parameterValue.setChoices(JsonUtils.valueToNode(parameterChoices));
    parameterValue.setState(State.ParameterExecution.EXECUTED);
    parameterValue.setReason(reason);


    return parameterValue;
  }

  private <T extends ParameterValueBase> T executeChecklistParameter(T parameterValue, Parameter parameter, String data, String reason,
                                                                     boolean isExecutedForCorrection, TempParameterValueDto tempParameterValueDto, User principalUserEntity) throws IOException, StreemException {
    Map<String, String> parameterChoices = new HashMap<>();
    List<ChecklistParameter> parameters = JsonUtils.jsonToCollectionType(data, List.class, ChecklistParameter.class);
    // set update id and choice selections
    for (ChecklistParameter checklistParameter : parameters) {
      String id = checklistParameter.getId();
      String state = checklistParameter.getState();
      if (State.Selection.SELECTED.equals(State.Selection.valueOf(state))) {
        parameterChoices.put(id, State.Selection.SELECTED.name());
      } else {
        parameterChoices.put(id, State.Selection.NOT_SELECTED.name());
      }
    }
    JsonNode jsonNode = JsonUtils.valueToNode(parameterChoices);
    State.ParameterExecution parameterExecutionState = parameterExecutionValidationService.isParameterExecutedPartially(parameter, data, isExecutedForCorrection, null, null) ? State.ParameterExecution.BEING_EXECUTED : State.ParameterExecution.EXECUTED;

    if (isExecutedForCorrection) {
      tempParameterValueDto.setChoices(jsonNode);
      tempParameterValueDto.setState(parameterExecutionState);
    }

    parameterValue.setChoices(jsonNode);
    parameterValue.setState(parameterExecutionState);
    parameterValue.setModifiedBy(principalUserEntity);
    parameterValue.setModifiedAt(DateTimeUtils.now());
    return parameterValue;
  }

  private <T extends ParameterValueBase> T executeMultiSelectParameter(T parameterValue, Parameter parameter, String data, String reason,
                                                                       boolean isExecutedForCorrection, TempParameterValueDto tempParameterValueDto, User principalUserEntity) throws IOException, StreemException {
    Map<String, String> parameterChoices = new HashMap<>();
    List<ChoiceParameterBase> parameters = JsonUtils.jsonToCollectionType(data, List.class, MultiSelectParameter.class);
    // set update id and choice selections
    setParameterSelectionState(parameterChoices, parameters);
    JsonNode jsonNode = JsonUtils.valueToNode(parameterChoices);
    State.ParameterExecution parameterExecutionState = parameterExecutionValidationService.isParameterExecutedPartially(parameter, data, isExecutedForCorrection, null, null) ? State.ParameterExecution.BEING_EXECUTED : State.ParameterExecution.EXECUTED;

    if (isExecutedForCorrection) {
      tempParameterValueDto.setChoices(jsonNode);
      tempParameterValueDto.setState(parameterExecutionState);
    }

    parameterValue.setModifiedBy(principalUserEntity);
    parameterValue.setModifiedAt(DateTimeUtils.now());
    parameterValue.setChoices(jsonNode);
    parameterValue.setState(parameterExecutionState);

    return parameterValue;
  }

  private <T extends ParameterValueBase> T executeSingleSelectParameter(T parameterValue, Parameter parameter, String data, boolean isExecutedForCorrection, TempParameterValueDto tempParameterValueDto, User principalUserEntity) throws IOException {
    Map<String, String> parameterChoices = new HashMap<>();
    List<ChoiceParameterBase> parameters = JsonUtils.jsonToCollectionType(data, List.class, SingleSelectParameter.class);
    // set update id and choice selections
    setParameterSelectionState(parameterChoices, parameters);
    JsonNode jsonNode = JsonUtils.valueToNode(parameterChoices);
    State.ParameterExecution parameterExecutionState = parameterExecutionValidationService.isParameterExecutedPartially(parameter, data, isExecutedForCorrection, null, null) ? State.ParameterExecution.BEING_EXECUTED : State.ParameterExecution.EXECUTED;

    if (isExecutedForCorrection) {
      tempParameterValueDto.setChoices(jsonNode);
      tempParameterValueDto.setState(parameterExecutionState);
    }

    parameterValue.setChoices(jsonNode);
    parameterValue.setState(parameterExecutionState);
    parameterValue.setModifiedAt(DateTimeUtils.now());
    parameterValue.setModifiedBy(principalUserEntity);
    return parameterValue;
  }

  private <T extends ParameterValueBase> T executeShouldBeParameter(T parameterValue, Parameter parameter, String data, String reason,
                                                                    boolean isExecutedForCorrection, TempParameterValueDto tempParameterValueDto, User principalUserEntity) throws IOException, StreemException, ResourceNotFoundException {
    ShouldBeParameter shouldBeParameter = JsonUtils.readValue(data, ShouldBeParameter.class);
    String shouldBeParameterInput = shouldBeParameter.getInput();
    if (parameterValue.getState() == State.ParameterExecution.PENDING_FOR_APPROVAL) {
      ValidationUtils.invalidate(parameter.getId(), ErrorCode.SHOULD_BE_PARAMETER_CANNOT_BE_EXECUTED_AS_IT_IS_PENDING_FOR_APPROVAL);
    }
    if (parameterValue.getParameterValueApproval() != null && parameterValue.getParameterValueApproval().getState() == State.ParameterValue.APPROVED) {
      ValidationUtils.invalidate(parameter.getId(), ErrorCode.SHOULD_BE_PARAMETER_CANNOT_BE_EXECUTED_AS_IT_HAS_BEEN_ALREADY_APPROVED);
    }
    if (parameterValue.isHasVariations()) {
      Variation shouldBeVariation = variationRepository.findAllByParameterValueIdAndType(parameterValue.getId(), Action.Variation.SHOULD_BE).get(0);
      if (!Utility.isEmpty(shouldBeVariation)) {
        shouldBeParameter = JsonUtils.readValue((shouldBeVariation.getNewDetails().toString()), ShouldBeParameter.class);
        shouldBeParameter.setInput(shouldBeParameterInput);
      }
    }
    boolean isInvalidState = false;
    String offLimitsReason = "";
    // Empty input is allowed for should be parameter, we don't send it for approval
    if (!Utility.isEmpty(shouldBeParameter.getInput())) {
      if (!Utility.isNumeric(shouldBeParameter.getInput())) {
        ValidationUtils.invalidate(parameter.getId(), ErrorCode.SHOULD_BE_PARAMETER_VALUE_INVALID);
      }

      LeastCount leastCount = shouldBeParameter.getLeastCount();
      if (!Utility.isEmpty(leastCount)) {
        BigDecimal inputValue = new BigDecimal(shouldBeParameter.getInput());
        parameterExecutionValidationService.validateLeastCount(parameter.getId(), leastCount, inputValue, parameterValue.getJobId());
      }

      double input = Double.parseDouble(shouldBeParameter.getInput());
      Operator.Parameter operator = Operator.Parameter.valueOf(shouldBeParameter.getOperator());

      switch (operator) {
        case BETWEEN:
          double lowerValue = Double.parseDouble(shouldBeParameter.getLowerValue());
          double upperValue = Double.parseDouble(shouldBeParameter.getUpperValue());
          if (input < lowerValue || input > upperValue) {
            ValidationUtils.validateNotEmpty(reason, parameter.getId(), ErrorCode.PROVIDE_REASON_FOR_PARAMETER_PARAMETER_OFF_LIMITS);
            offLimitsReason = reason;
            isInvalidState = true;
          }
          break;
        case EQUAL_TO:
          double value = Double.parseDouble(shouldBeParameter.getValue());
          if (input != value) {
            ValidationUtils.validateNotEmpty(reason, parameter.getId(), ErrorCode.PROVIDE_REASON_FOR_PARAMETER_PARAMETER_OFF_LIMITS);
            offLimitsReason = reason;
            isInvalidState = true;
          }
          break;
        case LESS_THAN:
          value = Double.parseDouble(shouldBeParameter.getValue());
          if (input >= value) {
            ValidationUtils.validateNotEmpty(reason, parameter.getId(), ErrorCode.PROVIDE_REASON_FOR_PARAMETER_PARAMETER_OFF_LIMITS);
            offLimitsReason = reason;
            isInvalidState = true;
          }
          break;
        case LESS_THAN_EQUAL_TO:
          value = Double.parseDouble(shouldBeParameter.getValue());
          if (input > value) {
            ValidationUtils.validateNotEmpty(reason, parameter.getId(), ErrorCode.PROVIDE_REASON_FOR_PARAMETER_PARAMETER_OFF_LIMITS);
            offLimitsReason = reason;
            isInvalidState = true;
          }
          break;
        case MORE_THAN:
          value = Double.parseDouble(shouldBeParameter.getValue());
          if (input <= value) {
            ValidationUtils.validateNotEmpty(reason, parameter.getId(), ErrorCode.PROVIDE_REASON_FOR_PARAMETER_PARAMETER_OFF_LIMITS);
            offLimitsReason = reason;
            isInvalidState = true;
          }
          break;
        case MORE_THAN_EQUAL_TO:
          value = Double.parseDouble(shouldBeParameter.getValue());
          if (input < value) {
            ValidationUtils.validateNotEmpty(reason, parameter.getId(), ErrorCode.PROVIDE_REASON_FOR_PARAMETER_PARAMETER_OFF_LIMITS);
            offLimitsReason = reason;
            isInvalidState = true;
          }
          break;
      }
    }

    parameterValue.setReason(offLimitsReason);
    parameterValue.setValue(shouldBeParameter.getInput());
    State.ParameterExecution parameterExecutionState = parameterExecutionValidationService.isParameterExecutedPartially(parameter, data, isExecutedForCorrection, null, null) ? State.ParameterExecution.BEING_EXECUTED : State.ParameterExecution.EXECUTED;
    parameterValue.setState(parameterExecutionState);
    parameterValue.setModifiedBy(principalUserEntity);
    parameterValue.setModifiedAt(DateTimeUtils.now());

    if (!isExecutedForCorrection) {
      if (isInvalidState && !validatePrincipleUserRole(Misc.SHOULD_BE_PARAMETER_REVIEWER)) {
        // For deviated parameters send for approval if roles doesn't belong to the above set
        /*--Check for Invalid State--*/
        handleParameterApprovalRequest(parameterValue);
      } else {
        /*--removed approver if present--*/
        if (parameterValue.getParameterValueApproval() != null) {
          iParameterValueApprovalRepository.delete(parameterValue.getParameterValueApproval());
          parameterValue.setParameterValueApproval(null);
        }

      }
    }

    return parameterValue;
  }

  private <T extends ParameterValueBase> T executeCalculationParameter(T parameterValue, String data,
                                                                       boolean isExecutedForCorrection, TempParameterValueDto tempParameterValueDto, User principalUserEntity, Long referencedParameterId) throws IOException {
    JsonNode choices = parameterValue.getChoices();

    CalculationParameter calculationParameter = JsonUtils.readValue(data, CalculationParameter.class);

    List<CalculationParameterVariableChoices> variableChoicesList = new ArrayList<>();

    Parameter referencedParameter = parameterRepository.getReferenceById(referencedParameterId);

    ParameterValueBase referencedParameterValueBase = isExecutedForCorrection ? tempParameterValueRepository.getReferenceById(referencedParameterId) :
      parameterValueRepository.findLatestByJobIdAndParameterId(parameterValue.getJobId(), referencedParameterId);


    Map<String, String> parameterIdAndParameterValueIdMap = new HashMap<>();

    Map<String, Double> variableChoicesValueMap = new HashMap<>();

    if (!Utility.isEmpty(choices)) {
      variableChoicesList = JsonUtils.jsonToCollectionType(choices.toString(), List.class, CalculationParameterVariableChoices.class);
      // Creating a map of parameter id and parameter value id with recent referenced parameter value
      parameterIdAndParameterValueIdMap = variableChoicesList.stream().collect(Collectors.toMap(CalculationParameterVariableChoices::getParameterId, CalculationParameterVariableChoices::getParameterValueId));
      //Storing the variable choices value in a map against parameter id
      variableChoicesValueMap = variableChoicesList.stream().collect(Collectors.toMap(CalculationParameterVariableChoices::getParameterId, cpvc -> Double.valueOf(cpvc.getValue())));
      variableChoicesList = variableChoicesList.stream().filter(cpvc -> !cpvc.getParameterId().equals(referencedParameter.getIdAsString())).collect(Collectors.toList());
    }

    parameterIdAndParameterValueIdMap.put(referencedParameter.getIdAsString(), referencedParameterValueBase.getIdAsString());
    String referencedParameterValue = referencedParameterValueBase.getValue();
    variableChoicesValueMap.put(referencedParameter.getIdAsString(), Utility.isEmpty(referencedParameterValue) ? null : Double.valueOf(referencedParameterValue));

    String expression = calculationParameter.getExpression();
    var variablesMap = calculationParameter.getVariables();

    var variables = new HashSet<String>();
    Map<String, Double> variableValueMap = new HashMap<>();

    // This boolean is used to check if all variables are set or not if not we mark the parameter as partially executed
    boolean allVariablesSet = true;

    if (!Utility.isEmpty(calculationParameter.getVariables())) {
      for (Map.Entry<String, CalculationParameterVariable> entry : variablesMap.entrySet()) {
        String expressionVariable = entry.getKey();
        CalculationParameterVariable variable = entry.getValue();

        String parameterValueId = parameterIdAndParameterValueIdMap.get(variable.getParameterId());

        if (Utility.isEmpty(parameterValueId) || Utility.isEmpty(variableChoicesValueMap.get(variable.getParameterId()))) {
          // If any of the variable is not set then we mark the parameter as partially executed
          // and don't continue the execution for calculation we just reset the calculation parameter choices and value
          allVariablesSet = false;
        }
        variables.add(expressionVariable);
        variableValueMap.put(expressionVariable, variableChoicesValueMap.get(variable.getParameterId()));
      }
    }

    // Storing the last updated referenced parameter value in the calculation parameter choices
    if (!Utility.isEmpty(referencedParameterValueBase.getValue())) {
      // This check is done for CJF parameters since it doesn't have taskExecutionId
      String referencedParameterValueTaskExecutionId = Utility.isEmpty(referencedParameterValueBase.getTaskExecutionId()) ? null : referencedParameterValueBase.getTaskExecutionId().toString();
      CalculationParameterVariableChoices choice = new CalculationParameterVariableChoices();
      choice.setParameterId(referencedParameter.getId().toString());
      choice.setTaskExecutionId(referencedParameterValueTaskExecutionId);
      choice.setParameterValueId(parameterValue.getIdAsString());
      choice.setValue(referencedParameterValueBase.getValue());
      variableChoicesList.add(choice);
    }


    State.ParameterExecution parameterExecutionState = State.ParameterExecution.EXECUTED;

    String value = "";

    if (!allVariablesSet) {
      parameterExecutionState = State.ParameterExecution.BEING_EXECUTED;
    } else {
      Expression e = new ExpressionBuilder(expression)
        .variables(variables)
        .build()
        .setVariables(variableValueMap);

      value = Utility.roundUpDecimalPlaces(e.evaluate(), calculationParameter.getPrecision());
    }

    JsonNode jsonNode = JsonUtils.valueToNode(variableChoicesList);

    if (isExecutedForCorrection) {
      tempParameterValueDto.setValue(value);
      tempParameterValueDto.setChoices(jsonNode);
      tempParameterValueDto.setState(parameterExecutionState);
    }
    parameterValue.setModifiedBy(principalUserEntity);
    parameterValue.setModifiedAt(DateTimeUtils.now());
    parameterValue.setChoices(jsonNode);
    parameterValue.setState(parameterExecutionState);
    parameterValue.setValue(value);
    parameterValue.setState(parameterExecutionState);

    return parameterValue;
  }

  private <T extends ParameterValueBase> T executeNumberParameter(T parameterValue, String data, TempParameterValueDto tempParameterValueDto, boolean isExecutedForCorrection, Parameter parameter, Long jobId, User principalUserEntity) throws IOException, StreemException, ResourceNotFoundException, ParameterExecutionException {
    NumberParameter numberParameter = JsonUtils.readValue(data, NumberParameter.class);

    // We consider null values and numeric values as valid, if there are non numeric values then we raise an error
    if (!Utility.isEmpty(numberParameter.getInput()) && !Utility.isNumeric(numberParameter.getInput())) {
      ValidationUtils.invalidate(parameterValue.getId(), ErrorCode.NUMBER_PARAMETER_INVALID_VALUE);
    }

    LeastCount leastCount = numberParameter.getLeastCount();
    if (!Utility.isEmpty(leastCount) && (!Utility.isEmpty(numberParameter.getInput()))) {
      BigDecimal inputValue = new BigDecimal(numberParameter.getInput());
      parameterExecutionValidationService.validateLeastCount(parameter.getId(), leastCount, inputValue, jobId);
    }

    State.ParameterExecution parameterExecutionState = parameterExecutionValidationService.isParameterExecutedPartially(parameter, data, isExecutedForCorrection, null, null) ? State.ParameterExecution.BEING_EXECUTED : State.ParameterExecution.EXECUTED;

    List<Error> numberParameterValidations = new ArrayList<>();
    if (!Utility.isEmpty(numberParameter.getInput())) {
      try {
        parameterExecutionValidationService.validateNumberParameterValidations(jobId, parameterValue.getId(), parameter.getId(), parameter.getValidations(), numberParameter.getInput());
      } catch (ParameterExecutionException e) {
        log.info("Error while validating number parameter validations: {}", e.getErrorList());
        numberParameterValidations.addAll(e.getErrorList());
      }
    }

    if (isExecutedForCorrection) {
      tempParameterValueDto.setValue(numberParameter.getInput());
      tempParameterValueRepository.updateParameterValuesAndState(jobId, parameter.getId(), tempParameterValueDto.getValue(), parameterExecutionState.name(), principalUserEntity.getId(), DateTimeUtils.now());
    }
    parameterValue.setModifiedAt(DateTimeUtils.now());
    parameterValue.setModifiedBy(principalUserEntity);
    parameterValue.setValue(numberParameter.getInput());
    parameterValue.setState(parameterExecutionState);

    if (!Utility.isEmpty(numberParameterValidations)) {
      parameterValue.setState(State.ParameterExecution.BEING_EXECUTED);
      throw new ParameterExecutionException(numberParameterValidations);
    }

    return parameterValue;
  }


  private <T extends ParameterValueBase> T executeResourceParameter(T parameterValue, Parameter parameter, Long jobId, String data,
                                                                    boolean isExecutedForCorrection, TempParameterValueDto tempParameterValueDto, User principalUserEntity, boolean isScheduled) throws IOException, StreemException, ResourceNotFoundException, ParameterExecutionException {
    ResourceParameter resourceParameter = JsonUtils.readValue(data, ResourceParameter.class);

    if (parameter.getType() == Type.Parameter.MULTI_RESOURCE && resourceParameter.isAllSelected()) {
      //For All Selected This block fetches all objects and sets them
      List<ResourceParameterChoiceDto> resourceParameterChoiceDtos = null;
      if (parameter.getTargetEntityType().equals(Type.ParameterTargetEntityType.TASK)) {
        resourceParameterChoiceDtos = getAllResourceParameterChoiceDtos(parameterValue.getId(), null, false);
        if (Utility.isEmpty(resourceParameterChoiceDtos)) {
          ValidationUtils.invalidate(parameter.getId(), ErrorCode.RESOURCE_PARAMETER_SELECT_ALL_NO_OBJECT_FOUND);
        }
        resourceParameter.setChoices(resourceParameterChoiceDtos);
      } else {
        resourceParameterChoiceDtos = getAllResourceParameterChoiceDtos(parameterValue.getId(), null, true);
        if (Utility.isEmpty(resourceParameterChoiceDtos)) {
          ValidationUtils.invalidate(parameter.getId(), ErrorCode.RESOURCE_PARAMETER_SELECT_ALL_NO_OBJECT_FOUND);
        }
        resourceParameter.setChoices(new ArrayList<>(resourceParameterChoiceDtos));
      }

      if (!Utility.isEmpty(resourceParameter.getDeselectChoices())) {
        resourceParameter.getDeselectChoices().forEach(deselectChoice -> {
          resourceParameter.getChoices().removeIf(resourceParameterChoiceDto -> resourceParameterChoiceDto.getObjectId().equals(deselectChoice.getObjectId()));
        });
      }
    }

    if (parameterValue.isHasVariations()) {
      List<Variation> validationVariationList = variationRepository.findAllByParameterValueIdAndType(parameterValue.getId(), Action.Variation.VALIDATION);
      if (!Utility.isEmpty(validationVariationList)) {
        Map<String, ParameterRelationPropertyValidationDto> configIdAndPropertyValidationDtoMap
          = resourceParameter.getPropertyValidations().stream()
          .collect(Collectors.toMap(ParameterRelationPropertyValidationDto::getId, Function.identity()));


        for (Variation variation : validationVariationList) {
          ParameterRelationPropertyValidationDto propertyValidationDto = JsonUtils.readValue(variation.getNewDetails().toString(), ParameterRelationPropertyValidationDto.class);
          configIdAndPropertyValidationDtoMap.put(propertyValidationDto.getId(), propertyValidationDto);
        }

        resourceParameter.setPropertyValidations(new ArrayList<>(configIdAndPropertyValidationDtoMap.values()));
      }
    }
    List<Error> resourceValidationErrors = new ArrayList<>();

    JsonNode currentResourceParameterChoiceDto = parameterValue.getChoices();
    Map<String, Boolean> exceptionApprovedChoicesMap = new HashMap<>();

    // Populate a map with `objectId` and its `exceptionApproved` value from `currentResourceParameterChoiceDto`
    if (currentResourceParameterChoiceDto != null && currentResourceParameterChoiceDto.isArray()) {
      for (JsonNode choice : currentResourceParameterChoiceDto) {
        String objectId = choice.get("objectId").asText();
        boolean exceptionApproved = choice.get("exceptionApproved").asBoolean();
        exceptionApprovedChoicesMap.put(objectId, exceptionApproved);
      }
    }

    // Update `exceptionApproved` in `resourceParameter.getChoices()`
    for (ResourceParameterChoiceDto resourceParameterChoiceDto : resourceParameter.getChoices()) {
      String objectId = resourceParameterChoiceDto.getObjectId();
      if (exceptionApprovedChoicesMap.containsKey(objectId)) {
        resourceParameterChoiceDto.setExceptionApproved(exceptionApprovedChoicesMap.get(objectId));
      }
    }

    for (ResourceParameterChoiceDto resourceParameterChoiceDto : resourceParameter.getChoices()) {
      if (!resourceParameterChoiceDto.isExceptionApproved()) {
        try {
          parameterExecutionValidationService.validateParameterValueChoice(resourceParameterChoiceDto.getObjectId(), resourceParameter.getObjectTypeExternalId(), parameter.getValidations(), parameter.getId().toString(), jobId, isScheduled);
        } catch (ParameterExecutionException e) {
          resourceValidationErrors.addAll(e.getErrorList());
        }
      }
    }

    State.ParameterExecution parameterExecutionState = parameterExecutionValidationService.isParameterExecutedPartially(parameter, JsonUtils.writeValueAsString(resourceParameter), isExecutedForCorrection, null, null) ? State.ParameterExecution.BEING_EXECUTED : State.ParameterExecution.EXECUTED;
    JsonNode jsonNode = JsonUtils.valueToNode(resourceParameter.getChoices());

    if (!isExecutedForCorrection) {
      boolean isParameterValueOfMasterTask = parameterValueRepository.countParameterValueByParameterIdAndJobId(parameter.getId(), jobId) == 1;

      if (!isParameterValueOfMasterTask) {
        // This case will fail if the master task's resource parameter is optional and not executed
        TaskExecution masterTaskExecution = taskExecutionRepository.findByTaskIdAndJobIdAndType(parameter.getTask().getId(), jobId, Type.TaskExecutionType.MASTER);
        ParameterValue masterTaskParameterValue = parameterValueRepository.findByTaskExecutionIdAndParameterId(masterTaskExecution.getId(), parameter.getId());

        if (!Utility.isEmpty(masterTaskParameterValue) && !Utility.isEmpty(masterTaskParameterValue.getChoices())) {
          List<ResourceParameterChoiceDto> latestExecutedResourceParameterChoices = JsonUtils.jsonToCollectionType(JsonUtils.writeValueAsString(masterTaskParameterValue.getChoices()), List.class, ResourceParameterChoiceDto.class);

          parameterExecutionState = validateResourceParameterChoices(parameter.getType(), resourceParameter.getChoices(), latestExecutedResourceParameterChoices, parameterExecutionState, parameterValue.getId());

        }
      }
    } else {

      tempParameterValueDto.setChoices(jsonNode);
      tempParameterValueDto.setState(parameterExecutionState);
    }
    parameterValue.setChoices(jsonNode);
    parameterValue.setState(parameterExecutionState);
    parameterValue.setModifiedAt(DateTimeUtils.now());
    parameterValue.setModifiedBy(principalUserEntity);

    if (!Utility.isEmpty(resourceValidationErrors)) {
      parameterValue.setState(State.ParameterExecution.BEING_EXECUTED);
      System.out.println("Resource parameter validation errors: {}" + resourceValidationErrors);
      throw new ParameterExecutionException(resourceValidationErrors);
    }
    return parameterValue;
  }

  // Method for getting all resource parameters for select all.
  private List<ResourceParameterChoiceDto> getAllResourceParameterChoiceDtos(Long parameterExecutionId, String query, Boolean isCjf) throws IOException, ResourceNotFoundException {
    // Fetch all data in one go (unpaged)
    Page<PartialEntityObject> page = getAllFilteredEntityObjects(parameterExecutionId, query, null, Pageable.unpaged(), isCjf);
    return page.getContent().stream()
      .map(partialEntityObject -> new ResourceParameterChoiceDto(
        partialEntityObject.getId().toString(),
        partialEntityObject.getDisplayName(),
        partialEntityObject.getExternalId(),
        partialEntityObject.getCollection(),
        false // Explicitly set isExceptionApproved to false
      ))
      .toList();
  }


  @Override
  public Page<PartialEntityObject> getAllFilteredEntityObjects(Long parameterExecutionId, String query, String shortCode, Pageable pageable, Boolean isCjf) throws IOException, ResourceNotFoundException {
    ParameterValue parameterValue = parameterValueRepository.getReferenceById(parameterExecutionId);
    boolean isCorrectionEnableOnTask = false;
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    Facility facility = facilityRepository.getReferenceById(principalUser.getCurrentFacilityId());
    String facilityTimeZone = facility.getTimeZone();
    Parameter parameter = parameterValue.getParameter();
    ResourceParameter resourceParameter = JsonUtils.readValue(parameter.getData().toString(), ResourceParameter.class);

    if (!isCjf) {
      TaskExecution taskExecution = taskExecutionRepository.findById(parameterValue.getTaskExecutionId())
        .orElseThrow(() -> new ResourceNotFoundException(parameterValue.getTaskExecutionId(), ErrorCode.TASK_EXECUTION_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));
      isCorrectionEnableOnTask = taskExecution.isCorrectionEnabled();
    }
    if (isCorrectionEnableOnTask) {
      return entityObjectService.findPartialByUsageStatus(resourceParameter.getCollection(), 1, "", "", pageable, null, query);
    }

    // Create filters and check their validity
    Map<String, Object> filterResult = createResourcePropertyFilters(resourceParameter, parameterValue, facilityTimeZone, shortCode);

    boolean invalidParameterFilter = (boolean) filterResult.get("invalidParameterFilter");
    String filters = (String) filterResult.get("filters");

    // If any PARAMETER filter is invalid, return an empty result
    if (invalidParameterFilter) {
      return new PageImpl<>(Collections.emptyList(), pageable, 0);
    }

    // Apply filters if they exist
    if (!Utility.isEmpty(filters)) {
      SearchFilter searchFilter = JsonUtils.readValue(filters, SearchFilter.class);
      filters = JsonUtils.writeValueAsString(searchFilter);
    }

    return entityObjectService.findPartialByUsageStatus(resourceParameter.getCollection(), 1, "", "", pageable, filters, query);
  }

  private Map<String, Object> createResourcePropertyFilters(ResourceParameter resourceParameter, ParameterValue parameterValue, String facilityTimeZone, String shortCode) throws IOException {
    ResourceParameterFilter resourceParameterFilter = resourceParameter.getPropertyFilters();
    Set<State.ParameterExecution> validParameterExecutionStates = EnumSet.of(
      State.ParameterExecution.EXECUTED,
      State.ParameterExecution.BEING_EXECUTED,
      State.ParameterExecution.PENDING_FOR_APPROVAL,
      State.ParameterExecution.APPROVAL_PENDING,
      State.ParameterExecution.VERIFICATION_PENDING,
      State.ParameterExecution.BEING_EXECUTED_AFTER_REJECTED
    );

    boolean invalidParameterFilter = false;

    if (!Utility.isEmpty(resourceParameterFilter)) {
      for (ResourceParameterFilterField field : resourceParameterFilter.getFields()) {
        if (field.getSelector() == Selector.PARAMETER) {
          // If any PARAMETER filter is invalid, set invalidParameterFilter to true and return null
          boolean isValid = handleParameterSelector(field, parameterValue, validParameterExecutionStates, facilityTimeZone);
          if (!isValid) {
            invalidParameterFilter = true;
            break;  // Stop processing if any PARAMETER filter is invalid
          }
        } else {
          handleConstantSelector(field, facilityTimeZone);
        }
      }
    }

    if (!Utility.isEmpty(shortCode)) {
      ResourceParameterFilterField shortCodeFilter = new ResourceParameterFilterField();
      shortCodeFilter.setField("shortCode");
      shortCodeFilter.setOp(SearchOperator.EQ);
      shortCodeFilter.setValues(Collections.singletonList(shortCode));

      if (Utility.isEmpty(resourceParameterFilter)) {
        resourceParameterFilter = new ResourceParameterFilter();
        resourceParameterFilter.setOp(SearchOperator.AND);
      }
      if (Utility.isEmpty(resourceParameterFilter.getFields())) {
        resourceParameterFilter.setFields(new ArrayList<>());
      }

      resourceParameterFilter.getFields().add(shortCodeFilter);
    }

    String filters = Utility.isEmpty(resourceParameterFilter) ? null : JsonUtils.writeValueAsString(resourceParameterFilter);

    // Create and return a map containing the filters, and invalidParameterFilter
    Map<String, Object> result = new HashMap<>();
    result.put("filters", filters);
    result.put("invalidParameterFilter", invalidParameterFilter);

    return result;
  }

  private boolean handleParameterSelector(ResourceParameterFilterField field, ParameterValue parameterValue, Set<State.ParameterExecution> validStates, String facilityTimeZone) throws IOException {
    field.setValues(new ArrayList<>());
    ParameterValue referencedParameterValue = parameterValueRepository.findLatestByJobIdAndParameterId(parameterValue.getJobId(), Long.valueOf(field.getReferencedParameterId()));
    Parameter referencedParameter = parameterRepository.getReferenceById(Long.valueOf(field.getReferencedParameterId()));
    boolean isInvalidFilter = !validStates.contains(referencedParameterValue.getState())
      || referencedParameterValue.isHidden()
      || (Type.RESOURCE_PARAMETER_TYPES.contains(referencedParameter.getType())
      ? Utility.isEmpty(referencedParameterValue.getChoices())
      : (Type.SELECT_PARAMETER_TYPES.contains(referencedParameter.getType())
      ? areAllChoicesNotSelected(referencedParameterValue.getChoices())
      : Utility.isEmpty(referencedParameterValue.getValue())));

    if (isInvalidFilter) {
      return false;
    }

    switch (referencedParameter.getType()) {
      case SINGLE_SELECT, MULTISELECT -> {
        if (!Utility.isEmpty(referencedParameterValue.getChoices())) {
          Map<String, String> parameterChoices = JsonUtils.convertValue(referencedParameterValue.getChoices());
          List<Object> selectedChoices = parameterChoices.entrySet().stream()
            .filter(entry -> State.Selection.SELECTED.name().equals(entry.getValue()))
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
          field.setValues(selectedChoices);
        } else {
          field.setValues(List.of("null"));
        }
      }
      case RESOURCE, MULTI_RESOURCE -> {
        if (!Utility.isEmpty(referencedParameterValue.getChoices())) {
          List<ResourceParameterChoiceDto> choices = JsonUtils.jsonToCollectionType(referencedParameterValue.getChoices().toString(), List.class, ResourceParameterChoiceDto.class);
          if (!Utility.isEmpty(choices)) {
            List<Object> choiceIds = choices.stream()
              .map(choice -> choice.getObjectId().toString())
              .collect(Collectors.toList());
            field.setValues(choiceIds);
          }
        } else {
          field.setValues(List.of("null"));
        }
      }
      case MULTI_LINE, SINGLE_LINE -> {
        String value = referencedParameterValue.getValue();
        if (!Utility.isEmpty(value)) {
          field.setValues(List.of(value));
        } else {
          field.setValues(List.of("null"));
        }
      }
      case NUMBER, CALCULATION -> {
        if (!Utility.isEmpty(referencedParameterValue.getValue())) {
          if (Objects.equals(field.getPropertyType(), "DATE")) {
            Long calculatedDate = DateTimeUtils.adjustDateByDaysAtEndOfDay(DateTimeUtils.now(), referencedParameterValue.getValue(), facilityTimeZone);
            field.setValues(List.of(calculatedDate));
          } else if (Objects.equals(field.getPropertyType(), "DATE_TIME")) {
            Long calculatedDateTime = DateTimeUtils.addHoursOffsetToTime(DateTimeUtils.now(), referencedParameterValue.getValue());
            field.setValues(List.of(calculatedDateTime));
          } else {
            String value = referencedParameterValue.getValue();
            field.setValues(List.of(value.contains(".") ? Double.parseDouble(value) : Long.parseLong(value)));
          }
        } else {
          field.setValues(List.of("null"));
        }
      }
    }
    return true;  // Indicate that the filter is valid
  }

  private void handleConstantSelector(ResourceParameterFilterField field, String facilityTimeZone) {
    if (Objects.equals(field.getPropertyType(), "DATE")) {
      Long calculatedDate = DateTimeUtils.adjustDateByDaysAtEndOfDay(DateTimeUtils.now(), field.getValues().get(0).toString(), facilityTimeZone);
      field.setValues(List.of(calculatedDate));
    } else if (Objects.equals(field.getPropertyType(), "DATE_TIME")) {
      Long calculatedDateTime = DateTimeUtils.addHoursOffsetToTime(DateTimeUtils.now(), field.getValues().get(0).toString());
      field.setValues(List.of(calculatedDateTime));
    } else {
      field.setValues(field.getValues());
    }
  }

//  private boolean areAllChoicesNotSelected(Map<String, String> choices) {
//    for (Map.Entry<String, String> entry : choices.entrySet()) {
//      String state = entry.getValue();
//      if (State.Selection.SELECTED.equals(State.Selection.valueOf(state))) {
//        return false;
//      }
//    }
//    return true;
//  }

  private boolean areAllChoicesNotSelected(JsonNode choices) {
    if (choices == null || !choices.isObject()) {
      throw new IllegalArgumentException("Invalid choices object. Must be a non-null JSON object.");
    }
    Iterator<Map.Entry<String, JsonNode>> fields = choices.fields();

    while (fields.hasNext()) {
      Map.Entry<String, JsonNode> field = fields.next();
      String value = field.getValue().asText();

      // If any value is "SELECTED", return false
      if ("SELECTED".equals(value)) {
        return false;
      }
    }
    return true;
  }


  public List<ParameterPartialDto> getParameterPartialData(Long jobId, ParameterPartialRequest parameterPartialRequest) throws JsonProcessingException {
    log.info("[getParameterPartialData] requested to get ParameterPartialData: {}", parameterPartialRequest);

    Set<Long> parameterIdsSet = parameterPartialRequest.getParameterIds().stream()
      .map(Long::valueOf)
      .collect(Collectors.toSet());
    List<ParameterPartialDto> parameterPartialDtoList = new ArrayList<>();
    ObjectMapper objectMapper = new ObjectMapper();

    if (!Utility.isEmpty(parameterIdsSet)) {
      List<ParameterValueView> parameterValueViewList = parameterValueRepository.getParameterPartialDataByIds(parameterIdsSet, jobId);
      for (ParameterValueView parameterValueView : parameterValueViewList) {
        ParameterPartialDto parameterPartialDto = new ParameterPartialDto();
        parameterPartialDto.setId(String.valueOf(parameterValueView.getId()));
        parameterPartialDto.setLabel(parameterValueView.getLabel());
        parameterPartialDto.setType(parameterValueView.getType());
        parameterPartialDto.setValue(parameterValueView.getValue());
        parameterPartialDto.setTaskId(String.valueOf(parameterValueView.getTaskId()));
        parameterPartialDto.setParameterValueId(String.valueOf(parameterValueView.getParameterValueId()));
        parameterPartialDto.setTaskExecutionId(String.valueOf(parameterValueView.getTaskExecutionId()));
        parameterPartialDto.setHidden(parameterValueView.getHidden());

        if (!Utility.isEmpty(parameterValueView.getChoices())) {
          parameterPartialDto.setChoices(JsonUtils.readValue(parameterValueView.getChoices(), JsonNode.class));
        }
        if (!Utility.isEmpty(parameterValueView.getData())) {
          JsonNode jsonNodeData = objectMapper.readTree(parameterValueView.getData());
          parameterPartialDto.setData(jsonNodeData);
        }

        parameterPartialDtoList.add(parameterPartialDto);
      }
    }
    return parameterPartialDtoList;
  }

  @Override
  public List<ParameterPartialDto> getParameterPartialDataForMaster(Long jobId, ParameterPartialRequest parameterPartialRequest) throws JsonProcessingException {
    //Todo: remove object mapper here
    log.info("[getParameterPartialDataForMaster] requested to get ParameterPartialData For Master: {}", parameterPartialRequest);
    Set<Long> parameterIdsSet = parameterPartialRequest.getParameterIds().stream()
      .map(Long::valueOf)
      .collect(Collectors.toSet());
    List<ParameterPartialDto> parameterPartialDtoList = new ArrayList<>();
    ObjectMapper objectMapper = new ObjectMapper();

    if (!Utility.isEmpty(parameterIdsSet)) {
      List<ParameterValueView> parameterValueViewList = parameterValueRepository.getParameterPartialDataFOrMasterByIds(parameterIdsSet, jobId);
      for (ParameterValueView parameterValueView : parameterValueViewList) {
        ParameterPartialDto parameterPartialDto = new ParameterPartialDto();
        parameterPartialDto.setId(String.valueOf(parameterValueView.getId()));
        parameterPartialDto.setLabel(parameterValueView.getLabel());
        parameterPartialDto.setType(parameterValueView.getType());
        parameterPartialDto.setValue(parameterValueView.getValue());
        parameterPartialDto.setTaskId(String.valueOf(parameterValueView.getTaskId()));
        parameterPartialDto.setParameterValueId(String.valueOf(parameterValueView.getParameterValueId()));
        parameterPartialDto.setTaskExecutionId(String.valueOf(parameterValueView.getTaskExecutionId()));
        parameterPartialDto.setHidden(parameterValueView.getHidden());

        if (!Utility.isEmpty(parameterValueView.getChoices())) {
          parameterPartialDto.setChoices(JsonUtils.readValue(parameterValueView.getChoices(), JsonNode.class));
        }

        if (!Utility.isEmpty(parameterValueView.getData())) {
          JsonNode jsonNodeData = objectMapper.readTree(parameterValueView.getData());
          parameterPartialDto.setData(jsonNodeData);
        }

        parameterPartialDtoList.add(parameterPartialDto);
      }
    }
    return parameterPartialDtoList;
  }

  private State.ParameterExecution validateResourceParameterChoices(Type.Parameter type, List<ResourceParameterChoiceDto> choices,
                                                                    List<ResourceParameterChoiceDto> latestExecutedResourceParameterChoices,
                                                                    State.ParameterExecution parameterExecutionState, Long currentParameterValueId) throws StreemException {
    // If it's empty, then any resource can be selected
    // (keeping in mind if resource parameter is optional it's first execution can occur in any repeated task)
    if (!Utility.isEmpty(latestExecutedResourceParameterChoices)) {
      Set<ResourceParameterChoiceDto> choicesSet = new HashSet<>(choices);
      switch (type) {
        case RESOURCE -> {
          // If it's not empty, then the latest-executed resource parameter choices should be equal of the current resource parameter choices
          if (!choicesSet.containsAll(latestExecutedResourceParameterChoices)) {
            ValidationUtils.invalidate(currentParameterValueId, ErrorCode.RESOURCE_PARAMETER_INVALID_SELECTION);
          }
          parameterExecutionState = State.ParameterExecution.EXECUTED;

        }
        case MULTI_RESOURCE -> {
          // If it's not empty,
          // then the latest-executed resource parameter choices should be a subset
          // or equal of the current resource parameter choices

          //Checking if the current executed resource parameter choices have number of selections as the latest executed resource parameter
          boolean isPotentiallyExecuted = choices.size() == latestExecutedResourceParameterChoices.size();
          for (ResourceParameterChoiceDto resourceParameterChoiceDto : choices) {
            if (!latestExecutedResourceParameterChoices.contains(resourceParameterChoiceDto)) {
              ValidationUtils.invalidate(currentParameterValueId, ErrorCode.MULTI_RESOURCE_PARAMETER_INVALID_SELECTION);
            }
          }
          parameterExecutionState = isPotentiallyExecuted ? State.ParameterExecution.EXECUTED : State.ParameterExecution.BEING_EXECUTED;
        }

      }
    }
    return parameterExecutionState;
  }

  private <T extends ParameterValueBase> T executeDateParameter(T parameterValue, String data, TempParameterValueDto tempParameterValueDto, boolean isExecutedForCorrection, Parameter parameter, User principalUserEntity) throws IOException, StreemException, ResourceNotFoundException, ParameterExecutionException {
    DateParameter dateParameter = JsonUtils.readValue(data, DateParameter.class);

    JsonNode validations = parameter.getValidations();
    List<Error> dateParameterValidationsErrors = new ArrayList<>();
    if (!validations.isEmpty() && validations.isArray() && !Utility.isEmpty(dateParameter.getInput())) {
      PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
      Facility facility = facilityRepository.getReferenceById(principalUser.getCurrentFacilityId());
      String facilityTimeZone = facility.getTimeZone();
      boolean isDateTimeParameter = parameter.getType() == Type.Parameter.DATE_TIME;

      try {
        parameterExecutionValidationService.validateDateAndDateTimeParameterValidations(parameterValue.getJob().getId(), validations, dateParameter.getInput(), isDateTimeParameter, facilityTimeZone, parameter.getId());
      } catch (ParameterExecutionException e) {
        log.info("Error while validating date parameter validations: {}", e.getErrorList());
        dateParameterValidationsErrors.addAll(e.getErrorList());
      }
    }

    State.ParameterExecution parameterExecutionState = parameterExecutionValidationService.isParameterExecutedPartially(parameter, data, isExecutedForCorrection, null, null) ? State.ParameterExecution.BEING_EXECUTED : State.ParameterExecution.EXECUTED;

    if (isExecutedForCorrection) {
      tempParameterValueDto.setValue(dateParameter.getInput());
    }
    parameterValue.setValue(dateParameter.getInput());
    parameterValue.setState(parameterExecutionState);
    parameterValue.setModifiedBy(principalUserEntity);
    parameterValue.setModifiedAt(DateTimeUtils.now());

    if (!Utility.isEmpty(dateParameterValidationsErrors)) {
      parameterValue.setState(State.ParameterExecution.BEING_EXECUTED);
      throw new ParameterExecutionException(dateParameterValidationsErrors);
    }

    return parameterValue;
  }

  private boolean hasRole(List<RoleDto> roles, Set<String> role) {
    for (RoleDto roleData : roles) {
      if (role.contains(roleData.getName())) {
        return true;
      }
    }
    return false;
  }

  private boolean validatePrincipleUserRole(Set<String> roles) {
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    return hasRole(principalUser.getRoles(), roles);
  }

  private void validateReviewerRoleForParameterApproval(Long parameterId) throws StreemException {
    if (!validatePrincipleUserRole(Misc.SHOULD_BE_PARAMETER_REVIEWER)) {
      ValidationUtils.invalidate(parameterId, ErrorCode.ONLY_SUPERVISOR_CAN_APPROVE_OR_REJECT_PARAMETER);
    }
  }

  private <T extends ParameterValueBase> void handleParameterApprovalRequest(T parameterValue) throws ResourceNotFoundException, StreemException, IOException {
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    User principalUserEntity = userRepository.getOne(principalUser.getId());

    Job job = parameterValue.getJob();

    Parameter parameter = parameterValue.getParameter();
    TaskExecution taskExecution = parameterValue.getTaskExecution();
    taskExecutionAssigneeRepository.findByTaskExecutionAndUser(taskExecution.getId(), principalUserEntity.getId())
      .orElseThrow(() -> new ResourceNotFoundException(taskExecution.getTaskId(), ErrorCode.USER_NOT_ASSIGNED_TO_EXECUTE_TASK, ExceptionType.ENTITY_NOT_FOUND));

    validateJobState(parameterValue.getJob().getId(), Action.Parameter.PENDING_FOR_APPROVAL, parameterValue.getJob().getState());
    validateTaskState(taskExecution.getState(), Action.Parameter.PENDING_FOR_APPROVAL, parameter.getId());

    /*Notify the Approvers*/
    notificationService.notifyAllShouldBeParameterReviewersForApproval(parameterValue.getJob().getId(), job.getChecklist().getId(), taskExecution.getId(), principalUser.getOrganisationId());

    parameterValue.setState(State.ParameterExecution.PENDING_FOR_APPROVAL);
  }

  private ParameterDto updateParameterState(ParameterValue parameterValue, State.ParameterExecution state, User approver, State.ParameterValue parameterApprovalState) {
    ParameterDto parameterDto = parameterMapper.toDto(parameterValue.getParameter());
    parameterValue.setState(state);
    if (parameterValue.getParameterValueApproval() == null) {
      parameterValue.setParameterValueApproval(new ParameterValueApproval());
    }
    parameterValue.getParameterValueApproval().setUser(approver);
    parameterValue.getParameterValueApproval().setCreatedAt(DateTimeUtils.now());
    parameterValue.getParameterValueApproval().setState(parameterApprovalState);
    List<ParameterValueDto> responses = new ArrayList<>();
    responses.add(parameterValueMapper.toDto(parameterValueRepository.save(parameterValue)));
    parameterDto.setResponse(responses);
    parameterDto.setResponse(responses);
    return parameterDto;
  }

  /**
   * @param parameterExecuteRequestMap List of all CJF parameters whose rules are to be evaluated
   * @param checklistId
   * @return
   * @throws IOException
   */
  @Override
  public RuleHideShowDto tempExecuteRules(Map<Long, ParameterExecuteRequest> parameterExecuteRequestMap, Long checklistId) throws IOException {
    return rulesExecutionService.tempExecuteRules(parameterExecuteRequestMap, checklistId);
  }

  @Override
  public RuleHideShowDto updateRules(Long jobId, Parameter parameter, ParameterValue parameterValue) throws IOException {
    return rulesExecutionService.updateRules(jobId, parameter, parameterValue);
  }

  @Override
  @Transactional
  public BasicDto createVariations(CreateVariationRequest createVariationRequest) throws StreemException, JsonProcessingException {
    log.info("[createVariations] requested to create variation: {}", createVariationRequest);
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    User principalUserEntity = userRepository.getReferenceById(principalUser.getId());
    List<Variation> variations = new ArrayList<>();

    ParameterValue parameterValue = parameterValueRepository.getMasterTaskParameterValue(createVariationRequest.getParameterId(), createVariationRequest.getJobId());
    Long taskId = parameterValue.getTaskExecution().getTaskId();

    validateIfUserIsAssignedToExecuteParameter(parameterValue.getTaskExecutionId(), principalUser.getId());
    boolean existsByVariationNameOrVariationNumber = variationRepository.existsAllByVariationNumberOrNameForJob(createVariationRequest.getVariationNumber(), createVariationRequest.getName(), parameterValue.getJobId(), taskId);

    if (existsByVariationNameOrVariationNumber) {
      ValidationUtils.invalidate(parameterValue.getId(), ErrorCode.VARIATION_NAME_OR_NUMBER_ALREADY_EXISTS);
    }

    Parameter parameter = parameterRepository.getReferenceById(parameterValue.getParameterId());
    JsonNode data = switch (createVariationRequest.getType()) {
      case FILTER -> parameter.getData().get("propertyFilters").get("fields");
      case VALIDATION -> {
        if (parameter.getType() == Type.Parameter.NUMBER) {
          yield parameter.getValidations().get("resourceParameterValidations");
        } else {
          yield parameter.getData().get("propertyValidations");
        }
      }
      case SHOULD_BE -> JsonUtils.createArrayNode().add(parameter.getData());
    };

    extractAllVariations(createVariationRequest, data, parameter.getType(), parameterValue, variations, principalUserEntity);

    if (Utility.isEmpty(variations)) {
      ValidationUtils.invalidate(parameterValue.getId(), ErrorCode.NO_VARIATION_CONFIGURED);
    }
    List<String> configIds = variations.stream().map(Variation::getConfigId).toList();
    boolean isExistByConfigIdForParameterValueId = variationRepository.existsByConfigIdsForParameterValueId(configIds, parameterValue.getId());

    if (isExistByConfigIdForParameterValueId) {
      ValidationUtils.invalidate(parameterValue.getId(), ErrorCode.VARIATION_ALREADY_EXISTS);
    }

    variations = variationRepository.saveAll(variations);
    parameterValue.setHasVariations(true);
    parameterValueRepository.save(parameterValue);


    VariationMediaRequest mediaRequest = createVariationRequest.getMedia();


    if (mediaRequest != null && !Utility.isEmpty(mediaRequest.getMediaId())) {
      Media media = mediaRepository.getReferenceById(Long.valueOf(mediaRequest.getMediaId()));
      if (Utility.trimAndCheckIfEmpty(mediaRequest.getName())) {
        ValidationUtils.invalidate(parameter.getId(), ErrorCode.PARAMETER_EXECUTION_MEDIA_NAME_CANNOT_BE_EMPTY);
      }
      media.setName(mediaRequest.getName());
      media.setDescription(mediaRequest.getDescription());

      mediaRepository.save(media);


      variations.forEach(variation -> {
        variation.addMedia(media, principalUserEntity);
        variation.setModifiedAt(DateTimeUtils.now());
        variation.setModifiedBy(principalUserEntity);
      });
      variationRepository.saveAll(variations);
    }
    jobAuditService.createVariation(createVariationRequest, principalUser);

    return new BasicDto("success", null, null);
  }

  @Override
  public List<VariationDto> getAllVariationsOfParameterExecution(Long parameterExecutionId) {
    log.info("[getAllVariationsOfParameterExecution] requested to get all variations of parameter with parameterValueId: {}", parameterExecutionId);
    ParameterValue parameterValue = parameterValueRepository.getReferenceById(parameterExecutionId);
    List<VariationView> variationsOfParameterValueId = variationRepository.findVariationsByParameterValueId(parameterValue.getId());
    Set<Long> variationIds = variationsOfParameterValueId.stream()
      .map(VariationView::getId)
      .map(Long::parseLong).
      collect(Collectors.toSet());

    List<VariationMediaMapping> variationMediaMappings = variationMediaMappingRepository.findAllByVariationIdIn(variationIds);
    Map<Long, List<VariationMediaMapping>> variationIdMediaMap = variationMediaMappings.stream()
      .collect(Collectors.groupingBy(variationMediaMapping -> variationMediaMapping.getVariation().getId()));
    return variationMapper.toDtoList(variationsOfParameterValueId, variationIdMediaMap);
  }

  @Override
  @Transactional
  public BasicDto deleteVariation(DeleteVariationRequest deleteVariationRequest) throws StreemException {
    log.info("[deleteVariation] requested to delete variation: {}", deleteVariationRequest);
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    Variation variation = variationRepository.getReferenceById(deleteVariationRequest.getVariationId());
    ParameterValue parameterValue = variation.getParameterValue();

    validateIfUserIsAssignedToExecuteParameter(parameterValue.getTaskExecutionId(), principalUser.getId());

    if (parameterValue.getTaskExecution().getState() != State.TaskExecution.NOT_STARTED) {
      ValidationUtils.invalidate(deleteVariationRequest.getParameterId(), ErrorCode.CANNOT_DELETE_VARIATION_ON_ONGOING_TASK);
    }

    jobAuditService.deleteVariation(deleteVariationRequest, principalUser);

    variationRepository.deleteByVariationId(deleteVariationRequest.getVariationId());
    variationRepository.reconfigureVariationsOfParameterValue(parameterValue.getId());

    return new BasicDto("success", null, null);
  }

  @Override
  public Page<VariationDto> getAllVariationsOfJob(Long jobId, String parameterName, Pageable pageable) {
    log.info("[getAllVariationsOfJob] requested to get all variations of job with jobId: {}, parameterName: {}, pageable: {}", jobId, parameterName, pageable);
    Page<VariationView> variationViewPage = variationRepository.findAllByJobIdAndParameterName(jobId, parameterName, pageable);
    Set<Long> variationIds = variationViewPage.getContent().stream()
      .map(VariationView::getId)
      .map(Long::parseLong).
      collect(Collectors.toSet());

    List<VariationMediaMapping> variationMediaMappings = variationMediaMappingRepository.findAllByVariationIdIn(variationIds);
    Map<Long, List<VariationMediaMapping>> variationIdMediaMap = variationMediaMappings.stream()
      .collect(Collectors.groupingBy(variationMediaMapping -> variationMediaMapping.getVariation().getId()));

    List<VariationDto> variationDtos = variationMapper.toDtoList(variationViewPage.getContent(), variationIdMediaMap);
    return new PageImpl<>(variationDtos, pageable, variationViewPage.getTotalElements());

  }

  @Override
  public Page<ParameterDto> getAllAllowedParametersForVariations(Long jobId, String filters, String parameterName, Pageable pageable) {
    log.info("[getAllAllowedParametersForVariations] requested to get allowed parameters for variation with jobId: {}, filters: {}, parameterName: {}, pageable: {}", jobId, filters, parameterName, pageable);
    Page<ParameterValue> parameterValues = parameterValueRepository.getAllParametersAvailableForVariations(jobId, parameterName, pageable);
    Set<Parameter> parameters = parameterValues.getContent().stream().map(ParameterValue::getParameter).collect(Collectors.toCollection(LinkedHashSet::new));

    Map<Long, List<ParameterValue>> parameterIdParameterValueMap = new HashMap<>();

    for (ParameterValue av : parameterValues) {
      var parameter = av.getParameter();
      parameterIdParameterValueMap.computeIfAbsent(parameter.getId(), k -> new ArrayList<>());
      parameterIdParameterValueMap.get(parameter.getId()).add(av);
    }

    return new PageImpl<>(parameterMapper.toDto(parameters, parameterIdParameterValueMap, new HashMap<>(), null, new HashMap<>(), new HashMap<>(), new HashMap<>()), pageable, parameterValues.getTotalElements());
  }

  @Override
  public List<ParameterDto> getParameterExecutionByParameterIdAndJobId(Long jobId, String filters) throws ResourceNotFoundException {
    log.info("[getParameterResponseById] Request to get parameter with JobId: {}, ", jobId);
    Specification<Parameter> specification = ParameterSpecificationBuilder.createSpecification(filters, null);
    List<Parameter> parameters = parameterRepository.findAll(specification);
    Map<Long, List<ParameterValue>> parameterIdParameterValueMap = new HashMap<>();

    for (Parameter parameter : parameters) {
      ParameterValue parameterValue = parameterValueRepository.findLatestByJobIdAndParameterId(jobId, parameter.getId());
      parameterIdParameterValueMap.put(parameter.getId(), List.of(parameterValue));
    }

    return parameterMapper.toDto(new HashSet<>(parameters), parameterIdParameterValueMap, new HashMap<>(), null, new HashMap<>(), new HashMap<>(), new HashMap<>());
  }


  public void updateUserAction(List<TaskExecutionUserMapping> taskExecutionUserMappingList) {
    log.info("[updateUserAction] Request to update user action for taskExecutionUserMappingList: {}", taskExecutionUserMappingList);
    taskExecutionAssigneeRepository.updateUserAction(taskExecutionUserMappingList.stream()
      .map(BaseEntity::getId)
      .collect(Collectors.toSet()));

  }

  private void setParameterSelectionState(Map<String, String> parameterChoices, List<ChoiceParameterBase> parameters) {
    for (ChoiceParameterBase choiceParameter : parameters) {
      String id = choiceParameter.getId();
      String state = choiceParameter.getState();
      if (null != state) {
        if (State.Selection.SELECTED.equals(State.Selection.valueOf(state))) {
          parameterChoices.put(id, State.Selection.SELECTED.name());
        } else {
          parameterChoices.put(id, State.Selection.NOT_SELECTED.name());
        }
      }
    }
  }

  private void extractAllVariations(CreateVariationRequest createVariationRequest, JsonNode data, Type.Parameter parameterType, ParameterValue parameterValue, List<Variation> variations, User principalUserEntity) throws StreemException, JsonProcessingException {
    switch (createVariationRequest.getType()) {
      case FILTER -> {
        if (parameterType == Type.Parameter.RESOURCE) {
          extractUpdatedConfigIdsAndCreateVariations(createVariationRequest, data, createVariationRequest.getDetails().get("fields"), parameterValue, variations, principalUserEntity);
        } else {
          ValidationUtils.invalidate(parameterValue.getId(), ErrorCode.INVALID_FILTER_CONFIGURATIONS);
        }
      }
      case SHOULD_BE -> {
        if (parameterType == Type.Parameter.SHOULD_BE) {
          validateShouldBeParameter(createVariationRequest.getDetails(), parameterValue.getParameter()
          );
          extractUpdatedConfigIdsAndCreateVariations(createVariationRequest, data, JsonUtils.createArrayNode().add(createVariationRequest.getDetails()), parameterValue, variations, principalUserEntity);
        } else {
          ValidationUtils.invalidate(parameterValue.getId(), ErrorCode.INVALID_SHOULD_BE_CONFIGURATIONS);
        }
      }
      case VALIDATION -> {
        if (parameterType == Type.Parameter.RESOURCE) {
          extractUpdatedConfigIdsAndCreateVariations(createVariationRequest, data, createVariationRequest.getDetails(), parameterValue, variations, principalUserEntity);
        } else if (parameterType == Type.Parameter.NUMBER) {
          extractUpdatedConfigIdsAndCreateVariations(createVariationRequest, data, createVariationRequest.getDetails(), parameterValue, variations, principalUserEntity);
        }
      }
    }
  }

  private void validateShouldBeParameter(JsonNode requestData, Parameter parameter) throws JsonProcessingException, StreemException {
    ShouldBeParameter shouldBeParameter = JsonUtils.readValue(requestData.toString(), ShouldBeParameter.class);
    ShouldBeParameter existingShouldBeParameter = JsonUtils.readValue(parameter.getData().toString(), ShouldBeParameter.class);
    Operator.Parameter operator = Operator.Parameter.valueOf(shouldBeParameter.getOperator());
    String newUom = shouldBeParameter.getUom();
    String oldUom = existingShouldBeParameter.getUom();

    if (operator == Operator.Parameter.BETWEEN) {
      double lowerValue = Double.parseDouble(shouldBeParameter.getLowerValue());
      double upperValue = Double.parseDouble(shouldBeParameter.getUpperValue());

      if (lowerValue > upperValue) {
        ValidationUtils.invalidate(ErrorCode.SHOULD_BE_PARAMETER_LOWER_VALUE_CANNOT_BE_GREATER_THAN_UPPER_VALUE.getDescription(), ErrorCode.SHOULD_BE_PARAMETER_LOWER_VALUE_CANNOT_BE_GREATER_THAN_UPPER_VALUE);
      }
    }

    if (!newUom.equals(oldUom) && newUom.equalsIgnoreCase(oldUom)) {
      ValidationUtils.invalidate(parameter.getId(), ErrorCode.SHOULD_BE_PARAMETER_UOM_CASE_INSENSITIVE);
    }
  }

  private void extractUpdatedConfigIdsAndCreateVariations(CreateVariationRequest createVariationRequest, JsonNode data, JsonNode variationDetails, ParameterValue parameterValue, List<Variation> variations, User principalUserEntity) {
    Job job = parameterValue.getJob();

    Map<String, JsonNode> configIdAndVariationMap = new HashMap<>();

    for (JsonNode variationNode : variationDetails) {
      String configId = variationNode.get("id").asText();
      configIdAndVariationMap.put(configId, variationNode);
    }

    for (JsonNode dataNode : data) {
      String configId = dataNode.get("id").asText();

      if (!Objects.equals(configIdAndVariationMap.get(configId), dataNode)) {
        Variation variation = new Variation();
        variation.setParameterValue(parameterValue);
        variation.setNewDetails(configIdAndVariationMap.get(configId));
        variation.setType(createVariationRequest.getType());
        variation.setJob(job);
        variation.setOldDetails(dataNode);
        variation.setName(createVariationRequest.getName());
        variation.setDescription(createVariationRequest.getDescription());
        variation.setVariationNumber(createVariationRequest.getVariationNumber());
        variation.setConfigId(configId);
        variation.setCreatedAt(DateTimeUtils.now());
        variation.setCreatedBy(principalUserEntity);
        variation.setModifiedAt(DateTimeUtils.now());
        variation.setModifiedBy(principalUserEntity);

        variations.add(variation);
      }
    }
  }

  public void attachParameterVerifications(Parameter parameter, Long jobId, ParameterValue parameterValue, ParameterValueDto parameterValueDto) {
    List<ParameterVerification> parameterVerifications = new ArrayList<>();
    if (!parameter.getVerificationType().equals(Type.VerificationType.NONE)) {
      ParameterVerification parameterVerificationSelf = parameterVerificationRepository.findByJobIdAndParameterIdAndVerificationType(jobId, parameter.getId(), String.valueOf(Type.VerificationType.SELF));
      ParameterVerification parameterVerificationPeer = parameterVerificationRepository.findByJobIdAndParameterIdAndVerificationType(jobId, parameter.getId(), String.valueOf(Type.VerificationType.PEER));
      if (!Utility.isEmpty(parameterVerificationSelf)) {
        parameterVerifications.add(parameterVerificationSelf);
      }
      if (!Utility.isEmpty(parameterVerificationPeer)) {
        parameterVerifications.add(parameterVerificationPeer);
      }
    }
    if (!Utility.isEmpty(parameterVerifications)) {
      List<ParameterVerificationDto> parameterVerificationDtos = parameterVerificationMapper.toDto(parameterVerifications);
      for (ParameterVerificationDto parameterVerificationDto : parameterVerificationDtos) {
        parameterVerificationDto.setEvaluationState(parameterValue.getState());
      }
      parameterValueDto.setParameterVerifications(parameterVerificationDtos);
    }
  }

  public void attachTempParameterVerifications(Parameter parameter, Long jobId, TempParameterValue tempParameterValue, TempParameterValueDto tempParameterValueDto) {
    List<TempParameterVerification> parameterVerifications = new ArrayList<>();
    if (!parameter.getVerificationType().equals(Type.VerificationType.NONE)) {
      TempParameterVerification parameterVerificationSelf = tempParameterVerificationRepository.findByJobIdAndParameterIdAndVerificationType(jobId, parameter.getId(), String.valueOf(Type.VerificationType.SELF));
      TempParameterVerification parameterVerificationPeer = tempParameterVerificationRepository.findByJobIdAndParameterIdAndVerificationType(jobId, parameter.getId(), String.valueOf(Type.VerificationType.PEER));
      if (!Utility.isEmpty(parameterVerificationSelf)) {
        parameterVerifications.add(parameterVerificationSelf);
      }
      if (!Utility.isEmpty(parameterVerificationPeer)) {
        parameterVerifications.add(parameterVerificationPeer);
      }
    }
    if (!Utility.isEmpty(parameterVerifications)) {
      List<ParameterVerificationDto> parameterVerificationDtos = tempParameterVerificationMapper.toDto(parameterVerifications);
      for (ParameterVerificationDto parameterVerificationDto : parameterVerificationDtos) {
        parameterVerificationDto.setEvaluationState(tempParameterValue.getState());
      }
      tempParameterValueDto.setParameterVerifications(parameterVerificationDtos);
    }
  }

  private void validateIfCurrentExecutorIsAllowedToInteractTask(Long taskId, Long jobId, Long currentUserId) throws StreemException {
    log.info("[validateIfCurrentExecutorIsAllowedToInteractTask] Request to validate if executor is assigned, tas: {}", taskId);
    List<TaskExecutorLockErrorView> invalidReferencedTaskExecutions = taskExecutorLockRepository.findInvalidReferencedTaskExecutions(taskId, jobId, currentUserId);
    if (!Utility.isEmpty(invalidReferencedTaskExecutions)) {
      Map<String, Object> taskExecutorLockError = new HashMap<>();
      Map<String, Map<String, List<TaskExecutorLockErrorView>>> taskExecutorLockErrorFormat = invalidReferencedTaskExecutions.stream()
        .collect(Collectors.groupingBy(TaskExecutorLockErrorView::getLockType,
          Collectors.groupingBy(telv -> telv.getIsValidTaskState() ? "validTaskState" : "invalidTaskState")));
      taskExecutorLockError.put("TASK_INITIATION_BLOCKED_DUE_TO_TASK_EXECUTOR_LOCK", taskExecutorLockErrorFormat);
      ValidationUtils.invalidate(taskId.toString(), ErrorCode.TASK_INITIATION_BLOCKED_DUE_TO_TASK_EXECUTOR_LOCK, ErrorMessage.TASK_INITIATION_BLOCKED_DUE_TO_TASK_EXECUTOR_LOCK, taskExecutorLockError);
    }
  }
}
