import { LoadingContainer } from '#components';
import { navigate, useLocation } from '@reach/router';
import React, { useEffect, useState } from 'react';
import { useDispatch } from 'react-redux';
import styled from 'styled-components';
import { login, reLogin, refreshTokenSuccess } from './actions';
import { releasePrototype, signOffPrototype } from '#PrototypeComposer/reviewer.actions';
import { SsoStates } from '#utils/globalTypes';
import { useTypedSelector } from '#store';
import { jobActions } from '#views/Job/jobStore';
import { decompressUrl } from '#utils/decompressUrl';
import { verifierLoginUtils } from '#views/Job/components/Task/Parameters/Verification/verifierLoginUtils';
import { setAuthHeader } from '#utils/axiosClient';

const SsoViewWrapper = styled.div`
  display: flex;
  height: 100%;
  width: 100dvw;
  justify-content: center;
  align-items: center;
`;

export const SsoView = () => {
  const dispatch = useDispatch();
  const { identity } = useTypedSelector((state) => state.auth);
  const { href } = useLocation();
  const ssoURL = new URL(href);
  const extractedTOkenCode = ssoURL.searchParams.get('code')!;
  const compressedState = ssoURL.searchParams.get('state') || '';

  const [decompressedState, setDecompressedState] = useState<any>(null);

  useEffect(() => {
    const processState = async () => {
      if (compressedState) {
        try {
          const formattedState = compressedState.replace(/^["']|["']$/g, '').replace(/ /g, '+');
          const decompressedStateString = await decompressUrl(formattedState);
          if (decompressedStateString && typeof decompressedStateString === 'string') {
            const parsedState = JSON.parse(decompressedStateString);
            setDecompressedState(parsedState);
          } else {
            console.error('Invalid decompressed state');
          }
        } catch (error: any) {
          console.error('Error decompressing the state:', error);
        }
      }
    };

    processState();
  }, [compressedState]);

  useEffect(() => {
    if (!decompressedState) return;

    const {
      state,
      checklistId,
      sameSession,
      initiatorJwtToken,
      parameterExecutionId,
      location = '/',
    } = decompressedState;

    switch (state) {
      case SsoStates.LOGIN:
        if (sameSession && initiatorJwtToken) {
          // Check if this is bulk verification
          if (decompressedState.bulkVerification && decompressedState.parameters) {
            // Handle bulk same session verification using shared utility
            verifierLoginUtils({
              verifierUserName: decompressedState.verifierUserName,
              password: null,
              bulkParameters: decompressedState.parameters,
              isBulk: true,
              initiatorToken: initiatorJwtToken,
              code: extractedTOkenCode,
              state: state,
              pendingAction: decompressedState.pendingAction || 'accept',
              rejectionReason: decompressedState.rejectionReason || '',
            })
              .then(() => {
                // Navigate back to the exact page/tab the user was on
                const returnToUrl = decompressedState.returnTo || location || '/inbox';
                navigate(returnToUrl, { replace: true });
              })
              .catch((error) => {
                // Navigate back even on error to close the SSO flow
                const returnToUrl = decompressedState.returnTo || location || '/inbox';
                navigate(returnToUrl, { replace: true });
              });
          } else if (parameterExecutionId) {
            // Handle single parameter verification
            verifierLoginUtils({
              verifierUserName: decompressedState.verifierUserName,
              password: null,
              parameterResponseId: parameterExecutionId,
              initiatorToken: initiatorJwtToken,
              code: extractedTOkenCode,
              state: state,
              pendingAction: decompressedState.pendingAction,
              rejectionReason: decompressedState.rejectionReason,
            })
              .then(() => {
                // Navigate back to the exact page/tab the user was on
                const returnToUrl = decompressedState.returnTo || location || '/inbox';
                navigate(returnToUrl, { replace: true });
              })
              .catch((error) => {
                // Navigate back even on error to close the SSO flow
                const returnToUrl = decompressedState.returnTo || location || '/inbox';
                navigate(returnToUrl, { replace: true });
              });
          }
        } else {
          dispatch(
            login({
              code: extractedTOkenCode,
              username: identity,
              state: state,
              pathname: location,
            }),
          );
        }

        break;
      case SsoStates.SIGN_OFF:
        dispatch(signOffPrototype({ checklistId, code: extractedTOkenCode, state: state }));
        break;
      case SsoStates.RELEASE:
        dispatch(releasePrototype({ checklistId, code: extractedTOkenCode, state: state }));
        break;
      case SsoStates.SELF_VERIFICATION:
        dispatch(
          jobActions.completeSelfVerification({
            parameterResponseId: decompressedState.parameterResponseId,
            parameterId: decompressedState.parameterId,
            code: extractedTOkenCode,
            state: state,
          }),
        );
        break;
      case SsoStates.PEER_VERIFICATION:
        dispatch(
          jobActions.acceptPeerVerification({
            parameterResponseId: decompressedState.parameterResponseId,
            parameterId: decompressedState.parameterId,
            code: extractedTOkenCode,
            state: state,
          }),
        );
        break;
      case SsoStates.INITIATE_CORRECTION:
        dispatch(
          jobActions.initiateErrorCorrectionOnParameter({
            parameterResponseId: decompressedState.parameterResponseId,
            code: extractedTOkenCode,
            state: state,
            initiatorReason: decompressedState.initiatorReason,
            correctors: decompressedState.correctors,
            reviewers: decompressedState.reviewers,
          }),
        );
        break;
      case SsoStates.PERFORM_CORRECTION:
        dispatch(
          jobActions.performErrorCorrectionOnParameter({
            parameterResponseId: decompressedState.parameterResponseId,
            code: extractedTOkenCode,
            state: state,
            correctionId: decompressedState.correctionId,
            newValue: decompressedState.newValue,
            newChoice: decompressedState.newChoice,
            correctorReason: decompressedState.correctorReason,
            medias: decompressedState.medias,
          }),
        );
        break;
      case SsoStates.REVIEW_CORRECTION:
        dispatch(
          jobActions.approveRejectErrorCorrectionOnParameter({
            parameterResponseId: decompressedState.parameterResponseId,
            correctionId: decompressedState.correctionId,
            code: extractedTOkenCode,
            state: state,
            reviewerReason: decompressedState.reviewerReason,
            performCorrectionStatus: decompressedState.performCorrectionStatus,
          }),
        );
        break;
      case SsoStates.RE_LOGIN:
        dispatch(
          reLogin({
            username: identity,
            code: extractedTOkenCode,
            state,
            pathname: location,
          }),
        );
        break;
      case SsoStates.INITIATE_EXCEPTION:
        dispatch(
          jobActions.initiateExceptionOnParameter({
            parameterResponseId: decompressedState?.parameterResponseId,
            code: extractedTOkenCode,
            state: state,
            initiatorReason: decompressedState?.initiatorReason,
            value: decompressedState?.value,
            choices: decompressedState?.choices,
            approver: decompressedState?.approver,
          }),
        );
        break;
      case SsoStates.REVIEW_EXCEPTION:
        dispatch(
          jobActions.submitExceptionOnParameter({
            parameterResponseId: decompressedState?.parameterResponseId,
            code: extractedTOkenCode,
            state: state,
            exceptionId: decompressedState?.exceptionId,
            reviewerReason: decompressedState.reviewerReason,
            reviewStatus: decompressedState.reviewStatus,
            isCjfException: decompressedState?.isCjfException,
            jobId: decompressedState?.jobId,
            rulesId: decompressedState?.rulesId,
          }),
        );
        break;
      case SsoStates.BULK_SELF_VERIFICATION:
        dispatch(
          jobActions.completeBulkSelfVerification({
            values: decompressedState.values,
            code: extractedTOkenCode,
            state,
          }),
        );
        break;
      case SsoStates.BULK_PEER_VERIFICATION:
        dispatch(
          jobActions.completeBulkPeerVerification({
            values: decompressedState.values,
            code: extractedTOkenCode,
            state,
          }),
        );
        break;
      case SsoStates.INITIATE_BULK_EXCEPTION:
        dispatch(
          jobActions.initiateBulkExceptionsOnParameter({
            state,
            code: extractedTOkenCode,
            parametersWithException: decompressedState.parametersWithException,
          }),
        );
      default:
        break;
    }
    if (![SsoStates.RE_LOGIN, SsoStates.LOGIN].includes(state)) {
      navigate(location, { replace: true });
    }
  }, [extractedTOkenCode, decompressedState]);

  return (
    <SsoViewWrapper>
      <LoadingContainer loading={true} component={<div>SsoView</div>} />
    </SsoViewWrapper>
  );
};
