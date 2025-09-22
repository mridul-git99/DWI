package com.leucine.streem.dto.mapper;

import com.leucine.streem.dto.AssigneeSignOffDto;
import com.leucine.streem.dto.UserAuditDto;
import com.leucine.streem.dto.UserDto;
import com.leucine.streem.dto.mapper.helper.IBaseMapper;
import com.leucine.streem.model.User;
import com.leucine.streem.model.UserGroupMember;
import com.leucine.streem.model.helper.PrincipalUser;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.ArrayList;
import java.util.List;

@Mapper
public interface IUserMapper extends IBaseMapper<UserDto, User> {
  UserAuditDto toUserAuditDto(User user);

  UserAuditDto toUserAuditDto(PrincipalUser principalUser);

  UserDto toDto(PrincipalUser principalUser);

  AssigneeSignOffDto toAssigneeSignOffDto(User user);

  @Mapping(target = "organisation", ignore = true)
  PrincipalUser toPrincipalUser(User user);

  PrincipalUser clone(PrincipalUser principalUser);


  default List<UserDto> toUserDto(List<UserGroupMember> userGroupMembers) {
    List<UserDto> userDtoList = new ArrayList<>();
    for (UserGroupMember userGroupMember : userGroupMembers) {
      userDtoList.add(toDto(userGroupMember.getUser()));

    }
    return userDtoList;
  }
}
