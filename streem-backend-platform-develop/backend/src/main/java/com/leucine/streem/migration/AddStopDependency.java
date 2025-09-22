package com.leucine.streem.migration;

import com.leucine.streem.constant.Misc;
import com.leucine.streem.dto.BasicDto;
import com.leucine.streem.dto.mapper.IUserMapper;
import com.leucine.streem.dto.request.TaskDependencyRequest;
import com.leucine.streem.model.*;
import com.leucine.streem.model.helper.PrincipalUser;
import com.leucine.streem.repository.IChecklistRepository;
import com.leucine.streem.repository.ITaskRepository;
import com.leucine.streem.repository.IUserRepository;
import com.leucine.streem.service.impl.TaskDependencyService;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class AddStopDependency {

  private final TaskDependencyService taskDependencyService;
  private final IChecklistRepository checklistRepository;
  private final ITaskRepository taskRepository;
  private final IUserRepository userRepository;
  private final IUserMapper userMapper;

  public BasicDto execute() {

    User principalUserEntity = userRepository.findById(Long.valueOf(Misc.SYSTEM_USER_ID)).get();
    PrincipalUser principalUser = userMapper.toPrincipalUser(principalUserEntity);
    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(principalUser, null, null);
    SecurityContextHolder.getContext().setAuthentication(authentication);

    List<Checklist> checklists = checklistRepository.findAll();
    for (Checklist checklist : checklists) {
      migrateHasStopToDependencyForChecklist(checklist);
    }

    BasicDto basicDto = new BasicDto();
    basicDto.setMessage("success");
    return basicDto;
  }

  public void migrateHasStopToDependencyForChecklist(Checklist checklist) {
    Map<Long, TaskDependencyRequest> taskDependencyRequestMap = new HashMap<>();


    List<TaskDTO> allTaskDTOs = checklist.getStages().stream()
      .sorted(Comparator.comparing(Stage::getOrderTree))
      .flatMap(stage -> stage.getTasks().stream()
        .sorted(Comparator.comparing(Task::getOrderTree)))
      .map(task -> TaskDTO.builder()
        .taskId(task.getId())
        .hasStop(task.isHasStop())
        .modifiedBy(task.getModifiedBy().getId())
        .build())
      .toList();

    List<TaskDependency> existingTaskDependencies = taskDependencyService.getTaskDependenciesByTaskIds(allTaskDTOs.stream().map(TaskDTO::getTaskId).toList());
    Map<Long, List<Long>> taskPrerequisiteMap = existingTaskDependencies.stream()
      .collect(Collectors.groupingBy(taskDependency -> taskDependency.getDependentTask().getId(),
        Collectors.mapping(taskDependency -> taskDependency.getPrerequisiteTask().getId(), Collectors.toList())));
    taskPrerequisiteMap.forEach((dependentTaskId, prerequisiteTaskIds) -> {
      TaskDependencyRequest taskDependencyRequest = TaskDependencyRequest.builder().prerequisiteTaskIds(prerequisiteTaskIds).build();
      taskDependencyRequestMap.put(dependentTaskId, taskDependencyRequest);
    });


    for (int i = 0; i < allTaskDTOs.size(); i++) {
      TaskDTO taskDTO = allTaskDTOs.get(i);
      if (taskDTO.isHasStop()) {
        PrincipalUser principal = PrincipalUser.builder().id(taskDTO.getModifiedBy()).build();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, null));
        List<Long> dependingTaskIds = allTaskDTOs.subList(i + 1, allTaskDTOs.size()).stream().map(TaskDTO::getTaskId).toList();
        for (Long dependingTaskId : dependingTaskIds) {
          TaskDependencyRequest taskDependencyRequest = taskDependencyRequestMap.getOrDefault(dependingTaskId,
            TaskDependencyRequest.builder().prerequisiteTaskIds(Collections.emptyList()).build());
          Set<Long> prerequisiteTaskIds = new HashSet<>(taskDependencyRequest.getPrerequisiteTaskIds());
          prerequisiteTaskIds.add(taskDTO.getTaskId());
          taskDependencyRequest.setPrerequisiteTaskIds(prerequisiteTaskIds.stream().toList());
          taskDependencyRequestMap.put(dependingTaskId, taskDependencyRequest);
        }
      }
    }

    log.info("Processing tasks for checklist ID {}: ", checklist.getId());
    taskDependencyRequestMap.forEach((task, taskDependencyRequest) -> {
      if (!taskDependencyRequest.getPrerequisiteTaskIds().isEmpty()) {
        taskDependencyService.updateTaskDependency(task, taskDependencyRequest);
        log.info("Updated task dependencies for task {}: ", task);
      }
    });
    taskRepository.updateTasksHasStopToFalseForChecklistId(checklist.getId());
  }

  @Getter
  @Setter
  @Builder
  public static class TaskDTO {
    private Long taskId;
    private boolean hasStop;
    private Long modifiedBy;
  }
}
