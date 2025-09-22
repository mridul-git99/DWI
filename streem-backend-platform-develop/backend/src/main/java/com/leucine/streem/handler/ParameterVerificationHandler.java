package com.leucine.streem.handler;

import com.leucine.streem.constant.State;
import com.leucine.streem.dto.response.ErrorCode;
import com.leucine.streem.exception.StreemException;
import com.leucine.streem.model.ParameterValueBase;
import com.leucine.streem.model.User;
import com.leucine.streem.model.VerificationBase;
import com.leucine.streem.service.impl.JobAuditService;
import com.leucine.streem.util.Utility;
import com.leucine.streem.util.ValidationUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@Slf4j
public class ParameterVerificationHandler implements IParameterVerificationHandler {

  private final JobAuditService jobAuditService;
  /**
   * Check if the user is allowed to initiate self verification
   * Parameter value should be in BEING_EXECUTED state and the user should be the one who executed the parameter
   * @param principalUserEntity
   * @param parameterValueBase
   * @throws StreemException
   */
  @Override
  public void canInitiateSelfVerification(User principalUserEntity, ParameterValueBase parameterValueBase) throws StreemException {
    if ((Utility.isEmpty(parameterValueBase)) || !parameterValueBase.getState().equals(State.ParameterExecution.BEING_EXECUTED)) {
      ValidationUtils.invalidate(parameterValueBase.getId(), ErrorCode.PARAMETER_VERIFICATION_NOT_ALLOWED);
    } else if (!principalUserEntity.getId().equals(parameterValueBase.getModifiedBy().getId())) {
      ValidationUtils.invalidate(principalUserEntity.getId(), ErrorCode.USER_NOT_ALLOWED_TO_SELF_VERIFIY_PARAMETER);
    }
  }

  /**
   * Check if the user is allowed to complete self verification
   * User should be the one who initiated the self verification and executed the parameter
   * @param principalUserEntity
   * @param parameterId
   * @param verificationBase
   * @throws StreemException
   */
  @Override
  public void canCompleteSelfVerification(User principalUserEntity, Long parameterId, VerificationBase verificationBase) throws StreemException {
    if (!principalUserEntity.getId().equals(verificationBase.getUser().getId())) {
      ValidationUtils.invalidate(parameterId, ErrorCode.SELF_VERIFICATION_NOT_ALLOWED);
    }
  }

  /**
   * Check if the user is allowed to complete peer verification
   * @param principalUserEntity
   * @param lastActionPerformed
   * @throws StreemException
   */
  @Override
  public void canCompletePeerVerification(User principalUserEntity, VerificationBase lastActionPerformed) throws StreemException {
    if (!principalUserEntity.getId().equals(lastActionPerformed.getUser().getId())) {
      ValidationUtils.invalidate(lastActionPerformed.getId(), ErrorCode.PEER_VERIFICATION_NOT_ALLOWED);
    }
  }


}

