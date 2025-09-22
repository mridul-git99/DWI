package com.leucine.streem.handler;

import com.leucine.streem.exception.StreemException;
import com.leucine.streem.model.ParameterValueBase;
import com.leucine.streem.model.User;
import com.leucine.streem.model.VerificationBase;

public interface IParameterVerificationHandler {
  void canInitiateSelfVerification(User principalUserEntity, ParameterValueBase parameterValueBase) throws StreemException;
  void canCompleteSelfVerification(User principalUserEntity, Long parameterId, VerificationBase verificationBase) throws StreemException;
  void canCompletePeerVerification(User principalUserEntity, VerificationBase lastActionPerformed) throws StreemException;
}
