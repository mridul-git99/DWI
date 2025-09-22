package com.leucine.streem.service;

import com.leucine.streem.dto.*;
import com.leucine.streem.dto.projection.TaskExecutionAssigneeView;
import com.leucine.streem.dto.request.*;
import com.leucine.streem.exception.ParameterExecutionException;
import com.leucine.streem.exception.ResourceNotFoundException;
import com.leucine.streem.exception.StreemException;
import com.leucine.streem.model.Task;
import com.leucine.streem.model.TaskExecution;
import com.leucine.streem.model.TaskExecutionUserMapping;
import com.leucine.streem.model.User;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.Set;

public interface ITaskExecutionService {
  TaskDto getTask(Long taskId) throws ResourceNotFoundException;

  TaskExecutionDto startTask(Long taskId, TaskExecutionRequest taskExecutionRequest) throws ResourceNotFoundException, StreemException, IOException, ParameterExecutionException;

  BasicDto validateTask(Long taskId) throws StreemException, ResourceNotFoundException;

  TaskExecutionDto completeTask(Long taskId, TaskCompletionRequest taskCompletionRequest) throws ResourceNotFoundException, StreemException, IOException, ParameterExecutionException;

  @Transactional(rollbackFor = Exception.class)
  TaskExecutionDto repeatTaskExecution(TaskRepeatRequest taskRepeatRequest) throws ResourceNotFoundException, StreemException, IOException;

  TaskExecutionDto skipTask(Long taskId, TaskExecutionRequest taskExecutionRequest) throws ResourceNotFoundException, StreemException, IOException;

  TaskExecutionDto completeWithException(Long taskId, TaskCompletionRequest taskCompletionRequest) throws ResourceNotFoundException, StreemException, IOException;

  TaskExecutionDto enableCorrection(Long taskId, TaskExecutionRequest taskExecutionRequest) throws StreemException;

  TaskExecutionDto completeCorrection(Long taskId, TaskExecutionRequest taskExecutionRequest) throws ResourceNotFoundException, StreemException, IOException;

  TaskExecutionDto cancelCorrection(Long taskId, TaskExecutionRequest taskExecutionRequest) throws ResourceNotFoundException, StreemException;

  List<TaskExecutionAssigneeView> getTaskExecutionAssignees(Set<Long> taskExecutionIds, boolean users, boolean userGroups);

  BasicDto signOff(TaskSignOffRequest taskSignOffRequest) throws StreemException;

  TaskExecution getTaskExecutionByJobAndTaskId(Long id);

  List<TaskExecutionUserMapping> validateAndGetAssignedUser(Long taskId, TaskExecution taskExecution, User user) throws ResourceNotFoundException;

  void updateUserAction(List<TaskExecutionUserMapping> taskExecutionUserMapping);

  boolean isInvalidTimedTaskCompletedState(Task task, Long startedAt, Long endedAt);

  TaskExecutionDto pauseTask(Long taskId, TaskPauseOrResumeRequest taskPauseOrResumeRequest) throws StreemException;

  TaskExecutionDto resumeTask(Long taskId, TaskPauseOrResumeRequest taskPauseOrResumeRequest) throws StreemException;

  BasicDto removeTaskExecution(Long taskExecutionId) throws StreemException, ResourceNotFoundException;

  BasicDto stopTaskExecutionRecurring(Long taskExecutionId) throws StreemException, ResourceNotFoundException;

  UserTaskAssigneeStatusDto checkTaskAssignee(Long taskExecutionId);

  void validateIfUserIsAssignedToExecuteParameter(Long taskExecutionId, Long currentUserId) throws StreemException;

  TaskDetailsDto getTaskData(Long taskExecutionId) throws ResourceNotFoundException;
}
