package com.leucine.streem.repository;

import com.leucine.streem.constant.Queries;
import com.leucine.streem.constant.Type;
import com.leucine.streem.dto.EngagedUserView;
import com.leucine.streem.dto.TaskExecutionLiteView;
import com.leucine.streem.dto.TaskPauseResumeAuditDto;
import com.leucine.streem.dto.TaskPendingOnMeView;
import com.leucine.streem.dto.projection.JobLogTaskExecutionView;
import com.leucine.streem.dto.projection.JobPendingTaskCountProjection;
import com.leucine.streem.dto.projection.TaskDetailsView;
import com.leucine.streem.dto.projection.TaskExecutionView;
import com.leucine.streem.dto.projection.TaskPauseResumeAuditView;
import com.leucine.streem.model.TaskExecution;
import org.springframework.data.jpa.repository.EntityGraph;
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
public interface ITaskExecutionRepository extends JpaRepository<TaskExecution, Long> {

  @Query(value = Queries.READ_TASK_EXECUTION_BY_JOB_AND_STAGE_ID, nativeQuery = true)
  List<TaskExecution> readByJobIdAndStageIdOrderByOrderTree(@Param("jobId") Long jobId, @Param("stageId") Long stageId);

  @Modifying(clearAutomatically = true)
  @Query(value = Queries.UPDATE_TASK_EXECUTION_ENABLE_CORRECTION, nativeQuery = true)
  void enableCorrection(@Param("correctionReason") String correctionReason, @Param("id") Long id);

  @Modifying(clearAutomatically = true)
  @Query(value = Queries.UPDATE_TASK_EXECUTION_CANCEL_CORRECTION, nativeQuery = true)
  void cancelCorrection(@Param("id") Long id);

  @EntityGraph("readTaskExecution")
  List<TaskExecution> readByJobIdAndTaskIdIn(@Param("jobId") Long jobId, @Param("taskIds") List<Long> taskIds);

  @Query(value = Queries.GET_NON_COMPLETED_TASKS_BY_JOB_ID, nativeQuery = true)
  List<Long> findNonCompletedTaskIdsByJobId(@Param("jobId") Long jobId);

  @Query(value = Queries.GET_ENABLED_FOR_CORRECTION_TASKS_BY_JOB_ID, nativeQuery = true)
  List<Long> findEnabledForCorrectionTaskIdsByJobId(@Param("jobId") Long jobId);

  @Query(value = Queries.GET_NON_SIGNED_OFF_TASKS_BY_JOB_AND_USER_ID, nativeQuery = true)
  List<Long> findNonSignedOffTaskIdsByJobIdAndUserId(@Param("jobId") Long jobId, @Param("userId") Long userId);

  @Query(value = Queries.GET_TASK_EXECUTION_COUNT_BY_JOB_ID, nativeQuery = true)
  Integer getTaskExecutionCountByJobId(@Param("jobId") Long jobId);


  @Query(value = """
    SELECT * FROM task_executions te
             INNER JOIN tasks t ON te.tasks_id = t.id
             WHERE te.jobs_id = :jobId AND t.stages_id IN :stageIds
    """, nativeQuery = true)
  List<TaskExecution> findByJobIdAndStageIdIn(@Param("jobId") Long jobId, @Param("stageIds") Set<Long> stageIds);

  @Query(value = Queries.FIND_TASK_EXECUTION_DETAILS_BY_JOB_ID, nativeQuery = true)
  List<JobLogTaskExecutionView> findTaskExecutionDetailsByJobId(@Param("jobIds") Set<Long> jobIds);

  TaskExecution findByTaskIdAndJobIdAndType(@Param("taskId") Long taskId, @Param("jobId") Long jobId, @Param("type") Type.TaskExecutionType type);

  @Query(value = Queries.FIND_LATEST_TASK_EXECUTION_BY_TASK_ID_AND_JOB_ID_ORDER_BY_ORDER_TREE_DESC, nativeQuery = true)
  TaskExecution findByTaskIdAndJobIdOrderByOrderTree(@Param("taskId") Long taskId, @Param("jobId") Long jobId);

  @Query(value = Queries.FIND_TASK_EXECUTIONS_NOT_IN_COMPLETED_STATE_BY_TASK_ID_AND_JOB_ID, nativeQuery = true)
  List<TaskExecutionView> findAllTaskExecutionsNotInCompletedStateByTaskIdAndJobId(@Param("taskId") Long taskId, @Param("jobId") Long jobId);

  @Query(value = Queries.DELETE_BY_TASK_EXECUTION_ID, nativeQuery = true)
  @Transactional(rollbackFor = Exception.class)
  @Modifying(clearAutomatically = true)
  void deleteByTaskExecutionId(@Param("id") Long id);


  @Query(value = Queries.CHECK_IF_ANY_TASK_RECURRENCE_CONTAINS_STOP_RECURRENCE, nativeQuery = true)
  boolean checkIfAnyTaskExecutionContainsStopRecurrence(@Param("taskId") Long taskId, @Param("jobId") Long jobId);

  @Query(value = Queries.SET_CONTINUE_RECCURENCE_TO_FALSE, nativeQuery = true)
  @Transactional(rollbackFor = Exception.class)
  @Modifying(clearAutomatically = true)
  void setAllTaskExecutionsContinueRecurrenceFalse(@Param("taskId") Long taskId, @Param("jobId") Long jobId);

  @Query(value = Queries.FIND_ALL_STARTED_TASK_EXECUTIONS_AFTER_ORDER_TREE, nativeQuery = true)
  List<TaskExecutionView> findAllStartedTaskExecutionsAfterStageOrderTree(@Param("orderTree") Integer orderTree, @Param("jobId") Long jobId);

  @Query(value = Queries.FIND_ALL_NON_COMPLETED_TASK_EXECUTIONS_BEFORE_ORDER_TREE_WHICH_HAS_ADD_STOP, nativeQuery = true)
  List<TaskExecutionView> findAllNonCompletedTaskExecutionBeforeCurrentStageAndHasStop(@Param("orderTree") Integer orderTree, @Param("jobId") Long jobId);

  @Query(value = Queries.FIND_ALL_TASK_EXECUTION_WITH_SCHEDULING_ENABLED)
  List<TaskExecution> findAllTaskExecutionsWithJobSchedule(@Param("jobId") Long jobId);

  @Query(value = Queries.FIND_ALL_STARTED_TASK_EXECUTIONS_IN_A_STAGE_AFTER_TASK_ORDER_TREE, nativeQuery = true)
  List<TaskExecutionView> findAllStartedTaskExecutionsOfStage(@Param("jobId") Long jobId, @Param("taskOrderTree") Integer taskOrderTree, @Param("stageId") Long stageId);


  @Query(value = Queries.FIND_ALL_NON_COMPLETED_TASK_EXECUTIONS_OF_CURRENT_STAGE_BEFORE_ORDER_TREE_WHICH_HAS_ADD_STOP, nativeQuery = true)
  List<TaskExecutionView> findAllNonCompletedTaskExecutionOfCurrentStageAndHasStop(@Param("stageId") Long stageId, @Param("orderTree") Integer orderTree, @Param("jobId") Long jobId);

  @Query(value = Queries.GET_ENABLED_FOR_CORRECTION_TASKS_BY_JOB_ID_AND_TASK_ID, nativeQuery = true)
  List<Long> findEnabledForCorrectionTaskExecutionIdsByJobIdAndTaskId(@Param("jobId") Long jobId, @Param("taskId") Long taskId);

  @Query(value = Queries.FIND_TASK_EXECUTIONS_NOT_IN_COMPLETED_STATE_OR_NOT_STARTED_BY_TASK_ID_AND_JOB_ID, nativeQuery = true)
  List<TaskExecutionView> findAllTaskExecutionsNotInCompletedOrNotInStartedStatedByTaskIdAndJobId(@Param("taskId") Long taskId, @Param("jobId") Long jobId);

  @Query(value = Queries.FIND_TASK_EXECUTION_ENABLED_FOR_CORRECTION, nativeQuery = true)
  TaskExecution findTaskExecutionEnabledForCorrection(@Param("taskId") Long taskId, @Param("jobId") Long jobId);

  @Query(value = Queries.GET_PENDING_TASKS_OF_USER_FOR_JOB, nativeQuery = true)
  List<TaskPendingOnMeView> getPendingTasksOfUserForJobs(@Param("jobIds") Set<Long> jobIds, @Param("userId") Long userId, @Param("jobPendingStates") Set<String> jobPendingStates);

  @Query(value = Queries.GET_ENGAGED_USERS_FOR_JOB, nativeQuery = true)
  List<EngagedUserView> getEngagedUsersForJob(@Param("jobIds") Set<Long> jobIds);


  @Query(value = Queries.CHECK_INCOMPLETE_DEPENDENCIES, nativeQuery = true)
  List<TaskExecution> findIncompleteDependencies(@Param("taskId") Long taskId, @Param("jobId") Long jobId);

  @Query(value = Queries.READ_TASK_EXECUTION_BY_JOB_ID, nativeQuery = true)
  List<TaskExecution> readByJobId(@Param("jobId") Long jobId, @Param("checklistId") Long checklistId);

  @Query(value = Queries.GET_ALL_LATEST_DEPENDANT_TASK_EXECUTION_IDS_HAVING_PREREQUISITE_TASK_ID, nativeQuery = true)
  List<TaskExecutionView> getAllLatestDependantTaskExecutionIdsHavingPrerequisiteTaskId(@Param("preRequisiteTaskId") Long preRequisiteTaskId, @Param("jobId") Long jobId);


  @Query(value = Queries.GET_ALL_COMPLETED_PREREQUISITE_TASK_DETAILS, nativeQuery = true)
  List<TaskExecutionView> getAllCompletedPreRequisiteTaskDetails(@Param("dependantTaskId") Long taskId, @Param("jobId") Long jobId);

  @Query(value = Queries.GET_TASK_EXECUTION_LITE_BY_JOB_ID, nativeQuery = true)
  List<TaskExecutionLiteView> getTaskExecutionsLiteByJobId(@Param("jobId") Long jobId);

  @Query(value = Queries.GET_TASK_PAUSE_RESUME_AUDIT_BY_TASK_EXECUTION_ID, nativeQuery = true)
  List<TaskPauseResumeAuditView> getTaskPauseResumeAuditDtoByTaskExecutionId(@Param("taskExecutionId") Long taskExecutionId);

  @Query(value = """
              SELECT te.id as taskExecutionId, te.tasks_id as taskId FROM task_executions te WHERE te.jobs_id = :jobId AND te.tasks_id IN :taskIds
          """, nativeQuery = true)
  Set<TaskDetailsView> findAllByJobIdAndTaskIdIn(@Param("jobId") Long jobId, @Param("taskIds") Set<String> taskIds);

}
