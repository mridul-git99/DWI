package com.leucine.streem.dto.mapper;

import com.leucine.streem.dto.TaskExecutionAssigneeDto;
import com.leucine.streem.dto.mapper.helper.IBaseMapper;
import com.leucine.streem.model.TaskExecutionUserMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface ITaskExecutionAssigneeMapper extends IBaseMapper<TaskExecutionAssigneeDto, TaskExecutionUserMapping> {
  @Mapping(source = "user.id", target = "userId")
  @Mapping(source = "user.employeeId", target = "employeeId")
  @Mapping(source = "user.firstName", target = "firstName")
  @Mapping(source = "user.lastName", target = "lastName")
  @Mapping(source = "user.email", target = "email")
  @Mapping(source = "state", target = "state")
  @Mapping(source = "actionPerformed", target = "actionPerformed")
  @Mapping(source = "userGroup.id", target = "userGroupId")
  @Mapping(source = "userGroup.name", target = "userGroupName")
  TaskExecutionAssigneeDto toDto(TaskExecutionUserMapping taskExecutionUserMapping);
}
