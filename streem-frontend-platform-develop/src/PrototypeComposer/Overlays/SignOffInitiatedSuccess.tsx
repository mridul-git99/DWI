import { BaseModal } from '#components';
import { CommonOverlayProps } from '#components/OverlayContainer/types';
import MemoSignOff from '#assets/svg/SignOff';
import React, { FC } from 'react';
import styled from 'styled-components';

const Wrapper = styled.div`
  .modal {
    max-width: 400px !important;
    min-width: 100px !important;

    .modal-body {
      padding: 54px 50px !important;
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
        color: #000;
      }
    }
  }
`;

const SignOffInitiatedSuccessModal: FC<CommonOverlayProps<any>> = ({
  closeAllOverlays,
  closeOverlay,
}) => {
  return (
    <Wrapper>
      <BaseModal
        closeAllModals={closeAllOverlays}
        closeModal={closeOverlay}
        showFooter={false}
        showHeader={false}
      >
        <MemoSignOff />
        <h3>Sign Off Initiated</h3>
        <span>Users will receive notification as per sequence</span>
      </BaseModal>
    </Wrapper>
  );
};

export default SignOffInitiatedSuccessModal;
