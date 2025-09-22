package com.leucine.streem.repository;

import com.leucine.streem.constant.Queries;
import com.leucine.streem.dto.projection.TrainedUsersView;
import com.leucine.streem.model.TrainedUserTaskMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Repository
public interface ITrainedUserTaskMappingRepository extends JpaRepository<TrainedUserTaskMapping, Long> {
  //  @Query(Queries.GET_CHECKLIST_DEFAULT_USER_IDS_BY_CHECKLIST_ID)
  @Query(value = """
    SELECT tu.users_id
    FROM trained_user_tasks_mapping tutm
             INNER JOIN trained_users tu ON tutm.trained_users_id = tu.id
    WHERE tu.checklists_id = :checklistId
      AND tu.facilities_id = :facilityId
      AND tu.users_id IS NOT NULL
    """, nativeQuery = true)
  Set<Long> findUserIdsByChecklistIdAndFacilityId(@Param("checklistId") Long checklistId, @Param("facilityId") Long facilityId);

  //  @Query(Queries.GET_TASK_IDS_BY_CHECKLIST_ID_AND_USER_ID)
  @Query(value = """
    SELECT tutm.tasks_id AS taskId, tu.users_id AS userId
          FROM trained_user_tasks_mapping tutm
                   INNER JOIN trained_users tu ON tutm.trained_users_id = tu.id
          WHERE tu.checklists_id = :checklistId
            AND tu.users_id IN (:userIds)
            AND tu.facilities_id = :facilityId
    """, nativeQuery = true)
  Set<TrainedUsersView> findTaskIdsByChecklistIdAndUserIdAndFacilityId(@Param("checklistId") Long checklistId, @Param("userIds") Set<Long> userIds, @Param("facilityId") Long facilityId);

//  @Query(value = Queries.EXISTS_CHECKLIST_DEFAULT_USERS_BY_CHECKLIST_ID_AND_USER_ID, nativeQuery = true)
//  boolean checkExistsChecklistDefaultUsersByChecklistIdAndUserId(@Param("checklistId") Long checklistId, @Param("userId") Long userId);

  @Query(value = Queries.FIND_ALL_TRAINED_USERS_BY_CHECKLIST_ID_AND_FACILITYID, nativeQuery = true)
  List<TrainedUsersView> findAllByChecklistIdAndFacilityId(@Param("checklistId") Long checklistId, @Param("facilityId") Long facilityId, @Param("isUser") Boolean isUser, @Param("isUserGroup") Boolean isUserGroup, @Param("query") String query, @Param("limit") int limit, @Param("offset") int offset);

  @Transactional
  @Modifying(clearAutomatically = true)
  @Query(value = Queries.DELETE_BY_CHECKLIST_ID_AND_USER_ID_IN_AND_TASK_ID_IN, nativeQuery = true)
  void deleteByChecklistIdAndUserIdInAndTaskIdIn(@Param("checklistId") Long checklistId, @Param("userIds") Set<Long> unassignedIds, @Param("taskIds") Set<Long> assignedTaskIds);

  @Transactional
  @Modifying(clearAutomatically = true)
  @Query(value = Queries.DELETE_BY_CHECKLIST_ID_AND_USER_GROUP_ID_IN_AND_TASK_ID_IN, nativeQuery = true)
  void deleteByChecklistIdAndUserGroupIdInAndTaskIdIn(@Param("checklistId") Long checklistId, @Param("userGroupIds") Set<Long> unassignedUserGroupIds, @Param("taskIds") Set<Long> assignedTaskIds);

  @Query(value = Queries.FIND_ALL_TRAINED_USERS_WITH_TASK_ID_BY_CHECKLIST_ID_AND_FACILITY_ID, nativeQuery = true)
  List<TrainedUsersView> findAllTrainedUsersWithAssignedTasksByChecklistIdAndFacilityId(@Param("checklistId") Long checklistId, @Param("facilityId") Long facilityId, @Param("isUser") Boolean isUser, @Param("isUserGroup") Boolean isUserGroup, @Param("trainedUserIds") Set<String> trainedUserIds);

  @Transactional
  @Modifying
  @Query(value = Queries.DELETE_BY_TASK_ID, nativeQuery = true)
  void deleteByTaskId(@Param("taskId") Long taskId);

  @Query(value = Queries.COUNT_ALL_TRAINED_USERS_WITH_ASSIGNED_TASKS_BY_CHECKLIST_ID_AND_FACILITY_ID, nativeQuery = true)
  Long countAllTrainedUsersWithAssignedTasksByChecklistIdAndFacilityId(@Param("checklistId") Long checklistId, @Param("facilityId") Long facilityId, @Param("isUser") Boolean isUser, @Param("isUserGroup") Boolean isUserGroup, @Param("query") String query);

  @Query(value = Queries.EXISTS_BY_CHECKLIST_ID_AND_TASK_ID_AND_USER_ID_AND_FACILITY_ID, nativeQuery = true)
  boolean existsByChecklistIdAndTaskIdAndUserIdAndFacilityId(@Param("checklistId") Long checklistId, @Param("taskId") Long taskId, @Param("userId") Long userId, @Param("facilityId") Long currentFacilityId);

  @Query(value = Queries.EXISTS_BY_CHECKLIST_ID_AND_TASK_ID_AND_USER_GROUP_ID_AND_FACILITY_ID, nativeQuery = true)
  boolean existsByChecklistIdAndTaskIdAndUserGroupIdAndFacilityId(@Param("checklistId") Long checklistId, @Param("taskId") Long taskId, @Param("userGroupId") Long userGroupId, @Param("facilityId") Long currentFacilityId);

  @Query(value = """
    SELECT tutm.* FROM trained_user_tasks_mapping tutm INNER JOIN trained_users tu ON tutm.trained_users_id = tu.id INNER JOIN tasks t ON tutm.tasks_id = t.id
     WHERE tu.checklists_id = :checklistId AND tu.facilities_id = :facilityId and t.archived = false
    """, nativeQuery = true)
  List<TrainedUserTaskMapping> findByFacilityIdAndChecklistId(@Param("facilityId") Long facilityId, @Param("checklistId") Long checklistId);

  @Query(value = """
    SELECT DISTINCT tu.user_groups_id
    FROM trained_user_tasks_mapping tutm
             INNER JOIN trained_users tu ON tutm.trained_users_id = tu.id
    WHERE tu.checklists_id = :checklistId
      AND tu.facilities_id = :facilityId
      AND tu.user_groups_id IS NOT NULL
    """, nativeQuery = true)
  Set<Long> findUserGroupIdsByChecklistIdAndFacilityId(@Param("checklistId") Long checklistId, @Param("facilityId") Long facilityId);

  @Query("""
    select tutm.task.id from TrainedUserTaskMapping tutm inner join TrainedUser tu on tutm.trainedUser.id = tu.id where tu.checklist.id = :checklistId and tu.userGroupId = :userGroupId and tu.facility.id =:facilityId
    """)
  Set<String> findTaskIdsByChecklistIdAndUserGroupIdAndFacilityId(@Param("checklistId") Long checklistId, @Param("userGroupId") Long userGroupId, @Param("facilityId") Long facilityId);

  @Query(value = Queries.GET_ALL_NON_TRAINED_USERS_BY_CHECKLIST_ID_AND_FACILITY_ID, nativeQuery = true)
  List<TrainedUsersView> findAllNonTrainedUsersByChecklistIdAndFacilityId(@Param("checklistId") Long checklistId, @Param("facilityId") Long facilityId, @Param("query") String query, @Param("limit") int limit, @Param("offset") int offset);

  @Query(value = Queries.GET_ALL_NON_TRAINED_USERS_COUNT_BY_CHECKLIST_ID_AND_FACILITY_ID, nativeQuery = true)
  long countAllNonTrainedUsersByChecklistIdAndFacilityId(@Param("checklistId") Long checklistId, @Param("facilityId") Long facilityId, @Param("query") String query);

  @Query(value = Queries.GET_ALL_NON_TRAINED_USER_GROUPS_BY_CHECKLIST_ID_AND_FACILITY_ID, nativeQuery = true)
  List<TrainedUsersView> findAllNonTrainedUserGroupsByChecklistIdAndFacilityId(@Param("checklistId") Long checklistId, @Param("currentFacilityId") Long facilityId, @Param("query") String query, @Param("limit") int limit, @Param("offset") int offset);

  @Query(value = Queries.GET_ALL_NON_TRAINED_USER_GROUP_COUNT, nativeQuery = true)
  long countAllNonTrainedUserGroupsByChecklistIdAndFacilityId(@Param("checklistId") Long checklistId, @Param("currentFacilityId") Long facilityId, @Param("query") String query);

  @Query(value = Queries.GET_ALL_ASSIGNED_TRAINED_USERS_OR_GROUPS, nativeQuery = true)
  List<TrainedUsersView> getTrainedUserTaskMappingByChecklistIdAndFacilityId(@Param("checklistId") Long checklistId, @Param("facilityId") Long facilityId, @Param("query") String query);

  @Query(value = Queries.TRAINED_USER_MATERIALIZED_VIEW, nativeQuery = true)
  void createMaterializedViewForChecklistId(@Param("viewName") String viewName, @Param("checklistId") Long checklistId, @Param("facilityId") Long currentFacilityId);

  @Query(value = Queries.ADD_INDEX_IN_TRAINED_USER_MATERIALISED_VIEW_ON_USER_GROUP_ID, nativeQuery = true)
  void addIndexInTrainedUserMaterialisedViewOnUserGroupId(@Param("viewName") String viewName);

  @Query(value = Queries.ADD_INDEX_IN_TRAINED_USER_MATERIALISED_VIEW_ON_USER_ID, nativeQuery = true)
  void addIndexInTrainedUserMaterialisedViewOnUserId(@Param("viewName") String viewName);

  @Query(value = Queries.GET_ALL_ASSIGNED_TRAINED_USER, nativeQuery = true)
  List<TrainedUsersView> getTrainedUserTaskMappingByChecklistIdAndFacilityId(@Param("viewName") String viewName, @Param("query") String query);


  @Query(value = Queries.REFRESH_MATERIALISED_TRAINED_USER_VIEW_FOR_CHECKLIST_ID, nativeQuery = true)
  void refreshMaterialisedViewForChecklistId(@Param("viewName") String viewName);
}

