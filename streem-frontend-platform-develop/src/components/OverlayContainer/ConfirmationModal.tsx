import { BaseModal } from '#components';
import { CommonOverlayProps } from '#components/OverlayContainer/types';
import React, { FC } from 'react';
import styled from 'styled-components';

export interface ConfirmationModalProps {
  onPrimary: () => void;
  primaryText?: string;
  secondaryText?: string;
  title?: string;
  body?: JSX.Element;
}

const Wrapper = styled.div`
  #modal-container {
    z-index: 1400 !important;
    .modal {
      min-width: 400px !important;
      max-width: 500px !important;

      h2 {
        color: #161616 !important;
        font-weight: bold !important;
      }

      .modal-footer {
        flex-direction: row-reverse !important;
      }

      .modal-body {
        font-size: 14px;
        color: #000000;
        display: flex;
        flex-direction: column;
        gap: 8px;
      }
    }
  }
`;

export const ConfirmationModal: FC<CommonOverlayProps<ConfirmationModalProps>> = ({
  closeAllOverlays,
  closeOverlay,
  props: { onPrimary, primaryText = 'Confirm', secondaryText = 'Cancel', title, body },
}) => {
  return (
    <Wrapper>
      <BaseModal
        closeAllModals={closeAllOverlays}
        closeModal={closeOverlay}
        onSecondary={closeOverlay}
        title={title}
        primaryText={primaryText}
        secondaryText={secondaryText}
        onPrimary={() => {
          onPrimary();
          closeOverlay();
        }}
      >
        {body && body}
      </BaseModal>
    </Wrapper>
  );
};
