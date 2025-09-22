package com.leucine.streem.repository;

import com.leucine.streem.constant.Queries;
import com.leucine.streem.dto.projection.*;
import com.leucine.streem.model.TaskExecutionUserMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface ITaskExecutionAssigneeRepository extends JpaRepository<TaskExecutionUserMapping, Long> {
  @Query(value = Queries.IS_USER_ASSIGNED_TO_ANY_TASK, nativeQuery = true)
  boolean isUserAssignedToAnyTask(@Param("jobId") Long jobId, @Param("userId") Long userId);

  @Query(value = Queries.IS_ALL_TASK_UNASSIGNED, nativeQuery = true)
  boolean isAllTaskUnassigned(@Param("jobId") Long jobId);

  @Query(value = Queries.GET_ALL_TASK_ASSIGNEES_DETAILS_BY_JOB_ID, nativeQuery = true)
  List<TaskExecutionAssigneeDetailsView> findByJobId(@Param("jobId") Long jobId, @Param("totalExecutionIds") Integer totalExecutionIds);

  @Query(value = Queries.GET_ALL_JOB_ASSIGNEES, nativeQuery = true)
  List<JobAssigneeView> getJobAssignees(@Param("jobIds") Set<Long> jobIds);

  @Query(value = Queries.GET_ALL_JOB_ASSIGNEES_COUNT, nativeQuery = true)
  Integer getJobAssigneesCount(@Param("jobId") Long jobId);

  @Query(value = Queries.IS_USER_ASSIGNED_TO_IN_PROGRESS_TASKS, nativeQuery = true)
  Boolean isUserAssignedToInProgressTasks(@Param("userId") Long userId);

  @Query(value = Queries.GET_TASK_EXECUTION_USER_MAPPING_BY_TASK_EXECUTION_AND_USER_ID_IN, nativeQuery = true)
  List<TaskExecutionAssigneeBasicView> findByTaskExecutionIdAndUserIdIn(@Param("taskExecutionId") Long taskExecutionId,
                                                                        @Param("userIds") Set<Long> userIds);

  @Query(value = Queries.GET_TASK_EXECUTION_USER_MAPPING_BY_TASK_EXECUTION_IN_AND_USER_ID_IN, nativeQuery = true)
  List<TaskExecutionAssigneeBasicView> findByTaskExecutionIdInAndUserIdIn(@Param("taskExecutionIds") Set<Long> taskExecutionIds,
                                                                          @Param("userIds") Set<Long> userIds);

  @Query(value = Queries.GET_TASK_EXECUTION_USER_MAPPING_BY_TASK_EXECUTION_IN, nativeQuery = true)
  List<TaskExecutionAssigneeView> findByTaskExecutionIdIn(@Param("taskExecutionIds") Set<Long> taskExecutionIds, @Param("totalExecutionIds") Integer totalExecutionIds, @Param("isUser") boolean users, @Param("isUserGroup") boolean userGroups);

  @Transactional(rollbackFor = Exception.class)
  @Modifying(clearAutomatically = true)
  @Query(value = Queries.UNASSIGN_USERS_FROM_NON_STARTED_AND_IN_PROGRESS_TASKS, nativeQuery = true)
  Set<Long> unassignUsersFromNonStartedAndInProgessTasks(@Param("userId") Long userId);

  @Query(value = """
    SELECT teum.*
    FROM task_execution_user_mapping teum
    WHERE teum.task_executions_id = :taskExecutionId
      AND (teum.users_id = :userId or teum.user_groups_id in (select ugm.groups_id from user_group_members ugm where ugm.users_id = :userId))
    """, nativeQuery = true)
  Optional<List<TaskExecutionUserMapping>> findByTaskExecutionAndUser(@Param("taskExecutionId") Long taskExecutionId, @Param("userId") Long userId);

  @Modifying(clearAutomatically = true)
  @Transactional(rollbackFor = Exception.class)
  @Query(value = Queries.UNASSIGN_USERS_FROM_TASK_EXECUTIONS, nativeQuery = true)
  void unassignUsersByTaskExecutions(@Param("taskExecutionIds") Set<Long> taskExecutionIds, @Param("userIds") Set<Long> userIds);

  @Modifying(clearAutomatically = true)
  @Transactional(rollbackFor = Exception.class)
  @Query(value = Queries.UPDATE_TASK_ASSIGNEE_STATE, nativeQuery = true)
  void updateAssigneeState(@Param("state") String state, @Param("userId") Long userId, @Param("taskExecutionIds") Set<Long> taskExecutionIds,
                           @Param("modifiedBy") Long modifiedBy, @Param("modifiedAt") Long modifiedAt);


  @Query(value = Queries.GET_TASK_EXECUTION_USER_MAPPING_BY_TASK_EXECUTION_AND_USER_GROUP_ID_IN, nativeQuery = true)
  List<TaskExecutionAssigneeBasicView> findByTaskExecutionIdInAndUserGroupIdIn(@Param("taskExecutionIds") Set<Long> taskExecutionIds, @Param("assignedUserGroupIds") Set<Long> assignedUserGroupIds);

  @Modifying(clearAutomatically = true)
  @Transactional(rollbackFor = Exception.class)
  @Query(value = Queries.UNASSIGN_USER_GROUPS_FROM_TASK_EXECUTIONS, nativeQuery = true)
  void unassignUserGroupIdsByTaskExecutions(@Param("taskExecutionIds") Set<Long> taskExecutionIds, @Param("userGroupsId") Set<Long> userGroupsId);

  @Query(value = """
      SELECT
      distinct ug.id as id, ug.name as name
       FROM task_execution_user_mapping teum
      inner join task_executions te on teum.task_executions_id = te.id
      inner join user_groups ug on teum.user_groups_id = ug.id
      AND (CAST(:query AS VARCHAR) IS NULL OR ug.name ILIKE CONCAT('%', CAST(:query AS VARCHAR), '%'))
      WHERE te.jobs_id = :jobId
    """, nativeQuery = true)
  List<UserGroupView> getUserGroupAssignees(@Param("jobId") Long jobId, @Param("query") String query);

  @Query(value = """
    SELECT exists(SELECT 1
                  FROM task_execution_user_mapping teum
                  WHERE teum.task_executions_id = :taskExecutionId
                    AND (teum.users_id = :currentUserId or
                         (select exists(select 1
                                        from user_group_members ugm
                                        where ugm.users_id = :currentUserId
                                          and teum.user_groups_id = ugm.groups_id))))
    """, nativeQuery = true)
  boolean existsByTaskExecutionIdAndUserId(@Param("taskExecutionId") Long taskExecutionId, @Param("currentUserId") Long currentUserId);

  boolean existsByTaskExecutionIdAndUserGroupId(Long taskExecutionId, Long userGroupId);


  @Query(value = Queries.IS_USER_GROUP_ASSIGNED_TO_IN_PROGRESS_TASKS, nativeQuery = true)
  Boolean isUserGroupAssignedToInProgressTasks(@Param("userGroupId") Long userGroupId);


  @Transactional
  @Modifying(clearAutomatically = true)
  @Query(value = Queries.REMOVE_USER_GROUP_ASSIGNEES, nativeQuery = true)
  void removeUserGroupAssignees(@Param("userGroupId") Long userGroupId);

  @Query(value = Queries.GET_ALL_JOB_ASSIGNEES_WITH_USER_GROUP_USERS, nativeQuery = true)
  List<JobAssigneeView> getAllJobAssigneesUsersAndUserGroups(@Param("jobId") Long jobId, @Param("query") String query);

  @Query(value = Queries.GET_ALL_JOB_ASSIGNEES_WITH_USER_GROUP_USERS_BY_ROLES, nativeQuery = true)
  List<JobAssigneeView> getAllJobAssigneesUsersAndUserGroupsByRoles(@Param("jobId") Long jobId, @Param("query") String query, @Param("roles")List<String> roles);

  @Modifying
  @Transactional
  @Query(value = Queries.DELETE_TASK_EXECUTION_FOR_ID, nativeQuery = true)
  void deleteAllByTaskExecutionId(@Param("taskExecutionId") Long taskExecutionId);

  @Query(value = Queries.GET_ALL_TASK_ASSIGNEES_DETAILS_BY_TASK_EXECUTION_IDS, nativeQuery = true)
  List<TaskExecutionAssigneeBasicView> findAllByTaskExecutionIdsIn(@Param("taskExecutionIds")Set<Long> taskExecutionIds);

  @Query(value = Queries.GET_ALL_TASK_EXECUTION_ASSIGNEES_BY_TASK_EXECUTION_ID, nativeQuery = true)
  List<TaskExecutionAssigneeBasicView> findAllByTaskExecutionId(@Param("taskExecutionId") Long taskExecutionId);

  @Transactional
  @Modifying
  @Query(value = """
    UPDATE task_execution_user_mapping SET action_performed = TRUE WHERE action_performed = FALSE AND id in :taskExecutionUserMapping
    """, nativeQuery = true)
  void updateUserAction(@Param("taskExecutionUserMapping") Set<Long> taskExecutionUserMappingList);

}
