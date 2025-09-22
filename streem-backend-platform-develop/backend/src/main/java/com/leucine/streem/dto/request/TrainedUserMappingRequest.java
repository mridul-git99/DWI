package com.leucine.streem.dto.request;

import com.leucine.streem.constant.ProcessPermissionType;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Data
public class TrainedUserMappingRequest {
  private Set<Long> assignedUserIds = new HashSet<>();
  private Set<Long> assignedUserGroupIds = new HashSet<>();
  private Set<Long> unassignedUserIds = new HashSet<>();
  private Set<Long> unassignedUserGroupIds = new HashSet<>();
  private Set<ProcessPermissionType> processPermissionTypes = new HashSet<>();
  private String reason;
}
