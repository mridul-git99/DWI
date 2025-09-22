package com.leucine.streem.dto.request;

import lombok.Data;

import java.util.Set;

@Data
public class PeerAssignRequest {
  private Set<Long> userId;
  private Set<Long> userGroupId;
}
