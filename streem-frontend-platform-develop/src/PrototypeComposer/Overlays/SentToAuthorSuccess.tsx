import { BaseModal } from '#components';
import { CommonOverlayProps } from '#components/OverlayContainer/types';
import MemoSentToAuthorSuccess from '#assets/svg/SentToAuthorSuccess';
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

const SentToAuthorSuccessModal: FC<
  CommonOverlayProps<{
    heading: string;
  }>
> = ({ closeAllOverlays, closeOverlay, props: { heading } }) => {
  return (
    <Wrapper>
      <BaseModal
        closeAllModals={closeAllOverlays}
        closeModal={closeOverlay}
        showFooter={false}
        showHeader={false}
      >
        <MemoSentToAuthorSuccess />
        <h3>{heading}</h3>
        <span>You and your Team Members Comments have been sent to Authors for Changes.</span>
      </BaseModal>
    </Wrapper>
  );
};

export default SentToAuthorSuccessModal;
