package com.leucine.streem.dto.request;

import lombok.Data;

@Data
public class GeneratePasswordResetTokenRequest {
  private String identity;
}
