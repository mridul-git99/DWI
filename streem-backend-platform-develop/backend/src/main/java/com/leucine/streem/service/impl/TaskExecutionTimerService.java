package com.leucine.streem.service.impl;

import com.leucine.streem.constant.State;
import com.leucine.streem.dto.TaskPauseReasonOrComment;
import com.leucine.streem.dto.request.TaskPauseOrResumeRequest;
import com.leucine.streem.model.TaskExecution;
import com.leucine.streem.model.TaskExecutionTimer;
import com.leucine.streem.model.User;
import com.leucine.streem.model.helper.BaseEntity;
import com.leucine.streem.repository.ITaskExecutionTimerRepository;
import com.leucine.streem.service.ITaskExecutionTimerService;
import com.leucine.streem.util.DateTimeUtils;
import com.leucine.streem.util.Utility;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Slf4j
public class TaskExecutionTimerService implements ITaskExecutionTimerService {
  private final ITaskExecutionTimerRepository taskExecutionTimerRepository;

  @Override
  public void saveTaskPauseTimer(TaskPauseOrResumeRequest taskPauseOrResumeRequest, TaskExecution taskExecution, User principalUserEntity) {
    log.info("Saving task pause timer for task execution id: {}", taskExecution.getId());
    long now = DateTimeUtils.now();
    TaskExecutionTimer pauseTimer = TaskExecutionTimer.builder()
      .taskExecutionId(taskExecution.getId())
      .pausedAt(now)
      .reason(taskPauseOrResumeRequest.reason())
      .comment(taskPauseOrResumeRequest.comment())
      .build();

    pauseTimer.setCreatedBy(principalUserEntity);
    pauseTimer.setCreatedAt(now);
    pauseTimer.setModifiedAt(now);
    pauseTimer.setModifiedBy(principalUserEntity);
    taskExecutionTimerRepository.save(pauseTimer);
  }

  @Override
  public Map<Long, List<TaskPauseReasonOrComment>> calculateDurationAndReturnReasonsOrComments(List<TaskExecution> taskExecutionList) {
    Map<Long, List<TaskPauseReasonOrComment>> reasonOrCommentsMap = new HashMap<>();
    Set<Long> taskExecutionIds = taskExecutionList.stream().map(BaseEntity::getId).collect(Collectors.toSet());

    List<TaskExecutionTimer> taskExecutionTimerList = taskExecutionTimerRepository.findAllByTaskExecutionIdIn(taskExecutionIds);
    Map<Long, List<TaskExecutionTimer>> taskExecutionTimerMap = taskExecutionTimerList.stream().collect(Collectors.groupingBy(TaskExecutionTimer::getTaskExecutionId));

    for (TaskExecution taskExecution : taskExecutionList) {
      if (taskExecution.getState() != State.TaskExecution.NOT_STARTED) {
        long duration;
        long now = DateTimeUtils.now();
        List<TaskExecutionTimer> totalTaskDurationList = taskExecutionTimerMap.get(taskExecution.getId());
        if (!Utility.isEmpty(totalTaskDurationList)) {
          List<TaskPauseReasonOrComment> taskPauseReasonOrCommentList = new ArrayList<>();

          // Check if task is started
          if (taskExecution.getStartedAt() != null) {
            int totalPauseResumeCount = 0;
            long pauseDuration = 0L, resumeDuration = 0L;

            for (TaskExecutionTimer timer : totalTaskDurationList) {
              if (!Utility.isEmpty(timer.getPausedAt())) {
                pauseDuration = pauseDuration + timer.getPausedAt();
                totalPauseResumeCount += 1;
              }
              if (!Utility.isEmpty(timer.getResumedAt())) {
                resumeDuration = resumeDuration + timer.getResumedAt();
                totalPauseResumeCount += 1;
              }
              if (!Utility.isEmpty(timer.getReason()) || !Utility.isEmpty(timer.getComment())) {
                taskPauseReasonOrCommentList.add(new TaskPauseReasonOrComment(timer.getReason(), timer.getComment()));
              }
            }

            pauseDuration = pauseDuration - taskExecution.getStartedAt();

            if (totalPauseResumeCount % 2 == 0) {
              pauseDuration = pauseDuration + now;
            }

            duration = Math.abs(resumeDuration - pauseDuration);
            taskExecution.setDuration(duration);
          }
          reasonOrCommentsMap.put(taskExecution.getId(), taskPauseReasonOrCommentList);
        } else {
          // This check is done because task execution with states SKIPPED or CWE can have null startedAt
          if (!State.TASK_EXECUTION_EXCEPTION_STATE.contains(taskExecution.getState())) {
            duration = now - taskExecution.getStartedAt();
            taskExecution.setDuration(duration);
          }
        }
      }
    }
    return reasonOrCommentsMap;
  }
}
