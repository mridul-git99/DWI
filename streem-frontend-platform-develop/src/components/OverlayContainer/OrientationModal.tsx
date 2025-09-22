import React, { FC } from 'react';
import styled from 'styled-components';
import { BaseModal } from '#components';
import { CommonOverlayProps } from './types';
import PhotoRotate from '#assets/svg/PhoneRotate.svg';
import { Close } from '@material-ui/icons';

const Wrapper = styled.div.attrs({})`
  #modal-container {
    background: rgba(22, 22, 22, 0.9);
  }
  #modal-container .modal-background .modal {
    background: transparent;
    box-shadow: none;
  }
  .modal {
    .modal-body {
      height: 100dvh;
      padding: 0px !important;
      overflow: hidden !important;
      .container {
        display: flex;
        flex-direction: column;
        justify-content: center;
        align-items: center;
        height: 100%;
        width: 90dvw;
        gap: 16px;
        > div {
          font-size: 16px;
          font-style: normal;
          font-weight: 700;
          color: #fff;
        }

        > div:last-child {
          font-size: 14px;
          text-align: center;
        }

        .close {
          position: fixed;
          top: 30px;
          right: 0px;
          color: #fff;
          width: 32px;
          height: 32px;
        }
      }
    }
  }
`;

const OrientationModal: FC<CommonOverlayProps<any>> = ({ closeAllOverlays, closeOverlay }) => {
  return (
    <Wrapper>
      <BaseModal
        closeAllModals={closeAllOverlays}
        closeModal={closeOverlay}
        onSecondary={closeOverlay}
        showFooter={false}
        showPrimary={false}
        showSecondary={false}
        showCloseIcon={false}
        showHeader={false}
        onPrimary={() => {
          closeOverlay();
        }}
      >
        <div className="container">
          <Close className="close" onClick={closeOverlay} />
          <img src={PhotoRotate} alt="Phone Rotate" />
          <div>Landscape mode not supported</div>
          <div>
            Landscape mode not supported, please use the app in portrait mode for best experience
          </div>
        </div>
      </BaseModal>
    </Wrapper>
  );
};

export default OrientationModal;
