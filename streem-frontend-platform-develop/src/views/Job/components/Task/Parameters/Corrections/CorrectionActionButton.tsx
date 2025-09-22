import { Media } from '#PrototypeComposer/checklist.types';
import { Button } from '#components';
import { showNotification } from '#components/Notification/actions';
import { NotificationType } from '#components/Notification/types';
import { openOverlayAction } from '#components/OverlayContainer/actions';
import { OverlayNames } from '#components/OverlayContainer/types';
import checkPermission, { isFeatureAllowed } from '#services/uiPermissions';
import { useTypedSelector } from '#store';
import { ParameterCorrectionStatus } from '#types';
import { apiRecallParameterCorrection } from '#utils/apiUrls';
import { SsoStates } from '#utils/globalTypes';
import { request, ssoSigningRedirect } from '#utils/request';
import { jobActions } from '#views/Job/jobStore';
import { useLocation } from '@reach/router';
import React, { FC, useMemo } from 'react';
import { useDispatch } from 'react-redux';
import styled from 'styled-components';

const CorrectionActionButtonWrapper = styled.div.attrs({
  className: 'correction-action-button',
})`
  margin-top: 8px;
  display: flex;
`;

const actionButtonText = (status: string) => {
  switch (status) {
    case ParameterCorrectionStatus.INITIATED:
      return 'Awaiting Correction';
    case ParameterCorrectionStatus.CORRECTED:
      return 'Awaiting Review';

    default:
      return 'View Correction';
  }
};
export const CorrectionActionButton: FC<any> = ({ correction, parameter }) => {
  const dispatch = useDispatch();
  const {
    auth: { userId: loggedInUserId, ssoIdToken },
  } = useTypedSelector((state) => state);
  const { pathname } = useLocation();
  const { status, corrector, reviewer } = correction;

  const isLoggedInUserCorrector = useMemo(() => {
    return (corrector || []).some((currCorrector) => currCorrector.user.id === loggedInUserId);
  }, [corrector, loggedInUserId]);

  const isLoggedInUserReviewer = useMemo(() => {
    return (reviewer || []).some((currReviewer) => currReviewer.user.id === loggedInUserId);
  }, [reviewer, loggedInUserId]);

  const canRecallCorrection = useMemo(() => {
    const hasPermission = checkPermission(['corrections', 'recall']);
    const isLoggedInUserInitiator = correction?.createdBy.id === loggedInUserId;
    return hasPermission || isLoggedInUserInitiator;
  }, [correction, loggedInUserId]);

  const handleCorrectionAction = () => {
    if (correction.status === ParameterCorrectionStatus.INITIATED) {
      dispatch(
        openOverlayAction({
          type: OverlayNames.PARAMETER_CORRECTION_MODAL,
          props: {
            isInitiated: true,
            correction: correction,
            parameter: parameter,
            isReadOnly:
              status === ParameterCorrectionStatus.INITIATED
                ? !isLoggedInUserCorrector
                : !isLoggedInUserReviewer,
            isLoggedInUserCorrector,
            onSubmit: ({
              newValue,
              correctorReason,
              password,
              newChoice,
              medias,
            }: {
              newValue: string;
              correctorReason: string;
              correctionId: string;
              password: string;
              newChoice: any[];
              medias: Media[];
            }) => {
              if (ssoIdToken) {
                ssoSigningRedirect({
                  state: SsoStates.PERFORM_CORRECTION,
                  parameterResponseId: parameter.response.id,
                  correctionId: correction.id,
                  newValue,
                  newChoice,
                  correctorReason,
                  medias,
                  location: pathname,
                });
              } else {
                dispatch(
                  jobActions.performErrorCorrectionOnParameter({
                    parameterResponseId: parameter.response.id,
                    correctionId: correction.id,
                    password,
                    newValue,
                    newChoice,
                    correctorReason,
                    medias,
                  }),
                );
              }
            },
          },
        }),
      );
    } else if (correction.status === ParameterCorrectionStatus.CORRECTED) {
      dispatch(
        openOverlayAction({
          type: OverlayNames.PARAMETER_CORRECTION_MODAL,
          props: {
            isCorrected: true,
            correction: correction,
            parameter: parameter,
            isReadOnly:
              status === ParameterCorrectionStatus.CORRECTED
                ? !isLoggedInUserReviewer
                : !isLoggedInUserCorrector,
            isLoggedInUserReviewer,
            isLoggedInUserCorrector,
            onSubmit: ({
              reviewerReason,
              password,
              performCorrectionStatus,
            }: {
              reviewerReason: string;
              password: string;
              performCorrectionStatus: string;
            }) => {
              if (ssoIdToken) {
                ssoSigningRedirect({
                  state: SsoStates.REVIEW_CORRECTION,
                  parameterResponseId: parameter.response.id,
                  correctionId: correction.id,
                  reviewerReason,
                  performCorrectionStatus,
                  location: pathname,
                });
              } else {
                dispatch(
                  jobActions.approveRejectErrorCorrectionOnParameter({
                    parameterResponseId: parameter.response.id,
                    correctionId: correction.id,
                    password,
                    reviewerReason,
                    performCorrectionStatus,
                  }),
                );
              }
            },
          },
        }),
      );
    }
  };

  const handleRecallAction = () => {
    dispatch(
      openOverlayAction({
        type: OverlayNames.REASON_MODAL,
        props: {
          modalTitle: 'Recall Correction',
          modalDesc: 'Enter reason for recalling this correction.',
          onSubmitModalText: 'Recall',
          onSubmitHandler: async (reason: string, setFormErrors: (errors?: any[]) => void) => {
            try {
              await request('PATCH', apiRecallParameterCorrection(parameter.response.id), {
                data: {
                  reason,
                  correctionId: correction.id,
                },
              });

              dispatch(
                showNotification({
                  type: NotificationType.SUCCESS,
                  msg: 'Correction recalled successfully.',
                }),
              );

              setFormErrors(); // Call with no errors to close modal
            } catch (error) {
              dispatch(
                showNotification({
                  type: NotificationType.ERROR,
                  msg: 'Failed to recall correction. Please try again.',
                }),
              );

              setFormErrors([{ message: 'Failed to recall correction. Please try again.' }]);
            }
          },
        },
      }),
    );
  };

  return (
    <CorrectionActionButtonWrapper>
      <Button variant="secondary" color="blue" onClick={handleCorrectionAction}>
        {actionButtonText(correction.status)}
      </Button>
      {isFeatureAllowed('recallErrorCorrection') &&
        [ParameterCorrectionStatus.INITIATED, ParameterCorrectionStatus.CORRECTED].includes(
          correction.status,
        ) &&
        canRecallCorrection && (
          <Button variant="secondary" color="red" onClick={handleRecallAction}>
            Recall
          </Button>
        )}
    </CorrectionActionButtonWrapper>
  );
};
