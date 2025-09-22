import { ParameterVerificationTypeEnum } from '#PrototypeComposer/checklist.types';
import { ParameterState } from '#types';
import { isEqual } from 'lodash';
import React, { FC, memo } from 'react';
import PeerVerification from './PeerVerification';
import SelfVerification from './SelfVerification';

const ParameterVerificationView: FC<{
  isLoggedInUserAssigned?: boolean;
  parameterState: ParameterState;
  verificationsByType: any;
  verificationType: string;
  parameterResponseId: string;
  modifiedBy: string;
  correctionEnabled: boolean;
  isExceptionEnabled?: boolean;
  parameterId: string;
}> = ({
  isLoggedInUserAssigned,
  parameterState,
  verificationsByType,
  verificationType,
  parameterResponseId,
  modifiedBy,
  correctionEnabled,
  isExceptionEnabled,
  parameterId,
}) => {
  const renderVerificationView = () => {
    switch (verificationType) {
      case ParameterVerificationTypeEnum.SELF:
        return (
          <SelfVerification
            parameterResponseId={parameterResponseId}
            verification={verificationsByType?.[ParameterVerificationTypeEnum.SELF]}
            isLoggedInUserAssigned={isLoggedInUserAssigned}
            parameterState={parameterState}
            modifiedBy={modifiedBy}
            correctionEnabled={correctionEnabled}
            parameterId={parameterId}
          />
        );
      case ParameterVerificationTypeEnum.PEER:
        return (
          <PeerVerification
            parameterResponseId={parameterResponseId}
            verifications={verificationsByType}
            verificationType={verificationType}
            isLoggedInUserAssigned={isLoggedInUserAssigned}
            parameterState={parameterState}
            modifiedBy={modifiedBy}
            correctionEnabled={correctionEnabled}
            parameterId={parameterId}
          />
        );
      case ParameterVerificationTypeEnum.BOTH:
        return (
          <>
            <SelfVerification
              parameterResponseId={parameterResponseId}
              verification={verificationsByType?.[ParameterVerificationTypeEnum.SELF]}
              isLoggedInUserAssigned={isLoggedInUserAssigned}
              parameterState={parameterState}
              modifiedBy={modifiedBy}
              correctionEnabled={correctionEnabled}
              parameterId={parameterId}
            />
            <PeerVerification
              parameterResponseId={parameterResponseId}
              verificationType={verificationType}
              verifications={verificationsByType}
              isLoggedInUserAssigned={isLoggedInUserAssigned}
              parameterState={parameterState}
              modifiedBy={modifiedBy}
              correctionEnabled={correctionEnabled}
              parameterId={parameterId}
            />
          </>
        );
      default:
        return null;
    }
  };

  return !verificationType ||
    verificationType === ParameterVerificationTypeEnum.NONE ||
    isExceptionEnabled
    ? null
    : renderVerificationView();
};

export default memo(ParameterVerificationView, (prev, next) => {
  return (
    prev.parameterState === next.parameterState &&
    prev.modifiedBy === next.modifiedBy &&
    prev.correctionEnabled === next.correctionEnabled &&
    isEqual(prev.verificationsByType, next.verificationsByType)
  );
});
