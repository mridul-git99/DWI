package com.leucine.streem.dto.mapper;

import com.leucine.streem.dto.UserGroupDto;
import com.leucine.streem.dto.mapper.helper.IBaseMapper;
import com.leucine.streem.model.UserGroup;
import org.mapstruct.Mapper;

@Mapper
public interface IUserGroupMapper extends IBaseMapper<UserGroupDto, UserGroup> {
}
