import { CommonOverlayProps } from '#components/OverlayContainer/types';
import { useTypedSelector } from '#store';
import { TaskActionType } from '#types';
import { BulkVerificationModal } from '#views/Job/components/Task/Header/BulkVerificationModal';
import React from 'react';

const BulkPeerVerificationModalContainer = (props: CommonOverlayProps) => {
  const { closeOverlay, closeAllOverlays } = props;

  const pendingPeerVerificationParameters = useTypedSelector(
    (state) => state.job.activeTask.pendingPeerVerificationParameters,
  );

  return (
    <BulkVerificationModal
      verificationPendingParameters={pendingPeerVerificationParameters || []}
      source={TaskActionType.BULK_PEER_VERIFICATION}
      closeOverlay={closeOverlay}
      closeAllOverlays={closeAllOverlays}
    />
  );
};

export default BulkPeerVerificationModalContainer;
