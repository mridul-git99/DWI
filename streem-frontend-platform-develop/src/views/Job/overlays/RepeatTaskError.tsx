import { BaseModal } from '#components';
import { CommonOverlayProps } from '#components/OverlayContainer/types';
import React, { FC } from 'react';
import styled from 'styled-components';
import repeatTaskError from '#assets/svg/repeat-task-error.svg';

const Wrapper = styled.div`
  .modal {
    width: 406px !important;
    .modal-body {
      padding: 24px !important;
      justify-content: center;
      align-items: center;
      display: flex;
      flex-direction: column;
      gap: 16px;
      .title {
        font-size: 20px;
        font-weight: 700;
        color: #525252;
        line-height: normal;
      }
      > p {
        margin: 0;
        font-size: 14px;
        color: #525252;
        line-height: 16px;
        text-align: center;
      }
    }
  }
`;

const RepeatTaskError: FC<CommonOverlayProps<{}>> = ({ closeAllOverlays, closeOverlay }) => {
  return (
    <Wrapper>
      <BaseModal
        closeAllModals={closeAllOverlays}
        closeModal={closeOverlay}
        showFooter={false}
        showHeader={false}
      >
        <img src={repeatTaskError} />
        <div className="title">Unable to Repeat Task</div>
        <p>
          If you wish to repeat this task, please complete any dependent tasks that have already
          been initiated. This ensures the integrity of the workflow and accurate recording of task
          completions.
        </p>
      </BaseModal>
    </Wrapper>
  );
};

export default RepeatTaskError;
