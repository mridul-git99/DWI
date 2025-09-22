import { CommonOverlayProps } from '#components/OverlayContainer/types';
import { useTypedSelector } from '#store';
import { TaskActionType } from '#types';
import { BulkVerificationModal } from '#views/Job/components/Task/Header/BulkVerificationModal';
import React from 'react';

const BulkSelfVerificationModalContainer = (props: CommonOverlayProps) => {
  const { closeOverlay, closeAllOverlays } = props;

  const pendingSelfVerificationParameters = useTypedSelector(
    (state) => state.job.activeTask.pendingSelfVerificationParameters,
  );

  return (
    <BulkVerificationModal
      verificationPendingParameters={pendingSelfVerificationParameters || []}
      source={TaskActionType.BULK_SELF_VERIFICATION}
      closeOverlay={closeOverlay}
      closeAllOverlays={closeAllOverlays}
    />
  );
};

export default BulkSelfVerificationModalContainer;
