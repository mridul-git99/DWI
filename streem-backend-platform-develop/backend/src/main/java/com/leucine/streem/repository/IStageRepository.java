package com.leucine.streem.repository;

import com.leucine.streem.constant.Queries;
import com.leucine.streem.constant.State;
import com.leucine.streem.dto.projection.StageLiteView;
import com.leucine.streem.dto.projection.StageTotalTasksView;
import com.leucine.streem.model.Stage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Repository
public interface IStageRepository extends JpaRepository<Stage, Long>, JpaSpecificationExecutor<Stage> {

  @Transactional(rollbackFor = Exception.class)
  @Modifying
  @Query(value = Queries.UPDATE_STAGE_ORDER_BY_STAGE_ID, nativeQuery = true)
  void reorderStage(@Param("stageId") Long stageId, @Param("order") Long order, @Param("userId") Long userId, @Param("modifiedAt") Long modifiedAt);

  @Query(value = Queries.GET_STAGE_BY_TASK_ID)
  Stage findByTaskId(@Param("taskId") Long taskId);

  @Query(value = Queries.GET_STAGES_BY_TASK_IDS)
  List<Stage> findByTaskIds(@Param("taskIds") List<Long> taskIds);

  @Query(value = Queries.GET_STAGE_ID_BY_TASK_ID)
  Long findStageIdByTaskId(@Param("taskId") Long taskId);

  @Query(value = Queries.GET_TOTAL_TASKS_VIEW_BY_CHECKLIST_ID,  nativeQuery = true)
  List<StageTotalTasksView> findByChecklistId(@Param("checklistId") Long checklistId);

  @Query(value = Queries.GET_STAGES_BY_CHECKLIST_ID_AND_ORDER_BY_ORDER_TREE)
  List<Stage> findByChecklistIdOrderByOrderTree(@Param("checklistId") Long checklistId);


  @Query(value = Queries.GET_STAGES_BY_JOB_ID_WHERE_ALL_TASK_EXECUTION_STATE_IN)
  List<Stage> findStagesByJobIdAndAllTaskExecutionStateIn(@Param("jobId") Long jobId, @Param("taskExecutionStates") Set<State.TaskExecution> taskExecutionStates);

  @Modifying(clearAutomatically = true)
  @Transactional
  @Query(value = Queries.INCREASE_ORDER_TREE_BY_ONE_AFTER_STAGE, nativeQuery = true)
  void increaseOrderTreeByOneAfterStage(@Param("checklistId") Long checklistId, @Param("orderTree") Integer orderTree, @Param("stageId") Long newElementId);

  @Query(value = "SELECT s.id AS id, s.name AS name, s.order_tree AS orderTree " +
    "FROM stages s " +
    "WHERE s.checklists_id = :checklistId " +
    "AND s.archived = false " +
    "ORDER BY s.order_tree ASC", nativeQuery = true)
  List<StageLiteView> getStagesByChecklistIdOrdered(@Param("checklistId") Long checklistId);

}
