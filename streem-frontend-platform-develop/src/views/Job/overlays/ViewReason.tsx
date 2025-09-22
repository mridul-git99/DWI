import { BaseModal } from '#components';
import { CommonOverlayProps } from '#components/OverlayContainer/types';
import { getFullName } from '#utils/stringUtils';
import React, { FC } from 'react';
import styled from 'styled-components';

const Wrapper = styled.div`
  font-size: 14px;

  .modal {
    min-width: 406px !important;
    max-width: 406px !important;
  }

  .title {
    font-size: 14px;
    font-weight: 700;
  }

  .reason {
    padding-top: 24px;
  }
`;

const Title = () => {
  return <div className="title">Parameter verification rejection reason</div>;
};

const ViewReason: FC<
  CommonOverlayProps<{
    rejectedBy?: any;
    reason?: string;
  }>
> = ({ closeAllOverlays, closeOverlay, props: { rejectedBy, reason } }) => {
  return (
    <Wrapper>
      <BaseModal
        closeAllModals={closeAllOverlays}
        closeModal={closeOverlay}
        showHeader={true}
        title={<Title />}
        showFooter={false}
        showCloseIcon={true}
      >
        <div>
          {getFullName(rejectedBy)}, (ID: {rejectedBy.employeeId}) reject the parameter verification
          stating the following reason
        </div>
        <div className="reason">{reason}</div>
      </BaseModal>
    </Wrapper>
  );
};

export default ViewReason;
