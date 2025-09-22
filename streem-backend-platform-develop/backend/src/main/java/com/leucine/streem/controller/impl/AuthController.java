package com.leucine.streem.controller.impl;

import com.leucine.streem.controller.IAuthController;
import com.leucine.streem.dto.LogoutRequest;
import com.leucine.streem.dto.SSORedirectUriDto;
import com.leucine.streem.dto.request.*;
import com.leucine.streem.dto.response.Response;
import com.leucine.streem.service.IAuthService;
import org.springframework.stereotype.Component;

@Component
public class AuthController implements IAuthController {
  private final IAuthService authService;

  public AuthController(IAuthService authService) {
    this.authService = authService;
  }

  @Override
  public Response<Object> authenticate(AuthenticationRequest authenticationRequest) {
    return authService.authenticate(authenticationRequest);
  }

  @Override
  public Response<Object> relogin(ReloginRequest reloginRequest) {
    return authService.relogin(reloginRequest);
  }

  @Override
  public Response<Object> logout(LogoutRequest logoutRequest) {
    return authService.logout(logoutRequest);
  }

  @Override
  public Response<Object> refreshToken(RefreshTokenRequest refreshTokenRequest) {
    return authService.refreshToken(refreshTokenRequest);
  }

  @Override
  public Response<Object> validateIdentity(ValidateIdentityRequest validateIdentityRequest) {
    return authService.validateIdentity(validateIdentityRequest);
  }

  @Override
  public Response<Object> resetToken(GeneratePasswordResetTokenRequest generatePasswordResetTokenRequest) {
    return authService.resetToken(generatePasswordResetTokenRequest);
  }

  @Override
  public Response<Object> notifyAdmin(NotifyAdminRequest notifyAdminRequest) {
    return authService.notifyAdmin(notifyAdminRequest);
  }

  @Override
  public Response<Object> validateChallengeQuestionsAnswer(ChallengeQuestionsAnswerRequest challengeQuestionsAnswerRequest) {
    return authService.validateChallengeQuestionsAnswer(challengeQuestionsAnswerRequest);
  }

  @Override
  public Response<Object> register(UserRegistrationRequest userRegistrationRequest) {
    return authService.register(userRegistrationRequest);
  }

  @Override
  public Response<Object> validateCredentials(ValidateCredentialsRequest validateCredentialsRequest) {
    return authService.validateCredentials(validateCredentialsRequest);
  }

  @Override
  public Response<Object> additionalVerificationRequest(AdditionalVerificationRequest additionalVerificationRequest) {
    return authService.additionalVerification(additionalVerificationRequest);
  }

  @Override
  public Response<Object> validateToken(ValidateTokenRequest validateTokenRequest) {
    return authService.validateToken(validateTokenRequest);
  }
  @Override
  public Response<Object> getExtras(String fqdn) {
    return authService.getExtras(fqdn);
  }

  @Override
  public Response<Object> accountLookup(String username) {
    return authService.accountLookup(username);
  }

  @Override
  public Response<Object> updatedPassword(PasswordUpdateRequest passwordUpdateRequest) {
    return authService.updatedPassword(passwordUpdateRequest);
  }

  @Override
  public Response<Object> getSSORedirectUrl(SSORedirectUriDto ssoRedirectUriDto) {
    return authService.getSSORedirectUrl(ssoRedirectUriDto);
  }

}
