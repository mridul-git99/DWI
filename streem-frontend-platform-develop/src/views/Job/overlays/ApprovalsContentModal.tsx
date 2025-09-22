import { BaseModal } from '#components';
import { CommonOverlayProps } from '#components/OverlayContainer/types';
import { useTypedSelector } from '#store';
import ApprovalsContent from '#views/Inbox/ListView/ApprovalsContent';
import { Job } from '#views/Jobs/ListView/types';
import React, { FC } from 'react';
import styled from 'styled-components';
import KeyboardArrowLeftIcon from '@material-ui/icons/KeyboardArrowLeft';
import { Link } from '#components';

const Wrapper = styled.div`
  .modal {
    height: 100%;
    min-height: 100dvh;
    min-width: 100dvw !important;
  }

  .modal-body {
    height: 100%;
  }
`;

const ApprovalsContentModal: FC<
  CommonOverlayProps<{
    jobId?: Job['id'];
  }>
> = ({ closeAllOverlays, closeOverlay, props: { jobId } }) => {
  const { isInboxView } = useTypedSelector((state) => state.job);
  return (
    <Wrapper>
      <BaseModal
        closeAllModals={closeAllOverlays}
        closeModal={closeOverlay}
        showHeader={true}
        title={
          <Link
            label="Back to Task"
            backIcon={KeyboardArrowLeftIcon}
            onClick={closeOverlay}
            iconColor="#000000"
            labelColor="#000000"
            addMargin={false}
          />
        }
        showFooter={false}
        showCloseIcon={true}
      >
        <ApprovalsContent values={{ jobId, isJobOpen: true, isInboxView }} />
      </BaseModal>
    </Wrapper>
  );
};

export default ApprovalsContentModal;
