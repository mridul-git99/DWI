package com.leucine.streem.dto.request;

import lombok.Data;

@Data
public class AuthenticationRequest {
  private String username;
  private String password;
  //Authorization Code from OIDC
  private String code;
  // state represents last state where frontend was in before authentication
  private String state;
  private String facilityId;
  private String clientId;
  private String clientSecret;
}
