package com.leucine.streem.repository;

import com.leucine.streem.model.UserGroupMember;
import com.leucine.streem.model.compositekey.UserGroupMemberCompositeKey;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import com.leucine.streem.constant.Queries;

import java.util.List;
import java.util.Set;

public interface IUserGroupMemberRepository extends JpaRepository<UserGroupMember, UserGroupMemberCompositeKey>, JpaSpecificationExecutor<UserGroupMember> {

  @Override
  Page<UserGroupMember> findAll(Specification<UserGroupMember> specification, Pageable pageable);

  @Modifying(clearAutomatically = true)
  @Transactional
  @Query("delete from UserGroupMember ugm where ugm.userGroupMemberCompositeKey.groupId = ?1 and ugm.userGroupMemberCompositeKey.userId in ?2")
  void deleteByUserGroupIdAndUserIdIn(Long userGroupId, Set<Long> userIds);

  List<UserGroupMember> findByUserGroupId(Long userGroupId);

  @Query(value = """
    select distinct ugm.users_id from user_group_members ugm
    where ugm.groups_id in :userGroupIds
    """, nativeQuery = true)
  Set<Long> findAllUsersByUserGroupIds(@Param("userGroupIds") Set<Long> userGroupIds);

  @Query("SELECT ugm FROM UserGroupMember ugm WHERE ugm.userGroup.id IN :userGroupIds")
  List<UserGroupMember> findByUserGroupIdIn(@Param("userGroupIds") List<Long> userGroupIds);

  @Query(value = """
       select distinct ugm.users_id, u.first_name, u.last_name from user_group_members ugm
                                             inner join users u on u.id = ugm.users_id
       where ugm.groups_id = :userGroupId
       order by u.first_name, u.last_name
    """, nativeQuery = true)
  List<Long> getAllUserIdsOfUserGroup(@Param("userGroupId") Long userGroupId);

  Long countByUserGroupId(Long userGroupId);

  @Transactional
  @Modifying
  @Query(value = Queries.DELETE_USER_FROM_USER_GROUP_MEMBER, nativeQuery = true)
  void deleteByUserId(@Param("userId") Long userId);

  @Query(value = Queries.FIND_USER_GROUP_MEMBERS_BY_USER_ID, nativeQuery = true)
  List<UserGroupMember> findByUserId(@Param("userId") Long userId);
}
