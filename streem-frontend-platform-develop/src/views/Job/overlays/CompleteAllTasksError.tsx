import EditingDisabledIcon from '#assets/svg/EditingDisabledIcon';
import { BaseModal } from '#components';
import { CommonOverlayProps } from '#components/OverlayContainer/types';
import React, { FC } from 'react';
import styled from 'styled-components';

const Wrapper = styled.div`
  .modal {
    min-width: 420px !important;
    width: 420px !important;
  }

  .modal-body {
    padding: 0 !important;
  }

  .close-icon {
    top: 24px !important;
    right: 32px !important;
  }

  .body {
    padding: 80px;

    .editing-disabled-icon {
      font-size: 120px;
    }

    .text {
      color: #000000;
      font-size: 20px;
      font-weight: bold;
      margin-top: 24px;
    }
  }
`;

const JobCompleteAllTasksError: FC<CommonOverlayProps<{}>> = ({
  closeAllOverlays,
  closeOverlay,
}) => (
  <Wrapper>
    <BaseModal
      closeAllModals={closeAllOverlays}
      closeModal={closeOverlay}
      showHeader={false}
      showFooter={false}
    >
      <div className="body">
        <EditingDisabledIcon className="editing-disabled-icon" />
        <div className="text">
          Some of the tasks are in progress. Please complete them before completing Job with
          Exception
        </div>
      </div>
    </BaseModal>
  </Wrapper>
);

export default JobCompleteAllTasksError;
