import { BaseModal, Button } from '#components';
import { CommonOverlayProps } from '#components/OverlayContainer/types';
import MemoRevisionError from '#assets/svg/RevisionError';
import React, { FC } from 'react';
import { navigate } from '@reach/router';
import styled from 'styled-components';
import { Checklist } from '#PrototypeComposer/checklist.types';

const Wrapper = styled.div`
  .modal {
    max-width: 400px !important;
    min-width: 100px !important;

    .modal-body {
      padding: 32px 42px 42px !important;
      justify-content: center;
      align-items: center;
      display: flex;
      flex-direction: column;

      > h3 {
        font-size: 20px;
        font-weight: bold;
        color: #000000;
        margin: 32px 0px 8px 0px;
      }

      > span {
        font-size: 14px;
        color: #666666;
        margin-bottom: 24px;
      }
    }
  }
`;

const RevisionErrorModal: FC<
  CommonOverlayProps<{
    id: Checklist['id'];
  }>
> = ({ closeAllOverlays, closeOverlay, props }) => {
  const id = props?.id || null;
  if (!id) {
    return closeOverlay();
  }
  return (
    <Wrapper>
      <BaseModal
        closeAllModals={closeAllOverlays}
        closeModal={closeOverlay}
        showFooter={false}
        showHeader={false}
      >
        <MemoRevisionError fontSize={280} style={{ height: '150px' }} />
        <h3>Process is already being Revised</h3>
        <span>
          You cannot Revise the Process, it is already being revised by your Team Members. But, you
          can view the ongoing Revision.
        </span>
        <Button
          onClick={() => {
            closeOverlay();
            navigate(`checklists/${id}`);
          }}
        >
          View Ongoing Revision
        </Button>
      </BaseModal>
    </Wrapper>
  );
};

export default RevisionErrorModal;
