package com.leucine.streem.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskExecutionAssignmentRequest {
  private Set<Long> taskExecutionIds = new HashSet<>();
  private Set<Long> assignedUserIds = new HashSet<>();
  private Set<Long> unassignedUserIds = new HashSet<>();
  private Set<Long> assignedUserGroupIds = new HashSet<>();
  private Set<Long> unassignedUserGroupIds = new HashSet<>();
  private boolean allUsersSelected = false;
  private boolean allUserGroupsSelected = false;
}
