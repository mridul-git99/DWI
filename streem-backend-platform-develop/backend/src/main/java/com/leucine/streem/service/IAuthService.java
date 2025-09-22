package com.leucine.streem.service;

import com.leucine.streem.dto.LogoutRequest;
import com.leucine.streem.dto.SSORedirectUriDto;
import com.leucine.streem.dto.request.*;
import com.leucine.streem.dto.response.Response;

public interface IAuthService {
  Response<Object> authenticate(AuthenticationRequest authenticationRequest);

  Response<Object> relogin(ReloginRequest reloginRequest);

  Response<Object> logout(LogoutRequest logoutRequest);

  Response<Object> validateCredentials(ValidateCredentialsRequest tokenValidateRequest);

  Response<Object>  additionalVerification(AdditionalVerificationRequest additionalVerificationRequest);

  Response<Object> validateToken(ValidateTokenRequest validateTokenRequest);

  Response<Object> refreshToken(RefreshTokenRequest refreshTokenRequest);

  Response<Object> register(UserRegistrationRequest userRegistrationRequest);

  Response<Object> updatedPassword(PasswordUpdateRequest passwordUpdateRequest);

  Response<Object> validateChallengeQuestionsAnswer(ChallengeQuestionsAnswerRequest challengeQuestionsAnswerRequest);

  Response<Object> notifyAdmin(NotifyAdminRequest notifyAdminRequest);

  Response<Object> resetToken(GeneratePasswordResetTokenRequest generatePasswordResetTokenRequest);

  Response<Object> validateIdentity(ValidateIdentityRequest validateIdentityRequest);

  Response<Object> accountLookup(String username);

  Response<Object> getExtras(String fqdn);

  Response<Object> getSSORedirectUrl(SSORedirectUriDto checklistId);
}
