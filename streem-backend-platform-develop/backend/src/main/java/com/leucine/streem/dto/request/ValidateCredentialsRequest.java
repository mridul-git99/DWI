package com.leucine.streem.dto.request;

import lombok.Data;

@Data
public class ValidateCredentialsRequest {
  private String password;
  private String purpose;
  private String code;
  private String state;
}
