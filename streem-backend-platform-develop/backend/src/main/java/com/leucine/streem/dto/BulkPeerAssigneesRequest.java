package com.leucine.streem.dto;

import com.leucine.streem.dto.request.PeerAssignRequest;
import lombok.Data;

@Data
public class BulkPeerAssigneesRequest {

  private Long parameterExecutionId;
  private PeerAssignRequest peerAssignees;
}
