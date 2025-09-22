package com.leucine.streem.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.leucine.streem.constant.*;
import com.leucine.streem.dto.*;
import com.leucine.streem.dto.mapper.IAutomationMapper;
import com.leucine.streem.dto.mapper.IChecklistMapper;
import com.leucine.streem.dto.mapper.IEffectMapper;
import com.leucine.streem.dto.request.*;
import com.leucine.streem.dto.response.ErrorCode;
import com.leucine.streem.exception.ExceptionType;
import com.leucine.streem.exception.ResourceNotFoundException;
import com.leucine.streem.exception.StreemException;
import com.leucine.streem.model.*;
import com.leucine.streem.model.Action;
import com.leucine.streem.model.helper.BaseEntity;
import com.leucine.streem.model.helper.ChecklistRevisionHelper;
import com.leucine.streem.model.helper.PrincipalUser;
import com.leucine.streem.model.helper.parameter.*;
import com.leucine.streem.repository.*;
import com.leucine.streem.service.*;
import com.leucine.streem.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
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
public class ChecklistRevisionService implements IChecklistRevisionService {
  private final IAutomationMapper automationMapper;
  private final IAutomationRepository automationRepository;
  private final IChecklistAuditService checklistAuditService;
  private final IChecklistMapper checklistMapper;
  private final IChecklistRepository checklistRepository;
  private final ICodeService codeService;
  private final IParameterRepository parameterRepository;
  private final ITaskAutomationMappingRepository taskAutomationMappingRepository;
  private final IUserRepository userRepository;
  private final IVersionService versionService;
  private final IVersionRepository versionRepository;
  private final ITrainedUserTaskMappingRepository trainedUsersTaskMappingRepository;
  private final ITaskRepository taskRepository;
  private final ITaskSchedulesRepository taskSchedulesRepository;
  private final IInterlockService interlockService;
  private final ITaskRecurrenceRepository taskRecurrenceRepository;
  private final ITaskDependencyService taskDependencyService;
  private final ITrainedUserRepository trainedUsersRepository;
  private final ITaskExecutorLockRepository taskExecutorLockRepository;
  private final ITrainedUserTaskMappingRepository trainedUserTaskMappingRepository;
  private final IActionRepository actionRepository;
  private final IEffectRepository effectRepository;
  private final IActionService actionService;
  private final IEffectMapper effectMapper;
  private final IFacilityRepository facilityRepository;
  private final IActionFacilityRepository actionFacilityRepository;

  @Transactional(rollbackFor = Exception.class)
  @Override
  public ChecklistDto createChecklistRevision(Long checklistId) throws ResourceNotFoundException, StreemException, IOException {
    log.info("[createChecklistRevision] Request to create revision of checklist, checklistId: {}", checklistId);
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    User principalUserEntity = userRepository.getReferenceById(principalUser.getId());
    Checklist parentChecklist = checklistRepository.findById(checklistId)
      .orElseThrow(() -> new ResourceNotFoundException(checklistId, ErrorCode.PROCESS_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));
    Checklist revisedChecklist = new Checklist();
    Long id = IdGenerator.getInstance().nextId();
    revisedChecklist.setId(id);
    revisedChecklist.setName(parentChecklist.getName());
    revisedChecklist.setDescription(parentChecklist.getDescription());
    revisedChecklist.setOrganisation(parentChecklist.getOrganisation());
    revisedChecklist.setUseCase(parentChecklist.getUseCase());
    revisedChecklist.setCreatedBy(principalUserEntity);
    revisedChecklist.setModifiedBy(principalUserEntity);
    revisedChecklist.setState(State.Checklist.BEING_BUILT);
    revisedChecklist.setCode(codeService.getCode(Type.EntityType.CHECKLIST, principalUser.getOrganisationId()));
    revisedChecklist.setGlobal(parentChecklist.isGlobal());
    revisedChecklist.addFacility(parentChecklist.getFacilities().stream().map(ChecklistFacilityMapping::getFacility).collect(Collectors.toSet()), principalUserEntity);
    revisedChecklist.setColorCode(parentChecklist.getColorCode());

    ChecklistRevisionHelper checklistRevisionHelper = new ChecklistRevisionHelper();

    reviseStages(parentChecklist, revisedChecklist, checklistRevisionHelper, principalUserEntity);
    addPrimaryAuthor(revisedChecklist, principalUserEntity);

    for (ChecklistPropertyValue checklistPropertyValue : parentChecklist.getChecklistPropertyValues()) {
      Property property = checklistPropertyValue.getFacilityUseCasePropertyMapping().getProperty();
      if (!property.isArchived()) {
        revisedChecklist.addProperty(checklistPropertyValue.getFacilityUseCasePropertyMapping(), checklistPropertyValue.getValue(), principalUserEntity);
      }
    }

    reviseProcessParameters(parentChecklist, revisedChecklist, checklistRevisionHelper, principalUserEntity);

    revisedChecklist = checklistRepository.save(revisedChecklist);

    reviseDataForTheRevisedCalculationParameters(checklistRevisionHelper);
    reviseParameterValidations(checklistRevisionHelper);
    reviseParameterAutoInitialize(checklistRevisionHelper);
    reviseParameterRules(checklistRevisionHelper);
    reviseTrainedUsers(parentChecklist.getId(), revisedChecklist, checklistRevisionHelper, principalUser.getCurrentFacilityId());
    reviseTaskAutomations(checklistRevisionHelper, principalUserEntity);
    reviseTaskDependencies(checklistRevisionHelper);
    reviseResourceParameterFilters(checklistRevisionHelper);
    reviseTaskSchedules(parentChecklist, checklistRevisionHelper, principalUserEntity);
    reviseTaskRecurrence(parentChecklist, checklistRevisionHelper, principalUserEntity);
    reviseTaskExecutorLocks(checklistRevisionHelper);
    reviseDataOfParameterHavingParameterizedLeastCount(checklistRevisionHelper);
    reviseResourceParameterValidations(checklistRevisionHelper);
    reviseActions(parentChecklist, revisedChecklist, checklistRevisionHelper, principalUserEntity);
    reviseTaskInterlocks(checklistRevisionHelper, principalUserEntity);


    Version version = versionService.createNewVersionFromParent(revisedChecklist.getId(), Type.EntityType.CHECKLIST, parentChecklist.getVersion(), parentChecklist.getId());
    revisedChecklist.setVersion(version);
    checklistRepository.save(revisedChecklist);

    List<Parameter> revisedParameters = new ArrayList<>(checklistRevisionHelper.getRevisedParameters().values());
    parameterRepository.saveAll(revisedParameters);


    checklistAuditService.revise(parentChecklist.getId(), parentChecklist.getCode(), revisedChecklist.getId(), revisedChecklist.getCode(), principalUser);

    return checklistMapper.toDto(revisedChecklist);
  }

  @Override
  public BasicDto validateIfCurrentUserCanReviseChecklist(Long checklistId) throws ResourceNotFoundException, StreemException {
    PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    User principalUserEntity = userRepository.getReferenceById(principalUser.getId());
    Checklist parentChecklist = checklistRepository.findById(checklistId).orElseThrow(() -> new ResourceNotFoundException(checklistId, ErrorCode.PROCESS_NOT_FOUND, ExceptionType.ENTITY_NOT_FOUND));
    versionService.validateForChecklistRevision(parentChecklist);
    boolean wasAprroverOrReviewer = versionRepository.wasUserRestrictedFromRecallingOrRevisingChecklist(principalUserEntity.getId(), checklistId);
    if (wasAprroverOrReviewer) {
      ValidationUtils.invalidate(checklistId, ErrorCode.APPROVER_REVIEWER_CANNOT_REVISION);
    }
    var basicDto = new BasicDto();
    basicDto.setMessage("success");
    return basicDto;
  }

  private void reviseTaskExecutorLocks(ChecklistRevisionHelper checklistRevisionHelper) {
    Map<Long, Task> oldTaskIdAndNewTaskMap = checklistRevisionHelper.getOldTaskIdAndNewTaskMapping();
    Set<Long> taskIdsToUpdate = new HashSet<>();
    for (Map.Entry<Long, Task> oldTaskIdAndNewTask : oldTaskIdAndNewTaskMap.entrySet()) {
      List<TaskExecutorLock> oldTaskExecutorLocks = taskExecutorLockRepository.findAllByTaskId(oldTaskIdAndNewTask.getKey());
      List<TaskExecutorLock> taskExecutorLockList = new ArrayList<>();

      for (TaskExecutorLock oldTaskExecutorLock : oldTaskExecutorLocks) {
        TaskExecutorLock taskExecutorLock = new TaskExecutorLock();
        Task newTask = oldTaskIdAndNewTaskMap.get(oldTaskExecutorLock.getTask().getId());
        taskExecutorLock.setTask(newTask);
        taskExecutorLock.setReferencedTask(oldTaskIdAndNewTaskMap.get(oldTaskExecutorLock.getReferencedTask().getId()));
        taskExecutorLock.setCreatedBy(oldTaskExecutorLock.getCreatedBy());
        taskExecutorLock.setModifiedBy(oldTaskExecutorLock.getModifiedBy());
        taskExecutorLock.setCreatedAt(oldTaskExecutorLock.getCreatedAt());
        taskExecutorLock.setModifiedAt(oldTaskExecutorLock.getModifiedAt());
        taskExecutorLock.setLockType(oldTaskExecutorLock.getLockType());
        taskExecutorLockList.add(taskExecutorLock);

        newTask.setHasExecutorLock(true);
        taskIdsToUpdate.add(newTask.getId());
      }
      if (!taskExecutorLockList.isEmpty()) {
        taskExecutorLockRepository.saveAll(taskExecutorLockList);
      }

      if (!taskIdsToUpdate.isEmpty()) {
        taskRepository.updateHasExecutorLockByIds(taskIdsToUpdate, true);
      }
    }
  }

  private void reviseTaskInterlocks(ChecklistRevisionHelper checklistRevisionHelper, User principalUserEntity) throws StreemException, ResourceNotFoundException, JsonProcessingException {
    Map<Long, Task> oldTaskIdAndNewTaskMap = checklistRevisionHelper.getOldTaskIdAndNewTaskMapping();
    Map<Task, InterlockDto> oldTaskAndOldInterlockMap = checklistRevisionHelper.getOldTaskAndOldInterlockMap();
    Map<Long, Long> revisedParametersOldAndNewIdMap = checklistRevisionHelper.getRevisedParametersOldAndNewIdMap();
    for (Task oldTask : oldTaskAndOldInterlockMap.keySet()) {
      if (!oldTask.isArchived()) {
        Task newTask = oldTaskIdAndNewTaskMap.get(oldTask.getId());
        InterlockDto interlockDto = oldTaskAndOldInterlockMap.get(oldTask);
        InterlockRequest interlockRequest = new InterlockRequest();

        if (!Utility.isEmpty(interlockDto.getValidations())) {
          InterlockValidationDto interlockValidation = JsonUtils.readValue(interlockDto.getValidations().toString(), InterlockValidationDto.class);
          if (!Utility.isEmpty(interlockValidation.getResourceParameterValidations())) {
            for (InterlockResourcePropertyValidationDto validation : interlockValidation.getResourceParameterValidations()) {
              validation.setParameterId(String.valueOf(revisedParametersOldAndNewIdMap.get(Long.valueOf(validation.getParameterId()))));
            }
          }
          interlockRequest.setValidations(JsonUtils.valueToNode(interlockValidation));
          interlockService.addInterlockForTask(newTask, interlockRequest, principalUserEntity);
        }
      }
    }
  }

  private void reviseTaskDependencies(ChecklistRevisionHelper checklistRevisionHelper) {
    Map<Long, Task> oldTaskIdAndNewTaskMap = checklistRevisionHelper.getOldTaskIdAndNewTaskMapping();

    for (Map.Entry<Long, Task> oldTaskIdAndNewTask : oldTaskIdAndNewTaskMap.entrySet()) {
      Task newTask = oldTaskIdAndNewTask.getValue();
      if (!Utility.isEmpty(newTask)) {
        List<Long> taskDependencyIds = taskDependencyService.getTaskDependenciesByTaskId(oldTaskIdAndNewTask.getKey());
        List<Long> newPrerequisiteIds = taskDependencyIds.stream().map(oldTaskIdAndNewTaskMap::get).map(BaseEntity::getId).toList();
        if (!Utility.isEmpty(newPrerequisiteIds)) {
          TaskDependencyRequest taskDependencyRequest = TaskDependencyRequest.builder().prerequisiteTaskIds(newPrerequisiteIds).build();
          taskDependencyRequest.setPrerequisiteTaskIds(newPrerequisiteIds);
          taskDependencyService.updateTaskDependency(newTask.getId(), taskDependencyRequest);
        }
      }
    }
  }

  private void reviseTaskRecurrence(Checklist parentChecklist, ChecklistRevisionHelper checklistRevisionHelper, User principalUserEntity) {
    List<Task> oldRecurrenceTasks = taskRepository.findAllTaskByEnableRecurrenceAndChecklistId(parentChecklist.getId(), true);
    List<TaskRecurrence> oldTaskRecurrences = taskRecurrenceRepository.findAllById(oldRecurrenceTasks.stream()
      .filter(task -> !task.isArchived())
      .map(Task::getTaskRecurrenceId)
      .collect(Collectors.toList()));
    Map<Long, TaskRecurrence> oldTaskIdAndTaskRecurrenceMap = oldTaskRecurrences.stream().collect(Collectors.toMap(BaseEntity::getId, Function.identity()));

    Map<Long, Task> oldTaskIdNewTaskIdMapping = checklistRevisionHelper.getOldTaskIdAndNewTaskMapping();

    List<TaskRecurrence> newTaskRecurrenceList = new ArrayList<>();
    Map<Long, TaskRecurrence> taskIdToRecurrenceMap = new HashMap<>();
    for (Task task : oldRecurrenceTasks) {

      TaskRecurrence taskRecurrences = oldTaskIdAndTaskRecurrenceMap.get(task.getTaskRecurrenceId());
      TaskRecurrence newTaskRecurrence = new TaskRecurrence();
      newTaskRecurrence.setStartDateInterval(taskRecurrences.getStartDateInterval());
      newTaskRecurrence.setStartDateDuration(taskRecurrences.getStartDateDuration());
      newTaskRecurrence.setDueDateInterval(taskRecurrences.getDueDateInterval());
      newTaskRecurrence.setDueDateDuration(taskRecurrences.getDueDateDuration());
      newTaskRecurrence.setPositiveStartDateToleranceInterval(taskRecurrences.getPositiveStartDateToleranceInterval());
      newTaskRecurrence.setPositiveStartDateToleranceDuration(taskRecurrences.getPositiveStartDateToleranceDuration());
      newTaskRecurrence.setNegativeStartDateToleranceInterval(taskRecurrences.getNegativeStartDateToleranceInterval());
      newTaskRecurrence.setNegativeStartDateToleranceDuration(taskRecurrences.getNegativeStartDateToleranceDuration());
      newTaskRecurrence.setPositiveDueDateToleranceInterval(taskRecurrences.getPositiveDueDateToleranceInterval());
      newTaskRecurrence.setPositiveDueDateToleranceDuration(taskRecurrences.getPositiveDueDateToleranceDuration());
      newTaskRecurrence.setNegativeDueDateToleranceInterval(taskRecurrences.getNegativeDueDateToleranceInterval());
      newTaskRecurrence.setNegativeDueDateToleranceDuration(taskRecurrences.getNegativeDueDateToleranceDuration());

      newTaskRecurrence.setModifiedBy(principalUserEntity);
      newTaskRecurrence.setCreatedBy(principalUserEntity);
      newTaskRecurrenceList.add(newTaskRecurrence);
      taskIdToRecurrenceMap.put(task.getId(), newTaskRecurrence);


    }
    taskRecurrenceRepository.saveAll(newTaskRecurrenceList);

    List<Task> tasksToUpdate = new ArrayList<>();
    for (Task task : oldRecurrenceTasks) {
      if (!task.isArchived()) {
        TaskRecurrence savedRecurrence = taskIdToRecurrenceMap.get(task.getId());
        if (savedRecurrence != null) {
          Task newTask = oldTaskIdNewTaskIdMapping.get(task.getId());
          newTask.setTaskRecurrence(savedRecurrence);
          tasksToUpdate.add(newTask);
        }
      }
    }
    taskRepository.saveAll(tasksToUpdate);
  }


  private void reviseTaskSchedules(Checklist parentChecklist, ChecklistRevisionHelper checklistRevisionHelper, User principalUserEntity) {
    List<Task> oldScheduledTasks = taskRepository.findAllTaskByEnableSchedulingAndChecklistId(parentChecklist.getId(), true);
    List<TaskSchedules> oldTaskSchedules = taskSchedulesRepository.findAllById(oldScheduledTasks.stream()
      .filter(task -> !task.isArchived())
      .map(Task::getTaskSchedulesId)
      .collect(Collectors.toList()));
    Map<Long, TaskSchedules> oldTaskIdAndTaskScheduleMap = oldTaskSchedules.stream().collect(Collectors.toMap(BaseEntity::getId, Function.identity()));

    Map<Long, Task> oldTaskIdNewTaskIdMapping = checklistRevisionHelper.getOldTaskIdAndNewTaskMapping();

    List<TaskSchedules> newTaskSchedulesList = new ArrayList<>();
    Map<Long, TaskSchedules> taskIdToSchedulesMap = new HashMap<>();

    for (Task task : oldScheduledTasks) {

      TaskSchedules taskSchedules = oldTaskIdAndTaskScheduleMap.get(task.getTaskSchedulesId());

      if (!Utility.isEmpty(taskSchedules)) {
        TaskSchedules newTaskSchedules = new TaskSchedules();
        newTaskSchedules.setType(taskSchedules.getType());
        newTaskSchedules.setCondition(taskSchedules.getCondition());
        newTaskSchedules.setModifiedBy(principalUserEntity);
        newTaskSchedules.setCreatedBy(principalUserEntity);
        newTaskSchedules.setStartDateInterval(taskSchedules.getStartDateInterval());
        newTaskSchedules.setStartDateDuration(taskSchedules.getStartDateDuration());
        newTaskSchedules.setDueDateInterval(taskSchedules.getDueDateInterval());
        newTaskSchedules.setDueDateDuration(taskSchedules.getDueDateDuration());

        if (!Utility.isEmpty(taskSchedules.getReferencedTaskId())) {
          newTaskSchedules.setReferencedTask(oldTaskIdNewTaskIdMapping.get(taskSchedules.getReferencedTaskId()));
        }

        newTaskSchedulesList.add(newTaskSchedules);
        taskIdToSchedulesMap.put(task.getId(), newTaskSchedules);
        
      }
    }

    taskSchedulesRepository.saveAll(newTaskSchedulesList);

    List<Task> tasksToUpdate = new ArrayList<>();
    for (Task task : oldScheduledTasks) {
      if (!task.isArchived()) {
        TaskSchedules savedSchedules = taskIdToSchedulesMap.get(task.getId());
        if (savedSchedules != null) {
          Task newTask = oldTaskIdNewTaskIdMapping.get(task.getId());
          newTask.setTaskSchedules(savedSchedules);
          tasksToUpdate.add(newTask);
        }
      }
    }
    taskRepository.saveAll(tasksToUpdate);
  }

  /**
   * This method adds default users to the revised checklist from existing checklist
   *
   * @param parentChecklistId       the id of the checklist whose revision is being created
   * @param revisedChecklist        the revised checklist
   * @param checklistRevisionHelper helper object to store all the required data
   */
  //TODO: Refactor this method
  private void reviseTrainedUsers(Long parentChecklistId, Checklist revisedChecklist, ChecklistRevisionHelper checklistRevisionHelper, Long currentFacilityId) {
    Map<Long, Task> oldTaskIdNewTaskIdMapping = checklistRevisionHelper.getOldTaskIdAndNewTaskMapping();
    List<TrainedUserTaskMapping> parentTrainedChecklistDefaultUsers = trainedUsersTaskMappingRepository.findByFacilityIdAndChecklistId(currentFacilityId, parentChecklistId);
    List<TrainedUser> trainedUsers = trainedUsersRepository.findAllByChecklistIdAndFacilityId(parentChecklistId, currentFacilityId);
    Set<Long> taskIds = oldTaskIdNewTaskIdMapping.values().stream().map(BaseEntity::getId).collect(Collectors.toSet());

    Map<Long, Task> revisedTaskMap = taskRepository.findAllByIdInAndArchived(taskIds, false)
      .stream().collect(Collectors.toMap(BaseEntity::getId, Function.identity()));

    List<TrainedUserTaskMapping> revisedTrainedChecklistDefaultUsers = new ArrayList<>();
    List<TrainedUser> trainedUserList = new ArrayList<>();
    Map<Long, List<TrainedUser>> taskAndTrainedUserMap = parentTrainedChecklistDefaultUsers.stream()
      .collect(Collectors.groupingBy(trainedUserTaskMapping -> trainedUserTaskMapping.getTask().getId(), Collectors.mapping(TrainedUserTaskMapping::getTrainedUser, Collectors.toList())));

    Map<Long, TrainedUser> oldTrainedUserIdNewTrainedUserMap = new HashMap<>();
    for (TrainedUser trainedUser : trainedUsers) {
      if (trainedUser.getUser() != null || trainedUser.getUserGroup() != null) {
        TrainedUser newTrainedUser = new TrainedUser();
        newTrainedUser.setId(IdGenerator.getInstance().nextId());
        newTrainedUser.setChecklist(revisedChecklist);
        newTrainedUser.setFacility(trainedUser.getFacility());
        newTrainedUser.setUser(trainedUser.getUser());
        newTrainedUser.setUserGroup(trainedUser.getUserGroup());
        newTrainedUser.setCreatedAt(trainedUser.getCreatedAt());
        newTrainedUser.setModifiedAt(trainedUser.getModifiedAt());
        newTrainedUser.setCreatedBy(trainedUser.getCreatedBy());
        newTrainedUser.setModifiedBy(trainedUser.getModifiedBy());
        trainedUserList.add(newTrainedUser);
        oldTrainedUserIdNewTrainedUserMap.put(trainedUser.getId(), newTrainedUser);
      }
    }
    trainedUsersRepository.saveAll(trainedUserList);

    for (TrainedUserTaskMapping trainedUserTaskMapping : parentTrainedChecklistDefaultUsers) {
      User user = trainedUserTaskMapping.getTrainedUser().getUser();
      UserGroup userGroup = trainedUserTaskMapping.getTrainedUser().getUserGroup();
      if (!Utility.isEmpty(user)) {
        if (!user.isArchived()) {
          Task newTask = oldTaskIdNewTaskIdMapping.get(trainedUserTaskMapping.getTask().getId());
          if (newTask != null) {
            Long newTaskId = oldTaskIdNewTaskIdMapping.get(trainedUserTaskMapping.getTaskId()).getId();
            Task task = revisedTaskMap.get(newTaskId);

            if (task != null) {
              if (!user.isArchived()) {
                TrainedUser trainedUser = oldTrainedUserIdNewTrainedUserMap.getOrDefault(trainedUserTaskMapping.getTrainedUser().getId(), null);
                if (!Utility.isEmpty(trainedUser) && !Utility.isEmpty(trainedUser.getUser())) {
                  //TODO:change
                  TrainedUserTaskMapping tutm = new TrainedUserTaskMapping();
                  tutm.setId(IdGenerator.getInstance().nextId());
                  tutm.setTask(task);
                  tutm.setTrainedUser(trainedUser);
                  tutm.setCreatedBy(trainedUser.getCreatedBy());
                  tutm.setModifiedBy(trainedUser.getModifiedBy());
                  tutm.setCreatedAt(DateTimeUtils.now());
                  tutm.setModifiedAt(DateTimeUtils.now());
                  revisedTrainedChecklistDefaultUsers.add(tutm);
                }
              }
            }
          }
        }
      }
      if (!Utility.isEmpty(userGroup)) {
        if (userGroup.isActive()) {
          Long newTaskId = oldTaskIdNewTaskIdMapping.get(trainedUserTaskMapping.getTaskId()).getId();
          Task task = revisedTaskMap.get(newTaskId);
          if (task != null) {
            TrainedUser trainedUser = oldTrainedUserIdNewTrainedUserMap.getOrDefault(trainedUserTaskMapping.getTrainedUser().getId(), null);
            if (!Utility.isEmpty(trainedUser) && !Utility.isEmpty(trainedUser.getUserGroup())) {
              TrainedUserTaskMapping tutm = new TrainedUserTaskMapping();
              tutm.setId(IdGenerator.getInstance().nextId());
              tutm.setTask(task);
              tutm.setTrainedUser(trainedUser);
              tutm.setCreatedBy(trainedUser.getCreatedBy());
              //TODO:change
              tutm.setModifiedBy(trainedUser.getModifiedBy());
              tutm.setCreatedAt(DateTimeUtils.now());
              tutm.setModifiedAt(DateTimeUtils.now());
              revisedTrainedChecklistDefaultUsers.add(tutm);
            }
          }
        }

      }
    }

    trainedUsersTaskMappingRepository.saveAll(revisedTrainedChecklistDefaultUsers);
  }


  /**
   * This method creates copies of process stages and adds them to the revised checklist
   *
   * @param checklistRevisionOf     the checklist whose revision is being created
   * @param revisedChecklist        the revised checklist
   * @param checklistRevisionHelper helper object to store all the required data
   * @param principalUserEntity     the user who is creating the revision
   */
  private void reviseStages(Checklist checklistRevisionOf, Checklist revisedChecklist,
                            ChecklistRevisionHelper checklistRevisionHelper, User principalUserEntity) throws JsonProcessingException {
    Map<Long, Long> revisedStageOldIdAndNewIdMap = checklistRevisionHelper.getOldStageIdNewStageIdMapping();

    for (Stage stageRevisionOf : checklistRevisionOf.getStages()) {
      Stage revisedStage = new Stage();
      revisedStage.setId(IdGenerator.getInstance().nextId());
      revisedStage.setOrderTree(stageRevisionOf.getOrderTree());
      revisedStage.setName(stageRevisionOf.getName());
      revisedStage.setModifiedBy(principalUserEntity);
      revisedStage.setCreatedBy(principalUserEntity);
      revisedChecklist.addStage(revisedStage);

      reviseTasks(revisedChecklist.getId(), stageRevisionOf, revisedStage, checklistRevisionHelper, principalUserEntity);
      revisedStageOldIdAndNewIdMap.put(stageRevisionOf.getId(), revisedStage.getId());

    }
  }

  /**
   * This method creates copies of process tasks and adds them to the revised checklist
   *
   * @param revisedChecklistId      the id of the revised checklist
   * @param stageRevisionOf         the stage whose revision is being created
   * @param revisedStage            the revised stage
   * @param checklistRevisionHelper helper object to store all the required data
   * @param principalUserEntity     the user who is creating the revision
   */
  private void reviseTasks(Long revisedChecklistId, Stage stageRevisionOf, Stage revisedStage,
                           ChecklistRevisionHelper checklistRevisionHelper, User principalUserEntity) throws JsonProcessingException {
    // Data from helper object
    List<Task> taskHavingAutomations = checklistRevisionHelper.getTaskHavingAutomations();
    Map<Long, Task> oldTaskIdNewTaskIdMapping = checklistRevisionHelper.getOldTaskIdAndNewTaskMapping();
    Map<Task, InterlockDto> oldTaskAndOldInterlockMap = checklistRevisionHelper.getOldTaskAndOldInterlockMap();

    for (Task taskRevisionOf : stageRevisionOf.getTasks()) {
      Task revisedTask = new Task();
      revisedTask.setId(IdGenerator.getInstance().nextId());
      revisedTask.setOrderTree(taskRevisionOf.getOrderTree());
      revisedTask.setName(taskRevisionOf.getName());
      revisedTask.setTimed(taskRevisionOf.isTimed());
      revisedTask.setTimerOperator(taskRevisionOf.getTimerOperator());
      revisedTask.setMinPeriod(taskRevisionOf.getMinPeriod());
      revisedTask.setMaxPeriod(taskRevisionOf.getMaxPeriod());
      revisedTask.setHasStop(taskRevisionOf.isHasStop());
      revisedTask.setSoloTask(taskRevisionOf.isSoloTask());
      revisedTask.setModifiedBy(principalUserEntity);
      revisedTask.setEnableRecurrence(taskRevisionOf.isEnableRecurrence());
      revisedTask.setEnableScheduling(taskRevisionOf.isEnableScheduling());
      revisedTask.setCreatedBy(principalUserEntity);
      revisedTask.setHasBulkVerification(taskRevisionOf.isHasBulkVerification());
      revisedTask.setHasInterlocks(taskRevisionOf.isHasInterlocks());
      revisedStage.addTask(revisedTask);

      if (!Utility.isEmpty(taskRevisionOf.getAutomations())) {
        taskHavingAutomations.add(taskRevisionOf);
      }
      try {
        InterlockDto interlock = interlockService.getInterlockByTaskId(taskRevisionOf, principalUserEntity);
        oldTaskAndOldInterlockMap.put(taskRevisionOf, interlock);
      } catch (StreemException | JsonProcessingException | ResourceNotFoundException exception) {
        log.error("Error while getting interlock for task during revision process. Error: {} ", exception.getMessage());
      }

      reviseTaskMedias(taskRevisionOf, revisedTask, principalUserEntity);
      reviseParameters(revisedChecklistId, taskRevisionOf, revisedTask, checklistRevisionHelper, principalUserEntity);

      oldTaskIdNewTaskIdMapping.put(taskRevisionOf.getId(), revisedTask);
    }
  }

  /**
   * This method creates copies of task medias and adds them to the revised task
   *
   * @param taskRevisionOf the task whose revision is being created
   * @param revisedTask    the revised task
   * @param principalUser  the user who is creating the revision
   */
  private void reviseTaskMedias(Task taskRevisionOf, Task revisedTask, User principalUser) {
    for (TaskMediaMapping taskMediaMapping : taskRevisionOf.getMedias()) {
      revisedTask.addMedia(taskMediaMapping.getMedia(), principalUser);
    }
  }

  /**
   * This method creates copies of process parameters and adds them to the revised checklist
   *
   * @param revisedChecklistId      the id of the revised checklist
   * @param taskRevisionOf          the task whose revision is being created
   * @param revisedTask             the revised task
   * @param checklistRevisionHelper helper object to store all the required data
   * @param principalUserEntity     the user who is creating the revision
   */
  private void reviseParameters(Long revisedChecklistId, Task taskRevisionOf, Task revisedTask, ChecklistRevisionHelper checklistRevisionHelper, User principalUserEntity) throws JsonProcessingException {
    Map<Long, Task> existingParameterRevisedTaskMap = checklistRevisionHelper.getExistingParameterRevisedTaskMap();
    List<Long> existingCalculationParameterIdList = checklistRevisionHelper.getExistingCalculationParameterIdList();
    Map<Long, Long> revisedParametersOldAndNewIdMap = checklistRevisionHelper.getRevisedParametersOldAndNewIdMap();
    List<Parameter> existingParameterHavingAutoInitialize = checklistRevisionHelper.getExistingParameterHavingAutoInitialize();
    List<Parameter> existingParameterHavingRules = checklistRevisionHelper.getExistingParameterHavingRules();
    List<Parameter> existingResourceParameters = checklistRevisionHelper.getExistingResourceParameters();
    List<Parameter> existingParameterHavingValidations = checklistRevisionHelper.getExistingParameterHavingValidations();
    List<Parameter> existingParameterHavingParameterizedLeastCount = checklistRevisionHelper.getExistingParameterHavingParameterizedLeastCount();

    Map<Long, Parameter> revisedParameters = checklistRevisionHelper.getRevisedParameters();

    for (var parameterRevisionOf : taskRevisionOf.getParameters()) {

      if (Type.Parameter.CALCULATION.equals(parameterRevisionOf.getType())) {
        // all the calculation parameters for the revised checklist are created at the end
        // this is because there may be parameters (parameter, number) inside calculation parameter which positioned in the later stages or tasks
        // so we create revision for them first and copy calculation parameters data in the end using this list
        existingCalculationParameterIdList.add(parameterRevisionOf.getId());
      }

      if (isParameterizedLeastCount(parameterRevisionOf)) {
        existingParameterHavingParameterizedLeastCount.add(parameterRevisionOf);
      }


      Parameter revisedParameter = new Parameter();
      Long newId = IdGenerator.getInstance().nextId();
      revisedParameter.setId(newId);
      revisedParameter.setChecklistId(revisedChecklistId);
      revisedParameter.setOrderTree(parameterRevisionOf.getOrderTree());
      revisedParameter.setMandatory(parameterRevisionOf.isMandatory());
      revisedParameter.setType(parameterRevisionOf.getType());
      revisedParameter.setVerificationType(parameterRevisionOf.getVerificationType());
      revisedParameter.setAutoInitialize(parameterRevisionOf.getAutoInitialize());
      revisedParameter.setHidden(parameterRevisionOf.isHidden());
      revisedParameter.setAutoInitialized(parameterRevisionOf.isAutoInitialized());
      revisedParameter.setTargetEntityType(parameterRevisionOf.getTargetEntityType());

      if (Utility.isEmpty(parameterRevisionOf.getVerificationType())) {
        revisedParameter.setVerificationType(Type.VerificationType.NONE);
      } else {
        revisedParameter.setVerificationType(parameterRevisionOf.getVerificationType());
      }
      if (revisedParameter.getType().equals(Type.Parameter.RESOURCE) || revisedParameter.getType().equals(Type.Parameter.MULTI_RESOURCE)) {
        existingResourceParameters.add(revisedParameter);
      }

      revisedParameter.setData(parameterRevisionOf.getData());
      revisedParameter.setMetadata(parameterRevisionOf.getMetadata());
      revisedParameter.setLabel(parameterRevisionOf.getLabel());
      revisedParameter.setModifiedBy(principalUserEntity);
      revisedParameter.setCreatedBy(principalUserEntity);
      //TODO: it will break for rules
      revisedParameter.setValidations(JsonUtils.valueToNode(new ArrayList<>()));
      revisedParameter.setMetadata(parameterRevisionOf.getMetadata());

      revisedParametersOldAndNewIdMap.put(parameterRevisionOf.getId(), newId);
      existingParameterRevisedTaskMap.put(parameterRevisionOf.getId(), revisedTask);

      if (Type.Parameter.MATERIAL.equals(parameterRevisionOf.getType())) {
        for (ParameterMediaMapping parameterMediaMapping : parameterRevisionOf.getMedias()) {
          if (!parameterMediaMapping.isArchived()) {
            revisedParameter.addMedia(parameterMediaMapping.getMedia(), principalUserEntity);
            if (Utility.isEmpty(parameterRevisionOf.getVerificationType())) {
              revisedParameter.setVerificationType(Type.VerificationType.NONE);
            } else {
              revisedParameter.setVerificationType(parameterRevisionOf.getVerificationType());
            }
          }
        }
      }
      revisedParameters.put(revisedParameter.getId(), revisedParameter);
      revisedTask.addParameter(revisedParameter);

      boolean isParameterHavingValidations = isParameterHavingValidations(parameterRevisionOf);
      if (isParameterHavingValidations) {
        existingParameterHavingValidations.add(parameterRevisionOf);
      }

      if (parameterRevisionOf.isAutoInitialized()) {
        existingParameterHavingAutoInitialize.add(parameterRevisionOf);
      }

      if (!Utility.isEmpty(parameterRevisionOf.getRules())) {
        existingParameterHavingRules.add(parameterRevisionOf);
      }

    }
  }

  /**
   * Checks if the given parameter contains non-empty validations.
   * <p>
   * This method inspects the validations of the provided parameter. It handles both
   * scenarios where the validations are an array or a single JSON object. For arrays,
   * it checks if the array is not empty. For single objects, it checks if the JSON
   * representation is not empty.
   * </p>
   *
   * @param parameterRevisionOf the parameter object containing validations to be checked
   * @return {@code true} if the parameter contains non-empty validations, {@code false} otherwise
   * @throws JsonProcessingException if there is an error processing the JSON representation of validations
   */
  private static boolean isParameterHavingValidations(Parameter parameterRevisionOf) throws JsonProcessingException {
    Object validations = parameterRevisionOf.getValidations();
    boolean isParameterHavingValidations = false;

    if (!Utility.isEmpty(validations)) {
      // Check if validations is an array
      if (validations.getClass().isArray()) {
        isParameterHavingValidations = ((Object[]) validations).length > 0;
      } else {
        // Check if validations is a non-empty JSON object
        JsonNode jsonNode = JsonUtils.valueToNode(validations.toString());
        isParameterHavingValidations = !jsonNode.isEmpty();
      }
    }
    return isParameterHavingValidations;
  }

  /**
   * Revise the data for a NumberParameter. This is used for both task and CJF parameters
   *
   * @param parameterRevisionOf     The original Parameter object containing the NumberParameter data.
   * @param revisedParameter        The revised Parameter object to update with the new NumberParameter data.
   * @param checklistRevisionHelper The checklist revision helper containing the mapping of old and new parameter IDs.
   * @throws JsonProcessingException If there is an error processing the JSON data.
   */
  private void reviseNumberParameterData(Parameter parameterRevisionOf, Parameter revisedParameter, ChecklistRevisionHelper checklistRevisionHelper) throws JsonProcessingException {
    NumberParameter numberParameter = JsonUtils.readValue(parameterRevisionOf.getData().toString(), NumberParameter.class);
    LeastCount leastCount = numberParameter.getLeastCount();
    reviseParameterLeastCount(leastCount, checklistRevisionHelper);
    revisedParameter.setData(JsonUtils.valueToNode(numberParameter));
  }

  /**
   * Revise the data for a ShouldBeParameter.
   *
   * @param parameterRevisionOf     The original Parameter object containing the ShouldBeParameter data.
   * @param revisedParameter        The revised Parameter object to update with the new ShouldBeParameter data.
   * @param checklistRevisionHelper The checklist revision helper containing the mapping of old and new parameter IDs.
   * @throws JsonProcessingException If there is an error processing the JSON data.
   */
  private void reviseShouldBeParameterData(Parameter parameterRevisionOf, Parameter revisedParameter, ChecklistRevisionHelper checklistRevisionHelper) throws JsonProcessingException {
    ShouldBeParameter shouldBeParameter = JsonUtils.readValue(parameterRevisionOf.getData().toString(), ShouldBeParameter.class);
    LeastCount leastCount = shouldBeParameter.getLeastCount();
    reviseParameterLeastCount(leastCount, checklistRevisionHelper);
    revisedParameter.setData(JsonUtils.valueToNode(shouldBeParameter));
  }

  /**
   * Revise the least count for a parameter.
   *
   * @param leastCount              The least count to be revised.
   * @param checklistRevisionHelper The checklist revision helper containing the mapping of old and new parameter IDs.
   */
  private void reviseParameterLeastCount(LeastCount leastCount, ChecklistRevisionHelper checklistRevisionHelper) {
    if (!Utility.isEmpty(leastCount) && leastCount.getSelector().equals(Type.SelectorType.PARAMETER)) {
      leastCount.setReferencedParameterId(String.valueOf(checklistRevisionHelper.getRevisedParametersOldAndNewIdMap().get(Long.valueOf(leastCount.getReferencedParameterId()))));
    }
  }


  /**
   * The calculation parameter have already been revised, now we need to copy the data from the old calculation parameter to the new one.
   * There are parameter ids inside the data field of calculation parameter, we need to replace them with the new ids
   *
   * @param checklistRevisionHelper helper object to store all the required data
   * @throws JsonProcessingException
   */
  private void reviseDataForTheRevisedCalculationParameters(ChecklistRevisionHelper checklistRevisionHelper) throws JsonProcessingException {
    List<Long> existingCalculationParameterList = checklistRevisionHelper.getExistingCalculationParameterIdList();
    Map<Long, Long> revisedParametersOldAndNewIdMap = checklistRevisionHelper.getRevisedParametersOldAndNewIdMap();
    Map<Long, Parameter> revisedParameterMap = checklistRevisionHelper.getRevisedParameters();


    Map<Long, Task> existingParameterRevisedTaskMap = checklistRevisionHelper.getExistingParameterRevisedTaskMap();
    for (Long parameterRevisionOf : existingCalculationParameterList) {
      Long revisedParameterId = revisedParametersOldAndNewIdMap.get(parameterRevisionOf);
      Parameter revisedParameter = revisedParameterMap.get(revisedParameterId);

      CalculationParameter calculationParameter = JsonUtils.readValue(revisedParameter.getData().toString(), CalculationParameter.class);
      CalculationParameter revisedCalculationParameter = new CalculationParameter();
      revisedCalculationParameter.setExpression(calculationParameter.getExpression());
      revisedCalculationParameter.setUom(calculationParameter.getUom());
      revisedCalculationParameter.setPrecision(calculationParameter.getPrecision());

      Map<String, CalculationParameterVariable> newVariables = new HashMap<>();
      if (!Utility.isEmpty(calculationParameter.getVariables())) {
        for (var variableEntrySet : calculationParameter.getVariables().entrySet()) {
          String key = variableEntrySet.getKey();
          CalculationParameterVariable oldVariable = variableEntrySet.getValue();
          Long oldParameterId = Long.valueOf(oldVariable.getParameterId());

          CalculationParameterVariable newVariable = new CalculationParameterVariable();
          Long newParameterId = revisedParametersOldAndNewIdMap.get(Long.valueOf(oldVariable.getParameterId()));
          newVariable.setParameterId(newParameterId.toString());
          // TODO this logic needs to be changed, this check is here because CJF parameters do not have task id
          // TODO instead what we need to do is we check the type of old parameter id
          if (!Utility.isEmpty(existingParameterRevisedTaskMap.get(oldParameterId))) {
            newVariable.setTaskId(existingParameterRevisedTaskMap.get(oldParameterId).getId().toString());
          }

          newVariables.put(key, newVariable);
        }
      }
      revisedCalculationParameter.setVariables(newVariables);

      JsonNode jsonNode = JsonUtils.valueToNode(revisedCalculationParameter);
      revisedParameter.setData(jsonNode);
    }
  }

  /**
   * This method updates the parameter inside the validation json object of the revised parameters
   *
   * @param checklistRevisionHelper helper object to store all the required data
   * @throws JsonProcessingException
   */
  private void reviseParameterValidations(ChecklistRevisionHelper checklistRevisionHelper) throws IOException {
    Map<Long, Long> revisedParameterOldAndNewIdMap = checklistRevisionHelper.getRevisedParametersOldAndNewIdMap();
    Map<Long, Parameter> revisedParameters = checklistRevisionHelper.getRevisedParameters();
    List<Parameter> existingParameterHavingValidations = checklistRevisionHelper.getExistingParameterHavingValidations();


    for (Parameter parameter : existingParameterHavingValidations) {
      Long revisedParameterId = revisedParameterOldAndNewIdMap.get(parameter.getId());
      List<ParameterValidationDto> parameterValidationDtoList = JsonUtils.jsonToCollectionType(parameter.getValidations(), List.class, ParameterValidationDto.class);
      List<ParameterValidationDto> revisedParameterValidationDtoList = new ArrayList<>();

      if (!Utility.isEmpty(parameterValidationDtoList)) {
        for (ParameterValidationDto parameterValidationDto : parameterValidationDtoList) {
          ParameterValidationDto revisedParameterValidationDto = new ParameterValidationDto();

          // Handle Criteria Validations
          List<CriteriaValidationDto> criteriaValidationDtosDNew = processCriteriaValidations(parameterValidationDto, revisedParameterOldAndNewIdMap);

          // Handle Resource Parameter Validations
          List<ResourceParameterPropertyValidationDto> resourceParameterPropertyValidationDtosNew = processResourceParameterValidations(parameterValidationDto, revisedParameterOldAndNewIdMap);

          // Handle Property Validations
          List<ParameterRelationPropertyValidationDto> propertyValidationsDtosNew = processPropertyValidations(parameterValidationDto, revisedParameterOldAndNewIdMap);

          //Handle Date and Date Time Property Validations
          List<DateParameterValidationDto> dateAndDateTImePropertyValidationsNew = processDateAndDateTimeValidations(parameterValidationDto, revisedParameterOldAndNewIdMap);


          revisedParameterValidationDto.setRuleId(Utility.generateUuid());
          revisedParameterValidationDto.setExceptionApprovalType(parameterValidationDto.getExceptionApprovalType());
          revisedParameterValidationDto.setValidationType(parameterValidationDto.getValidationType());
          revisedParameterValidationDto.setCriteriaValidations(criteriaValidationDtosDNew);
          revisedParameterValidationDto.setResourceParameterValidations(resourceParameterPropertyValidationDtosNew);
          revisedParameterValidationDto.setPropertyValidations(propertyValidationsDtosNew);
          revisedParameterValidationDto.setDateTimeParameterValidations(dateAndDateTImePropertyValidationsNew);

          revisedParameterValidationDtoList.add(revisedParameterValidationDto);
        }
      }
      Parameter revisedParameter = revisedParameters.get(revisedParameterId);
      revisedParameter.setValidations(JsonUtils.valueToNode(revisedParameterValidationDtoList));

    }
  }

  /**
   * Processes the criteria validations for a given ParameterValidationDto and maps any
   * necessary parameter IDs using the provided mapping of old to new parameter IDs.
   *
   * @param revisedParameterOldAndNewIdMap A map containing the old and new parameter IDs for remapping values.
   * @return A list of newly created CriteriaValidationDto with updated parameter IDs and values.
   */

  private List<CriteriaValidationDto> processCriteriaValidations(ParameterValidationDto parameterValidationDto, Map<Long, Long> revisedParameterOldAndNewIdMap) {
    List<CriteriaValidationDto> criteriaValidationDtosDNew = new ArrayList<>();

    if (!Utility.isEmpty(parameterValidationDto.getCriteriaValidations())) {
      for (CriteriaValidationDto criteriaValidationDto : parameterValidationDto.getCriteriaValidations()) {
        CriteriaValidationDto newCriteriaValidationDto = new CriteriaValidationDto();

        String valueParameterNewId = getValueFromMapIfNotNull(criteriaValidationDto.getValueParameterId(), revisedParameterOldAndNewIdMap);
        String upperValueParameterNewId = getValueFromMapIfNotNull(criteriaValidationDto.getUpperValueParameterId(), revisedParameterOldAndNewIdMap);
        String lowerValueParameterNewId = getValueFromMapIfNotNull(criteriaValidationDto.getLowerValueParameterId(), revisedParameterOldAndNewIdMap);

        newCriteriaValidationDto.setId(Utility.generateUuid());
        newCriteriaValidationDto.setUom(criteriaValidationDto.getUom());
        newCriteriaValidationDto.setOperator(criteriaValidationDto.getOperator());
        newCriteriaValidationDto.setType(criteriaValidationDto.getType());
        newCriteriaValidationDto.setLowerValue(criteriaValidationDto.getLowerValue());
        newCriteriaValidationDto.setUpperValue(criteriaValidationDto.getUpperValue());
        newCriteriaValidationDto.setValue(criteriaValidationDto.getValue());
        newCriteriaValidationDto.setCriteriaType(criteriaValidationDto.getCriteriaType());
        newCriteriaValidationDto.setUpperValueParameterId(upperValueParameterNewId);
        newCriteriaValidationDto.setLowerValueParameterId(lowerValueParameterNewId);
        newCriteriaValidationDto.setValueParameterId(valueParameterNewId);
        newCriteriaValidationDto.setErrorMessage(criteriaValidationDto.getErrorMessage());

        criteriaValidationDtosDNew.add(newCriteriaValidationDto);
      }
    }

    return criteriaValidationDtosDNew;
  }

  /**
   * Processes the resource parameter validations for a given {@link ParameterValidationDto} and maps
   * any parameter IDs using the provided mapping of old to new parameter IDs.
   *
   * @param revisedParameterOldAndNewIdMap A map containing the old and new parameter IDs for remapping values.
   * @return A list of newly created ResourceParameterPropertyValidationDto with updated parameter IDs and values.
   */
  private List<ResourceParameterPropertyValidationDto> processResourceParameterValidations(ParameterValidationDto parameterValidationDto, Map<Long, Long> revisedParameterOldAndNewIdMap) {
    List<ResourceParameterPropertyValidationDto> resourceParameterPropertyValidationDtosNew = new ArrayList<>();

    if (!Utility.isEmpty(parameterValidationDto.getResourceParameterValidations())) {
      for (ResourceParameterPropertyValidationDto resourceParameterValidation : parameterValidationDto.getResourceParameterValidations()) {
        ResourceParameterPropertyValidationDto newResourceParameterValidation = new ResourceParameterPropertyValidationDto();

        Long linkedParameterNewId = revisedParameterOldAndNewIdMap.get(Long.valueOf(resourceParameterValidation.getParameterId()));
        newResourceParameterValidation.setId(Utility.generateUuid());
        newResourceParameterValidation.setParameterId(String.valueOf(linkedParameterNewId));
        newResourceParameterValidation.setPropertyId(resourceParameterValidation.getPropertyId());
        newResourceParameterValidation.setPropertyExternalId(resourceParameterValidation.getPropertyExternalId());
        newResourceParameterValidation.setPropertyDisplayName(resourceParameterValidation.getPropertyDisplayName());
        newResourceParameterValidation.setPropertyInputType(resourceParameterValidation.getPropertyInputType());
        newResourceParameterValidation.setConstraint(resourceParameterValidation.getConstraint());
        newResourceParameterValidation.setErrorMessage(resourceParameterValidation.getErrorMessage());

        resourceParameterPropertyValidationDtosNew.add(newResourceParameterValidation);
      }
    }

    return resourceParameterPropertyValidationDtosNew;
  }

  /**
   * Processes the property validations for a given {@link ParameterValidationDto} and maps any
   * referenced parameter IDs using the provided mapping of old to new parameter IDs.
   *
   * @param revisedParameterOldAndNewIdMap A map containing the old and new referenced parameter IDs for remapping.
   * @return A list of newly created ParameterRelationPropertyValidationDto with updated parameter and referenced parameter IDs.
   */

  private List<ParameterRelationPropertyValidationDto> processPropertyValidations(ParameterValidationDto parameterValidationDto, Map<Long, Long> revisedParameterOldAndNewIdMap) {
    List<ParameterRelationPropertyValidationDto> propertyValidationsDtosNew = new ArrayList<>();

    if (!Utility.isEmpty(parameterValidationDto.getPropertyValidations())) {
      for (ParameterRelationPropertyValidationDto propertyValidationDto : parameterValidationDto.getPropertyValidations()) {
        ParameterRelationPropertyValidationDto newPropertyValidationDto = new ParameterRelationPropertyValidationDto();

        Long linkedReferencedParameterNewId = null;
        if (!Utility.isEmpty(propertyValidationDto.getReferencedParameterId())) {
          linkedReferencedParameterNewId = revisedParameterOldAndNewIdMap.get(Long.valueOf(propertyValidationDto.getReferencedParameterId()));
        }
        newPropertyValidationDto.setId(Utility.generateUuid());
        newPropertyValidationDto.setValue(propertyValidationDto.getValue());
        newPropertyValidationDto.setOptions(propertyValidationDto.getOptions());
        newPropertyValidationDto.setDateUnit(propertyValidationDto.getDateUnit());
        newPropertyValidationDto.setCollection(propertyValidationDto.getCollection());
        newPropertyValidationDto.setConstraint(propertyValidationDto.getConstraint());
        newPropertyValidationDto.setPropertyId(propertyValidationDto.getPropertyId());
        newPropertyValidationDto.setPropertyExternalId(propertyValidationDto.getPropertyExternalId());
        newPropertyValidationDto.setPropertyDisplayName(propertyValidationDto.getPropertyDisplayName());
        newPropertyValidationDto.setPropertyInputType(propertyValidationDto.getPropertyInputType());
        newPropertyValidationDto.setErrorMessage(propertyValidationDto.getErrorMessage());
        newPropertyValidationDto.setRelationId(propertyValidationDto.getRelationId());
        newPropertyValidationDto.setUrlPath(propertyValidationDto.getUrlPath());
        newPropertyValidationDto.setVariables(propertyValidationDto.getVariables());
        newPropertyValidationDto.setObjectTypeId(propertyValidationDto.getObjectTypeId());
        newPropertyValidationDto.setObjectTypeExternalId(propertyValidationDto.getObjectTypeExternalId());
        newPropertyValidationDto.setObjectTypeDisplayName(propertyValidationDto.getObjectTypeDisplayName());
        newPropertyValidationDto.setSelector(propertyValidationDto.getSelector());
        if (!Utility.isNull(linkedReferencedParameterNewId)) {
          newPropertyValidationDto.setReferencedParameterId(String.valueOf(linkedReferencedParameterNewId));
        }

        propertyValidationsDtosNew.add(newPropertyValidationDto);
      }
    }

    return propertyValidationsDtosNew;
  }

  /**
   * Processes the date and datetime validations for a given {@link ParameterValidationDto} and maps any
   * referenced parameter IDs using the provided mapping of old to new parameter IDs.
   *
   * @param revisedParameterOldAndNewIdMap A map containing the old and new referenced parameter IDs for remapping.
   * @return A list of newly created ParameterRelationPropertyValidationDto with updated parameter and referenced parameter IDs.
   */

  private List<DateParameterValidationDto> processDateAndDateTimeValidations(ParameterValidationDto parameterValidationDto, Map<Long, Long> revisedParameterOldAndNewIdMap) {
    List<DateParameterValidationDto> propertyValidationsDtosNew = new ArrayList<>();

    if (!Utility.isEmpty(parameterValidationDto.getDateTimeParameterValidations())) {
      for (DateParameterValidationDto dateParameterValidationDto : parameterValidationDto.getDateTimeParameterValidations()) {
        DateParameterValidationDto newDateParameterValidationDto = new DateParameterValidationDto();

        Long linkedReferencedParameterNewId = null;
        if (!Utility.isEmpty(dateParameterValidationDto.getReferencedParameterId())) {
          linkedReferencedParameterNewId = revisedParameterOldAndNewIdMap.get(Long.valueOf(dateParameterValidationDto.getReferencedParameterId()));
        }
        newDateParameterValidationDto.setId(Utility.generateUuid());
        newDateParameterValidationDto.setSelector(dateParameterValidationDto.getSelector());
        newDateParameterValidationDto.setValue(dateParameterValidationDto.getValue());
        newDateParameterValidationDto.setDateUnit(dateParameterValidationDto.getDateUnit());
        newDateParameterValidationDto.setConstraint(dateParameterValidationDto.getConstraint());
        newDateParameterValidationDto.setErrorMessage(dateParameterValidationDto.getErrorMessage());
        if (!Utility.isNull(linkedReferencedParameterNewId)) {
          newDateParameterValidationDto.setReferencedParameterId(String.valueOf(linkedReferencedParameterNewId));
        }
        propertyValidationsDtosNew.add(newDateParameterValidationDto);
      }
    }

    return propertyValidationsDtosNew;
  }


  private String getValueFromMapIfNotNull(String id, Map<Long, Long> revisedParameterOldAndNewIdMap) {
    return Optional.ofNullable(id)
      .map(Long::valueOf)
      .map(revisedParameterOldAndNewIdMap::get)
      .map(String::valueOf)
      .orElse(null);
  }


  /**
   * This method creates task automations for the revised tasks and updates the parameter ids inside the action details with the new ids
   *
   * @param checklistRevisionHelper helper object to store all the required data
   * @param principalUser           the user who is creating the revision
   * @throws JsonProcessingException
   */
  private void reviseTaskAutomations(ChecklistRevisionHelper checklistRevisionHelper, User principalUser) throws JsonProcessingException {
    Map<Long, Long> revisedParametersOldAndNewIdMap = checklistRevisionHelper.getRevisedParametersOldAndNewIdMap();
    List<Task> taskHavingAutomations = checklistRevisionHelper.getTaskHavingAutomations();
    Map<Long, Task> revisedTaskOldIdAndNewTaskMap = checklistRevisionHelper.getOldTaskIdAndNewTaskMapping();

    List<Automation> newAutomations = new ArrayList<>();
    List<TaskAutomationMapping> newTaskAutomationMappings = new ArrayList<>();

    for (Task task : taskHavingAutomations) {
      Task revisedTask = revisedTaskOldIdAndNewTaskMap.get(task.getId());
      for (TaskAutomationMapping taskAutomationMapping : task.getAutomations()) {
        Automation existingAutomation = taskAutomationMapping.getAutomation();
        Automation automation = automationMapper.clone(existingAutomation);
        automation.setId(IdGenerator.getInstance().nextId());

        switch (existingAutomation.getActionType()) {
          case INCREASE_PROPERTY, DECREASE_PROPERTY -> {
            AutomationActionForResourceParameterDto resourceParameterAction = JsonUtils.readValue(existingAutomation.getActionDetails().toString(), AutomationActionForResourceParameterDto.class);
            AutomationActionForResourceParameterDto updatedResourceParameterAction = new AutomationActionForResourceParameterDto();

            Long newParameterId;
            if (resourceParameterAction.getSelector() == Type.SelectorType.PARAMETER) {
              newParameterId = revisedParametersOldAndNewIdMap.get(Long.valueOf(resourceParameterAction.getParameterId()));
              updatedResourceParameterAction.setParameterId(String.valueOf(newParameterId));
              updatedResourceParameterAction.setSelector(Type.SelectorType.PARAMETER);
            } else {
              updatedResourceParameterAction.setSelector(Type.SelectorType.CONSTANT);
              updatedResourceParameterAction.setValue(resourceParameterAction.getValue());
            }
            Long newReferenceParameterId = revisedParametersOldAndNewIdMap.get(Long.valueOf(resourceParameterAction.getReferencedParameterId()));
            updatedResourceParameterAction.setReferencedParameterId(String.valueOf(newReferenceParameterId));


            updatedResourceParameterAction.setObjectTypeDisplayName(resourceParameterAction.getObjectTypeDisplayName());
            updatedResourceParameterAction.setPropertyId(resourceParameterAction.getPropertyId());
            updatedResourceParameterAction.setPropertyExternalId(resourceParameterAction.getPropertyExternalId());
            updatedResourceParameterAction.setPropertyDisplayName(resourceParameterAction.getPropertyDisplayName());
            updatedResourceParameterAction.setPropertyInputType(resourceParameterAction.getPropertyInputType());

            automation.setActionDetails(JsonUtils.valueToNode(updatedResourceParameterAction));
          }
          case SET_PROPERTY -> {
            AutomationSetPropertyBaseDto automationBase = JsonUtils.readValue(existingAutomation.getActionDetails().toString(), AutomationSetPropertyBaseDto.class);
            if (automationBase.getPropertyInputType().equals(CollectionMisc.PropertyType.DATE_TIME) || automationBase.getPropertyInputType().equals(CollectionMisc.PropertyType.DATE)) {
              AutomationActionDateTimeDto setPropertyActionDto = JsonUtils.readValue(existingAutomation.getActionDetails().toString(), AutomationActionDateTimeDto.class);
              AutomationActionDateTimeDto updatedSetPropertyAction = new AutomationActionDateTimeDto();

              Long newReferenceParameterId = revisedParametersOldAndNewIdMap.get(Long.valueOf(setPropertyActionDto.getReferencedParameterId()));
              // TODO currently assuming its just TASK
              updatedSetPropertyAction.setReferencedParameterId(String.valueOf(newReferenceParameterId));
              updatedSetPropertyAction.setPropertyId(setPropertyActionDto.getPropertyId());
              updatedSetPropertyAction.setPropertyExternalId(setPropertyActionDto.getPropertyExternalId());
              updatedSetPropertyAction.setPropertyDisplayName(setPropertyActionDto.getPropertyDisplayName());
              updatedSetPropertyAction.setPropertyInputType(setPropertyActionDto.getPropertyInputType());
              if (setPropertyActionDto.getSelector() == Type.SelectorType.PARAMETER) {
                Long parameterId = revisedParametersOldAndNewIdMap.get(Long.valueOf(setPropertyActionDto.getParameterId()));
                updatedSetPropertyAction.setParameterId(parameterId.toString());
                updatedSetPropertyAction.setSelector(Type.SelectorType.PARAMETER);
              } else {
                Task revisedTaskNew = revisedTaskOldIdAndNewTaskMap.get(Long.valueOf(setPropertyActionDto.getEntityId()));

                if (setPropertyActionDto.getOffsetParameterId() != null) {
                  Long revisedOffsetParameterId = revisedParametersOldAndNewIdMap.get(Long.valueOf(setPropertyActionDto.getOffsetParameterId()));
                  updatedSetPropertyAction.setOffsetParameterId(revisedOffsetParameterId.toString());
                }
                Long newEntityId = revisedTaskNew.getId();
                updatedSetPropertyAction.setSelector(Type.SelectorType.CONSTANT);
                updatedSetPropertyAction.setValue(setPropertyActionDto.getValue());
                updatedSetPropertyAction.setEntityId(String.valueOf(newEntityId));
                updatedSetPropertyAction.setEntityType(setPropertyActionDto.getEntityType());
                updatedSetPropertyAction.setCaptureProperty(setPropertyActionDto.getCaptureProperty());
                updatedSetPropertyAction.setOffsetSelector(setPropertyActionDto.getOffsetSelector());
                updatedSetPropertyAction.setOffsetValue(setPropertyActionDto.getOffsetValue());
                updatedSetPropertyAction.setOffsetDateUnit(setPropertyActionDto.getOffsetDateUnit());
                if (!Utility.isEmpty(setPropertyActionDto.getParameterId())) {
                  Long revisedParameterId = revisedParametersOldAndNewIdMap.get(Long.valueOf(setPropertyActionDto.getParameterId()));
                  updatedSetPropertyAction.setParameterId(revisedParameterId.toString());
                }
              }
              automation.setActionDetails(JsonUtils.valueToNode(updatedSetPropertyAction));
            } else {
              AutomationActionSetPropertyDto setPropertyActionDto = JsonUtils.readValue(existingAutomation.getActionDetails().toString(), AutomationActionSetPropertyDto.class);
              AutomationActionSetPropertyDto updatedSetPropertyAction = new AutomationActionSetPropertyDto();

              Long newReferenceParameterId = revisedParametersOldAndNewIdMap.get(Long.valueOf(setPropertyActionDto.getReferencedParameterId()));

              updatedSetPropertyAction.setReferencedParameterId(String.valueOf(newReferenceParameterId));
              updatedSetPropertyAction.setPropertyId(setPropertyActionDto.getPropertyId());
              updatedSetPropertyAction.setPropertyExternalId(setPropertyActionDto.getPropertyExternalId());
              updatedSetPropertyAction.setPropertyDisplayName(setPropertyActionDto.getPropertyDisplayName());
              updatedSetPropertyAction.setPropertyInputType(setPropertyActionDto.getPropertyInputType());
              updatedSetPropertyAction.setValue(setPropertyActionDto.getValue());
              updatedSetPropertyAction.setChoices(setPropertyActionDto.getChoices());
              if (setPropertyActionDto.getSelector() == Type.SelectorType.PARAMETER) {
                Long parameterId = revisedParametersOldAndNewIdMap.get(Long.valueOf(setPropertyActionDto.getParameterId()));
                updatedSetPropertyAction.setParameterId(parameterId.toString());
                updatedSetPropertyAction.setSelector(Type.SelectorType.PARAMETER);
              } else {
                updatedSetPropertyAction.setSelector(Type.SelectorType.CONSTANT);
              }

              automation.setActionDetails(JsonUtils.valueToNode(updatedSetPropertyAction));
            }
          }
          case ARCHIVE_OBJECT -> {
            AutomationActionArchiveObjectDto archiveObjectDto = JsonUtils.readValue(existingAutomation.getActionDetails().toString(), AutomationActionArchiveObjectDto.class);
            AutomationActionArchiveObjectDto updatedArchiveObjectDto = new AutomationActionArchiveObjectDto();
            Long newReferenceParameterId = revisedParametersOldAndNewIdMap.get(Long.valueOf(archiveObjectDto.getReferencedParameterId()));
            updatedArchiveObjectDto.setReferencedParameterId(String.valueOf(newReferenceParameterId));
            updatedArchiveObjectDto.setSelector(archiveObjectDto.getSelector());
            automation.setActionDetails(JsonUtils.valueToNode(updatedArchiveObjectDto));
          }
          case CREATE_OBJECT, BULK_CREATE_OBJECT -> {
            log.info("[AUtomation] Bulk Create Object:  {}, type: {}", automation.getId(), automation.getType());
            AutomationObjectCreationActionDto automationObjectCreationActionDto = JsonUtils.readValue(existingAutomation.getActionDetails().toString(), AutomationObjectCreationActionDto.class);
            List<PropertyParameterMappingDto> propertyParameterMappingDtos = Optional.ofNullable(automationObjectCreationActionDto.getConfiguration()).orElseGet(ArrayList::new);
            propertyParameterMappingDtos.forEach(
              propertyParameterMappingDto -> {
                if (!Utility.isEmpty(propertyParameterMappingDto.getParameterId())) {
                  String newParameterId = String.valueOf(revisedParametersOldAndNewIdMap.get(Long.valueOf(propertyParameterMappingDto.getParameterId())));
                  propertyParameterMappingDto.setParameterId(newParameterId);
                }
              }
            );
            Type.SelectorType selector = automationObjectCreationActionDto.getSelector();
            if (selector == Type.SelectorType.PARAMETER) {
              // Adding this additional non-empty check as the data can be corrupted it can break the code
              if (!Utility.isEmpty(automationObjectCreationActionDto.getReferencedParameterId())) {
                automationObjectCreationActionDto.setReferencedParameterId(String.valueOf(revisedParametersOldAndNewIdMap.get(Long.valueOf(automationObjectCreationActionDto.getReferencedParameterId()))));
              }
            }
            automationObjectCreationActionDto.setConfiguration(propertyParameterMappingDtos);
            automation.setActionDetails(JsonUtils.valueToNode(automationObjectCreationActionDto));
          }

          case SET_RELATION -> {
            AutomationActionMappedRelationDto automationActionMappedRelationDto = JsonUtils.readValue(existingAutomation.getActionDetails().toString(), AutomationActionMappedRelationDto.class);
            Long newReferenceParameterId = revisedParametersOldAndNewIdMap.get(Long.valueOf(automationActionMappedRelationDto.getReferencedParameterId()));
            Long parameterId = revisedParametersOldAndNewIdMap.get(Long.valueOf(automationActionMappedRelationDto.getParameterId()));
            automationActionMappedRelationDto.setReferencedParameterId(String.valueOf(newReferenceParameterId));
            automationActionMappedRelationDto.setSelector(automationActionMappedRelationDto.getSelector());
            automationActionMappedRelationDto.setParameterId(String.valueOf(parameterId));
            automation.setActionDetails(JsonUtils.valueToNode(automationActionMappedRelationDto));
          }
          default -> log.error("Automation action type not supported for revision yet");
        }

        automation.setCreatedBy(principalUser);
        automation.setModifiedBy(principalUser);

        newAutomations.add(automation);
        TaskAutomationMapping newTaskAutomationMapping = new TaskAutomationMapping(revisedTask, automation, taskAutomationMapping.getOrderTree(), taskAutomationMapping.getDisplayName(), principalUser);

        newTaskAutomationMappings.add(newTaskAutomationMapping);
      }
    }

    automationRepository.saveAll(newAutomations);
    taskAutomationMappingRepository.saveAll(newTaskAutomationMappings);
  }


  /**
   * This method creates copies of process parameters and adds them to the revised checklist
   *
   * @param parentChecklist         the checklist whose revision is being created
   * @param revisedChecklist        the revised checklist
   * @param checklistRevisionHelper helper object to store all the required data
   * @param principalUserEntity     the user who is creating the revision
   */
  private void reviseProcessParameters(Checklist parentChecklist, Checklist revisedChecklist, ChecklistRevisionHelper checklistRevisionHelper, User principalUserEntity) throws JsonProcessingException {
    List<Parameter> processParameters = parameterRepository.getParametersByChecklistIdAndTargetEntityType(parentChecklist.getId(), Type.ParameterTargetEntityType.PROCESS);
    Map<Long, Parameter> revisedParametersMap = checklistRevisionHelper.getRevisedParameters();
    Map<Long, Long> revisedParametersOldAndNewIdMap = checklistRevisionHelper.getRevisedParametersOldAndNewIdMap();
    List<Parameter> existingParameterHavingValidations = checklistRevisionHelper.getExistingParameterHavingValidations();
    List<Parameter> existingParameterHavingAutoInitialize = checklistRevisionHelper.getExistingParameterHavingAutoInitialize();
    List<Parameter> existingParameterHavingRules = checklistRevisionHelper.getExistingParameterHavingRules();
    List<Parameter> existingResourceParameters = checklistRevisionHelper.getExistingResourceParameters();
    List<Parameter> existingParameterHavingParameterizedLeastCount = checklistRevisionHelper.getExistingParameterHavingParameterizedLeastCount();

    List<Parameter> revisedParameters = new ArrayList<>();

    for (var parameterRevisionOf : processParameters) {

      boolean isParameterHavingValidations = isParameterHavingValidations(parameterRevisionOf);
      if (isParameterHavingValidations) {
        existingParameterHavingValidations.add(parameterRevisionOf);
      }

      if (parameterRevisionOf.isAutoInitialized()) {
        existingParameterHavingAutoInitialize.add(parameterRevisionOf);
      }
      if (!Utility.isEmpty(parameterRevisionOf.getRules())) {
        existingParameterHavingRules.add(parameterRevisionOf);
      }
      if (isParameterizedLeastCount(parameterRevisionOf)) {
        existingParameterHavingParameterizedLeastCount.add(parameterRevisionOf);
      }

      Parameter revisedParameter = new Parameter();
      Long newId = IdGenerator.getInstance().nextId();
      revisedParameter.setId(newId);
      revisedParameter.setChecklistId(revisedChecklist.getId());
      revisedParameter.setOrderTree(parameterRevisionOf.getOrderTree());
      revisedParameter.setMandatory(parameterRevisionOf.isMandatory());
      revisedParameter.setType(parameterRevisionOf.getType());

      if (Utility.isEmpty(parameterRevisionOf.getVerificationType())) {
        revisedParameter.setVerificationType(Type.VerificationType.NONE);
      } else {
        revisedParameter.setVerificationType(parameterRevisionOf.getVerificationType());
      }
      revisedParameter.setDescription(parameterRevisionOf.getDescription());
      revisedParameter.setAutoInitialize(parameterRevisionOf.getAutoInitialize());
      revisedParameter.setAutoInitialized(parameterRevisionOf.isAutoInitialized());
      revisedParameter.setHidden(parameterRevisionOf.isHidden());
      revisedParameter.setTargetEntityType(parameterRevisionOf.getTargetEntityType());
      if (Utility.isEmpty(parameterRevisionOf.getVerificationType())) {
        revisedParameter.setVerificationType(Type.VerificationType.NONE);
      } else {
        revisedParameter.setVerificationType(parameterRevisionOf.getVerificationType());
      }

      revisedParameter.setData(parameterRevisionOf.getData());
      revisedParameter.setMetadata(parameterRevisionOf.getMetadata());
      revisedParameter.setLabel(parameterRevisionOf.getLabel());
      revisedParameter.setModifiedBy(principalUserEntity);
      revisedParameter.setCreatedBy(principalUserEntity);
      revisedParameter.setValidations(JsonUtils.valueToNode(new ArrayList<>()));

      if (revisedParameter.getType().equals(Type.Parameter.RESOURCE) || revisedParameter.getType().equals(Type.Parameter.MULTI_RESOURCE)) {
        existingResourceParameters.add(revisedParameter);
      }

      revisedParametersOldAndNewIdMap.put(parameterRevisionOf.getId(), newId);

      if (Type.Parameter.MATERIAL.equals(parameterRevisionOf.getType())) {
        for (ParameterMediaMapping parameterMediaMapping : parameterRevisionOf.getMedias()) {
          if (!parameterMediaMapping.isArchived()) {
            revisedParameter.addMedia(parameterMediaMapping.getMedia(), principalUserEntity);
          }
        }
      }

      revisedParameters.add(revisedParameter);
      revisedParametersMap.put(revisedParameter.getId(), revisedParameter);
    }

    parameterRepository.saveAll(revisedParameters);
  }

  /**
   * This method is used to revise the auto initialize data of the parameters
   * This is required because the parameter ids are changed during the revision
   * So we need to update the auto initialize data with the new parameter ids
   *
   * @param checklistRevisionHelper helper object to store all the required data
   * @throws JsonProcessingException
   */
  void reviseParameterAutoInitialize(ChecklistRevisionHelper checklistRevisionHelper) throws JsonProcessingException {
    Map<Long, Long> revisedActivitiesOldAndNewIdMap = checklistRevisionHelper.getRevisedParametersOldAndNewIdMap();
    List<Parameter> existingParameterHavingAutoInitialize = checklistRevisionHelper.getExistingParameterHavingAutoInitialize();
    Map<Long, Parameter> revisedParameters = checklistRevisionHelper.getRevisedParameters();

    for (Parameter existingParameter : existingParameterHavingAutoInitialize) {
      if (null != existingParameter.getAutoInitialize()) {
        AutoInitializeDto autoInitializeDto = JsonUtils.readValue(existingParameter.getAutoInitialize().toString(), AutoInitializeDto.class);
        autoInitializeDto.setParameterId(String.valueOf(revisedActivitiesOldAndNewIdMap.get(Long.valueOf(autoInitializeDto.getParameterId()))));

        // Set the revised parameter data with new auto initialize json data
        Long revisedParameterId = revisedActivitiesOldAndNewIdMap.get(existingParameter.getId());
        Parameter revisedParameter = revisedParameters.get(revisedParameterId);
        revisedParameter.setAutoInitialize(JsonUtils.valueToNode(autoInitializeDto));
      }
    }

  }

  private void addPrimaryAuthor(Checklist checklist, User user) {
    checklist.addPrimaryAuthor(user, checklist.getReviewCycle(), user);
  }

  /**
   * This method is used to revise the rules of the parameters.
   * This is required because the parameter ids are changed during the revision,
   * So we need to update the rules data with the new parameter ids
   *
   * @param checklistRevisionHelper helper object to store all the required data
   * @throws JsonProcessingException
   */
  private void reviseParameterRules(ChecklistRevisionHelper checklistRevisionHelper) throws JsonProcessingException {
    Map<Long, Long> revisedParametersOldAndNewIdMap = checklistRevisionHelper.getRevisedParametersOldAndNewIdMap();
    Map<Long, Task> revisedTaskOldIdAndNewIdMap = checklistRevisionHelper.getOldTaskIdAndNewTaskMapping();
    Map<Long, Long> revisedStageOldIdAndNewIdMap = checklistRevisionHelper.getOldStageIdNewStageIdMapping();
    List<Parameter> existingParameterHavingRules = checklistRevisionHelper.getExistingParameterHavingRules();
    Map<Long, Parameter> revisedParameters = checklistRevisionHelper.getRevisedParameters();

    for (Parameter existingParameter : existingParameterHavingRules) {
      if (!Utility.isEmpty(existingParameter.getRules())) {
        List<RuleDto> ruleDtos = JsonUtils.readValue(existingParameter.getRules().toString(),
          new TypeReference<>() {
          });
        // TODO if this fails job still gets created
        if (!Utility.isEmpty(ruleDtos)) {
          for (RuleDto ruleDto : ruleDtos) {
            if (null != ruleDto.getHide()) {
              updateRule(ruleDto.getHide(), revisedStageOldIdAndNewIdMap, revisedTaskOldIdAndNewIdMap,
                revisedParametersOldAndNewIdMap);
            }

            if (null != ruleDto.getShow()) {
              updateRule(ruleDto.getShow(), revisedStageOldIdAndNewIdMap, revisedTaskOldIdAndNewIdMap,
                revisedParametersOldAndNewIdMap);
            }
          }
        }

        Long revisedParameterId = revisedParametersOldAndNewIdMap.get(existingParameter.getId());
        Parameter revisedParameter = revisedParameters.get(revisedParameterId);
        revisedParameter.setRules(JsonUtils.valueToNode(ruleDtos));
      }
    }
  }

  private void updateRule(RuleEntityIdDto rule, Map<Long, Long> revisedStageOldIdAndNewIdMap, Map<Long, Task> revisedTaskOldIdAndNewIdMap, Map<Long, Long> revisedActivitiesOldAndNewIdMap) {
    List<String> newStageIds = new ArrayList<>();
    List<String> newTaskIds = new ArrayList<>();
    List<String> newParameterIds = new ArrayList<>();

    for (String stageId : rule.getStages()) {
      if (null != revisedStageOldIdAndNewIdMap.get(Long.valueOf(stageId))) {
        newStageIds.add(String.valueOf(revisedStageOldIdAndNewIdMap.get(Long.valueOf(stageId))));
      }
    }
    for (String taskId : rule.getTasks()) {
      if (null != revisedTaskOldIdAndNewIdMap.get(Long.valueOf(taskId))) {
        newTaskIds.add(String.valueOf(revisedTaskOldIdAndNewIdMap.get(Long.valueOf(taskId)).getId()));
      }
    }

    for (String parameterId : rule.getParameters()) {
      if (null != revisedActivitiesOldAndNewIdMap.get(Long.valueOf(parameterId))) {
        newParameterIds.add(String.valueOf(revisedActivitiesOldAndNewIdMap.get(Long.valueOf(parameterId))));
      }
    }

    rule.setParameters(newParameterIds);
    rule.setStages(newStageIds);
    rule.setTasks(newTaskIds);
  }

  /**
   * This method is used to revise the resource parameter filters.
   * This is required because the parameter ids are changed during the revision,
   * So we need to update the process parameter filters data with the new parameter ids
   *
   * @param checklistRevisionHelper helper object to store all the required data
   * @throws JsonProcessingException
   */
  private void reviseResourceParameterFilters(ChecklistRevisionHelper checklistRevisionHelper) throws JsonProcessingException {
    List<Parameter> existingResourceParameters = checklistRevisionHelper.getExistingResourceParameters();
    Map<Long, Long> revisedParametersOldAndNewIdMap = checklistRevisionHelper.getRevisedParametersOldAndNewIdMap();

    for (Parameter existingParameter : existingResourceParameters) {
      ResourceParameter resourceParameter = JsonUtils.readValue(existingParameter.getData().toString(), ResourceParameter.class);
      ResourceParameterFilter resourceParameterFilter = resourceParameter.getPropertyFilters();
      if (!Utility.isNull(resourceParameterFilter)) {
        List<ResourceParameterFilterField> resourceParameterFilterFields = resourceParameterFilter.getFields();
        if (!Utility.isEmpty(resourceParameterFilterFields)) {
          for (ResourceParameterFilterField resourceParameterFilterField : resourceParameterFilterFields) {
            if (!Utility.isEmpty(resourceParameterFilterField.getReferencedParameterId())) {
              resourceParameterFilterField.setReferencedParameterId(String.valueOf(revisedParametersOldAndNewIdMap.get(Long.valueOf(resourceParameterFilterField.getReferencedParameterId()))));
            }
          }
        }
      }

      existingParameter.setData(JsonUtils.valueToNode(resourceParameter));
    }
  }

  private void reviseResourceParameterValidations(ChecklistRevisionHelper checklistRevisionHelper) throws JsonProcessingException {
    List<Parameter> existingResourceParameters = checklistRevisionHelper.getExistingResourceParameters();
    Map<Long, Long> revisedParametersOldAndNewIdMap = checklistRevisionHelper.getRevisedParametersOldAndNewIdMap();

    for (Parameter existingParameter : existingResourceParameters) {
      ResourceParameter resourceParameter = JsonUtils.readValue(existingParameter.getData().toString(), ResourceParameter.class);
      List<ParameterRelationPropertyValidationDto> resourceParameterValidation = resourceParameter.getPropertyValidations();
      if (!Utility.isEmpty(resourceParameterValidation)) {
        for (ParameterRelationPropertyValidationDto parameterRelationPropertyValidationDto : resourceParameterValidation) {
          if (!Utility.isEmpty(parameterRelationPropertyValidationDto.getReferencedParameterId()) && parameterRelationPropertyValidationDto.getSelector().equals(Type.SelectorType.PARAMETER)) {
            parameterRelationPropertyValidationDto.setReferencedParameterId(String.valueOf(revisedParametersOldAndNewIdMap.get(Long.valueOf(parameterRelationPropertyValidationDto.getReferencedParameterId()))));
          }
        }
      }

      existingParameter.setData(JsonUtils.valueToNode(resourceParameter));
    }
  }

  /**
   * This method is used to check whether parameter contains parameterized least count.
   *
   * @param parameter contains parameters data
   * @throws JsonProcessingException
   */
  private boolean isParameterizedLeastCount(Parameter parameter) throws JsonProcessingException {
    LeastCount leastCount = null;
    if (parameter.getType().equals(Type.Parameter.NUMBER)) {
      NumberParameter numberParameter = JsonUtils.readValue(parameter.getData().toString(), NumberParameter.class);
      leastCount = numberParameter.getLeastCount();
    } else if (parameter.getType().equals(Type.Parameter.SHOULD_BE)) {
      ShouldBeParameter shouldBeParameter = JsonUtils.readValue(parameter.getData().toString(), ShouldBeParameter.class);
      leastCount = shouldBeParameter.getLeastCount();
    }
    return !Utility.isEmpty(leastCount) && leastCount.getSelector().equals(Type.SelectorType.PARAMETER);
  }

  /**
   * This method is used to data of the parameters having least count.
   * This is required because the parameter ids are changed during the revision,
   * So we need to update the rules data with the new parameter ids
   *
   * @param checklistRevisionHelper helper object to store all the required data
   * @throws JsonProcessingException
   */
  private void reviseDataOfParameterHavingParameterizedLeastCount(ChecklistRevisionHelper checklistRevisionHelper) throws JsonProcessingException {
    Map<Long, Long> revisedParameterOldAndNewIdMap = checklistRevisionHelper.getRevisedParametersOldAndNewIdMap();
    Map<Long, Parameter> revisedParameterMap = checklistRevisionHelper.getRevisedParameters();
    List<Parameter> existingParameterHavingParameterizedLeastCount = checklistRevisionHelper.getExistingParameterHavingParameterizedLeastCount();

    for (Parameter parameterRevisionOf : existingParameterHavingParameterizedLeastCount) {
      Long revisedParameterId = revisedParameterOldAndNewIdMap.get(parameterRevisionOf.getId());
      Parameter revisedParameter = revisedParameterMap.get(revisedParameterId);
      if (revisedParameter.getType().equals(Type.Parameter.NUMBER)) {
        reviseNumberParameterData(parameterRevisionOf, revisedParameter, checklistRevisionHelper);
      } else if (revisedParameter.getType().equals(Type.Parameter.SHOULD_BE)) {
        reviseShouldBeParameterData(parameterRevisionOf, revisedParameter, checklistRevisionHelper);
      }
    }
  }

  private void reviseActions(Checklist parentChecklist, Checklist revisedChecklist, ChecklistRevisionHelper checklistRevisionHelper, User principalUserEntity) throws JsonProcessingException {
    List<Action> revisedActions = new ArrayList<>();
    Pageable pageable = Pageable.unpaged();
    Page<ActionDto> actionsPage = actionService.getActions(parentChecklist.getId(), pageable);
    List<ActionFacilityMapping> revisedActionFacilityMappings = new ArrayList<>();

    for (ActionDto actionDto : actionsPage.getContent()) {
      Action newAction = new Action();
      Long newActionId = IdGenerator.getInstance().nextId();
      newAction.setId(newActionId);
      newAction.setChecklist(revisedChecklist);
      newAction.setName(actionDto.getName());
      newAction.setDescription(actionDto.getDescription());
      newAction.setCode(codeService.getCode(Type.EntityType.ACTION, principalUserEntity.getOrganisationId()));
      newAction.setSuccessMessage(actionDto.getSuccessMessage());
      newAction.setFailureMessage(actionDto.getFailureMessage());
      Long oldTriggerEntityId = Long.valueOf(actionDto.getTriggerEntityId());
      Task newTriggerEntityId = checklistRevisionHelper.getOldTaskIdAndNewTaskMapping().get(oldTriggerEntityId);
      newAction.setTriggerEntityId(newTriggerEntityId.getId());
      newAction.setTriggerType(actionDto.getTriggerType());
      newAction.setCreatedBy(principalUserEntity);
      newAction.setModifiedBy(principalUserEntity);
      newAction.setCreatedAt(DateTimeUtils.now());
      newAction.setModifiedAt(DateTimeUtils.now());

      revisedActions.add(newAction);
      checklistRevisionHelper.getOldActionIdToNewActionIdMap().put(Long.valueOf(actionDto.getId()), newActionId);

      List<ActionFacilityMapping> actionFacility = actionFacilityRepository.findActionFacilityMappingByAction_Id(Long.valueOf(actionDto.getId()));

      for (ActionFacilityMapping facilityMapping : actionFacility) {
        Facility facility = facilityMapping.getFacility();

        ActionFacilityMapping actionFacilityMapping = new ActionFacilityMapping(newAction, facility);
        actionFacilityMapping.setAction(newAction);
        actionFacilityMapping.setFacility(facility);
        actionFacilityMapping.setCreatedBy(principalUserEntity);
        actionFacilityMapping.setModifiedBy(principalUserEntity);
        actionFacilityMapping.setCreatedAt(DateTimeUtils.now());
        actionFacilityMapping.setModifiedAt(DateTimeUtils.now());

        revisedActionFacilityMappings.add(actionFacilityMapping);
      }
    }

    actionRepository.saveAll(revisedActions);
    actionFacilityRepository.saveAll(revisedActionFacilityMappings);

    reviseEffects(checklistRevisionHelper, principalUserEntity);
  }

  private void reviseEffectRootNode(EffectRootNode rootNode, ChecklistRevisionHelper checklistRevisionHelper) {
    for (EffectChildNode childNode : rootNode.getChildren()) {
      if (!Utility.isEmpty(childNode.getData()) && !Utility.isEmpty(childNode.getData().getId())) {
        switch (Type.EffectEntityType.valueOf(childNode.getData().getEntity().toUpperCase())) {
          case PARAMETER -> {
            Long oldParameterId = Long.valueOf(childNode.getData().getId());
            Long revisedParameterId = checklistRevisionHelper.getRevisedParametersOldAndNewIdMap().get(oldParameterId);
            if (!Utility.isEmpty(revisedParameterId)) {
              childNode.getData().setId(String.valueOf(revisedParameterId));
            }
          }
          case TASK -> {
            Long oldTaskId = Long.valueOf(childNode.getData().getId());
            Task newTask = checklistRevisionHelper.getOldTaskIdAndNewTaskMapping().get(oldTaskId);
            if (!Utility.isEmpty(newTask)) {
              childNode.getData().setId(String.valueOf(newTask.getId()));
            }
          }
          case EFFECT -> {
            Long oldEffectId = Long.valueOf(childNode.getData().getId());
            Long RevisedEffectId = checklistRevisionHelper.getOldActionIdToNewActionIdMap().get(oldEffectId);
            if (!Utility.isEmpty(RevisedEffectId)) {
              childNode.getData().setId(String.valueOf(RevisedEffectId));
            }
          }
          case CONSTANT -> {
            childNode.getData().setId(childNode.getData().getId());
          }
        }
      }

      reviseEffectChildNode(childNode, checklistRevisionHelper);
    }
  }

  private void reviseEffectChildNode(EffectChildNode childNode, ChecklistRevisionHelper checklistRevisionHelper) {
    if (Utility.isEmpty(childNode) || Utility.isEmpty(childNode.getChildren())) {
      return;
    }

    for (EffectTextNode textNode : childNode.getChildren()) {
      if (!Utility.isEmpty(textNode.getData()) && !Utility.isEmpty(textNode.getData().getId())) {
        switch (Type.EffectEntityType.valueOf(textNode.getData().getEntity().toUpperCase())) {
          case PARAMETER -> {
            Long oldParameterId = Long.valueOf(textNode.getData().getId());
            Long revisedParameterId = checklistRevisionHelper.getRevisedParametersOldAndNewIdMap().get(oldParameterId);
            if (!Utility.isEmpty(revisedParameterId)) {
              textNode.getData().setId(String.valueOf(revisedParameterId));
            }
          }

          case TASK -> {
            Long oldTaskId = Long.valueOf(textNode.getData().getId());
            Task newTask = checklistRevisionHelper.getOldTaskIdAndNewTaskMapping().get(oldTaskId);
            if (!Utility.isEmpty(newTask)) {
              textNode.getData().setId(String.valueOf(newTask.getId()));
            }
          }
          case EFFECT -> {
            Long oldEffectId = Long.valueOf(textNode.getData().getId());
            Long RevisedEffectId = checklistRevisionHelper.getOldEffectIdToNewEffectIdMap().get(oldEffectId);
            if (!Utility.isEmpty(RevisedEffectId)) {
              textNode.getData().setId(String.valueOf(RevisedEffectId));
            }
          }
          case CONSTANT -> {
            textNode.getData().setId(textNode.getData().getId());
          }
        }
      }
    }
  }

  private void reviseEffects(ChecklistRevisionHelper checklistRevisionHelper, User principalUserEntity) throws JsonProcessingException {
    List<Long> oldActionIds = new ArrayList<>(checklistRevisionHelper.getOldActionIdToNewActionIdMap().keySet());
    List<EffectDto> effectDtoList = effectMapper.toDto(
      effectRepository.findAllByActionsIdInOrderByOrderTree(oldActionIds).stream()
        .filter(effect -> !effect.isArchived())
        .toList()
    );

    List<Effect> revisedEffects = new ArrayList<>();
    Map<Long, Long> effectIdMapping = new HashMap<>();

    for (EffectDto effectDto : effectDtoList) {
      Long newActionId = checklistRevisionHelper.getOldActionIdToNewActionIdMap().get(Long.valueOf(effectDto.getActionsId()));
      Action newAction = actionRepository.findById(newActionId)
        .orElseThrow(() -> new IllegalArgumentException("No Action found for ID: " + newActionId));
      Long oldEffectId = Long.valueOf(effectDto.getId());
      Long newEffectId = IdGenerator.getInstance().nextId();
      checklistRevisionHelper.getOldEffectIdToNewEffectIdMap().put(oldEffectId, newEffectId);

      JsonNode revisedQuery = null;
      if (!Utility.isEmpty(effectDto.getQuery()) && effectDto.getQuery().has("root")) {
        EffectRootNode queryRoot = JsonUtils.convertJsonNodeToPojo(effectDto.getQuery().get("root"), EffectRootNode.class);
        reviseEffectRootNode(queryRoot, checklistRevisionHelper);
        ObjectNode queryNode = (ObjectNode) JsonUtils.createObjectNode();
        queryNode.set("root", JsonUtils.valueToNode(queryRoot));
        revisedQuery = queryNode;
      }

      JsonNode revisedApiEndpoint = null;
      if (!Utility.isEmpty(effectDto.getApiEndpoint()) && effectDto.getApiEndpoint().has("root")) {
        EffectRootNode endpointRoot = JsonUtils.convertJsonNodeToPojo(effectDto.getApiEndpoint().get("root"), EffectRootNode.class);
        reviseEffectRootNode(endpointRoot, checklistRevisionHelper);
        ObjectNode endpointNode = (ObjectNode) JsonUtils.createObjectNode();
        endpointNode.set("root", JsonUtils.valueToNode(endpointRoot));
        revisedApiEndpoint = endpointNode;
      }

      JsonNode revisedApiPayload = null;
      if (!Utility.isEmpty(effectDto.getApiPayload()) && effectDto.getApiPayload().has("root")) {
        EffectRootNode payloadRoot = JsonUtils.convertJsonNodeToPojo(effectDto.getApiPayload().get("root"), EffectRootNode.class);
        reviseEffectRootNode(payloadRoot, checklistRevisionHelper);
        ObjectNode payloadNode = (ObjectNode) JsonUtils.createObjectNode();
        payloadNode.set("root", JsonUtils.valueToNode(payloadRoot));
        revisedApiPayload = payloadNode;
      }

      Effect revisedEffect = new Effect();
      revisedEffect.setId(newEffectId);
      revisedEffect.setActionsId(newActionId);
      revisedEffect.setAction(newAction);
      revisedEffect.setName(effectDto.getName());
      revisedEffect.setDescription(effectDto.getDescription());
      revisedEffect.setEffectType(effectDto.getEffectType());
      revisedEffect.setOrderTree(effectDto.getOrderTree());
      revisedEffect.setApiEndpoint(revisedApiEndpoint);
      revisedEffect.setApiMethod(effectDto.getApiMethod());
      revisedEffect.setApiPayload(revisedApiPayload);
      revisedEffect.setQuery(revisedQuery);
      revisedEffect.setApiHeaders(effectDto.getApiHeaders());
      revisedEffect.setCreatedBy(principalUserEntity);
      revisedEffect.setModifiedBy(principalUserEntity);
      revisedEffect.setCreatedAt(DateTimeUtils.now());
      revisedEffect.setModifiedAt(DateTimeUtils.now());
      revisedEffect.setJavascriptEnabled(effectDto.isJavascriptEnabled());
      revisedEffects.add(revisedEffect);
    }

    effectRepository.saveAll(revisedEffects);
  }
}
