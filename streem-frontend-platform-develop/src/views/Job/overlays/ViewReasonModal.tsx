import { BaseModal } from '#components';
import { CommonOverlayProps } from '#components/OverlayContainer/types';
import { User } from '#services/users';
import { getFullName } from '#utils/stringUtils';
import { formatDateTime } from '#utils/timeUtils';
import React, { FC } from 'react';
import styled from 'styled-components';

const Wrapper = styled.div`
  font-size: 14px;

  .modal {
    min-width: 600px !important;
    max-width: 600px !important;
  }

  .title {
    font-size: 14px;
    font-weight: 700;
  }

  .text {
    color: #525252;
    font-size: 12px;
  }

  .reason-title {
    margin-bottom: 8px;
  }

  .reason {
    padding: 12px 16px 12px 16px;
    border: 1px solid #e0e0e0;
    text-wrap: balance;
  }

  .details {
    margin-top: 16px;
  }
`;

const modalTitle = (title: any) => {
  return <div className="title">{title}</div>;
};

const ViewReasonModal: FC<
  CommonOverlayProps<{
    title: string;
    reasonType?: 'start' | 'end';
    reason: string;
    userDetails: User;
    timeStamp: number;
  }>
> = ({
  closeAllOverlays,
  closeOverlay,
  props: { title, reasonType, reason, userDetails, timeStamp },
}) => {
  return (
    <Wrapper>
      <BaseModal
        closeAllModals={closeAllOverlays}
        closeModal={closeOverlay}
        showHeader={true}
        title={modalTitle(title)}
        showFooter={false}
        showCloseIcon={true}
      >
        <div className="text reason-title">Reason</div>
        <pre className="reason">{reason}</pre>
        {reasonType ? (
          <div className="text details">
            Task {reasonType === 'start' ? 'started' : 'completed'} by {getFullName(userDetails)},
            ID: {userDetails?.employeeId} on {formatDateTime({ value: timeStamp })}
          </div>
        ) : null}
      </BaseModal>
    </Wrapper>
  );
};

export default ViewReasonModal;
