package com.leucine.streem.dto.request;

import lombok.Data;

@Data
public class RemoveUserRequest {
  private Long userId;
  private String reason;
}
