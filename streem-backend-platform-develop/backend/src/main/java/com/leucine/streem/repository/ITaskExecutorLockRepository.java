package com.leucine.streem.repository;

import com.leucine.streem.constant.Queries;
import com.leucine.streem.dto.projection.TaskExecutorLockErrorView;
import com.leucine.streem.dto.projection.TaskExecutorLockView;
import com.leucine.streem.model.TaskExecutorLock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

public interface ITaskExecutorLockRepository extends JpaRepository<TaskExecutorLock, Long> {
  List<TaskExecutorLock> findByTaskIdAndReferencedTaskIdIn(Long taskId, Set<Long> referencedTaskIds);

  @Modifying
  @Query(value = Queries.REMOVE_TASK_EXECUTOR_LOCK_BY_TASK_ID, nativeQuery = true)
  @Transactional
  void removeByTaskId(@Param("taskId") Long taskId);

  @Modifying
  @Query(value = Queries.REMOVE_TASK_EXECUTOR_LOCK_BY_TASK_ID_OR_REFERENCED_TASK_ID, nativeQuery = true)
  @Transactional
  void removeByTaskIdOrReferencedTaskId(@Param("taskId") Long taskId);


  @Query(value = Queries.FIND_TASK_EXECUTOR_LOCK_BY_TASK_ID, nativeQuery = true)
  List<TaskExecutorLockView> findByTaskId(@Param("taskId") Long taskId);

  List<TaskExecutorLock> findAllByTaskId(Long taskId);


  boolean existsByReferencedTaskId(Long taskId);

  @Query(value = Queries.FIND_ALL_TASKS_WHERE_TASK_EXECUTOR_LOCK_HAS_ONE_REFERENCED_TASK, nativeQuery = true)
  Set<Long> findTasksWhereTaskExecutorLockHasOneReferencedTask(@Param("taskId") Long taskId);


  @Query(value = Queries.FIND_ALL_INVALID_REFERENCED_TASKS_USED_IN_TASK_EXECUTOR_LOCK, nativeQuery = true)
  List<TaskExecutorLockErrorView> findInvalidReferencedTaskExecutions(@Param("taskId") Long taskId, @Param("jobId") Long jobId, @Param("currentUserId") Long currentUserId);
}
