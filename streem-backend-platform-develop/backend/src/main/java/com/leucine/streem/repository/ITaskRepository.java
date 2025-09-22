package com.leucine.streem.repository;

import com.leucine.streem.constant.Queries;
import com.leucine.streem.dto.projection.TaskAssigneeView;
import com.leucine.streem.dto.projection.TaskLiteView;
import com.leucine.streem.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Repository
@Transactional(rollbackFor = Exception.class)
public interface ITaskRepository extends JpaRepository<Task, Long> {
  @Query(value = Queries.GET_TASK_BY_PARMETER_ID)
  Task findByParameterId(@Param("parameterId") Long parameterId);

  @Transactional(rollbackFor = Exception.class)
  @Modifying
  @Query(value = Queries.UPDATE_TASK_ORDER, nativeQuery = true)
  void reorderTask(@Param("taskId") Long taskId, @Param("order") Long order, @Param("userId") Long userId, @Param("modifiedAt") Long modifiedAt);

  @Query(value = Queries.GET_TASKS_BY_STAGE_ID_IN_AND_ORDER_BY_ORDER_TREE)
  List<Task> findByStageIdInOrderByOrderTree(@Param("stageIds") Set<Long> stageIds);

  @Query(value = Queries.GET_TASK_USER_MAPPING_BY_TASK_IN, nativeQuery = true)
  List<TaskAssigneeView> findByTaskIdIn(@Param("checklistId") Long checklistId, @Param("taskIds") Set<Long> taskIds, @Param("totalTaskIds") int totalTaskIds, @Param("facilityId") Long facilityId, @Param("isUser") boolean isUser, @Param("isUserGroup") boolean isUserGroup);

  List<Task> findAllByIdInAndArchived(Set<Long> ids, boolean archived);

  Task findByTaskSchedulesId(Long taskSchedulesId);

  @Query(Queries.FIND_ALL_NON_ARCHIVED_TASK_OF_PROCESS_WHERE_SCHEDULING_IS_ENABLED)
  List<Task> findAllTaskByEnableSchedulingAndChecklistId(@Param("checklistId") Long checklistId, @Param("enableScheduling") boolean enableScheduling);

  @Query(value = Queries.FIND_ALL_NON_ARCHIVED_TASK_OF_PROCESS_WHERE_RECURRENCE_IS_ENABLED, nativeQuery = true)
  List<Task> findAllTaskByEnableRecurrenceAndChecklistId(@Param("checklistId") Long checklistId, @Param("enableRecurrence") boolean enableRecurrence);

  @Modifying(clearAutomatically = true)
  @Query(value = Queries.UPDATE_HAS_TASK_EXECUTOR_LOCK_FLAG, nativeQuery = true)
  @Transactional
  void updateHasTaskExecutorLock(@Param("tasksWhereTaskExecutorLockIsUsedOnce") Set<Long> tasksWhereTaskExecutorLockIsUsedOnce, @Param("isExecutorLock") boolean isExecutorLock);

  @Modifying
  @Query(value = Queries.UPDATE_TASKS_HAS_STOP_TO_FALSE_FOR_CHECKLIST_ID, nativeQuery = true)
  void updateTasksHasStopToFalseForChecklistId(@Param("checklistId") Long checklistId);

  @Query(value = Queries.GET_ALL_TASK_IDS_BY_STAGE_ID, nativeQuery = true)
  Set<Long> getAllTaskIdsByStageId(@Param("stageId") Long stageId);

  @Modifying(clearAutomatically = true)
  @Transactional
  @Query(value = Queries.UPDATE_HAS_INTERLOCKS_FLAG, nativeQuery = true)
  void updateHasInterlocks(@Param("tasksId") Long taskId ,@Param("flag") boolean flag);

  @Modifying(clearAutomatically = true)
  @Transactional
  @Query(value = Queries.DELETE_HAS_INTERLOCKS_FLAG, nativeQuery = true)
  void deleteHasInterlocks(@Param("interlocksIds") Long interlocksIds);

  @Modifying(clearAutomatically = true)
  @Transactional
  @Query(value = Queries.REMOVE_HAS_INTERLOCKS_FLAG, nativeQuery = true)
  void removeHasInterlocks(@Param("tasksId") Long taskId);

  @Transactional
  @Modifying(clearAutomatically = true)
  @Query(value = Queries.INCREASE_ORDER_TREE_BY_ONE_AFTER_TASK, nativeQuery = true)
  void increaseOrderTreeByOneAfterTask(@Param("stageId") Long stageId, @Param("orderTree") Integer orderTree, @Param("taskId") Long newElementId);

  @Query(value = "SELECT t.id as id, t.name as name, t.order_tree as orderTree, t.stages_id as stageId " +
    "FROM tasks t " +
    "WHERE t.archived = false AND t.stages_id IN :stageIds " +
    "ORDER BY t.order_tree ASC", nativeQuery = true)
  List<TaskLiteView> findTaskLiteInfoByStageIds(@Param("stageIds") Set<Long> stageIds);

  @Transactional
  @Modifying
  @Query(value = Queries.UPDATE_HAS_EXECUTOR_LOCK_BY_IDS, nativeQuery = true)
  void updateHasExecutorLockByIds(@Param("taskIds") Set<Long> taskIds, @Param("hasExecutorLock") boolean hasExecutorLock);

}
