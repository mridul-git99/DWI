package com.leucine.streem.repository;

import com.leucine.streem.constant.Queries;
import com.leucine.streem.model.TrainedUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface ITrainedUserRepository extends JpaRepository<TrainedUser, Long> {
  @Query(value = """
    SELECT EXISTS(
      SELECT 1
      FROM trained_users tu
      inner join trained_user_tasks_mapping tutm on tu.id = tutm.trained_users_id
      WHERE tu.checklists_id = :checklistId
        AND tu.user_groups_id IN :assignedUserGroupIds
    )
    """, nativeQuery = true)
  boolean isUserGroupAddedToChecklist(@Param("checklistId") Long checklistId, @Param("assignedUserGroupIds") Set<Long> assignedUserGroupIds);


  @Query(value = """
    DELETE FROM trained_users tu
    WHERE tu.checklists_id = :checklistId
      AND tu.facilities_id = :facilityId
      AND tu.user_groups_id IN :unassignedUserGroupIds
    """, nativeQuery = true)
  @Transactional(rollbackFor = Exception.class)
  @Modifying(clearAutomatically = true)
  void deleteByChecklistIdAndFacilityIdAndUserGroupId(@Param("checklistId") Long checklistId, @Param("facilityId") Long currentFacilityId, @Param("unassignedUserGroupIds") Set<Long> unassignedUserGroupIds);

  @Query(value = """
    DELETE FROM trained_users tu
    WHERE tu.checklists_id = :checklistId
      AND tu.facilities_id = :facilityId
      AND tu.users_id IN :unassignedUserIds
    """, nativeQuery = true)
  @Transactional(rollbackFor = Exception.class)
  @Modifying(clearAutomatically = true)
  void deleteByChecklistIdAndFacilityIdAndUserId(@Param("checklistId") Long checklistId, @Param("facilityId") Long currentFacilityId, @Param("unassignedUserIds") Set<Long> unassignedUserIds);

  @Query(value = """
    SELECT EXISTS(
      SELECT 1
      FROM trained_users tu
      WHERE tu.checklists_id = :checklistId
        AND tu.users_id IN :assignedUserIds
    )
    """, nativeQuery = true)
  boolean isUserAddedToChecklist(@Param("checklistId") Long checklistId, @Param("assignedUserIds") Set<Long> assignedUserIds);

  List<TrainedUser> findAllByChecklistIdAndFacilityIdAndUserGroupIdIn(Long checklistId, Long currentFacilityId, Set<Long> assignedUserGroupIds);

  List<TrainedUser> findAllByChecklistIdAndFacilityIdAndUserIdIn(Long checklistId, Long currentFacilityId, Set<Long> assignedUserIds);

  @Query(value = """
    DELETE
    FROM trained_user_tasks_mapping tutm where tutm.tasks_id in :taskIds
    AND EXISTS (SELECT 1
                  FROM trained_users tu
                  WHERE tu.id = tutm.trained_users_id
                  AND tu.checklists_id = :checklistId
                  AND tu.facilities_id = :facilityId                        
                    AND (tu.user_groups_id IN :unassignedUserGroupIds
                      OR tu.users_id IN :unassignedUserIds))
    """, nativeQuery = true)
  @Transactional(rollbackFor = Exception.class)
  @Modifying(clearAutomatically = true)
  void deleteByChecklistIdAndFacilityIdAndUserGroupId(@Param("checklistId") Long checklistId, @Param("facilityId") Long currentFacilityId, @Param("unassignedUserIds") Set<Long> unassignedUserIds, @Param("unassignedUserGroupIds") Set<Long> unassignedUserGroupIds, @Param("taskIds") Set<Long> taskIds);


  List<TrainedUser> findAllByChecklistIdAndUserIdIn(Long checklistId, Set<Long> assignedUserIds);

  List<TrainedUser> findAllByChecklistIdAndUserGroupIdIn(Long checklistId, Collection<Long> userGroupId);

  @Query(value = """
        select from trained_users tu where tu.checklists_id = :checklistId and tu.users_id in :assignedUserIds and tu.facilities_id = :facilityId
    """, nativeQuery = true)
  Set<Long> validateIfUsersAreTrainedUsersForChecklist(@Param("checklistId") Long checklistId, @Param("assignedUserIds") Set<Long> assignedUserIds, @Param("facilityId") Long currentFacilityId);

  @Query(value = """
    select from trained_users tu where tu.checklists_id = :checklistId and tu.user_groups_id in :assignedUserGroupIds and tu.facilities_id = :facilityId
     """, nativeQuery = true)
  Set<Long> validateIfUserGroupsAreTrainedUserGroupsForChecklist(@Param("checklistId") Long checklistId, @Param("assignedUserGroupIds") Set<Long> assignedUserGroupIds, @Param("facilityId") Long currentFacilityId);

  @Transactional
  @Query(value = """
    DELETE FROM trained_users tu
    WHERE tu.checklists_id = :checklistId
      AND tu.facilities_id = :facilityId
      AND tu.users_id IN :unassignedUserIds
    """, nativeQuery = true)
  @Modifying(clearAutomatically = true)
  void deleteByChecklistIdAndFacilityIdAndUserIdIn(@Param("checklistId") Long checklistId, @Param("facilityId") Long currentFacilityId, @Param("unassignedUserIds") Set<Long> unassignedUserIds);

  @Transactional
  @Query(value = """
    DELETE FROM trained_users tu
    WHERE tu.checklists_id = :checklistId
      AND tu.facilities_id = :facilityId
      AND tu.user_groups_id IN :unassignedUserGroupIds
    """, nativeQuery = true)
  @Modifying(clearAutomatically = true)
  void deleteByChecklistIdAndFacilityIdAndUserGroupIdIn(@Param("checklistId") Long checklistId,@Param("facilityId") Long currentFacilityId, @Param("unassignedUserGroupIds") Set<Long> unassignedUserGroupIds);

  List<TrainedUser> findAllByChecklistIdAndFacilityId(@Param("checklistId") Long parentChecklistId, @Param("currentFacilityId") Long currentFacilityId);

  void deleteByUserGroupId(Long id);

  @Query(value = Queries.FIND_ALL_TRAINED_USER_IDS_BY_CHECKLIST_ID, nativeQuery = true)
  Set<Long> findAllUserIdsByChecklistId(@Param("checklistId") Long checklistId);

  @Query(value = Queries.FIND_ALL_TRAINED_USER_GROUP_IDS_BY_CHEKCLISTID, nativeQuery = true)
  Set<Long> findAllUserGroupIdsByChecklistId(@Param("checklistId") Long checklistId);

  @Query(value = Queries.VERIFY_USER_IS_ASSIGNED_TO_THE_CHECKLIST, nativeQuery = true)
  boolean verifyUserIsAssignedToTheChecklist(@Param("checklist_id") Long checklistId,@Param("users_id") Long userId);

  @Query(value = Queries.FIND_ALL_USER_IDS_OF_FACILITY, nativeQuery = true)
  Set<Long> findAllUserIdsOfFacility(@Param("currentFacilityId") Long currentFacilityId);

  @Query(value = Queries.FIND_ALL_USER_GROUP_IDS_OF_FACILITY, nativeQuery = true)
  Set<Long> findAllUserGroupIdsOfFacility(@Param("currentFacilityId") Long currentFacilityId);

  @Transactional
  @Modifying
  @Query(value = Queries.DELETE_USER_FROM_TRAINED_USERS, nativeQuery = true)
  void deleteByUserId(@Param("userId") Long userId);

  List<TrainedUser> findByUserId(Long userId);
}
