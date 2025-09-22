package com.leucine.streem.dto;

import lombok.Data;

@Data
public class UserGroupAuditDto {
  private String details;
  private Long triggeredAt;
}
