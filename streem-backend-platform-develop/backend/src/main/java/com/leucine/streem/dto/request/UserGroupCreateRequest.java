package com.leucine.streem.dto.request;

import lombok.Data;

import java.util.Set;

@Data
public class UserGroupCreateRequest {
  private String name;
  private String description;
  private String reason;
  private Set<Long> userIds;
}
