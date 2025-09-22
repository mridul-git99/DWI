package com.leucine.streem.dto.mapper;

import com.leucine.streem.dto.TaskExecutionAssigneeDto;
import com.leucine.streem.dto.TaskExecutionDto;
import com.leucine.streem.dto.TaskPauseReasonOrComment;
import com.leucine.streem.dto.mapper.helper.IBaseMapper;
import com.leucine.streem.model.TaskExecution;
import com.leucine.streem.model.helper.PrincipalUser;
import org.mapstruct.*;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Mapper(uses = {ITaskExecutionAssigneeMapper.class})
public interface ITaskExecutionMapper extends IBaseMapper<TaskExecutionDto, TaskExecution> {
  @Override
  @Mapping(source = "modifiedAt", target = "audit.modifiedAt")
  @Mapping(source = "modifiedBy", target = "audit.modifiedBy")
  TaskExecutionDto toDto(TaskExecution taskExecution);

  TaskExecutionDto toDto(TaskExecution taskExecution, @Context PrincipalUser principalUser);

  TaskExecutionDto toDto(TaskExecution taskExecution, List<TaskPauseReasonOrComment> pauseReasons);

  @AfterMapping
  default void setAudit(TaskExecution taskExecution, @MappingTarget TaskExecutionDto taskExecutionDto,
                        @Context PrincipalUser principalUser) {
    List<TaskExecutionAssigneeDto> userAssignees = taskExecutionDto.getAssignees().stream().filter(tead -> tead.getUserId() != null).collect(Collectors.toList());
    userAssignees.sort(Comparator.comparing(tead ->  tead.getFirstName() + tead.getLastName()));
    taskExecutionDto.setAssignees(userAssignees);
    taskExecutionDto.setAudit(IAuditMapper.createAuditDtoFromPrincipalUser(principalUser, taskExecution.getModifiedAt()));
    List<TaskExecutionAssigneeDto> userGroupAssignees = taskExecutionDto.getAssignees().stream().filter(tead -> tead.getUserGroupId() != null).toList();
    taskExecutionDto.setUserGroupAssignees(userGroupAssignees);
  }

}
