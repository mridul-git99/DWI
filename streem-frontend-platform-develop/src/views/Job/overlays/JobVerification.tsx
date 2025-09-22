import { BaseModal } from '#components';
import { CommonOverlayProps } from '#components/OverlayContainer/types';
import { Job } from '#views/Jobs/ListView/types';
import React, { FC } from 'react';
import styled from 'styled-components';
import VerificationsContent from '#views/Inbox/ListView/VerificationsContent';
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

const JobVerification: FC<
  CommonOverlayProps<{
    jobId?: Job['id'];
    redirectedFromBanner?: boolean;
  }>
> = ({ closeAllOverlays, closeOverlay, props: { jobId, redirectedFromBanner } }) => {
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
        <VerificationsContent values={{ jobId, isJobOpen: true, redirectedFromBanner }} />
      </BaseModal>
    </Wrapper>
  );
};

export default JobVerification;
