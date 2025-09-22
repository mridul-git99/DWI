package com.leucine.streem.service.impl;


import com.leucine.streem.dto.TaskDependencyDetailsDto;
import com.leucine.streem.dto.TaskDependencyStageDetailsDto;
import com.leucine.streem.dto.TaskDependencyTaskDetailsDto;
import com.leucine.streem.dto.request.TaskDependencyRequest;
import com.leucine.streem.model.Stage;
import com.leucine.streem.model.Task;
import com.leucine.streem.model.TaskDependency;
import com.leucine.streem.model.User;
import com.leucine.streem.model.helper.PrincipalUser;
import com.leucine.streem.repository.IStageRepository;
import com.leucine.streem.repository.ITaskDependencyRepository;
import com.leucine.streem.repository.ITaskRepository;
import com.leucine.streem.repository.IUserRepository;
import com.leucine.streem.service.ITaskDependencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
public class TaskDependencyService implements ITaskDependencyService {

  private final ITaskDependencyRepository taskDependencyRepository;
  private final ITaskRepository taskRepository;
  private final IUserRepository userRepository;
  private final IStageRepository stageRepository;

  @Override
  public List<Long> getTaskDependenciesByTaskId(Long taskId) {
    List<TaskDependency> taskDependencies = taskDependencyRepository.findAllByDependentTaskId(taskId);
    return taskDependencies.stream().map(taskDependency -> taskDependency.getPrerequisiteTask().getId()).collect(Collectors.toList());
  }

  @Override
  public List<TaskDependency> getTaskDependenciesByTaskIds(List<Long> taskIds) {
    return taskDependencyRepository.findAllByDependentTaskIdIn(taskIds);
  }

  @Override
  public List<Long> updateTaskDependency(Long taskId, TaskDependencyRequest taskDependencyRequest) {
    // Retrieve the authenticated user entity directly
    User principalUserEntity = userRepository.getOne(((PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getId());

    List<Long> prerequisiteTaskIds = taskDependencyRequest.getPrerequisiteTaskIds();
    Task dependentTask = taskRepository.findById(taskId).orElseThrow(); // Assumes existence or throws

    // Current prerequisites as a Set for better performance in lookup
    Set<Long> currentPrerequisiteIds = taskDependencyRepository.findAllByDependentTaskId(taskId).stream()
      .map(taskDependency -> taskDependency.getPrerequisiteTask().getId())
      .collect(Collectors.toSet());

    // Tasks to be removed
    List<TaskDependency> toRemove = taskDependencyRepository.findAllByDependentTaskId(taskId).stream()
      .filter(taskDependency -> !prerequisiteTaskIds.contains(taskDependency.getPrerequisiteTask().getId()))
      .collect(Collectors.toList());
    taskDependencyRepository.deleteAll(toRemove);

    // New prerequisite IDs to add, using Set for efficiency
    Set<Long> newPrerequisiteIds = new HashSet<>(prerequisiteTaskIds);
    newPrerequisiteIds.removeAll(currentPrerequisiteIds);

    // Fetch and add new dependencies if necessary
    if (!newPrerequisiteIds.isEmpty()) {
      List<Task> prerequisiteTasks = taskRepository.findAllById(newPrerequisiteIds);
      List<TaskDependency> addedDependencies = prerequisiteTasks.stream().map(prerequisiteTask -> {
        TaskDependency taskDependency = new TaskDependency();
        taskDependency.setDependentTask(dependentTask);
        taskDependency.setPrerequisiteTask(prerequisiteTask);
        taskDependency.setCreatedBy(principalUserEntity);
        taskDependency.setModifiedBy(principalUserEntity);
        return taskDependency;
      }).collect(Collectors.toList());

      taskDependencyRepository.saveAll(addedDependencies);
    }

    return prerequisiteTaskIds;
  }


  @Override
  public TaskDependencyDetailsDto getTaskDependenciesDetailsByTaskId(Long taskId) {
    List<TaskDependency> taskDependencies = taskDependencyRepository.findAllByDependentTaskId(taskId);
    List<Long> prerequisiteTasks = taskDependencies.stream().map(taskDependency -> taskDependency.getPrerequisiteTask().getId()).collect(Collectors.toList());
    return buildTaskDependencyDetailsDto(prerequisiteTasks);
  }


  private TaskDependencyDetailsDto buildTaskDependencyDetailsDto(List<Long> taskIds) {
    List<Task> tasks = taskRepository.findAllById(taskIds); // Fetch tasks by IDs
    List<Stage> stages = stageRepository.findByTaskIds(taskIds);

    Map<Long, Stage> stageIdStagesMap = new HashSet<>(stages).stream().collect(Collectors.toMap(Stage::getId, Function.identity()));

    TaskDependencyDetailsDto taskDependencyDetailsDto = new TaskDependencyDetailsDto();

    Map<Long, List<Task>> stageIdTasksMap = tasks.stream()
      .collect(Collectors.groupingBy(Task::getStageId));

    stageIdTasksMap.forEach((stageId, tasksList) -> {
      Stage stage = stageIdStagesMap.get(stageId);
      List<TaskDependencyTaskDetailsDto> taskDependencyTaskDetailsDtos = tasksList.stream()
        .map(task -> new TaskDependencyTaskDetailsDto(task.getIdAsString(), task.getName(), task.getOrderTree()))
        .collect(Collectors.toList());

      TaskDependencyStageDetailsDto taskDependencyStageDetailsDto = new TaskDependencyStageDetailsDto(
        stage.getIdAsString(), stage.getName(), stage.getOrderTree(), taskDependencyTaskDetailsDtos);

      taskDependencyDetailsDto.getStages().add(taskDependencyStageDetailsDto);
    });

    taskDependencyDetailsDto.getStages().sort(Comparator.comparingInt(TaskDependencyStageDetailsDto::getOrderTree));
    taskDependencyDetailsDto.getStages().forEach(stage -> stage.getTasks().sort(Comparator.comparingInt(TaskDependencyTaskDetailsDto::getOrderTree)));

    return taskDependencyDetailsDto;
  }


}
