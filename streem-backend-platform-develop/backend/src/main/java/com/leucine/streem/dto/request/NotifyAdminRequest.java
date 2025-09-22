package com.leucine.streem.dto.request;

import lombok.Data;

@Data
public class NotifyAdminRequest {
  private String identity;
  private String purpose;
}
