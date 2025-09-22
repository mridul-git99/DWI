package com.leucine.streem.service;

import com.leucine.streem.dto.UserGroupDto;
import com.leucine.streem.dto.request.UserGroupUpdateRequest;
import com.leucine.streem.model.UserGroup;
import com.leucine.streem.model.UserGroupAudit;
import com.leucine.streem.model.UserGroupMember;
import com.leucine.streem.model.helper.PrincipalUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface IUserGroupAuditService {
  void create(Long organisationsId, Long facilityId, UserGroup userGroup, String reason);

  void update(Long organisationsId, Long facilityId, Long triggeredBy, UserGroupDto oldUserGroupDto, UserGroup newUserGroup, UserGroupUpdateRequest userGroupUpdateRequest);

  void archive(UserGroup userGroup, String reason);

  void unarchive(UserGroup userGroup, String reason);

  Page<UserGroupAudit> getAudits(String filters, Pageable pageable);

  void removeUserDueToArchival(List<UserGroupMember> userGroupMembers, String reason, PrincipalUser archivedByUser, PrincipalUser systemPrincipalUser);

}
