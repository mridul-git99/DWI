import SelfVerifiedIcon from '#assets/svg/SelfVerifiedIcon';
import { Button } from '#components';
import { useTypedSelector } from '#store';
import { ParameterState, ParameterVerificationStatus } from '#types';
import { getFullName } from '#utils/stringUtils';
import { formatDateTime } from '#utils/timeUtils';
import { jobActions } from '#views/Job/jobStore';
import { useJobStateToFlags } from '#views/Job/utils';
import React, { FC, memo } from 'react';
import { useDispatch } from 'react-redux';
import SelfVerificationAction from './SelfVerificationAction';

const SelfVerification: FC<{
  parameterResponseId: string;
  verification: any;
  isLoggedInUserAssigned?: boolean;
  parameterState: string;
  modifiedBy: string;
  correctionEnabled: boolean;
  parameterId: string;
}> = ({
  parameterResponseId,
  verification,
  isLoggedInUserAssigned,
  parameterState,
  modifiedBy,
  correctionEnabled,
  parameterId,
}) => {
  const dispatch = useDispatch();
  const {
    auth: { userId },
  } = useTypedSelector((state) => state);

  const { isCompletedWithException, isTaskCompletedWithException, isTaskCompleted } =
    useJobStateToFlags();

  const SelfVerifyButton = () => {
    if (modifiedBy === userId)
      return (
        <div className="parameter-verification">
          <Button
            onClick={() => {
              dispatch(jobActions.initiateSelfVerification({ parameterResponseId, parameterId }));
            }}
          >
            Self Verify
          </Button>
        </div>
      );
    return null;
  };

  if (
    (isCompletedWithException || (isTaskCompletedWithException && !correctionEnabled)) &&
    verification?.verificationStatus !== ParameterVerificationStatus.ACCEPTED
  )
    return null;

  if (
    parameterState === ParameterState.BEING_EXECUTED &&
    isLoggedInUserAssigned &&
    !isTaskCompleted
  ) {
    return <SelfVerifyButton />;
  }

  switch (verification?.verificationStatus) {
    case ParameterVerificationStatus.ACCEPTED:
      return (
        <div className="parameter-audit">
          <div className="parameter-verified">
            <SelfVerifiedIcon />
            <div>
              {`${
                verification.bulk
                  ? `Examined by ${getFullName(verification.modifiedBy)}, ID:${' '}
              ${verification.modifiedBy.employeeId} on${' '}
              ${formatDateTime({ value: verification.modifiedAt })}. Signed for Self Verification`
                  : `Self Verified by ${getFullName(verification.modifiedBy)}, ID:${' '}
              ${verification.modifiedBy.employeeId} on${' '}
              ${formatDateTime({ value: verification.modifiedAt })}`
              }`}
            </div>
          </div>
        </div>
      );

    case ParameterVerificationStatus.PENDING:
      return userId !== verification?.requestedTo?.id ? null : (
        <SelfVerificationAction
          parameterResponseId={parameterResponseId}
          parameterId={parameterId}
        />
      );

    default:
      return null;
  }
};

export default memo(SelfVerification);
