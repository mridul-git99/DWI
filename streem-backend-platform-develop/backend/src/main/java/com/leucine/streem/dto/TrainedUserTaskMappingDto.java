package com.leucine.streem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrainedUserTaskMappingDto {
  private String taskId;
  private Set<PartialUserDto> users;
  private Set<UserGroupDto> userGroups;
}
