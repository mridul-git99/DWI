import { BaseModal } from '#components';
import { CommonOverlayProps } from '#components/OverlayContainer/types';
import { navigate } from '@reach/router';
import React, { FC } from 'react';
import { useDispatch } from 'react-redux';
import styled from 'styled-components';
import { jobActions } from '../jobStore';

export enum RefetchJobErrorType {
  JOB = 'JOB',
  TASK = 'TASK',
  PARAMETER = 'PARAMETER',
}

const Wrapper = styled.div`
  .modal {
    .modal-header {
      border-bottom: 1px solid #f4f4f4 !important;
      h2 {
        color: #161616 !important;
        font-weight: bold !important;
        font-size: 14px !important;
      }
    }

    .modal-body {
      color: #525252;
      font-size: 14px;
      text-align: start;
    }

    .modal-footer {
      border-top: 1px solid #f4f4f4 !important;
    }
  }
`;

const RefetchJob: FC<
  CommonOverlayProps<{
    modalTitle: string;
    jobId: string;
    taskExecutionId?: string;
    errorType: RefetchJobErrorType;
  }>
> = ({ closeOverlay, closeAllOverlays, props: { modalTitle, taskExecutionId, errorType } }) => {
  const dispatch = useDispatch();

  return (
    <Wrapper>
      <BaseModal
        showSecondary={false}
        allowCloseOnOutsideClick={false}
        primaryText={'Refresh'}
        title={modalTitle}
        closeModal={closeOverlay}
        closeAllModals={closeAllOverlays}
        showCloseIcon={false}
        onPrimary={() => {
          dispatch(jobActions.reset());
          if (taskExecutionId) {
            navigate(`?taskExecutionId=${taskExecutionId}`);
          }
        }}
      >
        <div>
          Some actions were already performed on this {errorType.toLowerCase()}. Please refresh the
          job to see the changes.
        </div>
      </BaseModal>
    </Wrapper>
  );
};

export default RefetchJob;
