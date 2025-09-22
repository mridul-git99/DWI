package com.leucine.streem.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.leucine.streem.constant.*;
import com.leucine.streem.constant.Action;
import com.leucine.streem.dto.*;
import com.leucine.streem.dto.mapper.ITaskExecutionMapper;
import com.leucine.streem.dto.mapper.ITaskMapper;
import com.leucine.streem.dto.mapper.IUserMapper;
import com.leucine.streem.dto.projection.*;
import com.leucine.streem.dto.request.*;
import com.leucine.streem.dto.response.Error;
import com.leucine.streem.dto.response.ErrorCode;
import com.leucine.streem.exception.ExceptionType;
import com.leucine.streem.exception.ParameterExecutionException;
import com.leucine.streem.exception.ResourceNotFoundException;
import com.leucine.streem.exception.StreemException;
import com.leucine.streem.model.*;
import com.leucine.streem.model.helper.BaseEntity;
import com.leucine.streem.model.helper.PrincipalUser;
import com.leucine.streem.model.helper.parameter.*;
import com.leucine.streem.repository.*;
import com.leucine.streem.service.*;
import com.leucine.streem.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import static com.leucine.streem.constant.Misc.ACCOUNT_OWNER;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskExecutionService implements ITaskExecutionService {
  private final ITaskExecutionRepository taskExecutionRepository;
  private final ITaskExecutionMapper taskExecutionMapper;
  private final IParameterValueRepository parameterValueRepository;
  private final IUserRepository userRepository;
  private final ITempParameterValueRepository tempParameterValueRepository;
  private final ITaskExecutionAssigneeRepository taskExecutionAssigneeRepository;
  private final IJobAuditService jobAuditService;
  private final IStageReportService stageReportService;
  private final ITaskRepository taskRepository;
  private final ITaskMapper taskMapper;
  private final IJobLogService jobLogService;
  private final ITaskAutomationService taskAutomationService;
  private final IUserMapper userMapper;
  private final ITaskExecutionTimerRepository taskExecutionTimerRepository;
  private final ITaskExecutionTimerService taskExecutionTimerService;
  private final ITaskSchedulesRepository taskSchedulesRepository;
  private final IStageRepository stageRepository;
  private final ITempParameterVerificationRepository tempParameterVerificationRepository;
  private final ITempParameterMediaMappingRepository tempParameterMediaMappingRepository;
  private final IParameterVerificationRepository parameterVerificationRepository;
  private final IInterlockService interlockService;
  private final IParameterRuleMappingRepository parameterRuleMappingRepository;
  private final IParameterRepository parameterRepository;
  private final IRulesExecutionService rulesExecutionService;
  private final IParameterAutoInitializeService parameterAutoInitialiseService;
  private final IParameterExecutionHandler parameterExecutionHandler;
  private final IVariationRepository variationRepository;
  private final JdbcTemplate jdbcTemplate;
  private final ITaskExecutorLockRepository taskExecutorLockRepository;
  private final INotificationService notificationService;
  private final ParameterExecutionValidationService parameterExecutionValidationService;
  private final IJobRepository jobRepository;
  private final IParameterVerificationService parameterVerificationService;
  private final IFacilityRepository facilityRepository;

  @Override
  public TaskDto getTask(Long taskExecutionId) throws ResourceNotFoundException {
    log.info("[getTask] Request to fetch task with taskExecutionId: {}", taskExecutionId);
    TaskExecution taskExecution = taskExecutionRepository.getReferenceById(taskExecutionId);

    Task task = taskRepository.findById(taskExecution.getTaskId())
      .orElseThrow(() -> new ResourceNotFoundException(taskExecutionId, ErrorCode.TASK_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));

    Map<Long, TaskExecution> taskExecutionMap = new HashMap<>();
    taskExecutionMap.put(taskExecutionId, taskExecution);

    List<Long> parameterIds = task.getParameters().stream().map(BaseEntity::getId).collect(Collectors.toList());

    List<ParameterValue> parameterValues = parameterValueRepository.findByTaskExecutionIdAndParameterIdIn(taskExecutionId, parameterIds);
    Map<Long, List<ParameterValue>> parameterValueMap =
      parameterValues.stream()
        .collect(Collectors.groupingBy(
          av -> av.getParameter().getId(),
          Collectors.toList()
        ));

    List<TempParameterValue> tempParameterValues = tempParameterValueRepository.readByTaskExecutionIdAndParameterIdIn(taskExecution.getId(), parameterIds);
    Map<Long, List<TempParameterValue>> tempParameterValueMap =
      tempParameterValues.stream()
        .collect(Collectors.groupingBy(
          av -> av.getParameter().getId(),
          Collectors.toList()
        ));

    return taskMapper.toDto(task, parameterValueMap, taskExecutionMap, tempParameterValueMap, null, null, null);
  }

  //TODO add stats object if required, applies everywhere in task execution
  @Override
  @Transactional(rollbackFor = Exception.class)
  public TaskExecutionDto startTask(Long taskExecutionId, TaskExecutionRequest taskExecutionRequest) throws ResourceNotFoundException, StreemException, IOException, ParameterExecutionException {
    log.info("[startTask] Request to start task, taskExecutionId: {}, taskExecutionRequest: {} ", taskExecutionId, taskExecutionRequest);
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    TaskExecution taskExecution = taskExecutionRepository.getReferenceById(taskExecutionId);
    String jobId = String.valueOf(taskExecution.getJobId());

    Task task = taskRepository.findById(taskExecution.getTaskId())
      .orElseThrow(() -> new ResourceNotFoundException(taskExecutionId, ErrorCode.TASK_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));
    validateIfUserIsAssignedToExecuteParameter(taskExecutionId, principalUser.getId());

    validateIfTaskDependenciesAreCompleted(task, taskExecution.getJobId());
    validateIfPrecedingTasksAreCompleted(task, taskExecution.getJobId());
    validateIfSubsequentTasksAreNotStarted(task, taskExecution);
    validateIfAnyTaskExecutionsAreEnabledForCorrection(task.getId(), taskExecution.getJobId());
    if (task.isHasExecutorLock()) {
      validateIfCurrentExecutorIsAllowedToInteractTask(taskExecution.getTaskId(), taskExecution.getJobId(), principalUser.getId());
    }
    validateIfAllParameterValuesAreNotHidden(taskExecutionId);
    boolean soloTaskApplied = handleSoloTaskLock(taskExecutionId, Long.valueOf(jobId), task, principalUser);

    Set<String> taskScheduledIds = new HashSet<>();
    List<TaskSchedules> taskSchedulesList = taskSchedulesRepository.findByReferencedTaskIdAndCondition(task.getId(), Type.ScheduledTaskCondition.START);
    for (TaskSchedules taskSchedules : taskSchedulesList) {
      Task scheduledTask = taskRepository.findByTaskSchedulesId(taskSchedules.getId());

      TaskExecution scheduledTaskExecution = taskExecutionRepository.findByTaskIdAndJobIdOrderByOrderTree(scheduledTask.getId(), taskExecution.getJobId());
      // This check is added to avoid scheduling of task execution if it is not a master task
      if (scheduledTaskExecution.getOrderTree() == 1 && taskExecution.getOrderTree() == 1 && scheduledTaskExecution.getState() == State.TaskExecution.NOT_STARTED) {
        scheduledTaskExecution.setSchedulingExpectedStartedAt(taskSchedules.getStartDateInterval() + DateTimeUtils.now());
        scheduledTaskExecution.setSchedulingExpectedDueAt(taskSchedules.getDueDateInterval() + DateTimeUtils.now() + taskSchedules.getStartDateInterval());
        scheduledTaskExecution.setModifiedAt(DateTimeUtils.now());
        scheduledTaskExecution.setModifiedBy(userRepository.getReferenceById(principalUser.getId()));
        scheduledTaskExecution.setScheduled(true);

        scheduledTaskExecution = taskExecutionRepository.save(scheduledTaskExecution);
        taskScheduledIds.add(scheduledTaskExecution.getId().toString());
      }
    }

    User principalUserEntity = userRepository.getReferenceById(principalUser.getId());
    interlockService.validateInterlockForTaskExecution(task.getId(), jobId, Type.InterlockTriggerType.TASK_STARTED);
    validateJobState(taskExecution.getJob().getId(), Action.Task.START, taskExecution.getJob().getState());
    validateTaskState(taskExecution.getTask().getId(), Action.Task.START, taskExecution.getState());
    List<TaskExecutionUserMapping> taskExecutionUserMappingList = validateAndGetAssignedUser(task.getId(), taskExecution, principalUserEntity);

    taskExecution.setStartedAt(DateTimeUtils.now());
    taskExecution.setModifiedAt(DateTimeUtils.now());
    taskExecution.setStartedBy(principalUserEntity);
    taskExecution.setModifiedBy(principalUserEntity);
    taskExecution.setState(State.TaskExecution.IN_PROGRESS);
    taskExecution.setRecurringPrematureStartReason(taskExecutionRequest.getRecurringPrematureStartReason());
    taskExecution.setSchedulePrematureStartReason(taskExecutionRequest.getSchedulePrematureStartReason());

    TaskExecutionDto taskExecutionDto = taskExecutionMapper.toDto(taskExecutionRepository.save(taskExecution), principalUser);
    updateUserAction(taskExecutionUserMappingList);
    jobAuditService.startTask(taskExecution.getJobId(), task.getId(), taskExecutionRequest, principalUser);
    if (soloTaskApplied) {
      User systemUser = userRepository.findById(User.SYSTEM_USER_ID).get();
      PrincipalUser systemPrincipalUser = userMapper.toPrincipalUser(systemUser);
      try {
        Thread.sleep(1);
      } catch (InterruptedException ignore) {
      }
      jobAuditService.handleSoloTaskLock(task, Long.valueOf(jobId), principalUser, systemPrincipalUser);
    }
    stageReportService.setStageToInProgress(taskExecution.getJobId(), task.getId());
    UserAuditDto userAuditDto = userMapper.toUserAuditDto(principalUserEntity);
    jobLogService.recordJobLogTrigger(jobId, task.getIdAsString(), Type.JobLogTriggerType.TSK_STARTED_BY, task.getName(), null,
      Utility.getFullNameAndEmployeeIdFromPrincipalUser(principalUser), principalUser.getIdAsString(), userAuditDto);
    jobLogService.recordJobLogTrigger(jobId, task.getIdAsString(), Type.JobLogTriggerType.TSK_START_TIME, task.getName(), null, String.valueOf(
      taskExecution.getStartedAt()), String.valueOf(
      taskExecution.getStartedAt()), userAuditDto);

    List<AutomationResponseDto> executedAutomations = taskAutomationService.completeTaskAutomations(task.getId(), taskExecution.getJobId(), taskExecutionRequest.getCreateObjectAutomations(), taskExecutionRequest.getAutomationReason(), Type.AutomationTriggerType.TASK_STARTED);
    taskExecutionDto.setExecutedAutomations(executedAutomations);
    taskExecutionDto.setScheduledTaskExecutionIds(taskScheduledIds);

    if (!Utility.isEmpty(taskScheduledIds)) {
      jobAuditService.scheduleTask(taskExecution.getJobId(), task.getId(), principalUser, false, false, taskScheduledIds);
    }

    List<AutoInitializeParameterView> nonHiddenAutoInitializedParameterViewOfTaskExecution = parameterRepository.getNonHiddenAutoInitialisedParametersByTaskExecutionId(taskExecution.getId());
    List<ParameterExecuteRequest> parameterExecuteRequests = parameterAutoInitialiseService.getAllParameterExecuteRequestForParameterToAutoInitialize(taskExecution.getJobId(), nonHiddenAutoInitializedParameterViewOfTaskExecution, false);

    for (ParameterExecuteRequest parameterExecuteRequest : parameterExecuteRequests) {
      if (!Utility.isEmpty(parameterExecuteRequest)) {
        parameterExecutionHandler.executeParameter(parameterExecuteRequest.getJobId(), null, parameterExecuteRequest, Type.JobLogTriggerType.PARAMETER_VALUE, false, false, false);
      }
    }
    return taskExecutionDto;
  }

  private void validateIfAllParameterValuesAreNotHidden(Long taskExecutionId) throws StreemException {
    log.info("[validateIfAllParameterValuesAreNotHidden] Request to validate if all parameter values are not hidden, taskExecutionId: {}", taskExecutionId);
    boolean areAllParameterValuesHidden = parameterValueRepository.areAllParameterValuesHiddenByTaskExecutionId(taskExecutionId);
    if (areAllParameterValuesHidden) {
      ValidationUtils.invalidate(taskExecutionId.toString(), ErrorCode.TASK_CANNOT_BE_STARTED_WHEN_ALL_PARAMETERS_ARE_HIDDEN);
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


  @Override
  public BasicDto validateTask(Long taskExecutionId) throws StreemException, ResourceNotFoundException {
    log.info("[validateTask] Request to validate task, taskExecutionId: {}", taskExecutionId);
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    User principalUserEntity = userRepository.getOne(principalUser.getId());
    TaskExecution taskExecution = taskExecutionRepository.getReferenceById(taskExecutionId);

    Task task = taskRepository.findById(taskExecution.getTaskId())
      .orElseThrow(() -> new ResourceNotFoundException(taskExecutionId, ErrorCode.TASK_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));

    validateJobState(taskExecution.getJob().getId(), Action.Task.COMPLETE, taskExecution.getJob().getState());
    validateTaskState(taskExecution.getTask().getId(), Action.Task.COMPLETE, taskExecution.getState());
    validateAndGetAssignedUser(task.getId(), taskExecution, principalUserEntity);
    validateIncompleteParameters(taskExecution.getJobId(), taskExecutionId);

    BasicDto basicDto = new BasicDto();
    basicDto.setMessage("success");
    return basicDto;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public TaskExecutionDto completeTask(Long taskExecutionId, TaskCompletionRequest taskCompletionRequest) throws ResourceNotFoundException, StreemException, IOException, ParameterExecutionException {
    log.info("[completeTask] Request to complete task, taskExecutionId: {}, taskCompletionRequest: {}", taskExecutionId, taskCompletionRequest);
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    User principalUserEntity = userRepository.getReferenceById(principalUser.getId());
    TaskExecution taskExecution = taskExecutionRepository.getReferenceById(taskExecutionId);

    verifyDataIntegrity(taskExecution.getId(), taskCompletionRequest);

    validateIfUserIsAssignedToExecuteParameter(taskExecutionId, principalUser.getId());
    Job job = taskExecution.getJob();

    Task task = taskRepository.findById(taskExecution.getTaskId())
      .orElseThrow(() -> new ResourceNotFoundException(taskExecutionId, ErrorCode.TASK_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));

    if (task.isHasExecutorLock()) {
      validateIfCurrentExecutorIsAllowedToInteractTask(taskExecution.getTaskId(), taskExecution.getJobId(), principalUser.getId());
    }
    validateTaskLock(taskExecution, principalUser, taskExecutionId);
    validateJobState(taskExecution.getJobId(), Action.Task.COMPLETE, job.getState());
    validateTaskState(taskExecution.getTask().getId(), Action.Task.COMPLETE, taskExecution.getState());
    validateIfTaskIsInResumedState(taskExecutionId);
    List<TaskExecutionUserMapping> taskExecutionUserMappingList = validateAndGetAssignedUser(task.getId(), taskExecution, principalUserEntity);
    validateIncompleteVerificationParameters(taskExecutionId, false);
    validateIncompleteParameters(taskExecution.getJobId(), taskExecutionId);
    validateIfAllParametersOfTasksAreNotHidden(taskExecutionId);
    validateParameterValueValidations(taskExecutionId, job.getId());

    Set<String> taskScheduledIds = new HashSet<>();
    List<TaskSchedules> taskSchedulesList = taskSchedulesRepository.findByReferencedTaskIdAndCondition(task.getId(), Type.ScheduledTaskCondition.COMPLETE);

    for (TaskSchedules taskSchedules : taskSchedulesList) {
      Task scheduledTask = taskRepository.findByTaskSchedulesId(taskSchedules.getId());
      TaskExecution scheduledTaskExecution = taskExecutionRepository.findByTaskIdAndJobIdOrderByOrderTree(scheduledTask.getId(), taskExecution.getJobId());
      // This check is added to avoid scheduling of task execution if it is not a master task
      if (scheduledTaskExecution.getOrderTree() == 1 && taskExecution.getOrderTree() == 1 && scheduledTaskExecution.getState() == State.TaskExecution.NOT_STARTED) {
        scheduledTaskExecution.setSchedulingExpectedStartedAt(taskSchedules.getStartDateInterval() + DateTimeUtils.now());
        scheduledTaskExecution.setSchedulingExpectedDueAt(taskSchedules.getDueDateInterval() + DateTimeUtils.now() + taskSchedules.getStartDateInterval());
        scheduledTaskExecution.setModifiedAt(DateTimeUtils.now());
        scheduledTaskExecution.setModifiedBy(userRepository.getReferenceById(principalUser.getId()));
        scheduledTaskExecution.setScheduled(true);

        scheduledTaskExecution = taskExecutionRepository.save(scheduledTaskExecution);
        taskScheduledIds.add(scheduledTaskExecution.getId().toString());
      }
    }

    long endedAt = DateTimeUtils.now();

    TaskPauseOrResumeRequest taskPauseOrResumeRequest = new TaskPauseOrResumeRequest(taskExecution.getJobId(), TaskPauseReason.TASK_COMPLETED, null);
    taskExecutionTimerService.saveTaskPauseTimer(taskPauseOrResumeRequest, taskExecution, principalUserEntity);


    taskExecutionTimerService.calculateDurationAndReturnReasonsOrComments(List.of(taskExecution));
    if (isInvalidTimedTaskCompletedState(task, taskExecution.getDuration(), endedAt)) {
      ValidationUtils.validateNotEmpty(taskCompletionRequest.getReason(), task.getId(), ErrorCode.TIMED_TASK_REASON_CANNOT_BE_EMPTY);
      taskExecution.setReason(taskCompletionRequest.getReason());
    }

    taskExecution.setEndedAt(endedAt);
    taskExecution.setEndedBy(principalUserEntity);
    taskExecution.setModifiedBy(principalUserEntity);
    taskExecution.setModifiedAt(DateTimeUtils.now());
    taskExecution.setState(State.TaskExecution.COMPLETED);
    taskExecution.setRecurringOverdueCompletionReason(taskCompletionRequest.getRecurringOverdueCompletionReason());
    taskExecution.setScheduleOverdueCompletionReason(taskCompletionRequest.getScheduleOverdueCompletionReason());
    taskExecution.setContinueRecurrence(taskCompletionRequest.isContinueRecurrence() && task.isEnableRecurrence());


    taskExecution = taskExecutionRepository.save(taskExecution);
    TaskExecutionDto taskExecutionDto = taskExecutionMapper.toDto(taskExecution, principalUser);

    updateUserAction(taskExecutionUserMappingList);



    stageReportService.incrementTaskCompleteCount(taskExecution.getJobId(), task.getId());
    jobAuditService.completeTask(taskExecution.getJobId(), task.getId(), taskCompletionRequest, taskExecution.getOrderTree(), principalUser);

    UserAuditDto userAuditDto = userMapper.toUserAuditDto(principalUserEntity);
    jobLogService.recordJobLogTrigger(job.getIdAsString(), task.getIdAsString(), Type.JobLogTriggerType.TSK_ENDED_BY, task.getName(), null,
      Utility.getFullNameAndEmployeeIdFromPrincipalUser(principalUser), principalUser.getIdAsString(), userAuditDto);
    jobLogService.recordJobLogTrigger(job.getIdAsString(), task.getIdAsString(), Type.JobLogTriggerType.TSK_END_TIME, task.getName(), null, String.valueOf(
      taskExecution.getEndedAt()), String.valueOf(
      taskExecution.getEndedAt()), userAuditDto);

    List<AutomationResponseDto> executedAutomations = taskAutomationService.completeTaskAutomations(task.getId(), job.getId(), taskCompletionRequest.getCreateObjectAutomations(), taskCompletionRequest.getAutomationReason(), Type.AutomationTriggerType.TASK_COMPLETED);
    taskExecutionDto.setExecutedAutomations(executedAutomations);
    interlockService.validateInterlockForTaskExecution(task.getId(), job.getIdAsString(), Type.InterlockTriggerType.TASK_COMPLETED);
    taskExecutionDto.setScheduledTaskExecutionIds(taskScheduledIds);
    // ordering is important to create a recurring task after all the features related to previous tasks is done
    TaskExecution expectedRecurredTaskExecution = validateAndCreateRecurringTask(task, taskExecution, job, taskCompletionRequest.isContinueRecurrence(), principalUserEntity);
    if (task.isEnableRecurrence()) {
      jobAuditService.recurrenceTask(taskExecution.getJobId(), taskExecution.getId(), expectedRecurredTaskExecution.getId(), task.getId(), taskCompletionRequest.isContinueRecurrence(), principalUser);
      taskExecutionDto.setRecurredTaskExecutionId(String.valueOf(expectedRecurredTaskExecution.getId()));
    }
    if (!Utility.isEmpty(taskScheduledIds)) {
      jobAuditService.scheduleTask(taskExecution.getJobId(), task.getId(), principalUser, true, false, taskScheduledIds);
    }
    notificationService.notifyIfAllPrerequisiteTasksCompleted(task.getId(), job.getId(), job.getOrganisationId());
    return taskExecutionDto;
  }

  private void validateParameterValueValidations(Long taskExecutionId, Long jobId) throws IOException, StreemException, ResourceNotFoundException {
    List<ParameterValueView> parameterValueList = parameterValueRepository.findAllExecutionDataByTaskExecutionId(taskExecutionId);

    for (ParameterValueView parameterValueView : parameterValueList) {
      if (parameterValueView.getState() == State.ParameterExecution.EXECUTED && !parameterValueView.getHidden()) {
        switch (parameterValueView.getType()) {
          case NUMBER -> {
            if (!Utility.isEmpty(parameterValueView.getData())) {
              NumberParameter numberParameter = JsonUtils.readValue(parameterValueView.getData(), NumberParameter.class);
              LeastCount leastCount = numberParameter.getLeastCount();
              if (!Utility.isEmpty(leastCount)) {
                BigDecimal inputValue = new BigDecimal(parameterValueView.getValue());
                parameterExecutionValidationService.validateLeastCount(parameterValueView.getParameterId(), leastCount, inputValue, jobId);
              }
              if (!parameterValueView.getHasExceptions()) {
                try {
                  parameterExecutionValidationService.validateNumberParameterValidations(jobId, parameterValueView.getId(), parameterValueView.getParameterId(), JsonUtils.valueToNode(parameterValueView.getValidations()), parameterValueView.getValue());
                } catch (ParameterExecutionException e) {
                  throw new StreemException(e.getErrorList());
                }
              }
            }
          }
          case RESOURCE, MULTI_RESOURCE -> {
            ResourceParameter resourceParameter = JsonUtils.readValue(parameterValueView.getData(), ResourceParameter.class);
            if (!Utility.isEmpty(parameterValueView.getChoices()) && !parameterValueView.getHasExceptions()) {
              List<ResourceParameterChoiceDto> choices = JsonUtils.jsonToCollectionType(parameterValueView.getChoices(), List.class, ResourceParameterChoiceDto.class);
              for (ResourceParameterChoiceDto resourceParameterChoiceDto : choices) {
                try {
                  parameterExecutionValidationService.validateParameterValueChoice(resourceParameterChoiceDto.getObjectId(), resourceParameter.getObjectTypeExternalId(), JsonUtils.valueToNode(parameterValueView.getValidations()), parameterValueView.getParameterId().toString(), jobId, false);
                } catch (ParameterExecutionException e) {
                  throw new StreemException(e.getErrorList());
                }
              }
            }
          }
          case DATE, DATE_TIME -> {
            if (!Utility.isEmpty(parameterValueView.getValidations()) && !Utility.isEmpty(parameterValueView.getData())) {
              JsonNode validations = JsonUtils.valueToNode(parameterValueView.getValidations());

              if (!validations.isEmpty() && !Utility.isEmpty(parameterValueView.getValue())) {
                PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
                Facility facility = facilityRepository.getReferenceById(principalUser.getCurrentFacilityId());
                String facilityTimeZone = facility.getTimeZone();
                boolean isDateTimeParameter = parameterValueView.getType() == Type.Parameter.DATE_TIME;
                if (!parameterValueView.getHasExceptions()) {
                  try {
                    parameterExecutionValidationService.validateDateAndDateTimeParameterValidations(jobId, validations, parameterValueView.getValue(), isDateTimeParameter, facilityTimeZone, parameterValueView.getParameterId());
                  } catch (ParameterExecutionException e) {
                    throw new StreemException(e.getErrorList());
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  private void validateIfTaskIsInResumedState(Long taskExecutionId) throws StreemException {
    log.info("[validateIfTaskIsInResumedState] Request to validate if task is in resumed state, taskExecutionId: {}", taskExecutionId);
    TaskExecutionTimer taskExecutionTimer = taskExecutionTimerRepository.findPausedTimerByTaskExecutionIdAndJobId((taskExecutionId));
    if (!Utility.isEmpty(taskExecutionTimer)) {
      ValidationUtils.invalidate(taskExecutionId.toString(), ErrorCode.TASK_CANNOT_BE_COMPLETED_WHEN_PAUSED);
    }
  }

  private TaskExecution validateAndCreateRecurringTask(Task task, TaskExecution maxTaskExecution, Job job, boolean continueRecurrence, User principalUserEntity) throws StreemException, IOException {
    log.info("[validateAndCreateRecurringTask] Request to validate and create recurring task, task: {}, maxTaskExecution: {}, job: {}, continueRecurrence: {}", task, maxTaskExecution, job, continueRecurrence);
    if (continueRecurrence && task.isEnableRecurrence()) {
      long now = DateTimeUtils.now();
      validateIfAnyTaskExecutionContainsStopRecurrence(task.getId(), job.getId());

      TaskExecution recurredTaskExecution = createTaskExecution(job, task, maxTaskExecution.getOrderTree() + 1, Type.TaskExecutionType.RECURRING, principalUserEntity);
      TaskRecurrence taskRecurrence = task.getTaskRecurrence();

      // If master task then recurrence start time will be from task start time otherwise it will be expected start time of task start
      if (maxTaskExecution.getOrderTree() == 1) {
        if (!Utility.isEmpty(taskRecurrence.getStartDateInterval()) && taskRecurrence.getStartDateInterval() != 0) {
          Long taskStartedAtTime = maxTaskExecution.getStartedAt() == null ? now : maxTaskExecution.getStartedAt();
          recurredTaskExecution.setRecurringExpectedStartedAt(taskRecurrence.getStartDateInterval() + taskStartedAtTime);
        }
        if (!Utility.isEmpty(taskRecurrence.getDueDateInterval()) && taskRecurrence.getDueDateInterval() != 0) {
          recurredTaskExecution.setRecurringExpectedDueAt(recurredTaskExecution.getRecurringExpectedStartedAt() + taskRecurrence.getDueDateInterval());
        }
      } else {
        if (!Utility.isEmpty(taskRecurrence.getStartDateInterval()) && taskRecurrence.getStartDateInterval() != 0) {
          recurredTaskExecution.setRecurringExpectedStartedAt(taskRecurrence.getStartDateInterval() + maxTaskExecution.getRecurringExpectedStartedAt());
        }
        if (!Utility.isEmpty(taskRecurrence.getDueDateInterval()) && taskRecurrence.getDueDateInterval() != 0) {
          recurredTaskExecution.setRecurringExpectedDueAt(recurredTaskExecution.getRecurringExpectedStartedAt() + taskRecurrence.getDueDateInterval());
        }
      }
      recurredTaskExecution.setContinueRecurrence(true);

      // This checks if early start or delayed completion reasons are present
      if (!State.TASK_EXECUTION_EXCEPTION_STATE.contains(maxTaskExecution.getState())) {
        validateIfRecurrenceReasonsArePresent(task, maxTaskExecution, now, recurredTaskExecution, taskRecurrence);
      }

      recurredTaskExecution = taskExecutionRepository.save(recurredTaskExecution);

      Set<TaskExecutionUserMapping> taskExecutionUserMappings = maxTaskExecution.getAssignees();

      List<TaskExecutionUserMapping> newTaskExecutionUserMappings = new ArrayList<>();
      for (TaskExecutionUserMapping taskExecutionUserMapping : taskExecutionUserMappings) {
        TaskExecutionUserMapping taskExecutionUserMappingForRecurringTask;
        if (!Utility.isEmpty(taskExecutionUserMapping.getUser())) {
          taskExecutionUserMappingForRecurringTask = new TaskExecutionUserMapping(taskExecutionRepository.getReferenceById(recurredTaskExecution.getId()), userRepository.getReferenceById(taskExecutionUserMapping.getUsersId()), principalUserEntity);
        } else {
          taskExecutionUserMappingForRecurringTask = new TaskExecutionUserMapping(taskExecutionRepository.getReferenceById(recurredTaskExecution.getId()), taskExecutionUserMapping.getUserGroup(), principalUserEntity);
        }
        //TODO: use list of taskExecutionIds instead of taskExecutionId
        newTaskExecutionUserMappings.add(taskExecutionUserMappingForRecurringTask);
      }
      updateUserAction(newTaskExecutionUserMappings);

      Map<Long, ParameterValue> previousParameterIdAndParameterValueMap = maxTaskExecution.getParameterValues().stream()
        .collect(Collectors.toMap(parameterValue -> parameterValue.getParameter().getId(), Function.identity()));

      List<ParameterValue> parameterValues = createParameterValues(job, recurredTaskExecution, task, principalUserEntity, previousParameterIdAndParameterValueMap);

      for (ParameterValue parameterValue : parameterValues) {
        applyRules(parameterValue.getParameter(), job);
      }

      validateIfAllParametersOfRecurringTasksAreNotHidden(recurredTaskExecution.getId());

      copyVariations(maxTaskExecution, recurredTaskExecution, principalUserEntity, parameterValues);

      return recurredTaskExecution;

    } else {
      taskExecutionRepository.setAllTaskExecutionsContinueRecurrenceFalse(task.getId(), job.getId());
      return maxTaskExecution;
    }

  }

  private static void validateIfRecurrenceReasonsArePresent(Task task, TaskExecution maxTaskExecution, long now, TaskExecution recurredTaskExecution, TaskRecurrence taskRecurrence) throws StreemException {
    if (maxTaskExecution.getOrderTree() != 1) { // Since recurrence related reasons come into picture after the master task is completed. Hence this check is added
      Long expectedDueAt = recurredTaskExecution.getRecurringExpectedDueAt();

      if (!Utility.isEmpty(expectedDueAt) && now > expectedDueAt) {
        Integer positiveDueDateInterval = taskRecurrence.getPositiveDueDateToleranceInterval();
        String recurringOverdueCompletionReason = maxTaskExecution.getRecurringOverdueCompletionReason();

        if (!Utility.isEmpty(positiveDueDateInterval) && now > (expectedDueAt + positiveDueDateInterval) && Utility.isEmpty(recurringOverdueCompletionReason)) {
          ValidationUtils.invalidate(task.getId(), ErrorCode.RECURRING_TASK_OVERDUE_COMPLETION_REASON_MISSING);
        } else if (Utility.isEmpty(recurringOverdueCompletionReason)) {
          ValidationUtils.invalidate(task.getId(), ErrorCode.RECURRING_TASK_OVERDUE_COMPLETION_REASON_MISSING);
        }

      }
    }
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public TaskExecutionDto repeatTaskExecution(TaskRepeatRequest taskRepeatRequest) throws ResourceNotFoundException, StreemException, IOException {
    log.info("[repeatTaskExecution] Request to repeat task, taskRepeatRequest: {}", taskRepeatRequest);
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    User principalUserEntity = userRepository.getReferenceById(principalUser.getId());
    validateIfAllTaskExecutionsAreInCompletedState(taskRepeatRequest.getTaskId(), taskRepeatRequest.getJobId());

    Task task = taskRepository.findById(taskRepeatRequest.getTaskId())
      .orElseThrow(() -> new ResourceNotFoundException(taskRepeatRequest.getTaskId(), ErrorCode.TASK_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));

    if (task.isEnableRecurrence()) {
      ValidationUtils.invalidate(taskRepeatRequest.getTaskId(), ErrorCode.RECURRING_TASK_CANNOT_BE_REPEATED);
    }
    TaskExecution taskExecution = taskExecutionRepository.findByTaskIdAndJobIdAndType(taskRepeatRequest.getTaskId(), taskRepeatRequest.getJobId(), Type.TaskExecutionType.MASTER);
    TaskExecution maxTaskExecution = taskExecutionRepository.findByTaskIdAndJobIdOrderByOrderTree(taskRepeatRequest.getTaskId(), taskExecution.getJobId());

    validateIfSubsequentTasksAreNotStarted(task, maxTaskExecution);
    validateIfAllParametersOfTasksAreNotHidden(maxTaskExecution.getId());
    validateJobState(taskRepeatRequest.getJobId(), Action.Task.REPEAT, taskExecution.getJob().getState());

    Job job = taskExecution.getJob();
    Integer orderTreeRequired = maxTaskExecution.getOrderTree() + 1;
    TaskExecution repeatedTaskExecution = createTaskExecution(job, task, orderTreeRequired, Type.TaskExecutionType.REPEAT, principalUserEntity);
    repeatedTaskExecution = taskExecutionRepository.save(repeatedTaskExecution);

    Set<TaskExecutionUserMapping> taskExecutionUserMappings = taskExecution.getAssignees();

    List<TaskExecutionUserMapping> newTaskExecutionUserMappings = new ArrayList<>();
    for (TaskExecutionUserMapping taskExecutionUserMapping : taskExecutionUserMappings) {
      TaskExecutionUserMapping taskExecutionUserMappingForRepeatTask;
      if (!Utility.isEmpty(taskExecutionUserMapping.getUser())) {
        taskExecutionUserMappingForRepeatTask = new TaskExecutionUserMapping(taskExecutionRepository.getReferenceById(repeatedTaskExecution.getId()), userRepository.getReferenceById(taskExecutionUserMapping.getUsersId()), principalUserEntity);
      } else {
        taskExecutionUserMappingForRepeatTask = new TaskExecutionUserMapping(taskExecutionRepository.getReferenceById(repeatedTaskExecution.getId()), taskExecutionUserMapping.getUserGroup(), principalUserEntity);
      }
      newTaskExecutionUserMappings.add(taskExecutionUserMappingForRepeatTask);
    }
    updateUserAction(newTaskExecutionUserMappings);

    repeatedTaskExecution = taskExecutionRepository.save(repeatedTaskExecution);

    Map<Long, ParameterValue> previousParameterIdAndParameterValueMap = maxTaskExecution.getParameterValues().stream()
      .collect(Collectors.toMap(parameterValue -> parameterValue.getParameter().getId(), Function.identity()));

    List<ParameterValue> parameterValues = createParameterValues(job, repeatedTaskExecution, task, principalUserEntity, previousParameterIdAndParameterValueMap);

    for (ParameterValue parameterValue : parameterValues) {
      applyRules(parameterValue.getParameter(), job);
    }
    Set<Parameter> parameters = task.getParameters();
    Set<Long> parameterIds = parameters.stream().map(BaseEntity::getId).collect(Collectors.toSet());

    Set<Parameter> triggeringParameters = parameterRuleMappingRepository.findAllByImpactedParameterIdIn(parameterIds);

    for (Parameter triggeringParameter : triggeringParameters) {
      //TODO: fetch all latest parameter_values for all triggering parameters
      ParameterValue latestTriggeringParameterValue = parameterValueRepository.findLatestByJobIdAndParameterId(job.getId(), triggeringParameter.getId());
      rulesExecutionService.updateRules(job.getId(), triggeringParameter, latestTriggeringParameterValue);
    }

    validateIfAllParametersOfRepeatedTasksAreNotHidden(repeatedTaskExecution.getId());

    copyVariations(maxTaskExecution, repeatedTaskExecution, principalUserEntity, parameterValues);

    jobAuditService.repeatTask(taskRepeatRequest, principalUser, maxTaskExecution.getId(), repeatedTaskExecution.getId());

    return taskExecutionMapper.toDto(repeatedTaskExecution, principalUser);
  }

  private void copyVariations(TaskExecution previousTaskExecution, TaskExecution repeatedTaskExecution, User principalUserEntity, List<ParameterValue> repeatedParameterValues) {
    log.info("[copyVariations] Request to copy variations, previousTaskExecution: {}, repeatedTaskExecution: {}", previousTaskExecution, repeatedTaskExecution);
    Map<Long, ParameterValue> repeatedTaskExecutionParameterValues = repeatedParameterValues.stream().collect(Collectors.toMap(parameterValue -> parameterValue.getParameter().getId(), Function.identity()));


    List<Variation> variationList = variationRepository.findByTaskExecutionId(previousTaskExecution.getId());
    List<Variation> newVariationList = new ArrayList<>();


    for (Variation variation : variationList) {
      Variation newVariation = new Variation();
      newVariation.setId(IdGenerator.getInstance().generateUnique());
      //TODO: find why parameterValue.getParameterId() is not working anywhere
      newVariation.setParameterValue(repeatedTaskExecutionParameterValues.get(variation.getParameterValue().getParameter().getId()));
      newVariation.setNewDetails(variation.getNewDetails());
      newVariation.setOldDetails(variation.getOldDetails());
      newVariation.setType(variation.getType());
      newVariation.setJob(repeatedTaskExecution.getJob());
      newVariation.setName(variation.getName());
      newVariation.setDescription(variation.getDescription());
      newVariation.setVariationNumber(variation.getVariationNumber());
      newVariation.setConfigId(variation.getConfigId());
      newVariation.setCreatedBy(principalUserEntity);
      newVariation.setModifiedBy(principalUserEntity);
      newVariation.setCreatedAt(DateTimeUtils.now());
      newVariation.setModifiedAt(DateTimeUtils.now());
      newVariationList.add(newVariation);
    }
    variationRepository.saveAll(newVariationList);
  }


  @Override
  @Transactional(rollbackFor = Exception.class)
  public TaskExecutionDto skipTask(Long taskExecutionId, TaskExecutionRequest taskExecutionRequest) throws ResourceNotFoundException, StreemException, IOException {
    log.info("[skipTask] Request to skip task, taskExecutionId: {}, taskExecutionRequest: {}", taskExecutionId, taskExecutionRequest);
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    User principalUserEntity = userRepository.getOne(principalUser.getId());
    TaskExecution taskExecution = taskExecutionRepository.getReferenceById(taskExecutionId);
    Task task = taskRepository.findById(taskExecution.getTaskId())
      .orElseThrow(() -> new ResourceNotFoundException(taskExecutionId, ErrorCode.TASK_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));

    if (task.isHasExecutorLock()) {
      validateIfCurrentExecutorIsAllowedToInteractTask(taskExecution.getTaskId(), taskExecution.getJobId(), principalUser.getId());
    }

    ValidationUtils.validateNotEmpty(taskExecutionRequest.getReason(), taskExecution.getTaskId(), ErrorCode.PROVIDE_REASON_TO_SKIP_TASK);
    validateJobState(taskExecution.getJob().getId(), Action.Task.SKIP, taskExecution.getJob().getState());
    validateTaskState(taskExecution.getJob().getId(), Action.Task.SKIP, taskExecution.getState());
    validateIfTaskIsInResumedState(taskExecutionId);
    List<TaskExecutionUserMapping> taskExecutionUserMappingList = validateAndGetAssignedUser(taskExecution.getTaskId(), taskExecution, principalUserEntity);
    validateIfUserIsAssignedToExecuteParameter(taskExecutionId, principalUser.getId());

    taskExecution.setReason(taskExecutionRequest.getReason());
    taskExecution.setEndedAt(DateTimeUtils.now());
    taskExecution.setEndedBy(principalUserEntity);
    taskExecution.setModifiedBy(principalUserEntity);
    taskExecution.setModifiedAt(DateTimeUtils.now());
    taskExecution.setState(State.TaskExecution.SKIPPED);
    taskExecution.setRecurringOverdueCompletionReason(taskExecutionRequest.getRecurringOverdueCompletionReason());
    taskExecution.setContinueRecurrence(taskExecutionRequest.isContinueRecurrence() && task.isEnableRecurrence());
    TaskExecutionDto taskExecutionDto = taskExecutionMapper.toDto(taskExecutionRepository.save(taskExecution), principalUser);

    Set<String> taskScheduledIds = new HashSet<>();
    List<TaskSchedules> taskSchedulesList = taskSchedulesRepository.findByReferencedTaskIdAndCondition(task.getId(), Type.ScheduledTaskCondition.COMPLETE);
    for (TaskSchedules taskSchedules : taskSchedulesList) {
      Task scheduledTask = taskRepository.findByTaskSchedulesId(taskSchedules.getId());
      TaskExecution scheduledTaskExecution = taskExecutionRepository.findByTaskIdAndJobIdOrderByOrderTree(scheduledTask.getId(), taskExecution.getJobId());
      // This check is added to avoid scheduling of task execution if it is not a master task
      if (scheduledTaskExecution.getOrderTree() == 1 && taskExecution.getOrderTree() == 1 && scheduledTaskExecution.getState() == State.TaskExecution.NOT_STARTED) {
        scheduledTaskExecution.setSchedulingExpectedStartedAt(taskSchedules.getStartDateInterval() + DateTimeUtils.now());
        scheduledTaskExecution.setSchedulingExpectedDueAt(taskSchedules.getDueDateInterval() + DateTimeUtils.now() + taskSchedules.getStartDateInterval());
        scheduledTaskExecution.setModifiedAt(DateTimeUtils.now());
        scheduledTaskExecution.setModifiedBy(userRepository.getReferenceById(principalUser.getId()));
        scheduledTaskExecution.setScheduled(true);

        scheduledTaskExecution = taskExecutionRepository.save(scheduledTaskExecution);
        taskScheduledIds.add(scheduledTaskExecution.getId().toString());
      }
    }

    updateUserAction(taskExecutionUserMappingList);

    TaskPauseOrResumeRequest taskPauseOrResumeRequest = new TaskPauseOrResumeRequest(taskExecution.getJobId(), TaskPauseReason.TASK_COMPLETED, null);
    taskExecutionTimerService.saveTaskPauseTimer(taskPauseOrResumeRequest, taskExecution, principalUserEntity);

    stageReportService.incrementTaskCompleteCount(taskExecution.getJobId(), taskExecution.getTaskId());
    // ordering is important to create a recurring task after all the features related to previous tasks is done
    TaskExecution expectedRecurredTaskExecution = validateAndCreateRecurringTask(task, taskExecution, taskExecution.getJob(), taskExecutionRequest.isContinueRecurrence(), principalUserEntity);
    if (task.isEnableRecurrence()) {
      jobAuditService.recurrenceTask(taskExecution.getJobId(), taskExecution.getId(), expectedRecurredTaskExecution.getId(), task.getId(), taskExecutionRequest.isContinueRecurrence(), principalUser);
    }
    jobAuditService.skipTask(taskExecution.getJobId(), taskExecution.getTaskId(), taskExecutionRequest, principalUser);
    taskExecutionDto.setScheduledTaskExecutionIds(taskScheduledIds);
    notificationService.notifyIfAllPrerequisiteTasksCompleted(task.getId(), taskExecution.getJobId(), principalUser.getOrganisationId());
    return taskExecutionDto;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public TaskExecutionDto completeWithException(Long taskExecutionId, TaskCompletionRequest taskCompletionRequest) throws ResourceNotFoundException, StreemException, IOException {
    log.info("[completeWithException] Request to complete task with Exception, taskExecutionId: {}, taskCompletionRequest: {}", taskExecutionId, taskCompletionRequest);
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    User principalUserEntity = userRepository.getOne(principalUser.getId());
    TaskExecution taskExecution = taskExecutionRepository.getReferenceById(taskExecutionId);

    verifyDataIntegrity(taskExecutionId, taskCompletionRequest);

    Task task = taskRepository.findById(taskExecution.getTaskId())
      .orElseThrow(() -> new ResourceNotFoundException(taskExecutionId, ErrorCode.TASK_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));

    List<RoleBasicView> roles = userRepository.getUserRoles(String.valueOf(principalUser.getId()));
    boolean isAccountOwner = roles.stream()
        .anyMatch(role -> ACCOUNT_OWNER.equalsIgnoreCase(role.getName()));

    if (!isAccountOwner) {
      if (task.isSoloTask()) {
        validateIfUserIsAssignedToExecuteJob(taskExecution.getJobId(), principalUser.getId());
      } else {
        validateIfUserIsAssignedToExecuteParameter(taskExecutionId, principalUser.getId());
      }
    }

    Job job = taskExecution.getJob();

    ValidationUtils.validateNotEmpty(taskCompletionRequest.getReason(), taskExecution.getTaskId(), ErrorCode.PROVIDE_REASON_TO_FORCE_CLOSE_TASK);
    validateJobState(taskExecution.getJob().getId(), Action.Task.COMPLETE_WITH_EXCEPTION, taskExecution.getJob().getState());
    validateTaskState(taskExecution.getTask().getId(), Action.Task.COMPLETE_WITH_EXCEPTION, taskExecution.getState());
    validateIfTaskIsInResumedState(taskExecutionId);
    validateParameterState(taskExecutionId);
    // TODO uncomment and test
    //    verifyDataIntegrity(taskExecution.getJobId(), taskCompletionRequest);
    List<TaskExecutionUserMapping> taskExecutionUserMappingList = validateAndGetAssignedUser(taskExecution.getTaskId(), taskExecution, principalUserEntity);

    taskExecution.setReason(taskCompletionRequest.getReason());
    taskExecution.setEndedAt(DateTimeUtils.now());
    taskExecution.setEndedBy(principalUserEntity);
    taskExecution.setModifiedAt(DateTimeUtils.now());
    taskExecution.setModifiedBy(principalUserEntity);
    taskExecution.setState(State.TaskExecution.COMPLETED_WITH_EXCEPTION);

    TaskExecutionDto taskExecutionDto = taskExecutionMapper.toDto(taskExecutionRepository.save(taskExecution), principalUser);

    Set<String> taskScheduledIds = new HashSet<>();
    List<TaskSchedules> taskSchedulesList = taskSchedulesRepository.findByReferencedTaskIdAndCondition(task.getId(), Type.ScheduledTaskCondition.COMPLETE);
    for (TaskSchedules taskSchedules : taskSchedulesList) {
      Task scheduledTask = taskRepository.findByTaskSchedulesId(taskSchedules.getId());
      TaskExecution scheduledTaskExecution = taskExecutionRepository.findByTaskIdAndJobIdOrderByOrderTree(scheduledTask.getId(), taskExecution.getJobId());
      // THis check is added to avoid scheduling of task if it's not a master task
      if (scheduledTaskExecution.getOrderTree() == 1 && taskExecution.getOrderTree() == 1 && scheduledTaskExecution.getState() == State.TaskExecution.NOT_STARTED) {
        scheduledTaskExecution.setSchedulingExpectedStartedAt(taskSchedules.getStartDateInterval() + DateTimeUtils.now());
        scheduledTaskExecution.setSchedulingExpectedDueAt(taskSchedules.getDueDateInterval() + DateTimeUtils.now() + taskSchedules.getStartDateInterval());
        scheduledTaskExecution.setModifiedAt(DateTimeUtils.now());
        scheduledTaskExecution.setModifiedBy(userRepository.getReferenceById(principalUser.getId()));
        scheduledTaskExecution.setScheduled(true);

        scheduledTaskExecution = taskExecutionRepository.save(scheduledTaskExecution);
        taskScheduledIds.add(scheduledTaskExecution.getId().toString());
      }
    }

    updateUserAction(taskExecutionUserMappingList);

    TaskPauseOrResumeRequest taskPauseOrResumeRequest = new TaskPauseOrResumeRequest(taskExecution.getJobId(), TaskPauseReason.TASK_COMPLETED, null);
    taskExecutionTimerService.saveTaskPauseTimer(taskPauseOrResumeRequest, taskExecution, principalUserEntity);

    stageReportService.incrementTaskCompleteCount(taskExecution.getJobId(), taskExecution.getTaskId());
    jobAuditService.completeTaskWithException(taskExecution.getJobId(), taskExecution.getTaskId(), taskCompletionRequest, taskExecution.getOrderTree(), principalUser);
    taskExecutionDto.setScheduledTaskExecutionIds(taskScheduledIds);

    UserAuditDto userAuditDto = userMapper.toUserAuditDto(principalUserEntity);
    jobLogService.recordJobLogTrigger(job.getIdAsString(), task.getIdAsString(), Type.JobLogTriggerType.TSK_ENDED_BY, task.getName(), null,
      Utility.getFullNameAndEmployeeIdFromPrincipalUser(principalUser), principalUser.getIdAsString(), userAuditDto);
    jobLogService.recordJobLogTrigger(job.getIdAsString(), task.getIdAsString(), Type.JobLogTriggerType.TSK_END_TIME, task.getName(), null, String.valueOf(
      taskExecution.getEndedAt()), String.valueOf(
      taskExecution.getEndedAt()), userAuditDto);

    TaskExecution expectedRecurredTaskExecution = validateAndCreateRecurringTask(task, taskExecution, job, taskCompletionRequest.isContinueRecurrence(), principalUserEntity);
    if (task.isEnableRecurrence()) {
      jobAuditService.recurrenceTask(taskExecution.getJobId(), taskExecution.getId(), expectedRecurredTaskExecution.getId(), task.getId(), taskCompletionRequest.isContinueRecurrence(), principalUser);
    }
    if (!Utility.isEmpty(taskScheduledIds)) {
      jobAuditService.scheduleTask(taskExecution.getJobId(), task.getId(), principalUser, true, false, taskScheduledIds);
    }

    notificationService.notifyIfAllPrerequisiteTasksCompleted(task.getId(), job.getId(), job.getOrganisationId());
    return taskExecutionDto;
  }

  @Override
  public BasicDto signOff(TaskSignOffRequest taskSignOffRequest) throws StreemException {
    log.info("[signOff] Request to sign off tasks,  taskSignOffRequest: {}", taskSignOffRequest);
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    List<Long> nonSignedOffTaskIds = taskExecutionRepository.findNonSignedOffTaskIdsByJobIdAndUserId(taskSignOffRequest.getJobId(), principalUser.getId());
    List<TaskExecution> taskExecutions = taskExecutionRepository.readByJobIdAndTaskIdIn(taskSignOffRequest.getJobId(), nonSignedOffTaskIds);

    if (!Utility.isEmpty(taskExecutions)) {
      validateTasksAndSignOff(taskExecutions);
    }

    jobAuditService.signedOffTasks(taskSignOffRequest, principalUser);
    BasicDto basicDto = new BasicDto();
    basicDto.setMessage("success");
    return basicDto;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public TaskExecutionDto enableCorrection(Long taskExecutionId, TaskExecutionRequest taskExecutionRequest) throws StreemException {
    log.info("[enableCorrection] Request to enable correction for task, taskExecutionId: {}, taskExecutionRequest: {}", taskExecutionId, taskExecutionRequest);
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    User principalUserEntity = userRepository.getReferenceById(principalUser.getId());

    if (!Utility.trimAndCheckIfEmpty(taskExecutionRequest.getReason())) {
      ValidationUtils.invalidate(taskExecutionId, ErrorCode.REASON_CANNOT_BE_EMPTY);
    }

    TaskExecution taskExecution = taskExecutionRepository.getReferenceById(taskExecutionId);
    validateIfUserIsAssignedToExecuteParameter(taskExecutionId, principalUser.getId());

    // We are allowing error correction if a repeated instance's task is not started
    validateIfAllTaskExecutionsAreInCompletedStateOrNotStarted(taskExecution.getTaskId(), taskExecution.getJobId());


    if (taskExecution.isCorrectionEnabled()) {
      ValidationUtils.invalidate(taskExecution.getId(), ErrorCode.TASK_ALREADY_ENABLED_FOR_CORRECTION);
    }

    Job job = taskExecution.getJob();
    if (State.JOB_COMPLETED_STATES.contains(job.getState())) {
      ValidationUtils.invalidate(job.getId(), ErrorCode.JOB_ALREADY_COMPLETED);
    }
    List<Long> parameterIds = parameterValueRepository.findExecutableParameterIdsByTaskId(taskExecution.getTaskId());
    List<ParameterValue> parameterValues = parameterValueRepository.findByTaskExecutionIdAndParameterIdIn(taskExecutionId, parameterIds);
    List<Long> parameterValueIds = parameterValues.stream().map(BaseEntity::getId).toList();
    List<ParameterVerification> parameterVerificationList = parameterVerificationRepository.findByJobIdAndParameterValueIdIn(taskExecution.getJobId(), parameterValueIds);


    //select all parameter values for this particular task
    List<TempParameterValue> tempParameterValuesBeforeMapping = tempParameterValueRepository.readByTaskExecutionIdAndParameterIdIn(taskExecution.getId(), parameterIds);

    List<TempParameterValue> tempParameterValues = new ArrayList<>();
    Map<Long, Set<Long>> tempParameterIdAndArchiveMediaIdsMap = new HashMap<>();
    if ((null != parameterValues && tempParameterValuesBeforeMapping != null) && parameterValues.size() != tempParameterValuesBeforeMapping.size()) {
      for (ParameterValue parameterValue : parameterValues) {
        TempParameterValue tempParameterValue = new TempParameterValue();
        tempParameterValue.setId(IdGenerator.getInstance().nextId());
        tempParameterValue.setJob(job);
        tempParameterValue.setChoices(parameterValue.getChoices());
        tempParameterValue.setValue(parameterValue.getValue());
        tempParameterValue.setParameter(parameterValue.getParameter());
        tempParameterValue.setState(parameterValue.getState());
        tempParameterValue.setTaskExecution(taskExecution);
        tempParameterValue.setCreatedBy(principalUserEntity);
        tempParameterValue.setCreatedAt(parameterValue.getCreatedAt());
        tempParameterValue.setModifiedBy(parameterValue.getModifiedBy());
        tempParameterValue.setModifiedAt(parameterValue.getModifiedAt());
        tempParameterValue.setHidden(parameterValue.isHidden());
        tempParameterValue.setReason(parameterValue.getReason());
        if (null != parameterValue.getMedias()) {
          Set<Long> archivedMediaIds = new HashSet<>();
          for (ParameterValueMediaMapping media : parameterValue.getMedias()) {
            tempParameterValue.addMedia(media.getMedia(), principalUserEntity);
            if (media.isArchived()) {
              archivedMediaIds.add(media.getMedia().getId());
            }
          }
          tempParameterIdAndArchiveMediaIdsMap.put(tempParameterValue.getId(), archivedMediaIds);
        }
        tempParameterValues.add(tempParameterValue);
      }
      tempParameterValues = tempParameterValueRepository.saveAll(tempParameterValues);
      tempParameterIdAndArchiveMediaIdsMap.forEach(tempParameterMediaMappingRepository::archiveMediaByTempParameterValueIdAndMediaIdIn);
    }

    // We are copying temp parameter verifications to handle the case when user executes a parameter with both self and peer
    // verification, completes the self verification and then completes the task with exception with  after requesting for peer
    // peer verification. After enabling error correction the state of the parameter should be same since the operator already has
    // done self verification

    if (!Utility.isEmpty(parameterVerificationList)) {
      Map<Long, TempParameterValue> tempParameterValueMap = tempParameterValues.stream().collect(Collectors.toMap(tempParameterValue -> tempParameterValue.getParameter().getId(), Function.identity()));
      List<TempParameterVerification> tempParameterVerificationList = new ArrayList<>();
      for (ParameterVerification parameterVerification : parameterVerificationList) {
        TempParameterVerification tempParameterVerification = new TempParameterVerification();
        tempParameterVerification.setId(parameterVerification.getId());
        tempParameterVerification.setJob(job);
        tempParameterVerification.setUser(parameterVerification.getUser());
        tempParameterVerification.setComments(parameterVerification.getComments());
        tempParameterVerification.setTempParameterValue(tempParameterValueMap.get(parameterVerification.getParameterValue().getParameter().getId()));
        tempParameterVerification.setCreatedBy(parameterVerification.getCreatedBy());
        tempParameterVerification.setCreatedAt(parameterVerification.getCreatedAt());
        tempParameterVerification.setModifiedBy(parameterVerification.getModifiedBy());
        tempParameterVerification.setModifiedAt(parameterVerification.getModifiedAt());
        tempParameterVerification.setVerificationStatus(parameterVerification.getVerificationStatus());
        tempParameterVerification.setVerificationType(parameterVerification.getVerificationType());
        tempParameterVerificationList.add(tempParameterVerification);
      }
      tempParameterVerificationRepository.saveAll(tempParameterVerificationList);
    }


    TaskExecutionDto taskExecutionDto = taskExecutionMapper.toDto(taskExecution);

//    parameterValueRepository.updateStateForParameters(taskExecution.getId(), State.ParameterExecution.ENABLED_FOR_CORRECTION.name(), parameterIds);

    taskExecutionRepository.enableCorrection(taskExecutionRequest.getCorrectionReason(), taskExecution.getId());
    taskExecutionDto.setCorrectionEnabled(true);

    taskExecutionDto.setCorrectionReason(taskExecutionRequest.getCorrectionReason());

    jobAuditService.enableTaskForCorrection(taskExecution.getJobId(), taskExecutionId, taskExecutionRequest, principalUser);
    return taskExecutionDto;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public TaskExecutionDto completeCorrection(Long taskExecutionId, TaskExecutionRequest taskExecutionRequest) throws ResourceNotFoundException, StreemException, IOException {
    log.info("[completeCorrection] Request to complete correction, taskExecutionId: {}, taskExecutionRequest: {}", taskExecutionId, taskExecutionRequest);
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    User principalUserEntity = userRepository.getReferenceById(principalUser.getId());
    TaskExecution taskExecution = taskExecutionRepository.getReferenceById(taskExecutionId);
    validateIfUserIsAssignedToExecuteParameter(taskExecutionId, principalUser.getId());

    if (!taskExecution.isCorrectionEnabled()) {
      ValidationUtils.invalidate(taskExecution.getId(), ErrorCode.TASK_NOT_ENABLED_FOR_CORRECTION);
    }
    Job job = taskExecution.getJob();
    if (State.JOB_COMPLETED_STATES.contains(job.getState())) {
      ValidationUtils.invalidate(job.getId(), ErrorCode.JOB_ALREADY_COMPLETED);
    }
    validateIncompleteVerificationParameters(taskExecution.getId(), true);
    validateTempIncompleteParameters(job.getId(), taskExecution.getId());


    List<Long> parameterIds = parameterValueRepository.findExecutableParameterIdsByTaskId(taskExecution.getTaskId());
    List<TempParameterValue> tempParameterValues = tempParameterValueRepository.readByTaskExecutionIdAndParameterIdIn(taskExecution.getId(), parameterIds);
    List<Long> tempParameterValueIds = tempParameterValues.stream().map(BaseEntity::getId).toList();

    List<TempParameterVerification> tempParameterVerifications = tempParameterVerificationRepository.findByJobIdAndTempParameterValueIdIn(
      taskExecution.getJobId(), tempParameterValueIds);

    Map<Long, Long> tempParameterValueIdAndParameterIdMap = tempParameterValues.stream().collect(Collectors.toMap(TempParameterValue::getId, TempParameterValue::getParameterId));


    List<ParameterValue> parameterValues = parameterValueRepository.findByTaskExecutionIdAndParameterIdIn(taskExecutionId, parameterIds);

    Map<Long, ParameterValue> parameterIdAndParameterValueMap = parameterValues.stream().collect(Collectors.toMap(ParameterValue::getParameterId, Function.identity()));

    Map<Long, ParameterValue> parameterValueMap = new HashMap<>();
    for (ParameterValue parameterValue : parameterValues) {
      parameterValueMap.put(parameterValue.getParameter().getId(), parameterValue);
    }
    Set<Long> hide = new HashSet<>();
    Set<Long> show = new HashSet<>();
    for (TempParameterValue tempParameterValue : tempParameterValues) {
      ParameterValue parameterValue;
      if (Type.PARAMETER_MEDIA_TYPES.contains(tempParameterValue.getParameter().getType())) {
        parameterValue = parameterValueMap.get(tempParameterValue.getParameter().getId());
        updateParameterMediasOnErrorCorrection(parameterValue, parameterValue.getMedias(), tempParameterValue, tempParameterValue.getMedias(), principalUserEntity);
      } else {
        String choices = tempParameterValue.getChoices() == null ? null : tempParameterValue.getChoices().toString();
        var modifiedBy = tempParameterValue.getModifiedBy();
        parameterValueRepository.updateParameterValues(taskExecution.getId(), tempParameterValue.getParameter().getId(), tempParameterValue.getState().name(),
          tempParameterValue.getValue(), choices, tempParameterValue.getReason(), modifiedBy == null ? null : modifiedBy.getId(), tempParameterValue.getModifiedAt());
        parameterValue = parameterValueRepository.findByParameterIdAndTaskExecutionId(tempParameterValue.getParameter().getId(), taskExecutionId)
          .orElseThrow(() -> new ResourceNotFoundException(tempParameterValue.getParameter().getId(), ErrorCode.PARAMETER_VALUE_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));
        JsonNode oldChoices = parameterValue.getChoices();
        // TODO Move to new service
//        RuleHideShowDto ruleHideShowDto = parameterExecutionService.updateRules(job.getId(), tempParameterValue.getParameter(), oldChoices);
//        hide.addAll(ruleHideShowDto.getHide());
//        show.addAll(ruleHideShowDto.getShow());
      }
      jobLogService.updateJobLog(job.getId(), parameterValue.getParameterId(), parameterValue.getParameter().getType(),
        taskExecutionRequest.getCorrectionReason(), parameterValue.getParameter().getLabel(),
        Type.JobLogTriggerType.PARAMETER_VALUE, userMapper.toUserAuditDto(principalUser));
    }

    List<ParameterVerification> parameterVerificationList = new ArrayList<>();

    if (!Utility.isEmpty(tempParameterVerifications)) {
      for (TempParameterVerification tempParameterVerification : tempParameterVerifications) {
        ParameterVerification parameterVerification = new ParameterVerification();
        parameterVerification.setId(tempParameterVerification.getId());
        parameterVerification.setJob(tempParameterVerification.getJob());
        parameterVerification.setCreatedBy(tempParameterVerification.getCreatedBy());
        parameterVerification.setCreatedAt(tempParameterVerification.getCreatedAt());
        parameterVerification.setModifiedBy(tempParameterVerification.getModifiedBy());
        parameterVerification.setModifiedAt(tempParameterVerification.getModifiedAt());

        parameterVerification.setParameterValue(
          parameterIdAndParameterValueMap.get(tempParameterValueIdAndParameterIdMap.get(tempParameterVerification.getTempParameterValueId()))
        );
        parameterVerification.setComments(tempParameterVerification.getComments());
        parameterVerification.setVerificationStatus(tempParameterVerification.getVerificationStatus());
        parameterVerification.setVerificationType(tempParameterVerification.getVerificationType());
        parameterVerification.setUser(tempParameterVerification.getUser());
        parameterVerificationList.add(parameterVerification);

        Parameter parameter = parameterVerification.getParameterValue().getParameter();

        Long modifiedAt = parameterVerification.getModifiedAt();
        User modifiedBy = parameterVerification.getModifiedBy();
        if (parameterVerification.getVerificationType() == Type.VerificationType.PEER) {
          jobLogService.recordJobLogTrigger(String.valueOf(tempParameterVerification.getJobId()), parameter.getIdAsString(), Type.JobLogTriggerType.PARAMETER_PEER_VERIFIED_BY, parameter.getLabel(), null, Utility.getFullNameAndEmployeeIdFromPrincipalUser(userMapper.toPrincipalUser(modifiedBy)), modifiedBy.getIdAsString(), userMapper.toUserAuditDto(modifiedBy));
          jobLogService.recordJobLogTrigger(String.valueOf(tempParameterVerification.getJobId()), parameter.getIdAsString(), Type.JobLogTriggerType.PARAMETER_PEER_VERIFIED_AT, parameter.getLabel(), null, String.valueOf(modifiedAt), String.valueOf(modifiedAt), userMapper.toUserAuditDto(modifiedBy));
        } else {
          jobLogService.recordJobLogTrigger(String.valueOf(tempParameterVerification.getJobId()), parameter.getIdAsString(), Type.JobLogTriggerType.PARAMETER_SELF_VERIFIED_BY, parameter.getLabel(), null, Utility.getFullNameAndEmployeeIdFromPrincipalUser(userMapper.toPrincipalUser(modifiedBy)), modifiedBy.getIdAsString(), userMapper.toUserAuditDto(modifiedBy));
          jobLogService.recordJobLogTrigger(String.valueOf(tempParameterVerification.getJobId()), parameter.getIdAsString(), Type.JobLogTriggerType.PARAMETER_SELF_VERIFIED_AT, parameter.getLabel(), null, String.valueOf(modifiedAt), String.valueOf(modifiedAt), userMapper.toUserAuditDto(modifiedBy));

        }
      }
    }
    parameterVerificationRepository.saveAll(parameterVerificationList);

    taskExecution.setCorrectedBy(principalUserEntity);
    taskExecution.setCorrectedAt(DateTimeUtils.now());
    taskExecution.setCorrectionEnabled(false);
    taskExecutionRepository.save(taskExecution);


    // Deleting all temp data from temp tables
    //TODO: Find why this is not working for media parameters
//    tempParameterMediaMappingRepository.deleteAllByTempParameterValueIdIn(tempParameterValueIds);
//    tempParameterVerificationRepository.deleteAllByTempParameterValueIdIn(tempParameterValueIds);
//    tempParameterValueRepository.deleteAllById(tempParameterValueIds);


    TaskExecutionDto taskExecutionDto = taskExecutionMapper.toDto(taskExecution);
    taskExecutionDto.setShow(show);
    taskExecutionDto.setHide(hide);
    taskExecutionDto.setCorrectionEnabled(false);

    String sql = "delete from temp_parameter_values where task_executions_id = " + taskExecutionId;
    jdbcTemplate.execute(sql);

    jobAuditService.completeCorrection(taskExecution.getJobId(), taskExecution.getId(), taskExecutionRequest, principalUser);
    return taskExecutionDto;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public TaskExecutionDto cancelCorrection(Long taskExecutionId, TaskExecutionRequest taskExecutionRequest) throws StreemException {
    log.info("[cancelCorrection] Request to cancel correction, taskExecutionId: {}, taskExecutionRequest: {}", taskExecutionId, taskExecutionRequest);
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    TaskExecution taskExecution = taskExecutionRepository.getReferenceById(taskExecutionId);
    validateIfUserIsAssignedToExecuteParameter(taskExecutionId, principalUser.getId());

    if (!taskExecution.isCorrectionEnabled()) {
      ValidationUtils.invalidate(taskExecution.getId(), ErrorCode.TASK_NOT_ENABLED_FOR_CORRECTION);
    }
    Job job = taskExecution.getJob();
    if (State.JOB_COMPLETED_STATES.contains(job.getState())) {
      ValidationUtils.invalidate(job.getId(), ErrorCode.JOB_ALREADY_COMPLETED);
    }

    // Deleting all temp data from temp tables
    TaskExecutionDto taskExecutionDto = taskExecutionMapper.toDto(taskExecution);
    taskExecutionDto.setCorrectionReason(Utility.isEmpty(taskExecution.getCorrectionReason()) ? null : taskExecution.getCorrectionReason());
    taskExecutionRepository.cancelCorrection(taskExecution.getId());
    taskExecutionDto.setCorrectionEnabled(false);

    jobAuditService.cancelCorrection(taskExecution.getJobId(), taskExecution.getId(), taskExecutionRequest, principalUser);
    String sql = "delete from temp_parameter_values where task_executions_id = " + taskExecutionId;
    jdbcTemplate.execute(sql);

    return taskExecutionDto;
  }

  @Override
  public List<TaskExecutionAssigneeView> getTaskExecutionAssignees(Set<Long> taskExecutionIds, boolean users, boolean userGroups) {
    log.info("[getTaskExecutionAssignees] Request to fetch task execution assignees, taskExecutionIds: {}", taskExecutionIds);
    return taskExecutionAssigneeRepository.findByTaskExecutionIdIn(taskExecutionIds, taskExecutionIds.size(), users, userGroups);
  }

  @Override
  public TaskExecution getTaskExecutionByJobAndTaskId(Long taskExecutionId) {
    log.info("[getTaskExecutionByJobAndTaskId] Request to fetch task execution, taskExecutionId: {}", taskExecutionId);
    return taskExecutionRepository.getReferenceById(taskExecutionId);
  }

  @Override
  /**
   * Reason for findByTaskExecutionAndUser to return a list is a task can be assigned to an user 'U1' and to and User group "UG1" containing U1
   */
  public List<TaskExecutionUserMapping> validateAndGetAssignedUser(Long taskId, TaskExecution taskExecution, User user) throws ResourceNotFoundException {
    log.info("[validateAndGetAssignedUser] Request to validate and getting assigned user,  taskId: {}, taskExecution: {}, user: {}", taskId, taskExecution, user);
    return taskExecutionAssigneeRepository.findByTaskExecutionAndUser(taskExecution.getId(), user.getId())
      .orElseThrow(() -> new ResourceNotFoundException(taskId, ErrorCode.USER_NOT_ASSIGNED_TO_EXECUTE_TASK, ExceptionType.ENTITY_NOT_FOUND));
  }

  @Override
  //TODO: use list of taskExecutionIds instead of taskExecutionId
  public void updateUserAction(List<TaskExecutionUserMapping> taskExecutionUserMappingList) {
    log.info("[updateUserAction] Request to update user action for taskExecutionUserMappingList: {}", taskExecutionUserMappingList);
    for (TaskExecutionUserMapping taskExecutionUserMapping : taskExecutionUserMappingList) {
      if (!taskExecutionUserMapping.isActionPerformed()) {
        taskExecutionUserMapping.setActionPerformed(true);
        taskExecutionAssigneeRepository.save(taskExecutionUserMapping);
      }
    }
  }

  @Override
  public boolean isInvalidTimedTaskCompletedState(Task task, Long duration, Long endedAt) {
    log.info("[isInvalidTimedTaskCompletedState] Request to check if task is invalid timed task completed state, task: {}, duration: {}, endedAt: {}", task, duration, endedAt);
    if (duration == null || endedAt == null || !task.isTimed()) {
      return false;
    }
    long totalTime = duration;
    Operator.Timer timerOperator = Operator.Timer.valueOf(task.getTimerOperator());

    return (Operator.Timer.NOT_LESS_THAN.equals(timerOperator)
      && (task.getMinPeriod() > totalTime
      || (!Utility.isEmpty(task.getMaxPeriod()) && task.getMaxPeriod() != 0 && task.getMaxPeriod() < totalTime)))
      || Operator.Timer.LESS_THAN.equals(timerOperator) && task.getMaxPeriod() < totalTime;
  }

  private void validateTasksAndSignOff(List<TaskExecution> taskExecutions) throws StreemException {
    log.info("[validateTasksAndSignOff] Request to validate tasks and sign off, taskExecutions: {}", taskExecutions);
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    Set<Long> taskExecutionIds = new HashSet<>();
    Job job = taskExecutions.get(0).getJob();
    validateJobState(job.getId(), Action.Task.SIGN_OFF, job.getState());

    List<Error> errorList = new ArrayList<>();
    for (TaskExecution taskExecution : taskExecutions) {
      if (!State.TASK_COMPLETED_STATES.contains(taskExecution.getState()) || taskExecution.isCorrectionEnabled()) {
        ValidationUtils.addError(taskExecution.getTask().getId(), errorList, ErrorCode.TASK_INCOMPLETE);
      }
      taskExecutionIds.add(taskExecution.getId());
    }

    if (!Utility.isEmpty(errorList)) {
      ValidationUtils.invalidate(ErrorMessage.TASKS_INCOMPLETE, errorList);
    }

    if (!taskExecutionIds.isEmpty()) {
      taskExecutionAssigneeRepository.updateAssigneeState(State.TaskExecutionAssignee.SIGNED_OFF.name(), principalUser.getId()
        , taskExecutionIds, principalUser.getId(), DateTimeUtils.now());
    }
  }

  private void updateParameterMediasOnErrorCorrection(ParameterValue parameterValue, List<ParameterValueMediaMapping> parameterValueMediaMappings, TempParameterValue tempParameterValue,
                                                      List<TempParameterValueMediaMapping> tempParameterValueMediaMappings, User principalUserEntity) {
    log.info("[updateParameterMediasOnErrorCorrection] Request to update parameter medias on error correction, parameterValue: {}, parameterValueMediaMappings: {}, tempParameterValue: {}, tempParameterValueMediaMappings: {}, principalUserEntity: {}", parameterValue, parameterValueMediaMappings, tempParameterValue, tempParameterValueMediaMappings, principalUserEntity);
    Map<Long, Pair<ParameterValueMediaMapping, Media>> mediaMap = new HashMap<>();
    for (ParameterValueMediaMapping parameterValueMediaMapping : parameterValueMediaMappings) {
      Pair<ParameterValueMediaMapping, Media> mediaPair = Pair.of(parameterValueMediaMapping, parameterValueMediaMapping.getMedia());
      mediaMap.put(parameterValueMediaMapping.getMedia().getId(), mediaPair);
    }

    for (TempParameterValueMediaMapping parameterValueMedia : tempParameterValueMediaMappings) {
      if (mediaMap.containsKey(parameterValueMedia.getMedia().getId())) {
        Pair<ParameterValueMediaMapping, Media> mediaPair = mediaMap.get(parameterValueMedia.getMedia().getId());
        mediaPair.getFirst().setArchived(parameterValueMedia.isArchived());
      } else {
        parameterValue.addMedia(parameterValueMedia.getMedia(), principalUserEntity);
        parameterValue.setModifiedBy(principalUserEntity);
      }
    }
    parameterValue.setState(tempParameterValue.getState());
    parameterValueRepository.save(parameterValue);
  }

  private List<Error> createIncompleteParameterErrorList(List<Long> parameterIds) {
    List<Error> errorList = new ArrayList<>();
    for (Long id : parameterIds) {
      ValidationUtils.addError(id, errorList, ErrorCode.PARAMETER_INCOMPLETE);
    }
    return errorList;
  }

  private List<Error> createPendingApprovalParameterErrorList(List<Long> parameterIds) {
    List<Error> errorList = new ArrayList<>();
    for (Long id : parameterIds) {
      ValidationUtils.addError(id, errorList, ErrorCode.PARAMETER_PENDING_FOR_APPROVAL);
    }
    return errorList;
  }

  private List<Error> createIncompleteVerificationParameterErrorList(List<Long> parameterIds) {
    List<Error> errorList = new ArrayList<>();
    for (Long id : parameterIds) {
      ValidationUtils.addError(id, errorList, ErrorCode.PARAMETER_VERIFICATION_INCOMPLETE);
    }
    return errorList;
  }

  private void validateIncompleteParameters(Long jobId, Long taskExecutionId) throws StreemException {
    List<Long> incompleteMandatoryParameterValueIds = parameterValueRepository.findIncompleteMandatoryParameterValueIdsByJobIdAndTaskExecutionId(jobId, taskExecutionId);
    List<Long> incompleteMandatoryParameterShouldBePendingForApprovalValueIds = parameterValueRepository.findIncompleteMandatoryParameterShouldBePendingForApprovalValueIdsByJobIdAndTaskExecutionId(jobId, taskExecutionId);

    if (!Utility.isEmpty(incompleteMandatoryParameterShouldBePendingForApprovalValueIds)) {
      throw new StreemException(ErrorMessage.PARAMETERS_PENDING_FOR_APPROVAL, createPendingApprovalParameterErrorList(incompleteMandatoryParameterShouldBePendingForApprovalValueIds));
    }
    if (!Utility.isEmpty(incompleteMandatoryParameterValueIds)) {
      throw new StreemException(ErrorMessage.MANDATORY_PARAMETERS_NOT_COMPLETED, createIncompleteParameterErrorList(incompleteMandatoryParameterValueIds));
    }
  }

  private void validateParameterState(Long taskExecutionId) throws StreemException {
    List<Long> pendingForApprovalParameterValueIds = parameterValueRepository.findPendingApprovalParameterValueIdsByJobIdAndTaskExecutionId(taskExecutionId);

    if (!Utility.isEmpty(pendingForApprovalParameterValueIds)) {
      throw new StreemException(ErrorMessage.PARAMETERS_PENDING_FOR_APPROVAL, createPendingApprovalParameterErrorList(pendingForApprovalParameterValueIds));
    }
  }

  private void validateTempIncompleteParameters(Long jobId, Long taskExecutionId) throws StreemException {
    List<Long> incompleteMandatoryParameterIds = tempParameterValueRepository.findTempIncompleteMandatoryParameterIdsByJobIdAndTaskExecutionId(jobId, taskExecutionId);

    if (!Utility.isEmpty(incompleteMandatoryParameterIds)) {
      throw new StreemException(ErrorMessage.MANDATORY_PARAMETERS_NOT_COMPLETED, createIncompleteParameterErrorList(incompleteMandatoryParameterIds));
    }
  }

  private void validateIncompleteVerificationParameters(Long taskExecutionId, boolean isEnabledForCorrection) throws StreemException {
    List<Long> incompleteVerificationParameterIds;
    if (isEnabledForCorrection) {
      incompleteVerificationParameterIds = tempParameterValueRepository.findVerificationIncompleteParameterExecutionIdsByTaskExecutionId(taskExecutionId);
    } else {
      incompleteVerificationParameterIds = parameterValueRepository.findVerificationIncompleteParameterExecutionIdsByTaskExecutionId(taskExecutionId);
    }
    if (!Utility.isEmpty(incompleteVerificationParameterIds)) {
      throw new StreemException(ErrorMessage.PENDING_VERIFICATION_PARAMETERS, createIncompleteVerificationParameterErrorList(incompleteVerificationParameterIds));
    }
  }

  private void validateTimer(Long taskId, Operator.Timer timerOperator, Long minPeriod, Long maxPeriod, Long totalPeriod, String reason) throws StreemException {
    if ((timerOperator.equals(Operator.Timer.NOT_LESS_THAN) && (minPeriod > totalPeriod || maxPeriod < totalPeriod))
      || (timerOperator.equals(Operator.Timer.LESS_THAN) && maxPeriod < totalPeriod)) {
      ValidationUtils.validateNotEmpty(reason, taskId, ErrorCode.TIMED_TASK_REASON_CANNOT_BE_EMPTY);
    }
  }

  private void validateJobState(Long jobId, Action.Task taskAction, State.Job jobState) throws StreemException {

    if (State.Job.BLOCKED.equals(jobState)) {
      ValidationUtils.invalidate(jobId, ErrorCode.JOB_IS_BLOCKED);
    }

    switch (taskAction) {
      case START:
        if (!State.Job.IN_PROGRESS.equals(jobState)) {
          ValidationUtils.invalidate(jobId, ErrorCode.JOB_IS_NOT_IN_PROGRESS);
        }
        break;
      case ASSIGN:
      case COMPLETE_WITH_EXCEPTION:
      case COMPLETE:
      case SIGN_OFF:
      case SKIP:
      case REPEAT:
        if (State.JOB_COMPLETED_STATES.contains(jobState)) {
          ValidationUtils.invalidate(jobId, ErrorCode.JOB_ALREADY_COMPLETED);
        }
        break;
    }
  }


  //TODO state management ?
  private void validateTaskState(Long id, Action.Task taskAction, State.TaskExecution state) throws StreemException {
    switch (taskAction) {
      case ASSIGN, COMPLETE_WITH_EXCEPTION:
        if (State.TASK_COMPLETED_STATES.contains(state)) {
          ValidationUtils.invalidate(id, ErrorCode.TASK_ALREADY_COMPLETED);
        }
        break;
      case START:
        if (State.TASK_COMPLETED_STATES.contains(state)) {
          ValidationUtils.invalidate(id, ErrorCode.TASK_ALREADY_COMPLETED);
        }
        if (state == State.TaskExecution.IN_PROGRESS || state == State.TaskExecution.PAUSED)
          ValidationUtils.invalidate(id, ErrorCode.TASK_ALREADY_IN_PROGRESS);

        break;
      case COMPLETE:
        if (!State.TaskExecution.IN_PROGRESS.equals(state)) {
          ValidationUtils.invalidate(id, ErrorCode.TASK_NOT_IN_PROGRESS);
        }
        if (state == State.TaskExecution.PAUSED) {
          ValidationUtils.invalidate(id, ErrorCode.TASK_IS_IN_PAUSED_STATE);
        }
        break;
      case SKIP:
        if (state != State.TaskExecution.NOT_STARTED) {
          ValidationUtils.invalidate(id, ErrorCode.TASK_CANNOT_BE_SKIPPED);
        }
        break;
    }
  }

  /**
   * @param taskExecutionId          id of the task to pause
   * @param taskPauseOrResumeRequest contains jobId, reason and comment for the pause request
   * @return
   * @throws StreemException
   */
  @Override
  public TaskExecutionDto pauseTask(Long taskExecutionId, TaskPauseOrResumeRequest taskPauseOrResumeRequest) throws StreemException {
    log.info("[pauseTask] Request to pause task: {} and TaskPauseOrResumeRequest :{} ", taskExecutionId, taskPauseOrResumeRequest);
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    TaskExecution taskExecution = taskExecutionRepository.getReferenceById(taskExecutionId);
    if (taskExecution == null) {
      ValidationUtils.invalidate(taskExecutionId, ErrorCode.TASK_NOT_FOUND);
    }
    User principalUserEntity = userRepository.getReferenceById(principalUser.getId());
    List<TaskPauseReasonOrComment> pausedReason = new ArrayList<>();

    if (taskExecution.getTask().isHasExecutorLock()) {
      validateIfCurrentExecutorIsAllowedToInteractTask(taskExecution.getTaskId(), taskExecution.getJobId(), principalUser.getId());
    }
    validateTaskLock(taskExecution, principalUser, taskExecutionId);

    if (taskExecution.getState() == State.TaskExecution.IN_PROGRESS) {

      taskExecution.setState(State.TaskExecution.PAUSED);
      taskExecutionTimerService.saveTaskPauseTimer(taskPauseOrResumeRequest, taskExecution, principalUserEntity);
      pausedReason = taskExecutionTimerService.calculateDurationAndReturnReasonsOrComments(List.of(taskExecution)).get(taskExecution.getId());
      taskExecution.setModifiedAt(DateTimeUtils.now());
      taskExecution.setModifiedBy(principalUserEntity);
      taskExecutionRepository.save(taskExecution);

    } else {
      ValidationUtils.invalidate(taskExecutionId, ErrorCode.TASK_IS_IN_NON_RESUMABLE_STATE);
    }
    jobAuditService.pauseTask(taskExecution.getTaskId(), taskPauseOrResumeRequest, principalUser);
    return taskExecutionMapper.toDto(taskExecution, pausedReason);
  }

  /**
   * @param taskExecutionId          id of the task to pause
   * @param taskPauseOrResumeRequest contains jobId, reason and comment for the pause request
   * @return
   * @throws StreemException
   */
  @Override
  public TaskExecutionDto resumeTask(Long taskExecutionId, TaskPauseOrResumeRequest taskPauseOrResumeRequest) throws StreemException {
    log.info("[resumeTask] Request to resume task: {} and TaskPauseOrResumeRequest :{} ", taskExecutionId, taskPauseOrResumeRequest);
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    User principalUserEntity = userRepository.getReferenceById(principalUser.getId());
    TaskExecution taskExecution = taskExecutionRepository.getReferenceById(taskExecutionId);
    List<TaskPauseReasonOrComment> pausedReason = new ArrayList<>();

    if (taskExecution.getTask().isHasExecutorLock()) {
      validateIfCurrentExecutorIsAllowedToInteractTask(taskExecution.getTaskId(), taskExecution.getJobId(), principalUser.getId());
    }
    validateTaskLock(taskExecution, principalUser, taskExecutionId);

    long now = DateTimeUtils.now();
    if (taskExecution.getState() == State.TaskExecution.PAUSED) {

      TaskExecutionTimer toBeResumedTimer = taskExecutionTimerRepository.findPausedTimerByTaskExecutionIdAndJobId(taskExecution.getId());
      toBeResumedTimer.setResumedAt(now);
      toBeResumedTimer.setModifiedBy(principalUserEntity);
      toBeResumedTimer.setModifiedAt(now);
      taskExecutionTimerRepository.save(toBeResumedTimer);

      pausedReason = taskExecutionTimerService.calculateDurationAndReturnReasonsOrComments(List.of(taskExecution)).get(taskExecution.getId());
      taskExecution.setState(State.TaskExecution.IN_PROGRESS);
      taskExecution.setModifiedAt(now);
      taskExecution.setModifiedBy(principalUserEntity);
      taskExecutionRepository.save(taskExecution);

    } else {
      ValidationUtils.invalidate(taskExecution.getTaskId(), ErrorCode.TASK_IS_IN_PAUSED_STATE);
    }
    jobAuditService.resumeTask(taskExecution.getTaskId(), taskPauseOrResumeRequest, principalUser);
    return taskExecutionMapper.toDto(taskExecution, pausedReason);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public BasicDto removeTaskExecution(Long taskExecutionId) throws StreemException, ResourceNotFoundException {
    log.info("[removeTask] Request to remove task with task execution id: {} ", taskExecutionId);
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    TaskExecution taskExecution = taskExecutionRepository.findById(taskExecutionId)
      .orElseThrow(() -> new ResourceNotFoundException(taskExecutionId, ErrorCode.TASK_EXECUTION_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));

    if (Type.TaskExecutionType.MASTER.equals(taskExecution.getType())) {
      ValidationUtils.invalidate(taskExecutionId, ErrorCode.MASTER_TASK_CANNOT_BE_DELETED);
    }

    if (taskExecution.getState() == State.TaskExecution.NOT_STARTED) {
      taskExecutionRepository.deleteByTaskExecutionId(taskExecutionId);
    } else {
      ValidationUtils.invalidate(taskExecutionId, ErrorCode.TASK_EXECUTION_ALREADY_IN_PROGRESS_OR_COMPLETED);
    }
    jobAuditService.removeTask(taskExecution.getTaskId(), taskExecution.getJobId(), taskExecution.getOrderTree(), principalUser);
    return new BasicDto(taskExecutionId.toString(), "success", null);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public BasicDto stopTaskExecutionRecurring(Long taskExecutionId) throws ResourceNotFoundException {
    log.info("[stopTaskExecutionRecurring] Request to stop task recurrence at taskExecution with Id: {} ", taskExecutionId);
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    TaskExecution taskExecution = taskExecutionRepository.findById(taskExecutionId)
      .orElseThrow(() -> new ResourceNotFoundException(taskExecutionId, ErrorCode.TASK_EXECUTION_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));

    taskExecutionRepository.setAllTaskExecutionsContinueRecurrenceFalse(taskExecution.getTaskId(), taskExecution.getJobId());

    TaskCompletionRequest taskCompletionRequest = new TaskCompletionRequest();
    taskCompletionRequest.setContinueRecurrence(false);

    jobAuditService.recurrenceTask(taskExecution.getJobId(), taskExecution.getId(), null, taskExecution.getTask().getId(), taskCompletionRequest.isContinueRecurrence(), principalUser);
    return new BasicDto(taskExecutionId.toString(), "success", null);
  }


  //TODO Copy of create job service, use this function in create job service expose via interface make it public
  private TaskExecution createTaskExecution(Job job, Task task, Integer orderTree, Type.TaskExecutionType type, User principalUserEntity) {
    TaskExecution taskExecution = new TaskExecution();
    taskExecution.setId(IdGenerator.getInstance().nextId());
    taskExecution.setJob(job);
    taskExecution.setOrderTree(orderTree);
    taskExecution.setType(type);
    taskExecution.setTask(task);
    taskExecution.setCreatedBy(principalUserEntity);
    taskExecution.setModifiedBy(principalUserEntity);
    taskExecution.setState(State.TaskExecution.NOT_STARTED);
    return taskExecution;
  }

  // TODO same goes for below two methods, can be moved to parameter execution service
  private List<ParameterValue> createParameterValues(Job job, TaskExecution taskExecution, Task task, User principalUserEntity, Map<Long, ParameterValue> previousParameterIdAndParameterValueMap) throws IOException {
    List<ParameterValue> parameterValues = new ArrayList<>();
    for (Parameter parameter : task.getParameters()) {
      ParameterValue parameterValue = createParameterValue(job, taskExecution, parameter, principalUserEntity, previousParameterIdAndParameterValueMap);
      parameterValues.add(parameterValue);
      job.addParameterValue(parameterValue);
    }
    return parameterValues;
  }

  private void validateIfPrecedingTasksAreCompleted(Task task, Long jobId) throws StreemException {
    Stage stage = task.getStage();
    List<TaskExecutionView> taskExecutionsViewListBeforeStage = taskExecutionRepository.findAllNonCompletedTaskExecutionBeforeCurrentStageAndHasStop(stage.getOrderTree(), jobId);
    List<TaskExecutionView> taskExecutionViewListOfCurrentStage = taskExecutionRepository.findAllNonCompletedTaskExecutionOfCurrentStageAndHasStop(stage.getId(), task.getOrderTree(), jobId);
    taskExecutionViewListOfCurrentStage.addAll(taskExecutionsViewListBeforeStage);

    List<Error> errorList = new ArrayList<>();
    for (TaskExecutionView taskExecutionView : taskExecutionViewListOfCurrentStage) {
      ValidationUtils.addError(taskExecutionView.getId(), errorList, ErrorCode.PRECEDING_TASKS_ARE_NOT_COMPLETED);
    }
    if (!Utility.isEmpty(errorList)) {
      ValidationUtils.invalidate(task.getIdAsString(), errorList);
    }
  }

  private ParameterValue createParameterValue(Job job, TaskExecution taskExecution, Parameter parameter, User principalUserEntity, Map<Long, ParameterValue> parameterValueMap) throws IOException {
    ParameterValue previousParameterValue = parameterValueMap.get(parameter.getId());
    ParameterValue parameterValue = new ParameterValue();
    parameterValue.setJob(job);
    parameterValue.setTaskExecution(taskExecution);
    parameterValue.setParameter(parameter);
    parameterValue.setHidden(parameter.isHidden());
    parameterValue.setHasVariations(previousParameterValue.isHasVariations());
    parameterValue.setState(State.ParameterExecution.NOT_STARTED);
    parameterValue.setCreatedBy(principalUserEntity);


    return parameterValue;
  }

  private void applyRules(Parameter parameter, Job job) throws IOException {
    log.info("[applyRules] Request to apply rules for parameter: {} and job: {}", parameter, job);
    Set<Long> parameterRuleMapping = parameterRuleMappingRepository.findAllByImpactedParameterId(parameter.getId());

    List<Parameter> triggeringParameters = parameterRepository.findAllById(parameterRuleMapping);

    for (Parameter triggeringParameter : triggeringParameters) {
      ParameterValue triggeringParameterValue = parameterValueRepository.findLatestByJobIdAndParameterId(job.getId(), triggeringParameter.getId());
      rulesExecutionService.updateRules(job.getId(), triggeringParameter, triggeringParameterValue);
    }

  }

  private void validateIfAllTaskExecutionsAreInCompletedState(Long taskId, Long jobId) throws StreemException {
    List<TaskExecutionView> taskExecutionViews = taskExecutionRepository.findAllTaskExecutionsNotInCompletedStateByTaskIdAndJobId(taskId, jobId);
    if (!Utility.isEmpty(taskExecutionViews)) {
      List<Error> errorList = new ArrayList<>();
      for (TaskExecutionView taskExecutionView : taskExecutionViews) {
        errorList.add(Error.builder()
          .id(taskExecutionView.getId().toString())
          .code(ErrorCode.TASK_EXECUTION_NOT_IN_COMPLETED_STATE.getCode())
          .message(ErrorCode.TASK_EXECUTION_NOT_IN_COMPLETED_STATE.getDescription())
          .build());
      }
      ValidationUtils.invalidate(ErrorMessage.TASK_EXECUTION_NOT_IN_COMPLETED_STATE, errorList);
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

  private void validateIfAnyTaskExecutionContainsStopRecurrence(Long taskId, Long jobId) throws StreemException {
    boolean isRecurrenceStopped = taskExecutionRepository.checkIfAnyTaskExecutionContainsStopRecurrence(taskId, jobId);
    if (isRecurrenceStopped) {
      ValidationUtils.invalidate(taskId, ErrorCode.TASK_EXECUTION_CONTAINS_STOP_RECURRENCE);
    }
  }

  /**
   * This function checks if subsequent tasks are not started. It's for add stop functionality
   *
   * @param task
   * @throws StreemException
   */
  private void validateIfSubsequentTasksAreNotStarted(Task task, TaskExecution taskExecution) throws StreemException {
    if (task.isHasStop()) {
      Stage stage = task.getStage();

      Integer stageOrderTree = stage.getOrderTree();
      List<TaskExecutionView> taskExecutionsView = taskExecutionRepository.findAllStartedTaskExecutionsAfterStageOrderTree(stageOrderTree, taskExecution.getJobId());
      List<TaskExecutionView> taskExecutionForStage = taskExecutionRepository.findAllStartedTaskExecutionsOfStage(taskExecution.getJobId(), task.getOrderTree(), stage.getId());

      taskExecutionsView.addAll(taskExecutionForStage);
      if (!Utility.isEmpty(taskExecutionsView)) {
        List<Error> errorList = new ArrayList<>();
        for (TaskExecutionView taskExecutionView : taskExecutionsView) {
          ValidationUtils.addError(taskExecutionView.getId(), errorList, ErrorCode.SUBSEQUENT_TASKS_ARE_IN_PROGRESS_OR_COMPLETED);
        }
        ValidationUtils.invalidate(task.getIdAsString(), errorList);
      }
    }
  }

  private void validateIfAllParametersOfTasksAreNotHidden(Long taskExecutionId) throws StreemException {
    long totalParametersCountForTaskExecution = parameterValueRepository.countAllByTaskExecutionId(taskExecutionId);
    long totalHiddenParametersCountForTaskExecution = parameterValueRepository.countAllByTaskExecutionIdWithHidden(taskExecutionId, true);

    if (totalParametersCountForTaskExecution == totalHiddenParametersCountForTaskExecution) {
      ValidationUtils.invalidate(taskExecutionId, ErrorCode.ALL_PARAMETERS_ARE_HIDDEN_FOR_TASK_EXECUTION);

    }

  }


  private void verifyDataIntegrity(Long taskExecutionId, TaskCompletionRequest taskCompletionRequest) throws StreemException, IOException {
    log.info("[verifyDataIntegrity] Request to verify data integrity for taskExecutionId: {} and taskCompletionRequest: {}", taskExecutionId, taskCompletionRequest);
    var parameterRequestList = taskCompletionRequest.getParameters();
    List<ParameterValueView> parameterValueViewList = parameterValueRepository.findAllExecutionDataByTaskExecutionId(taskExecutionId);
    Map<Long, ParameterValueView> parameterValueViewMap = parameterValueViewList.stream()
      .collect(Collectors.toMap(ParameterValueView::getId, Function.identity()));

    Map<Long, List<ParameterValueMediaView>> parameterValueMediaMap = parameterValueRepository.findAllMediaDetailsByTaskExecutionId(taskExecutionId)
      .stream()
      .collect(Collectors.groupingBy(ParameterValueMediaView::getParameterValueId));


    Map<Long, Long> parameterIdParameterValueIdMap = parameterValueViewList.stream()
      .collect(Collectors.toMap(ParameterValueView::getParameterId, ParameterValueView::getId));

    List<Error> errorList = new ArrayList<>();
    for (ParameterCompletionRequest parameterRequest : parameterRequestList) {
      ParameterValueView parameterValueView = parameterValueViewMap.get(parameterIdParameterValueIdMap.get(parameterRequest.getId()));
      List<ParameterValueMediaView> parameterValueMediaViewList = parameterValueMediaMap.getOrDefault(parameterValueView.getId(), new ArrayList<>());
      String requestData = Utility.isEmpty(parameterRequest.getData()) ? null : parameterRequest.getData().toString();
      verifyDataIntegrityOfParameter(parameterValueView, parameterValueMediaViewList, requestData, errorList);
      verifyDataIntegrityForReason(parameterValueView.getId(), parameterRequest.getReason(), parameterValueView.getReason(), errorList);
    }
    if (!Utility.isEmpty(errorList)) {
      ValidationUtils.invalidate(ErrorMessage.DATA_INCONSISTENCY_ERROR, errorList);
    }
  }


  private void verifyDataIntegrityForReason(Long parameterValueId, String requestReason, String responseReason, List<Error> errorList) {
    if (!Utility.nullSafeEquals(requestReason, responseReason)) {
      ValidationUtils.addError(parameterValueId, errorList, ErrorCode.PARAMETER_DATA_INCONSISTENCY);
    }
  }

  private void verifyDataIntegrityOfParameter(ParameterValueView parameterValueView, List<ParameterValueMediaView> parameterValueMediaViewList, String requestData, List<Error> errorList) throws IOException {
    var parameterType = parameterValueView.getType();
    // We don't need Calculation parameter since it's system calculated (syncing issues can happen in polling only)
    switch (parameterType) {
      case YES_NO, MULTISELECT, SINGLE_SELECT, CHECKLIST ->
        verifyDataIntegrityOfChoiceParameter(parameterValueView, requestData, errorList);
      case MULTI_LINE, SHOULD_BE, DATE, DATE_TIME, NUMBER, SINGLE_LINE ->
        verifyDataIntegrityOfValueParameter(parameterValueView, requestData, errorList);
      case RESOURCE, MULTI_RESOURCE ->
        verifyDataIntegrityOfResourceParameter(parameterValueView, requestData, errorList);
      case MEDIA, FILE_UPLOAD, SIGNATURE ->
        verifyDataIntegrityOfMediaParameter(parameterValueView, parameterValueMediaViewList, requestData, errorList);

    }
  }

  private void verifyDataIntegrityOfMediaParameter(ParameterValueView parameterValueView, List<ParameterValueMediaView> parameterValueMediaViewList, String requestData, List<Error> errorList) throws IOException {
    MediaReconciliationRequest mediaReconciliationRequest = (MediaReconciliationRequest) JsonUtils.readValue(requestData, MediaReconciliationRequest.class);

    Set<Long> requestedMediaIds = mediaReconciliationRequest.getMedias()
      .stream()
      .map(MediaDto::getId)
      .map(Long::parseLong)
      .collect(Collectors.toSet());

    Set<Long> executedMediaIds = parameterValueMediaViewList.stream()
      .map(ParameterValueMediaView::getMediaId)
      .collect(Collectors.toSet());

    if (!executedMediaIds.containsAll(requestedMediaIds)) {
      ValidationUtils.addError(parameterValueView.getId(), errorList, ErrorCode.PARAMETER_DATA_INCONSISTENCY);
    }
  }

  private void verifyDataIntegrityOfResourceParameter(ParameterValueView parameterValueView, String requestData, List<Error> errorList) throws IOException {
    List<ResourceParameterChoiceDto> choices = new ArrayList<>();
    List<ResourceParameterChoiceDto> requestedChoices = new ArrayList<>();

    String choicesJson = parameterValueView.getChoices();
    if (choicesJson != null) {
      choices = JsonUtils.jsonToCollectionType(choicesJson, List.class, ResourceParameterChoiceDto.class);
    }
    if (requestData != null) {
      requestedChoices = JsonUtils.jsonToCollectionType(requestData, List.class, ResourceParameterChoiceDto.class);
    }

    Set<String> choicesObjectIds = choices.stream().map(ResourceParameterChoiceDto::getObjectId).collect(Collectors.toSet());
    Set<String> requestedChoicesObjectIds = requestedChoices.stream().map(ResourceParameterChoiceDto::getObjectId).collect(Collectors.toSet());

    if (!choicesObjectIds.containsAll(requestedChoicesObjectIds)) {
      ValidationUtils.addError(parameterValueView.getId(), errorList, ErrorCode.PARAMETER_DATA_INCONSISTENCY);
    }

  }

  private void verifyDataIntegrityOfChoiceParameter(ParameterValueView parameterValue, String requestData, List<Error> errorList) throws IOException {
    List<ChoiceParameterBase> choiceParameterList = JsonUtils.jsonToCollectionType(requestData, List.class, ChoiceParameterBase.class);
    var choices = parameterValue.getChoices();
    if (choices == null) {
      return;
    }
    Map<String, String> responseData = JsonUtils.convertValue(JsonUtils.valueToNode(choices), new TypeReference<>() {
    });

    for (var choiceParameter : choiceParameterList) {
      String state = responseData.get(choiceParameter.getId());
      if (!Utility.isEmpty(choiceParameter) && !Utility.isEmpty(state) && !choiceParameter.getState().equals(state)) {
        ValidationUtils.addError(parameterValue.getId(), errorList, ErrorCode.PARAMETER_DATA_INCONSISTENCY);
      }
    }
  }

  private void verifyDataIntegrityOfValueParameter(ParameterValueView parameterValue, String requestData, List<Error> errorList) throws JsonProcessingException {
    var parameterRequest = JsonUtils.readValue(requestData, ValueParameterBase.class);
    var requestValue = parameterRequest.getInput();
    var responseValue = parameterValue.getValue();
    if (!Utility.nullSafeEquals(requestValue, responseValue)) {
      ValidationUtils.addError(parameterValue.getId(), errorList, ErrorCode.PARAMETER_DATA_INCONSISTENCY);
    }
  }

  private void validateIfAnyTaskExecutionsAreEnabledForCorrection(Long taskId, Long jobId) throws StreemException {
    List<Long> taskExecutionIds = taskExecutionRepository.findEnabledForCorrectionTaskExecutionIdsByJobIdAndTaskId(jobId, taskId);
    List<Error> errors = new ArrayList<>();
    if (!Utility.isEmpty(taskExecutionIds)) {
      for (Long taskExecutionId : taskExecutionIds) {
        ValidationUtils.addError(taskExecutionId, errors, ErrorCode.TASKS_ARE_ENABLED_FOR_CORRECTION);
      }
      ValidationUtils.invalidate(taskId.toString(), errors);
    }
  }

  private void validateIfAllParametersOfRepeatedTasksAreNotHidden(Long taskExecutionId) throws StreemException {
    long totalParametersCountForTaskExecution = parameterValueRepository.countAllByTaskExecutionId(taskExecutionId);
    long totalHiddenParametersCountForTaskExecution = parameterValueRepository.countAllByTaskExecutionIdWithHidden(taskExecutionId, true);
    log.info("[validateIfAllParametersOfRepeatedTasksAreNotHidden] totalParametersCountForTaskExecution: {}, totalHiddenParametersCountForTaskExecution: {}", totalParametersCountForTaskExecution, totalHiddenParametersCountForTaskExecution);

    if (totalParametersCountForTaskExecution == totalHiddenParametersCountForTaskExecution) {
      ValidationUtils.invalidate(taskExecutionId, ErrorCode.CANNOT_REPEAT_TASK_ALL_PARAMETERS_ARE_HIDDEN_FOR_TASK_EXECUTION);

    }

  }

  private void validateIfAllParametersOfRecurringTasksAreNotHidden(Long taskExecutionId) throws StreemException {
    long totalParametersCountForTaskExecution = parameterValueRepository.countAllByTaskExecutionId(taskExecutionId);
    long totalHiddenParametersCountForTaskExecution = parameterValueRepository.countAllByTaskExecutionIdWithHidden(taskExecutionId, true);
    log.info("[validateIfAllParametersOfRepeatedTasksAreNotHidden] totalParametersCountForTaskExecution: {}, totalHiddenParametersCountForTaskExecution: {}", totalParametersCountForTaskExecution, totalHiddenParametersCountForTaskExecution);

    if (totalParametersCountForTaskExecution == totalHiddenParametersCountForTaskExecution) {
      ValidationUtils.invalidate(taskExecutionId, ErrorCode.CANNOT_REPEAT_TASK_ALL_PARAMETERS_ARE_HIDDEN_FOR_TASK_EXECUTION);

    }

  }

  private void validateTaskLock(TaskExecution taskExecution, PrincipalUser principalUser, Long taskExecutionId) throws StreemException {
    if (taskExecution.getTask().isSoloTask() && !Objects.equals(taskExecution.getStartedBy().getId(), principalUser.getId())) {
      String errorMessage = String.format("%s %s %s (ID: %s)", ErrorCode.SOLO_TASK_LOCKED.getDescription(), taskExecution.getStartedBy().getFirstName(), taskExecution.getStartedBy().getLastName(), taskExecution.getStartedBy().getEmployeeId());
      ValidationUtils.invalidate(String.valueOf(taskExecutionId), ErrorCode.SOLO_TASK_LOCKED, errorMessage);
    }
  }

  @Override
  public UserTaskAssigneeStatusDto checkTaskAssignee(Long taskExecutionId) {
    log.info("[checkTaskAssignee] Request to check task assignee for taskExecutionId: {}", taskExecutionId);
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    boolean isUserAssignedToTask = taskExecutionAssigneeRepository.existsByTaskExecutionIdAndUserId(taskExecutionId, principalUser.getId());
    return new UserTaskAssigneeStatusDto(isUserAssignedToTask, taskExecutionId.toString(), principalUser.getId().toString());
  }

  @Override
  @Transactional(readOnly = true)
  public TaskDetailsDto getTaskData(Long taskExecutionId) throws ResourceNotFoundException {
    TaskExecution taskExecution = taskExecutionRepository.findById(taskExecutionId)
      .orElseThrow(() -> new ResourceNotFoundException(taskExecutionId, ErrorCode.TASK_EXECUTION_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));

    Long jobId = taskExecution.getJobId();
    State.Job jobState = jobRepository.getStateByJobId(jobId);
    TaskDetailsDto taskDetailsDto = new TaskDetailsDto();
    taskDetailsDto.setJobState(jobState);
    taskDetailsDto.setJobId(jobId.toString());

    List<ParameterValue> parameterValues = parameterValueRepository.findAllByTaskExecutionId(taskExecutionId);

    Map<Long, List<TaskPauseReasonOrComment>> pauseCommentsOrReason = taskExecutionTimerService.calculateDurationAndReturnReasonsOrComments(List.of(taskExecution));


    Map<Long, List<ParameterValue>> parameterValueMap =
      parameterValues.stream()
        .collect(Collectors.groupingBy(
          av -> av.getParameter().getId(),
          Collectors.toList()
        ));

    Map<Long, List<TaskExecution>> taskIdTaskExecutionListMap = new HashMap<>();
    Map<Long, TaskExecution> taskExecutionMap = new HashMap<>();

    taskIdTaskExecutionListMap.computeIfAbsent(taskExecution.getTaskId(), k -> new ArrayList<>());
    taskIdTaskExecutionListMap.get(taskExecution.getTaskId()).add(taskExecution);
    taskExecutionMap.put(taskExecution.getId(), taskExecution);

    List<TempParameterValue> tempParameterValues = tempParameterValueRepository.findAllByTaskExecutionId(taskExecutionId);

    Map<Long, List<TempParameterValue>> tempParameterValueMap =
      tempParameterValues.stream()
        .collect(Collectors.groupingBy(
          av -> av.getParameter().getId(),
          Collectors.toList()
        ));


    Map<Long, List<TempParameterVerification>> tempParameterVerificationPeerAndSelf = parameterVerificationService.getTempParameterVerificationsDataForAJob(jobId);
    Map<Long, List<ParameterVerification>> parameterVerificationPeerAndSelf = parameterVerificationService.getParameterVerificationsDataForAJob(jobId);
    taskDetailsDto.setTask(taskMapper.toDto(taskExecution.getTask(), parameterValueMap, taskExecutionMap, tempParameterValueMap, pauseCommentsOrReason, parameterVerificationPeerAndSelf, tempParameterVerificationPeerAndSelf));
    taskDetailsDto.setHidden(parameterValues.stream().allMatch(ParameterValueBase::isHidden));
    return taskDetailsDto;

  }

  public void validateIfUserIsAssignedToExecuteParameter(Long taskExecutionId, Long currentUserId) throws StreemException {
    TaskExecution taskExecution = taskExecutionRepository.getReferenceById(taskExecutionId);
    boolean isAllowedUser = taskExecutionAssigneeRepository.existsByTaskExecutionIdAndUserId(taskExecutionId, currentUserId);
    if (!isAllowedUser && !taskExecution.getTask().isSoloTask()) {
      ValidationUtils.invalidate(taskExecutionId, ErrorCode.USER_NOT_ASSIGNED_TO_EXECUTE_TASK);
    } else if (!isAllowedUser && taskExecution.getTask().isSoloTask()) {
      String errorMessage = String.format("%s %s %s (ID: %s)", ErrorCode.SOLO_TASK_LOCKED.getDescription(), taskExecution.getStartedBy().getFirstName(), taskExecution.getStartedBy().getLastName(), taskExecution.getStartedBy().getEmployeeId());
      ValidationUtils.invalidate(String.valueOf(taskExecution.getId()), ErrorCode.SOLO_TASK_LOCKED, errorMessage);
    }
  }

  private void validateIfUserIsAssignedToExecuteJob(Long jobId, Long userId) throws StreemException {
    if (!taskExecutionAssigneeRepository.isUserAssignedToAnyTask(jobId, userId)) {
      ValidationUtils.invalidate(jobId, ErrorCode.USER_NOT_ASSIGNED_TO_EXECUTE_JOB);
    }
  }

  private void validateIfTaskDependenciesAreCompleted(Task task, Long jobId) throws StreemException {
    List<TaskExecution> invalidTaskExecutions = taskExecutionRepository.findIncompleteDependencies(task.getId(), jobId);

    if (!invalidTaskExecutions.isEmpty()) {
      log.info("[validateIfTaskDependenciesAreCompleted] Task dependencies are not completed for task: {} and job: {}", task, jobId);
      List<TaskDependencyErrorStageDto> taskDependencyErrorStageDtos = buildTaskDependencyErrorStageDtos(invalidTaskExecutions.stream().toList());
      Map<String, List<TaskDependencyErrorStageDto>> errorInfo = new HashMap<>();
      errorInfo.put("TASK_DEPENDENCY_ERROR", taskDependencyErrorStageDtos);
      ValidationUtils.invalidate(task.getIdAsString(), ErrorCode.TASK_DEPENDENCIES_NOT_COMPLETED, ErrorCode.TASK_DEPENDENCIES_NOT_COMPLETED.getDescription(), errorInfo);
    }
  }

  // need to improve this and make it Reusable
  private List<TaskDependencyErrorStageDto> buildTaskDependencyErrorStageDtos(List<TaskExecution> taskExecutions) {
    Set<Long> taskIds = taskExecutions.stream().map(TaskExecution::getTaskId).collect(Collectors.toSet());
    Set<Task> tasks = new HashSet<>(taskRepository.findAllById(taskIds));
    Set<Stage> stages = new HashSet<>(stageRepository.findByTaskIds(taskIds.stream().toList()));

    List<TaskDependencyErrorStageDto> taskDependencyErrorStageDtos = new ArrayList<>();

    Map<Long, List<Task>> stageIdTasksMap = tasks.stream()
      .collect(Collectors.groupingBy(Task::getStageId));
    Map<Long, List<TaskExecution>> taskIdTaskExecutionsMap = taskExecutions.stream()
      .collect(Collectors.groupingBy(TaskExecution::getTaskId));

    for (Stage stage : stages) {
      TaskDependencyErrorStageDto taskDependencyErrorStageDto = new TaskDependencyErrorStageDto();
      taskDependencyErrorStageDto.setId(stage.getIdAsString());
      taskDependencyErrorStageDto.setOrderTree(stage.getOrderTree());
      taskDependencyErrorStageDto.setTasks(new ArrayList<>());
      List<Task> stageTasks = stageIdTasksMap.get(stage.getId());
      for (Task task : stageTasks) {
        TaskDependencyErrorTaskDto taskDependencyErrorTaskDto = new TaskDependencyErrorTaskDto();
        taskDependencyErrorTaskDto.setId(task.getIdAsString());
        taskDependencyErrorTaskDto.setName(task.getName());
        taskDependencyErrorTaskDto.setOrderTree(task.getOrderTree());
        taskDependencyErrorTaskDto.setTaskExecutions(new ArrayList<>());
        List<TaskExecution> taskExecutionsOfTask = taskIdTaskExecutionsMap.get(task.getId());
        for (TaskExecution taskExecution : taskExecutionsOfTask) {
          TaskDependencyErrorTaskExecutionDto taskDependencyErrorTaskExecutionDto = new TaskDependencyErrorTaskExecutionDto();
          taskDependencyErrorTaskExecutionDto.setId(taskExecution.getIdAsString());
          taskDependencyErrorTaskExecutionDto.setOrderTree(taskExecution.getOrderTree());
          taskDependencyErrorTaskDto.getTaskExecutions().add(taskDependencyErrorTaskExecutionDto);
        }
        taskDependencyErrorStageDto.getTasks().add(taskDependencyErrorTaskDto);
      }
      taskDependencyErrorStageDtos.add(taskDependencyErrorStageDto);
    }

    taskDependencyErrorStageDtos.sort(Comparator.comparingInt(TaskDependencyErrorStageDto::getOrderTree));
    taskDependencyErrorStageDtos.forEach(stage -> stage.getTasks().sort(Comparator.comparingInt(TaskDependencyErrorTaskDto::getOrderTree)));
    taskDependencyErrorStageDtos.forEach(stage -> stage.getTasks().forEach(task -> task.getTaskExecutions().sort(Comparator.comparingInt(TaskDependencyErrorTaskExecutionDto::getOrderTree))));

    return taskDependencyErrorStageDtos;
  }

  private boolean handleSoloTaskLock(Long taskExecutionId, Long jobId, Task task, PrincipalUser principalUser) throws ResourceNotFoundException {
    if (task.isSoloTask()) {
      User principalUserEntity = userRepository.getReferenceById(principalUser.getId());
      taskExecutionAssigneeRepository.deleteAllByTaskExecutionId(taskExecutionId);
      TaskExecutionUserMapping taskExecutionUserMapping = new TaskExecutionUserMapping(taskExecutionRepository.getReferenceById(taskExecutionId), userRepository.getReferenceById(principalUserEntity.getId()), principalUserEntity);
      taskExecutionAssigneeRepository.save(taskExecutionUserMapping);
      return true;
    }
    return false;
  }

}
