package com.leucine.streem.dto.request;

import lombok.Data;

import java.util.Set;

@Data
public class TaskAssigneeDto {
  private boolean users;
  private boolean userGroups;
  private Set<Long> task;
}
