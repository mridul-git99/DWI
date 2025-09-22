package com.leucine.streem.repository;

import com.leucine.streem.constant.Queries;
import com.leucine.streem.dto.projection.RoleBasicView;
import com.leucine.streem.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Repository
@Transactional(rollbackFor = Exception.class)
public interface IUserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
  @Query(value = Queries.GET_USER_WHO_PERFORMED_CORRECTION_BY_CORRECTION_ID, nativeQuery = true)
  User getUserWhoCorrectedByCorrectionId(@Param("correctionId") Long correctionId);

  @Query(value = Queries.CHECK_IF_USERS_EXISTS_BY_ROLES, nativeQuery = true)
  boolean existsByRoles(@Param("userIds") Set<Long> userIds, @Param("roles") List<String> userGroupRoles);

  @Query(value = Queries.GET_USER_ROLES, nativeQuery = true)
  List<RoleBasicView> getUserRoles(@Param("userId") String userId);

  List<User> findAllByIdIn(@Param("ids") Set<Long> ids);

  List<User> findAllByIdInAndArchivedFalse(Set<Long> ids);
}
