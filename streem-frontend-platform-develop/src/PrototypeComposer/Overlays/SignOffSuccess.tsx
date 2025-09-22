import { BaseModal } from '#components';
import { CommonOverlayProps } from '#components/OverlayContainer/types';
import MemoSignOffSuccess from '#assets/svg/SignOffSuccess';
import React, { FC } from 'react';
import styled from 'styled-components';

const Wrapper = styled.div`
  .modal {
    max-width: 425px !important;
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

const SignOffSuccessModal: FC<CommonOverlayProps<any>> = ({ closeAllOverlays, closeOverlay }) => {
  return (
    <Wrapper>
      <BaseModal
        closeAllModals={closeAllOverlays}
        closeModal={closeOverlay}
        showFooter={false}
        showHeader={false}
      >
        <MemoSignOffSuccess fontSize={280} style={{ height: '150px' }} />
        <h3>You have successfully Signed Off the process</h3>
      </BaseModal>
    </Wrapper>
  );
};

export default SignOffSuccessModal;
