import CheckIcon from '@material-ui/icons/Check';
import { BaseModal } from '#components';
import { CommonOverlayProps } from '#components/OverlayContainer/types';
import React, { FC } from 'react';
import styled from 'styled-components';

const Wrapper = styled.div`
  .modal {
    max-width: 480px !important;
    min-width: unset !important;
    border-radius: 8px !important;

    .modal-body {
      padding: 48px 48px 56px !important;
      justify-content: center;
      align-items: center;
      display: flex;
      flex-direction: column;

      .icon-container {
        display: flex;
        align-items: center;
        justify-content: center;
        border-radius: 50px;
        width: 100px;
        height: 100px;
        background-color: #e1fec0;

        .icon {
          color: #4f9913;
          font-size: 55px;
        }
      }

      .heading {
        font-size: 28px;
        line-height: 1.14;
        color: #333333;
        margin-top: 40px;
      }

      .sub-heading {
        font-size: 14px;
        line-height: 1.14;
        letter-spacing: 0.16px;
        color: #666666;
        margin-top: 8px;
      }

      .key-content {
        font-size: 20px;
        font-weight: 600;
        line-height: 1.2;
        color: #333333;
        margin-top: 24px;
      }

      .alert {
        display: flex;
        flex-direction: row;
        padding: 4px;
        border-radius: 4px;
        border: solid 1px #f7b500;
        background-color: rgba(247, 181, 0, 0.2);
        font-size: 12px;
        line-height: 1.33;
        letter-spacing: 0.32px;
        color: #aa7e05;
        margin-top: 16px;
        max-width: 340px;
      }
    }
  }
`;

const SecretKeyModal: FC<
  CommonOverlayProps<{
    heading: string;
    subHeading: string;
    key: string;
    showSecretKeyInfo?: boolean;
  }>
> = ({
  closeAllOverlays,
  closeOverlay,
  props: { heading, subHeading, key, showSecretKeyInfo = true },
}) => {
  return (
    <Wrapper>
      <BaseModal
        closeAllModals={closeAllOverlays}
        closeModal={closeOverlay}
        showFooter={false}
        showHeader={false}
      >
        <div className="icon-container">
          <CheckIcon className="icon" />
        </div>
        <div className="heading">{heading}</div>
        {showSecretKeyInfo && (
          <>
            <div className="sub-heading">{subHeading}</div>
            <div className="key-content">Secret Key : {key}</div>
            <div className="alert">
              The Secret Key will not be visible again. The Secret key should be given to the user
              for registration.
            </div>
          </>
        )}
      </BaseModal>
    </Wrapper>
  );
};

export default SecretKeyModal;
