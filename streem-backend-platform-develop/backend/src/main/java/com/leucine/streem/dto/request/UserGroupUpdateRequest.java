package com.leucine.streem.dto.request;

import lombok.Data;

import java.util.List;
import java.util.Set;

@Data
public class UserGroupUpdateRequest {
  private String name;
  private String description;
  private String reason;
  private Set<Long> assignedUserIds;
  private List<RemoveUserRequest> removedUser;
  private boolean unAssignAllUsers = false;
}
