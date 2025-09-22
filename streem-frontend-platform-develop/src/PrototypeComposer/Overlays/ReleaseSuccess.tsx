import { BaseModal } from '#components';
import { CommonOverlayProps } from '#components/OverlayContainer/types';
import MemoReleaseSuccess from '#assets/svg/ReleaseSuccess';
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

const ReleaseSuccessModal: FC<CommonOverlayProps<any>> = ({ closeAllOverlays, closeOverlay }) => {
  return (
    <Wrapper>
      <BaseModal
        closeAllModals={closeAllOverlays}
        closeModal={closeOverlay}
        showFooter={false}
        showHeader={false}
      >
        <MemoReleaseSuccess fontSize={280} style={{ height: '150px' }} />
        <h3>Process has been Released</h3>
      </BaseModal>
    </Wrapper>
  );
};

export default ReleaseSuccessModal;
