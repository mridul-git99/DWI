package com.leucine.streem.dto.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.leucine.streem.constant.Type;
import com.leucine.streem.dto.*;
import com.leucine.streem.dto.mapper.helper.IBaseMapper;
import com.leucine.streem.dto.projection.TaskExecutionAssigneeBasicView;
import com.leucine.streem.dto.request.InterlockDto;
import com.leucine.streem.exception.ResourceNotFoundException;
import com.leucine.streem.exception.StreemException;
import com.leucine.streem.model.*;
import com.leucine.streem.model.helper.PrincipalUser;
import com.leucine.streem.repository.ITaskExecutionAssigneeRepository;
import com.leucine.streem.repository.ITaskExecutorLockRepository;
import com.leucine.streem.repository.IUserRepository;
import com.leucine.streem.service.IInterlockService;
import com.leucine.streem.util.Utility;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Mapper(uses = {IParameterMapper.class, ITaskMediaMapper.class, ITaskAutomationMapper.class, IInterlockMapper.class})
public abstract class ITaskMapper implements IBaseMapper<TaskDto, Task> {
  private Map<Long, List<TaskExecution>> taskIdTaskExecutionListMap;
  @Autowired
  private IInterlockService interlockService;
  @Autowired
  private IUserRepository userRepository;
  @Autowired
  private ITaskExecutorLockRepository taskExecutorLockRepository;
  @Autowired
  private ITaskExecutionAssigneeRepository taskExecutionAssigneeRepository;

  @Named(value = "toTaskDto")
  @Mapping(target = "parameters", source = "parameters", qualifiedByName = "toParameterDtoList")
  @Mapping(target = "hasPrerequisites", expression = "java(task.getPrerequisiteTasks().size() > 0 )")
  @Mapping(target = "hasDependents", expression = "java(task.getDependentTasks().size() > 0 )")
  @Mapping(target = "prerequisiteTaskIds", expression = "java(getPrerequisiteTaskIds(task))")
  @Mapping(target = "taskExecutions", ignore = true)
  public abstract TaskDto toDto(Task task, @Context Map<Long, List<ParameterValue>> parameterValueMap,
                                @Context Map<Long, TaskExecution> taskExecutionMap,
                                @Context Map<Long, List<TempParameterValue>> tempParameterValueMap,
                                @Context Map<Long, List<TaskPauseReasonOrComment>> pauseReasonOrCommentMap,
                                @Context Map<Long, List<ParameterVerification>> parameterVerificationMapPeerAndSelf,
                                @Context Map<Long, List<TempParameterVerification>> tempParameterVerificationMapPeerAndSelf
  );

  @Named(value = "toTaskDtoList")
  @IterableMapping(qualifiedByName = "toTaskDto")
  public abstract List<TaskDto> toDto(Set<Task> tasks, @Context Map<Long, List<ParameterValue>> parameterValueMap,

                                      @Context Map<Long, TaskExecution> taskExecutionMap,
                                      @Context Map<Long, List<TempParameterValue>> tempParameterValueMap,
                                      @Context Map<Long, List<TaskPauseReasonOrComment>> pauseReasonOrCommentMap,
                                      @Context Map<Long, List<ParameterVerification>> parameterVerificationMapPeerAndSelf,
                                      @Context Map<Long, List<TempParameterVerification>> tempParameterVerificationMapPeerAndSelf
  );

  @Mapping(target = "parameters", source = "parameters")
  @Mapping(target = "hasPrerequisites", expression = "java(task.getPrerequisiteTasks().size() > 0 )")
  @Mapping(target = "hasDependents", expression = "java(task.getDependentTasks().size() > 0 )")
  @Mapping(target = "prerequisiteTaskIds", expression = "java(getPrerequisiteTaskIds(task))")
  @Override
  public abstract TaskDto toDto(Task task);

  public abstract TaskReportDto toTaskReportDto(Task task);

  @AfterMapping
  protected void setTaskExecutions(Task task, @MappingTarget TaskDto taskDto,
                                   @Context Map<Long, List<ParameterValue>> parameterValueMap,
                                   @Context Map<Long, TaskExecution> taskExecutionMap,
                                   @Context Map<Long, List<TaskPauseReasonOrComment>> pauseReasonOrCommentMap,
                                   @Context Map<Long, List<ParameterVerification>> parameterVerificationMapPeerAndSelf,
                                   @Context Map<Long, List<TempParameterVerification>> tempParameterVerificationMapPeerAndSelf) {

    initTaskExecutionMap(taskExecutionMap);

    List<TaskExecution> taskExecutions = taskIdTaskExecutionListMap.get(task.getId());


    if (!Utility.isEmpty(taskExecutions)) {
      List<TaskExecutionDto> taskExecutionDtos = new ArrayList<>();
      Set<Long> taskExecutionIds = taskExecutions.stream().map(TaskExecution::getId).collect(Collectors.toSet());

      Map<Long, List<TaskExecutionAssigneeBasicView>> taskExecutionAssigneeViews = taskExecutionAssigneeRepository.findAllByTaskExecutionIdsIn(taskExecutionIds)
        .stream().collect(Collectors.groupingBy(teav -> Long.parseLong(teav.getTaskExecutionId()), Collectors.toList()));

      for (TaskExecution taskExecution : taskExecutions) {
        TaskExecutionDto taskExecutionDto = new TaskExecutionDto();
        if (null != taskExecution) {
          taskExecutionDto.setState(taskExecution.getState());
          taskExecutionDto.setOrderTree(taskExecution.getOrderTree());
          taskExecutionDto.setType(taskExecution.getType());
          taskExecutionDto.setStartedAt(taskExecution.getStartedAt());
          taskExecutionDto.setEndedAt(taskExecution.getEndedAt());
          taskExecutionDto.setCorrectionReason(taskExecution.getCorrectionReason());
          taskExecutionDto.setCorrectionEnabled(taskExecution.isCorrectionEnabled());
          taskExecutionDto.setReason(taskExecution.getReason());
          taskExecutionDto.setId(taskExecution.getIdAsString());
          taskExecutionDto.setDuration(taskExecution.getDuration());
          taskExecutionDto.setPauseReasons(pauseReasonOrCommentMap.get(taskExecution.getId()));
          taskExecutionDto.setAudit(IAuditMapper.createAuditDto(taskExecution.getModifiedBy(), taskExecution.getModifiedAt()));
          taskExecutionDto.setContinueRecurrence(taskExecution.isContinueRecurrence());
          taskExecutionDto.setRecurringExpectedStartedAt(taskExecution.getRecurringExpectedStartedAt());
          taskExecutionDto.setRecurringExpectedDueAt(taskExecution.getRecurringExpectedDueAt());
          taskExecutionDto.setRecurringPrematureStartReason(taskExecution.getRecurringPrematureStartReason());
          taskExecutionDto.setRecurringOverdueCompletionReason(taskExecution.getRecurringOverdueCompletionReason());
          taskExecutionDto.setSchedulingExpectedStartedAt(taskExecution.getSchedulingExpectedStartedAt());
          taskExecutionDto.setSchedulingExpectedDueAt(taskExecution.getSchedulingExpectedDueAt());
          taskExecutionDto.setScheduleOverdueCompletionReason(taskExecution.getScheduleOverdueCompletionReason());
          taskExecutionDto.setSchedulePrematureStartReason(taskExecution.getSchedulePrematureStartReason());

          UserAuditDto startedBy = new UserAuditDto();
          if (null != taskExecution.getStartedBy()) {
            startedBy.setId(taskExecution.getStartedBy().getIdAsString());
            startedBy.setEmployeeId(taskExecution.getStartedBy().getEmployeeId());
            startedBy.setFirstName(taskExecution.getStartedBy().getFirstName());
            startedBy.setLastName(taskExecution.getStartedBy().getLastName());
          }
          taskExecutionDto.setStartedBy(startedBy);
          UserAuditDto endedByDto = new UserAuditDto();
          if (null != taskExecution.getEndedBy()) {
            User taskExecutionEndedBy = taskExecution.getEndedBy();
            endedByDto.setId(taskExecutionEndedBy.getIdAsString());
            endedByDto.setEmployeeId(taskExecutionEndedBy.getEmployeeId());
            endedByDto.setFirstName(taskExecutionEndedBy.getFirstName());
            endedByDto.setLastName(taskExecutionEndedBy.getLastName());
          }
          taskExecutionDto.setEndedBy(endedByDto);
          List<TaskExecutionAssigneeDto> taskExecutionAssigneeDtos = new ArrayList<>();
          List<TaskExecutionAssigneeDto> taskExecutionAssigneeUserGroupDtos = new ArrayList<>();
          List<TaskExecutionAssigneeBasicView> taskExecutionAssigneeBasicViews = taskExecutionAssigneeViews.getOrDefault(taskExecution.getId(), new ArrayList<>());
          for (TaskExecutionAssigneeBasicView taskExecutionAssigneeView : taskExecutionAssigneeBasicViews) {
            String userId = taskExecutionAssigneeView.getUserId();
            String userGroupId = taskExecutionAssigneeView.getUserGroupId();
            TaskExecutionAssigneeDto taskExecutionAssigneeDto = new TaskExecutionAssigneeDto();
            taskExecutionAssigneeDto.setActionPerformed(taskExecutionAssigneeView.getIsActionPerformed());
            taskExecutionAssigneeDto.setState(taskExecutionAssigneeView.getAssigneeState());
            if (!Utility.isEmpty(userId)) {
              taskExecutionAssigneeDto.setUserId(userId);
              taskExecutionAssigneeDto.setEmployeeId(taskExecutionAssigneeView.getEmployeeId());
              taskExecutionAssigneeDto.setFirstName(taskExecutionAssigneeView.getFirstName());
              taskExecutionAssigneeDto.setLastName(taskExecutionAssigneeView.getLastName());
              taskExecutionAssigneeDto.setEmail(taskExecutionAssigneeView.getEmail());
              taskExecutionAssigneeDtos.add(taskExecutionAssigneeDto);
            }
            if (!Utility.isEmpty(userGroupId)) {
              taskExecutionAssigneeDto.setUserGroupId(userGroupId);
              taskExecutionAssigneeDto.setUserGroupName(taskExecutionAssigneeView.getUserGroupName());
              taskExecutionAssigneeDto.setUserGroupDescription(taskExecutionAssigneeView.getUserGroupDescription());
              taskExecutionAssigneeUserGroupDtos.add(taskExecutionAssigneeDto);
            }
          }
//          taskExecutionAssigneeDtos.sort(Comparator.comparing(tead -> ((TaskExecutionAssigneeDto) tead).getFirstName().toLowerCase())
//            .thenComparing(tead -> ((TaskExecutionAssigneeDto) tead).getLastName().toLowerCase()));

          taskExecutionDto.setAssignees(taskExecutionAssigneeDtos);
          taskExecutionDto.setUserGroupAssignees(taskExecutionAssigneeUserGroupDtos);
          User correctedBy = taskExecution.getCorrectedBy();
          if (correctedBy != null) {
            UserAuditDto correctedByUserAuditDto = UserAuditDto.builder()
              .id(correctedBy.getIdAsString())
              .employeeId(correctedBy.getEmployeeId())
              .firstName(correctedBy.getFirstName())
              .lastName(correctedBy.getLastName())
              .build();
            taskExecutionDto.setCorrectedBy(correctedByUserAuditDto);
            taskExecutionDto.setCorrectedAt(taskExecution.getCorrectedAt());
          }
        }
        taskExecutionDtos.add(taskExecutionDto);
      }
      taskExecutionDtos = taskExecutionDtos.stream().sorted(Comparator.comparing(TaskExecutionDto::getOrderTree)).toList();
      taskDto.setTaskExecutions(taskExecutionDtos);
    }
  }

  @AfterMapping
  protected void setInterlockDto(Task task, @MappingTarget TaskDto taskDto) {
    try {
      PrincipalUser principalUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
      User principalUserEntity = userRepository.getOne(principalUser.getId());
      if (taskDto.isHasInterlocks()) {
        InterlockDto interlockDto = interlockService.getInterlockByTaskId(task, principalUserEntity);
        if (interlockDto != null) {
          taskDto.setInterlocks(interlockDto);
        }
      }
    }catch (ResourceNotFoundException | JsonProcessingException | StreemException e) {
      log.error("Exception occurred while setting interlocks for Task with Id {}", task.getId());
    }
  }

  @AfterMapping
  protected void setExecutorLock(Task task, @MappingTarget TaskDto taskDto) {
    boolean isReferencedTaskExecutorLock = taskExecutorLockRepository.existsByReferencedTaskId(task.getId());
    if (task.isHasExecutorLock()) {

      List<TaskExecutorLock> taskExecutorLocks = taskExecutorLockRepository.findAllByTaskId(task.getId());
      if (!Utility.isEmpty(taskExecutorLocks)) {
        Long hasToBeExecutorLockTaskId = taskExecutorLocks.stream().filter(taskExecutorLock -> taskExecutorLock.getLockType() == Type.TaskExecutorLockType.EQ)
          .map(tel -> tel.getReferencedTask().getId())
          .findFirst().orElse(null);

        Set<Long> cannotBeTaskExecutorLockIds = taskExecutorLocks.stream().filter(taskExecutorLock -> taskExecutorLock.getLockType() == Type.TaskExecutorLockType.NIN)
          .map(tel -> tel.getReferencedTask().getId())
          .collect(Collectors.toSet());
        taskDto.setTaskExecutorLock(new TaskExecutorLockRequest(hasToBeExecutorLockTaskId, cannotBeTaskExecutorLockIds));
      }

    }
    if (isReferencedTaskExecutorLock) {
      taskDto.setReferencedTaskExecutorLock(true);
    }
  }

  // TODO: This is performance overhead (Alternate is using it as an instance variable and adding null checks)
  private void initTaskExecutionMap(Map<Long, TaskExecution> taskExecutionMap) {
    taskIdTaskExecutionListMap = taskExecutionMap.values().stream().collect(Collectors.groupingBy(TaskExecution::getTaskId));
  }

  List<Long> getPrerequisiteTaskIds(Task task) {
    return task.getPrerequisiteTasks().stream()
      .map(dependency -> dependency.getPrerequisiteTask().getId())
      .collect(Collectors.toList());
  }
}
