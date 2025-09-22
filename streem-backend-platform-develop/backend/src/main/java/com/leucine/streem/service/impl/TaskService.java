package com.leucine.streem.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.leucine.streem.constant.State;
import com.leucine.streem.constant.Type;
import com.leucine.streem.dto.*;
import com.leucine.streem.dto.mapper.IMediaMapper;
import com.leucine.streem.dto.mapper.ITaskMapper;
import com.leucine.streem.dto.mapper.ITaskRecurrenceMapper;
import com.leucine.streem.dto.mapper.ITaskSchedulesMapper;
import com.leucine.streem.dto.projection.TaskExecutorLockView;
import com.leucine.streem.dto.request.*;
import com.leucine.streem.dto.response.ErrorCode;
import com.leucine.streem.exception.ExceptionType;
import com.leucine.streem.exception.ResourceNotFoundException;
import com.leucine.streem.exception.StreemException;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService implements ITaskService {
  private final IChecklistService checklistService;
  private final IChecklistAuditService checklistAuditService;
  private final IMediaMapper mediaMapper;
  private final IMediaRepository mediaRepository;
  private final IStageRepository stageRepository;
  private final ITaskMapper taskMapper;
  private final ITaskMediaMappingRepository taskMediaMappingRepository;
  private final ITaskRepository taskRepository;
  private final IUserRepository userRepository;
  private final ITaskAutomationService automationService;
  private final IInterlockService interlockService;
  private final ITaskRecurrenceRepository taskRecurrenceRepository;
  private final ITaskRecurrenceMapper taskRecurrenceMapper;
  private final ITaskSchedulesRepository taskSchedulesRepository;
  private final ITaskSchedulesMapper taskSchedulesMapper;
  private final ITrainedUserTaskMappingRepository trainedUsersTaskMappingRepository;
  private final IParameterService parameterService;
  private final ITaskDependencyRepository taskDependencyRepository;
  private final ITaskExecutorLockRepository taskExecutorLockRepository;

  @Override
  public TaskDto getTask(Long taskId) throws ResourceNotFoundException, StreemException {
    log.info("[getTask] Request to get task, taskId: {}", taskId);
    Task task = taskRepository.findById(taskId)
      .orElseThrow(() -> new ResourceNotFoundException(taskId, ErrorCode.TASK_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));
    return taskMapper.toDto(taskRepository.save(task));
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public TaskDto createTask(Long checklistId, Long stageId, TaskRequest taskRequest) throws ResourceNotFoundException, StreemException {
    log.info("[createTask] Request to create task, checklistId: {}, stageId: {}, taskRequest: {}", checklistId, stageId, taskRequest);
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    User principalUserEntity = userRepository.getOne(principalUser.getId());
    Stage stage = stageRepository.findById(stageId).orElseThrow(() -> new ResourceNotFoundException(stageId, ErrorCode.STAGE_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));
    Checklist checklist = stage.getChecklist();
    checklistService.validateChecklistModificationState(checklist.getId(), checklist.getState());
    checklistService.validateIfUserIsAuthorForPrototype(checklist.getId(), principalUser.getId());

    Task task = new Task();
    task.setName(taskRequest.getName());
    task.setOrderTree(taskRequest.getOrderTree());
    task.setCreatedBy(principalUserEntity);
    task.setModifiedBy(principalUserEntity);
    task.setStage(stage);
    checklistAuditService.createTask(checklist.getId(), task, principalUser);
    return taskMapper.toDto(taskRepository.save(task));
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public TaskDto updateTask(Long taskId, TaskRequest taskRequest) throws ResourceNotFoundException, StreemException {
    log.info("[updateTask] Request to update task, taskId: {}, taskRequest: {}", taskId, taskRequest);
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    User principalUserEntity = userRepository.getOne(principalUser.getId());

    Task task = taskRepository.findById(taskId)
      .orElseThrow(() -> new ResourceNotFoundException(taskId, ErrorCode.TASK_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));
    Checklist checklist = task.getStage().getChecklist();
    checklistService.validateChecklistModificationState(checklist.getId(), checklist.getState());
    checklistService.validateIfUserIsAuthorForPrototype(checklist.getId(), principalUser.getId());

    task.setName(taskRequest.getName());
    task.setModifiedBy(principalUserEntity);
    checklistAuditService.updateTask(checklist.getId(), task, principalUser);
    return taskMapper.toDto(taskRepository.save(task));
  }

  @Override
  public TaskRecurrenceDto getTaskRecurrence(Long taskId) throws ResourceNotFoundException {
    log.info("[setTaskRecurrence] Request to set task recurrence, taskId: {}", taskId);
    Task task = taskRepository.findById(taskId)
      .orElseThrow(() -> new ResourceNotFoundException(taskId, ErrorCode.TASK_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));
    TaskRecurrence taskRecurrence = null;
    if (!Utility.isEmpty(task.getTaskRecurrenceId())) {
      taskRecurrence = taskRecurrenceRepository.findById(task.getTaskRecurrenceId()).orElseThrow(() -> new ResourceNotFoundException(task.getTaskRecurrenceId(), ErrorCode.TASK_RECURRENCE_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));
    }

    return taskRecurrenceMapper.toDto(taskRecurrence);
  }


  @Override
  @Transactional(rollbackFor = Exception.class)
  public TaskDto setTaskRecurrence(Long taskId, SetTaskRecurrentRequest setTaskRecurrentRequest) throws ResourceNotFoundException, StreemException, JsonProcessingException {
    log.info("[setTaskRecurrence] Request to set task recurrence, taskId: {}, setTaskRecurrentRequest: {}", taskId, setTaskRecurrentRequest);
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

    validateTaskRecurrentRequest(setTaskRecurrentRequest, taskId);

    User principalUserEntity = userRepository.getReferenceById(principalUser.getId());
    Task task = taskRepository.findById(taskId)
      .orElseThrow(() -> new ResourceNotFoundException(taskId, ErrorCode.TASK_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));
    Checklist checklist = task.getStage().getChecklist();
    checklistService.validateChecklistModificationState(checklist.getId(), checklist.getState());
    checklistService.validateIfUserIsAuthorForPrototype(checklist.getId(), principalUser.getId());

    TaskRecurrence taskRecurrence;
    if (!Utility.isEmpty(task.getTaskRecurrenceId())) {
      taskRecurrence = taskRecurrenceRepository.findById(task.getTaskRecurrenceId()).orElseThrow(() -> new ResourceNotFoundException(task.getTaskRecurrenceId(), ErrorCode.TASK_RECURRENCE_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));
    } else {
      taskRecurrence = new TaskRecurrence();
      taskRecurrence.setCreatedBy(principalUserEntity);
    }

    task.setEnableRecurrence(true);
    taskRecurrence.setDueDateDuration(setTaskRecurrentRequest.getDueDateDuration());
    taskRecurrence.setDueDateInterval(setTaskRecurrentRequest.getDueDateInterval());
    taskRecurrence.setStartDateDuration(setTaskRecurrentRequest.getStartDateDuration());
    taskRecurrence.setStartDateInterval(setTaskRecurrentRequest.getStartDateInterval());
    taskRecurrence.setPositiveStartDateToleranceInterval(setTaskRecurrentRequest.getPositiveStartDateToleranceInterval());
    taskRecurrence.setPositiveStartDateToleranceDuration(setTaskRecurrentRequest.getPositiveStartDateToleranceDuration());
    taskRecurrence.setNegativeStartDateToleranceInterval(setTaskRecurrentRequest.getNegativeStartDateToleranceInterval());
    taskRecurrence.setNegativeStartDateToleranceDuration(setTaskRecurrentRequest.getNegativeStartDateToleranceDuration());
    taskRecurrence.setPositiveDueDateToleranceInterval(setTaskRecurrentRequest.getPositiveDueDateToleranceInterval());
    taskRecurrence.setPositiveDueDateToleranceDuration(setTaskRecurrentRequest.getPositiveDueDateToleranceDuration());
    taskRecurrence.setNegativeDueDateToleranceInterval(setTaskRecurrentRequest.getNegativeDueDateToleranceInterval());
    taskRecurrence.setNegativeDueDateToleranceDuration(setTaskRecurrentRequest.getNegativeDueDateToleranceDuration());
    taskRecurrence.setModifiedBy(principalUserEntity);
    task.setTaskRecurrence(taskRecurrence);
    task.setModifiedBy(principalUserEntity);

    taskRecurrenceRepository.save(taskRecurrence);


    return taskMapper.toDto(taskRepository.save(task));
  }

  private void validateTaskRecurrentRequest(SetTaskRecurrentRequest setTaskRecurrentRequest, Long taskId) throws StreemException {

    try {
      if (!Utility.isEmpty(setTaskRecurrentRequest.getDueDateDuration())) {
        JsonUtils.readValue(setTaskRecurrentRequest.getDueDateDuration().toString(), DateDuration.class);
      }
      if (!Utility.isEmpty(setTaskRecurrentRequest.getStartDateDuration())){
        JsonUtils.readValue(setTaskRecurrentRequest.getStartDateDuration().toString(), DateDuration.class);
      }
    } catch (Exception e) {
      ValidationUtils.invalidate(taskId, ErrorCode.INVALID_DATE_DURATION);
    }
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public TaskDto unsetTaskRecurrence(Long taskId) throws ResourceNotFoundException, StreemException {
    log.info("[setTaskRecurrence] Request to unset task recurrence, taskId: {}", taskId);
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    User principalUserEntity = userRepository.getReferenceById(principalUser.getId());
    Task task = taskRepository.findById(taskId)
      .orElseThrow(() -> new ResourceNotFoundException(taskId, ErrorCode.TASK_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));
    Checklist checklist = task.getStage().getChecklist();
    checklistService.validateChecklistModificationState(checklist.getId(), checklist.getState());
    checklistService.validateIfUserIsAuthorForPrototype(checklist.getId(), principalUser.getId());

    TaskRecurrence taskRecurrence = taskRecurrenceRepository.findById(task.getTaskRecurrenceId()).orElseThrow(() -> new ResourceNotFoundException(task.getTaskRecurrenceId(), ErrorCode.TASK_RECURRENCE_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));

    task.setEnableRecurrence(false);
    task.setTaskRecurrence(null);
    task.setModifiedBy(principalUserEntity);

    taskRepository.save(task);
    taskRecurrenceRepository.deleteById(taskRecurrence.getId());

    return taskMapper.toDto(task);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public TaskDto setTimer(Long taskId, TimerRequest timerRequest) throws StreemException, ResourceNotFoundException {
    log.info("[setTimer] Request to set timer to task, taskId: {}, timerRequest: {}", taskId, timerRequest);
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    validateTimerRequest(timerRequest, taskId);
    User principalUserEntity = userRepository.getOne(principalUser.getId());
    Task task = taskRepository.findById(taskId)
      .orElseThrow(() -> new ResourceNotFoundException(taskId, ErrorCode.TASK_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));
    Checklist checklist = task.getStage().getChecklist();
    checklistService.validateChecklistModificationState(checklist.getId(), checklist.getState());
    checklistService.validateIfUserIsAuthorForPrototype(checklist.getId(), principalUser.getId());

    task.setTimed(true);
    task.setModifiedBy(principalUserEntity);
    task.setTimerOperator(timerRequest.getTimerOperator().name());
    task.setMinPeriod(timerRequest.getMinPeriod());
    task.setMaxPeriod(Utility.nullIfZero(timerRequest.getMaxPeriod()));

    return taskMapper.toDto(taskRepository.save(task));
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public TaskDto unsetTimer(Long taskId) throws StreemException, ResourceNotFoundException {
    log.info("[unsetTimer] Request unset timer from task, taskId: {}", taskId);
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    User principalUserEntity = userRepository.getOne(principalUser.getId());
    Task task = taskRepository.findById(taskId)
      .orElseThrow(() -> new ResourceNotFoundException(taskId, ErrorCode.TASK_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));
    Checklist checklist = task.getStage().getChecklist();
    checklistService.validateChecklistModificationState(checklist.getId(), checklist.getState());
    checklistService.validateIfUserIsAuthorForPrototype(checklist.getId(), principalUser.getId());

    task.setTimed(false);
    task.setModifiedBy(principalUserEntity);
    task.setTimerOperator(null);
    task.setMinPeriod(null);
    task.setMaxPeriod(null);

    return taskMapper.toDto(taskRepository.save(task));
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public TaskDto addStop(Long taskId) throws ResourceNotFoundException, StreemException {
    log.info("[addStop] Request to add stop to task, taskId: {}", taskId);
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    User principalUserEntity = userRepository.getOne(principalUser.getId());
    Task task = taskRepository.findById(taskId)
      .orElseThrow(() -> new ResourceNotFoundException(taskId, ErrorCode.TASK_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));
    Checklist checklist = task.getStage().getChecklist();
    checklistService.validateChecklistModificationState(checklist.getId(), checklist.getState());
    checklistService.validateIfUserIsAuthorForPrototype(checklist.getId(), principalUser.getId());

    task.setHasStop(true);
    task.setModifiedBy(principalUserEntity);

    return taskMapper.toDto(taskRepository.save(task));
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public TaskDto removeStop(Long taskId) throws ResourceNotFoundException, StreemException {
    log.info("[removeStop] Request to remove stop from task, taskId: {}", taskId);
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    User principalUserEntity = userRepository.getOne(principalUser.getId());
    Task task = taskRepository.findById(taskId)
      .orElseThrow(() -> new ResourceNotFoundException(taskId, ErrorCode.TASK_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));
    Checklist checklist = task.getStage().getChecklist();
    checklistService.validateChecklistModificationState(checklist.getId(), checklist.getState());
    checklistService.validateIfUserIsAuthorForPrototype(checklist.getId(), principalUser.getId());

    task.setHasStop(false);
    task.setModifiedBy(principalUserEntity);

    return taskMapper.toDto(taskRepository.save(task));
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public TaskDto addMedia(Long taskId, MediaRequest mediaRequest) throws ResourceNotFoundException, StreemException {
    log.info("[addMedia] Request to add media to task, taskId: {}, mediaRequest: {}", taskId, mediaRequest);
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    User principalUserEntity = userRepository.getOne(principalUser.getId());
    Task task = taskRepository.findById(taskId)
      .orElseThrow(() -> new ResourceNotFoundException(taskId, ErrorCode.TASK_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));
    Checklist checklist = task.getStage().getChecklist();
    checklistService.validateChecklistModificationState(checklist.getId(), checklist.getState());
    checklistService.validateIfUserIsAuthorForPrototype(checklist.getId(), principalUser.getId());

    Media media = mediaRepository.findById(mediaRequest.getMediaId())
      .orElseThrow(() -> new ResourceNotFoundException(mediaRequest.getMediaId(), ErrorCode.MEDIA_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));
    media.setName(mediaRequest.getName());
    media.setDescription(mediaRequest.getDescription());

    task.addMedia(mediaRepository.save(media), principalUserEntity);
    return taskMapper.toDto(taskRepository.save(task));
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public MediaDto updateMedia(Long taskId, Long mediaId, MediaRequest mediaRequest) throws ResourceNotFoundException, StreemException {
    log.info("[updateMedia] Request to update media of a task, taskId: {}, mediaId: {}, mediaRequest: {}", taskId, mediaRequest);
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    User principalUserEntity = userRepository.getOne(principalUser.getId());
    TaskMediaMapping taskMediaMapping = taskMediaMappingRepository.getByTaskIdAndMediaId(taskId, mediaId)
      .orElseThrow(() -> new ResourceNotFoundException(taskId, ErrorCode.TASK_MEDIA_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));
    Checklist checklist = checklistService.findByTaskId(taskId);
    checklistService.validateChecklistModificationState(checklist.getId(), checklist.getState());
    checklistService.validateIfUserIsAuthorForPrototype(checklist.getId(), principalUser.getId());

    Media media = taskMediaMapping.getMedia();

    mediaMapper.update(mediaRequest, media);
    media.setModifiedBy(principalUserEntity);

    return mediaMapper.toDto(mediaRepository.save(media));
  }

  //TODO Work with UI to remove unnecessary returns, UI knows the action done
  // need not return whole object, applies at every other place
  @Override
  @Transactional(rollbackFor = Exception.class)
  public TaskDto deleteMedia(Long taskId, Long mediaId) {
    log.info("[deleteMedia] Request to delete media from task, taskId: {}, mediaId: {}", taskId, mediaId);
    taskMediaMappingRepository.deleteByTaskIdAndMediaId(taskId, mediaId);
    return taskMapper.toDto(taskRepository.findById(taskId).get());
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public TaskDto archiveTask(Long taskId) throws StreemException, ResourceNotFoundException {
    log.info("[archiveTask] Request to archive task, taskId: {}", taskId);
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    User principalUserEntity = userRepository.getOne(principalUser.getId());
    Task task = taskRepository.findById(taskId)
      .orElseThrow(() -> new ResourceNotFoundException(taskId, ErrorCode.TASK_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));
    Checklist checklist = task.getStage().getChecklist();
    checklistService.validateChecklistModificationState(checklist.getId(), checklist.getState());
    checklistService.validateIfUserIsAuthorForPrototype(checklist.getId(), principalUser.getId());

    if (!Utility.isEmpty(task.getDependentTasks())) {
      ValidationUtils.invalidate(taskId, ErrorCode.CHANGE_THE_TASK_DEPENDENCY);
    }

    if(!Utility.isEmpty(task.getPrerequisiteTasks())){
      List<TaskDependency> taskPreRequisites = task.getPrerequisiteTasks().stream().toList();
      task.getPrerequisiteTasks().clear();
      taskDependencyRepository.deleteAll(taskPreRequisites);
    }

    Set<Long> tasksWhereTaskExecutorLockIsUsedOnce = taskExecutorLockRepository.findTasksWhereTaskExecutorLockHasOneReferencedTask(taskId);
    taskExecutorLockRepository.removeByTaskIdOrReferencedTaskId(taskId);


    for (Parameter parameter : task.getParameters()) {
      parameterService.archiveParameter(parameter.getId());
    }

    for (TaskAutomationMapping automation : task.getAutomations()) {
      automationService.deleteTaskAutomation(taskId, automation.getAutomationId());
    }

    trainedUsersTaskMappingRepository.deleteByTaskId(taskId);

    task.setArchived(true);
    task.setModifiedBy(principalUserEntity);

    taskRepository.updateHasTaskExecutorLock(tasksWhereTaskExecutorLockIsUsedOnce, false);

    checklistAuditService.archiveTask(checklist.getId(), task, principalUser);
    return taskMapper.toDto(taskRepository.save(task));
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public BasicDto reorderTasks(TaskReorderRequest taskReorderRequest) throws ResourceNotFoundException, StreemException {
    log.info("[reorderTasks] Request to reorder tasks, taskReorderRequest: {}", taskReorderRequest);
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    var state = checklistService.findById(taskReorderRequest.getChecklistId()).getState();
    if (!State.CHECKLIST_EDIT_STATES.contains(state)) {
      ValidationUtils.invalidate(taskReorderRequest.getChecklistId(), ErrorCode.PROCESS_CANNOT_BE_MODFIFIED);
    }
    taskReorderRequest.getTasksOrder().forEach((taskId, order) -> taskRepository.reorderTask(taskId, order, principalUser.getId(), DateTimeUtils.now()));

    BasicDto basicDto = new BasicDto();
    basicDto.setMessage("success");
    return basicDto;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public TaskDto addAutomation(Long taskId, AutomationRequest automationRequest) throws ResourceNotFoundException, StreemException, JsonProcessingException, JsonProcessingException {
    log.info("[addAutomations] Request to add task automations, taskId: {}, automationRequest: {}", taskId, automationRequest);
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    Task task = taskRepository.findById(taskId)
      .orElseThrow(() -> new ResourceNotFoundException(taskId, ErrorCode.TASK_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));
    Checklist checklist = task.getStage().getChecklist();
    checklistService.validateChecklistModificationState(checklist.getId(), checklist.getState());
    checklistService.validateIfUserIsAuthorForPrototype(checklist.getId(), principalUser.getId());

    return automationService.addTaskAutomation(taskId, automationRequest);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public TaskDto deleteAutomation(Long taskId, Long automationId) throws ResourceNotFoundException, StreemException {
    log.info("[addAutomation] Request to delete task automation, taskId: {}, automationId: {}", taskId, automationId);
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    Task task = taskRepository.findById(taskId)
      .orElseThrow(() -> new ResourceNotFoundException(taskId, ErrorCode.TASK_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));
    Checklist checklist = task.getStage().getChecklist();
    checklistService.validateChecklistModificationState(checklist.getId(), checklist.getState());
    checklistService.validateIfUserIsAuthorForPrototype(checklist.getId(), principalUser.getId());

    return automationService.deleteTaskAutomation(taskId, automationId);
  }

  @Override
  public TaskDto unsetBulkVerification(Long taskId) {
    Task task = taskRepository.getReferenceById(taskId);
    task.setHasBulkVerification(false);
    taskRepository.save(task);
    return taskMapper.toDto(task);
  }

  @Override
  public TaskDto setBulkVerification(Long taskId) {
    Task task = taskRepository.getReferenceById(taskId);
    task.setHasBulkVerification(true);
    taskRepository.save(task);
    return taskMapper.toDto(task);
  }

  @Override
  public List<TaskExecutorLockView> getTaskExecutorLockView(Long taskId) {
    return taskExecutorLockRepository.findByTaskId(taskId);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public BasicDto unsetExecutor(Long taskId) {
    Task task = taskRepository.getReferenceById(taskId);
    task.setHasExecutorLock(false);
    taskRepository.save(task);
    taskExecutorLockRepository.removeByTaskId(taskId);
    return new BasicDto(null, "success", null);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public BasicDto addTaskExecutorLock(Long taskId, TaskExecutorLockRequest taskExecutorLockRequest) throws ResourceNotFoundException, StreemException {
    log.info("[addTaskExecutorLock] Request to add task executor lock, taskId: {}, taskExecutorLockRequest: {}", taskId, taskExecutorLockRequest);
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    Task executorTask = taskRepository.findById(taskId)
      .orElseThrow(() -> new ResourceNotFoundException(taskId, ErrorCode.TASK_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));

    taskExecutorLockRepository.removeByTaskId(taskId);

    User principalUserEntity = userRepository.getReferenceById(principalUser.getId());

    validateCorrectTaskExecutorLockRequest(taskExecutorLockRequest, taskId);
    List<Task> cannotBeExecutorTasks = taskRepository.findAllById(taskExecutorLockRequest.getCannotBeExecutorIds());


    List<TaskExecutorLock> taskExecutorLocks = new ArrayList<>();
    for (Task cannotBeExecutorTask : cannotBeExecutorTasks) {
      TaskExecutorLock taskExecutorLock = new TaskExecutorLock();
      taskExecutorLock.setTask(executorTask);
      taskExecutorLock.setReferencedTask(cannotBeExecutorTask);
      taskExecutorLock.setLockType(Type.TaskExecutorLockType.NIN);
      taskExecutorLock.setCreatedAt(DateTimeUtils.now());
      taskExecutorLock.setCreatedBy(principalUserEntity);
      taskExecutorLock.setModifiedBy(principalUserEntity);
      taskExecutorLock.setModifiedAt(DateTimeUtils.now());
      taskExecutorLocks.add(taskExecutorLock);
    }

    // For has to be executor task
    if (!Utility.isEmpty(taskExecutorLockRequest.getHasToBeExecutorId())) {

      TaskExecutorLock taskExecutorLock = new TaskExecutorLock();
      taskExecutorLock.setTask(executorTask);
      taskExecutorLock.setReferencedTask(taskRepository.findById(taskExecutorLockRequest.getHasToBeExecutorId()).get());
      taskExecutorLock.setLockType(Type.TaskExecutorLockType.EQ);
      taskExecutorLock.setCreatedAt(DateTimeUtils.now());
      taskExecutorLock.setCreatedBy(principalUserEntity);
      taskExecutorLock.setModifiedBy(principalUserEntity);
      taskExecutorLock.setModifiedAt(DateTimeUtils.now());
      taskExecutorLocks.add(taskExecutorLock);
    }
    taskExecutorLockRepository.saveAll(taskExecutorLocks);

    executorTask.setHasExecutorLock(true);
    taskRepository.save(executorTask);

    return new BasicDto(null, "success", null);
  }

  private void validateCorrectTaskExecutorLockRequest(TaskExecutorLockRequest taskExecutorLockRequest, Long taskId) throws StreemException {
    Set<Long> cannotBeExecutorIds = new HashSet<>(taskExecutorLockRequest.getCannotBeExecutorIds());
    Long hasToBeExecutorId = taskExecutorLockRequest.getHasToBeExecutorId();

    if (cannotBeExecutorIds.contains(hasToBeExecutorId)) {
      ValidationUtils.invalidate(taskId, ErrorCode.INCORRECT_CONFIGURATION_FOR_TASK_EXECUTOR_LOCK);
    }

    if (!Utility.isEmpty(hasToBeExecutorId)) {
      cannotBeExecutorIds.add(hasToBeExecutorId);
    }



    List<TaskExecutorLock> taskExecutorLocks = taskExecutorLockRepository.findByTaskIdAndReferencedTaskIdIn(taskId, cannotBeExecutorIds);
    if (!taskExecutorLocks.isEmpty()) {
      ValidationUtils.invalidate(taskId, ErrorCode.TASK_EXECUTOR_LOCK_ALREADY_EXISTS);
    }

  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public TaskDto unsetTaskSchedules(Long taskId) throws ResourceNotFoundException {
    log.info("[unsetTaskSchedules] Request to unset task schedules, taskId: {}", taskId);
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    Task task = taskRepository.findById(taskId)
      .orElseThrow(() -> new ResourceNotFoundException(taskId, ErrorCode.TASK_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));

    TaskSchedules taskSchedules = task.getTaskSchedules();
    task.setTaskSchedules(null);
    task.setEnableScheduling(false);
    if (taskSchedules != null) {
      taskSchedulesRepository.deleteById(taskSchedules.getId());
    }
    task.setModifiedAt(DateTimeUtils.now());
    task.setModifiedBy(userRepository.getReferenceById(principalUser.getId()));
    taskRepository.save(task);
    return taskMapper.toDto(task);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public TaskDto setTaskSchedules(Long taskId, TaskSchedulesRequest taskSchedulesRequest) throws ResourceNotFoundException, StreemException {
    log.info("[setTaskSchedules] Request to set task schedules, taskId: {}, taskSchedulesRequest: {}", taskId, taskSchedulesRequest);
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    User principalUserEntity = userRepository.getReferenceById(principalUser.getId());

    validateTaskSchedulesRequest(taskSchedulesRequest, taskId);

    // task to be scheduled
    Task task = taskRepository.findById(taskId)
      .orElseThrow(() -> new ResourceNotFoundException(taskId, ErrorCode.TASK_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));
    unsetTaskSchedules(taskId);

    // task that triggers scheduling on another task
    Task referencedTask = null;
    if (!Utility.isEmpty(taskSchedulesRequest.getReferencedTaskId())) {
      referencedTask = taskRepository.findById(Long.valueOf(taskSchedulesRequest.getReferencedTaskId()))
        .orElseThrow(() -> new ResourceNotFoundException(taskSchedulesRequest.getReferencedTaskId(), ErrorCode.TASK_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));
    }

    TaskSchedules taskSchedules = new TaskSchedules();
    taskSchedules.setCondition(taskSchedulesRequest.getCondition());
    taskSchedules.setReferencedTask(referencedTask);
    taskSchedules.setType(taskSchedulesRequest.getType());
    taskSchedules.setStartDateDuration(taskSchedulesRequest.getStartDateDuration());
    taskSchedules.setStartDateInterval(taskSchedulesRequest.getStartDateInterval());
    taskSchedules.setDueDateDuration(taskSchedulesRequest.getDueDateDuration());
    taskSchedules.setDueDateInterval(taskSchedulesRequest.getDueDateInterval());
    taskSchedules.setCreatedAt(DateTimeUtils.now());
    taskSchedules.setCreatedBy(principalUserEntity);
    taskSchedules.setModifiedBy(principalUserEntity);
    taskSchedules.setModifiedAt(DateTimeUtils.now());

    taskSchedules = taskSchedulesRepository.save(taskSchedules);
    task.setTaskSchedules(taskSchedules);
    task.setEnableScheduling(true);
    task = taskRepository.save(task);
    return taskMapper.toDto(task);
  }

  @Override
  public TaskSchedulesDto getTaskSchedule(Long taskId) throws ResourceNotFoundException {
    log.info("[getTaskSchedule] Request to get task schedule, taskId: {}", taskId);
    Task task = taskRepository.findById(taskId)
      .orElseThrow(() -> new ResourceNotFoundException(taskId, ErrorCode.TASK_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));

    return taskSchedulesMapper.toDto(task.getTaskSchedules());
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public TaskDto updateAutomation(Long taskId, Long automationId, AutomationRequest automationRequest) throws ResourceNotFoundException, StreemException, JsonProcessingException {
    log.info("[addAutomation] Request to update task automation, taskId: {}, automationId: {}, automationRequest: {}", taskId, automationId, automationRequest);
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    Task task = taskRepository.findById(taskId)
      .orElseThrow(() -> new ResourceNotFoundException(taskId, ErrorCode.TASK_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));
    Checklist checklist = task.getStage().getChecklist();
    checklistService.validateChecklistModificationState(checklist.getId(), checklist.getState());
    checklistService.validateIfUserIsAuthorForPrototype(checklist.getId(), principalUser.getId());

    return automationService.updateAutomation(taskId, automationId, automationRequest);
  }

  @Override
  public InterlockDto addInterlockForTask(String taskId, InterlockRequest interlockRequest) throws StreemException, ResourceNotFoundException, JsonProcessingException {
    log.info("[addInterlockForTask] Request to add Interlock, taskId: {}", taskId);
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    User principalUserEntity = userRepository.getOne(principalUser.getId());
    Task task = taskRepository.findById(Long.valueOf(taskId))
      .orElseThrow(() -> new ResourceNotFoundException(taskId, ErrorCode.TASK_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));
    Checklist checklist = task.getStage().getChecklist();
    checklistService.validateChecklistModificationState(checklist.getId(), checklist.getState());
    checklistService.validateIfUserIsAuthorForPrototype(checklist.getId(), principalUser.getId());
    return interlockService.addInterlockForTask(task, interlockRequest, principalUserEntity);
  }

  @Override
  public InterlockDto getInterlockByTaskId(String taskId) throws StreemException, ResourceNotFoundException, JsonProcessingException {
    log.info("[getAndCreateInterlockByTaskId] Request to get Interlock, taskId: {}", taskId);
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    User principalUserEntity = userRepository.getOne(principalUser.getId());
    Task task = taskRepository.findById(Long.valueOf(taskId))
      .orElseThrow(() -> new ResourceNotFoundException(taskId, ErrorCode.TASK_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));
    return interlockService.getInterlockByTaskId(task, principalUserEntity);
  }

  @Override
  public InterlockDto updateInterlockForTask(String taskId, InterlockRequest interlockRequest) throws StreemException, ResourceNotFoundException, JsonProcessingException {
    log.info("[updateInterlockForTask] Request to update Interlock, taskId: {}", taskId);
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    User principalUserEntity = userRepository.getOne(principalUser.getId());
    Task task = taskRepository.findById(Long.valueOf(taskId))
      .orElseThrow(() -> new ResourceNotFoundException(taskId, ErrorCode.TASK_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));
    Checklist checklist = task.getStage().getChecklist();
    checklistService.validateChecklistModificationState(checklist.getId(), checklist.getState());
    checklistService.validateIfUserIsAuthorForPrototype(checklist.getId(), principalUser.getId());
    return interlockService.updateInterlockForTask(task, interlockRequest, principalUserEntity);
  }

  @Override
  public BasicDto deleteInterlockByTaskId(String interlockId, Long taskId) throws StreemException, ResourceNotFoundException, JsonProcessingException {
    log.info("[deleteInterlockByTaskId] Request to delete Interlock, taskId: {}", taskId);
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    User principalUserEntity = userRepository.getOne(principalUser.getId());
    Task task = taskRepository.findById(Long.valueOf(taskId))
      .orElseThrow(() -> new ResourceNotFoundException(taskId, ErrorCode.TASK_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));
    Checklist checklist = task.getStage().getChecklist();
    checklistService.validateChecklistModificationState(checklist.getId(), checklist.getState());
    checklistService.validateIfUserIsAuthorForPrototype(checklist.getId(), principalUser.getId());
    return interlockService.deleteInterlockByTaskId(interlockId, taskId, principalUserEntity);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public TaskDto addSoloTaskLock(Long taskId) throws ResourceNotFoundException, StreemException {
    log.info("[addSoloTaskLock] Request to add solo task lock to task, taskId: {}", taskId);
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    User principalUserEntity = userRepository.getOne(principalUser.getId());
    Task task = taskRepository.findById(taskId)
      .orElseThrow(() -> new ResourceNotFoundException(taskId, ErrorCode.TASK_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));
    Checklist checklist = task.getStage().getChecklist();
    checklistService.validateChecklistModificationState(checklist.getId(), checklist.getState());
    checklistService.validateIfUserIsAuthorForPrototype(checklist.getId(), principalUser.getId());

    task.setSoloTask(true);
    task.setModifiedBy(principalUserEntity);

    return taskMapper.toDto(taskRepository.save(task));
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public TaskDto removeSoloTaskLock(Long taskId) throws ResourceNotFoundException, StreemException {
    log.info("[removeSoloTaskLock] Request to remove solo task lock from task, taskId: {}", taskId);
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    User principalUserEntity = userRepository.getOne(principalUser.getId());
    Task task = taskRepository.findById(taskId)
      .orElseThrow(() -> new ResourceNotFoundException(taskId, ErrorCode.TASK_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));
    Checklist checklist = task.getStage().getChecklist();
    checklistService.validateChecklistModificationState(checklist.getId(), checklist.getState());
    checklistService.validateIfUserIsAuthorForPrototype(checklist.getId(), principalUser.getId());

    task.setSoloTask(false);
    task.setModifiedBy(principalUserEntity);

    return taskMapper.toDto(taskRepository.save(task));
  }


  private void validateTimerRequest(TimerRequest timerRequest, Long taskId) throws StreemException {
    Long minPeriod = timerRequest.getMinPeriod();
    Long maxPeriod = timerRequest.getMaxPeriod();

    switch (timerRequest.getTimerOperator()) {
      case LESS_THAN:
        if (maxPeriod <= 0) {
          ValidationUtils.invalidate(taskId, ErrorCode.TIMED_TASK_LT_TIMER_CANNOT_BE_ZERO);
        }
        break;
      case NOT_LESS_THAN:
        if (minPeriod <= 0) {
          ValidationUtils.invalidate(taskId, ErrorCode.TIMED_TASK_NLT_MIN_PERIOD_CANNOT_BE_ZERO);
        }
          if (minPeriod >= maxPeriod && maxPeriod!=0) {
            ValidationUtils.invalidate(taskId, ErrorCode.TIMED_TASK_NLT_MAX_PERIOD_SHOULD_BE_GT_MIN_PERIOD);
          }
    }
  }

  private void validateTaskSchedulesRequest(TaskSchedulesRequest taskSchedulesRequest, Long taskId) throws StreemException {
    JsonNode startDateDuration = taskSchedulesRequest.getStartDateDuration();
    JsonNode dueDateDuration = taskSchedulesRequest.getDueDateDuration();

    try {
      JsonUtils.readValue(startDateDuration.toString(), DateDuration.class);
      JsonUtils.readValue(dueDateDuration.toString(), DateDuration.class);
    } catch (JsonProcessingException e) {
      ValidationUtils.invalidate(taskId, ErrorCode.TASK_SCHEDULES_REQUEST_INVALID);
    }
    if (Utility.isEmpty(taskSchedulesRequest.getDueDateInterval()) || Utility.isEmpty(taskSchedulesRequest.getStartDateInterval()) || Utility.isEmpty(taskSchedulesRequest.getCondition())) {
      ValidationUtils.invalidate(taskId, ErrorCode.TASK_SCHEDULES_REQUEST_INVALID);
    }

  }
}
