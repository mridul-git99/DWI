import { ParameterVerificationTypeEnum } from '#PrototypeComposer/checklist.types';
import PeerVerifiedIcon from '#assets/svg/PeerVerifiedIcon';
import SelfVerifiedIcon from '#assets/svg/SelfVerifiedIcon';
import peerRejectedIcon from '#assets/svg/rejected-icon.svg';
import { Button } from '#components';
import { openOverlayAction } from '#components/OverlayContainer/actions';
import { OverlayNames } from '#components/OverlayContainer/types';
import { useTypedSelector } from '#store/helpers';
import { ParameterState, ParameterVerificationStatus } from '#types';
import { getFullName } from '#utils/stringUtils';
import { formatDateTime } from '#utils/timeUtils';
import { jobActions } from '#views/Job/jobStore';
import { Verification } from '#views/Jobs/ListView/types';
import { Dictionary } from 'lodash';
import React, { FC, memo, useState } from 'react';
import { useDispatch } from 'react-redux';
import PeerVerificationAction from './PeerVerificationAction';
import { useJobStateToFlags } from '#views/Job/utils';
import { AssignPeerVerificationContainer } from './AssignPeerVerificationContainer';
import { isFeatureAllowed } from '#services/uiPermissions';

type PeerVerificationProps = {
  parameterResponseId: string;
  verificationType: ParameterVerificationTypeEnum | ParameterVerificationTypeEnum.BOTH;
  verifications: Dictionary<Verification[]>;
  isLoggedInUserAssigned?: boolean;
  parameterState: ParameterState;
  modifiedBy: string;
  correctionEnabled: boolean;
  parameterId: string;
};

export const getVerificationStatus = (verificationArray: Verification[] = [], userId: string) => {
  let isUserReviewer = false;
  let isUserSubmitter = false;
  let approvalDetails = null;
  let rejectionDetails = null;
  let isBulkVerification = false;
  let status = '';

  verificationArray.forEach((verification: any) => {
    switch (verification?.verificationStatus) {
      case ParameterVerificationStatus.PENDING:
        status = ParameterVerificationStatus.PENDING;
        break;
      case ParameterVerificationStatus.ACCEPTED:
        status = ParameterVerificationStatus.ACCEPTED;
        approvalDetails = verification;
        break;
      case ParameterVerificationStatus.REJECTED:
        status = ParameterVerificationStatus.REJECTED;
        rejectionDetails = verification;
        break;
      default:
        break;
    }

    if (!isUserReviewer && verification.requestedTo.id === userId) {
      isUserReviewer = true;
    }

    if (!isUserSubmitter && verification.createdBy.id === userId) {
      isUserSubmitter = true;
    }

    if (verification.bulk) {
      isBulkVerification = true;
    }
  });

  return {
    isUserSubmitter,
    isUserReviewer,
    approvalDetails,
    rejectionDetails,
    status,
    isBulkVerification,
  };
};

const PeerVerification: FC<PeerVerificationProps> = ({
  parameterResponseId,
  verificationType,
  verifications,
  isLoggedInUserAssigned,
  parameterState,
  modifiedBy,
  correctionEnabled,
  parameterId,
}) => {
  const {
    auth: { userId },
  } = useTypedSelector((state) => state);
  const [createJobDrawerVisible, setCreateJobDrawerVisible] = useState(false);

  const { isCompletedWithException, isTaskCompletedWithException, isTaskCompleted } =
    useJobStateToFlags();

  const dispatch = useDispatch();
  const peerVerificationsArray = verifications?.[ParameterVerificationTypeEnum.PEER] || [];
  let showVerification = true;

  const {
    isUserReviewer,
    isUserSubmitter,
    approvalDetails,
    rejectionDetails,
    status: verificationStatus,
    isBulkVerification,
  } = getVerificationStatus(peerVerificationsArray, userId!);

  if (
    (isCompletedWithException || (isTaskCompletedWithException && !correctionEnabled)) &&
    verificationStatus === ParameterVerificationStatus.PENDING
  )
    return null;

  if (
    (verificationType === ParameterVerificationTypeEnum.BOTH &&
      verifications?.[ParameterVerificationTypeEnum.SELF]?.verificationStatus !==
        ParameterVerificationStatus.ACCEPTED) ||
    verifications?.[ParameterVerificationTypeEnum.SELF]?.evaluationState ===
      ParameterState.BEING_EXECUTED
  ) {
    showVerification = false;
  }

  if (!showVerification) {
    return null;
  }

  const renderByParameterState = () => {
    if (
      [userId, '1'].includes(modifiedBy) &&
      isLoggedInUserAssigned &&
      !isCompletedWithException &&
      !(isTaskCompleted && !correctionEnabled) &&
      (parameterState === ParameterState.BEING_EXECUTED ||
        parameterState === ParameterState.VERIFICATION_PENDING)
    )
      return (
        <div className="parameter-verification">
          <Button
            onClick={() => {
              setCreateJobDrawerVisible(true);
            }}
          >
            Request Verification
          </Button>
        </div>
      );
  };

  const renderByVerificationState = () => {
    if (verificationStatus === ParameterVerificationStatus.PENDING) {
      return (
        (isUserReviewer || isUserSubmitter) && (
          <div>
            {isUserReviewer && (
              <PeerVerificationAction
                parameterResponseId={parameterResponseId}
                parameterId={parameterId}
              />
            )}
            {isUserSubmitter && (
              <div className="parameter-verification">
                <Button
                  onClick={() => {
                    dispatch(
                      openOverlayAction({
                        type: OverlayNames.CONFIRMATION_MODAL,
                        props: {
                          primaryText: 'Submit',
                          title: 'Recall From Verification',
                          body: 'Are you sure you want to Recall from verification ?',
                          onPrimary: () =>
                            dispatch(
                              jobActions.recallPeerVerification({
                                parameterResponseId,
                                parameterId,
                                type: 'peer',
                              }),
                            ),
                        },
                      }),
                    );
                  }}
                >
                  Recall Verification
                </Button>
                {isFeatureAllowed('sameSessionVerification') && (
                  <Button
                    onClick={() => {
                      dispatch(
                        openOverlayAction({
                          type: OverlayNames.SAME_SESSION_VERIFICATION_MODAL,
                          props: {
                            parameterResponseId,
                            parameterId,
                            verifications,
                          },
                        }),
                      );
                    }}
                  >
                    Same Session Verification
                  </Button>
                )}
              </div>
            )}
          </div>
        )
      );
    } else if (verificationStatus === ParameterVerificationStatus.ACCEPTED && approvalDetails) {
      return (
        <div className="parameter-audit">
          <div className="parameter-verified">
            {isBulkVerification ? <SelfVerifiedIcon color="#1D84FF" /> : <PeerVerifiedIcon />}
            <div>
              {isBulkVerification
                ? `Reviewed by ${getFullName(approvalDetails.modifiedBy)}, ID:${' '}
              ${approvalDetails.modifiedBy.employeeId} on${' '}
              ${formatDateTime({
                value: approvalDetails.modifiedAt,
              })}. Signed for Peer Verification`
                : `Peer Verification approved by ${getFullName(
                    approvalDetails.modifiedBy,
                  )}, ID:${' '}
              ${approvalDetails.modifiedBy.employeeId} on${' '}
              ${formatDateTime({ value: approvalDetails.modifiedAt })}`}
              .
            </div>
          </div>
        </div>
      );
    } else if (verificationStatus === ParameterVerificationStatus.REJECTED && rejectionDetails) {
      return (
        <div className="parameter-audit">
          <div className="parameter-verified">
            <img src={peerRejectedIcon} alt="Peer rejected" />
            <div>
              Peer Verification has been rejected by {getFullName(rejectionDetails.modifiedBy)}, ID:{' '}
              {rejectionDetails.modifiedBy.employeeId} on{' '}
              {formatDateTime({ value: rejectionDetails.modifiedAt })}.{' '}
              <span
                onClick={() => {
                  dispatch(
                    openOverlayAction({
                      type: OverlayNames.VIEW_REASON,
                      props: {
                        rejectedBy: rejectionDetails.modifiedBy,
                        reason: rejectionDetails.comments,
                      },
                    }),
                  );
                }}
              >
                View Reason
              </span>
            </div>
          </div>
        </div>
      );
    } else {
      return null;
    }
  };

  return (
    <>
      {renderByParameterState()}
      {renderByVerificationState()}
      {createJobDrawerVisible && (
        <AssignPeerVerificationContainer
          onCloseDrawer={setCreateJobDrawerVisible}
          onSubmit={(data: any, callback: any) => {
            dispatch(
              jobActions.sendPeerVerification({
                parameterResponseId,
                parameterId,
                data,
              }),
            );
            callback();
          }}
        />
      )}
    </>
  );
};

export default memo(PeerVerification);
