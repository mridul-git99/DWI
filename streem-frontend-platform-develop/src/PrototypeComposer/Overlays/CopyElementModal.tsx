import { BaseModal, Button } from '#components';
import { CommonOverlayProps } from '#components/OverlayContainer/types';
import { useTypedSelector } from '#store';
import React, { FC, useEffect, useRef } from 'react';
import styled from 'styled-components';

export interface CopyElementModalProps {
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

      .modal-body {
        font-size: 14px;
        color: #000000;
        display: flex;
        flex-direction: column;
        gap: 8px;
      }

      .footer-btn {
        display: flex;
        margin-left: auto;
        margin-top: 16px;
      }
    }
  }
`;

export const CopyElementModal: FC<CommonOverlayProps<CopyElementModalProps>> = ({
  closeAllOverlays,
  closeOverlay,
  props: { onPrimary, title, body },
}) => {
  const isSubmitted = useRef(false);
  const { isCopying } = useTypedSelector((state) => state.prototypeComposer);

  useEffect(() => {
    if (isSubmitted.current && !isCopying) {
      closeOverlay();
      isSubmitted.current = false;
    }
  }, [isCopying, isSubmitted.current]);

  return (
    <Wrapper>
      <BaseModal
        closeAllModals={closeAllOverlays}
        closeModal={closeOverlay}
        onSecondary={closeOverlay}
        title={title}
        allowCloseOnOutsideClick={isCopying}
        showFooter={false}
      >
        {body && body}
        <div className="footer-btn">
          <Button
            style={{ width: 'auto' }}
            variant="secondary"
            onClick={() => {
              !isCopying && closeOverlay();
            }}
          >
            No
          </Button>
          <Button
            disabled={isCopying}
            loading={isCopying}
            onClick={() => {
              onPrimary();
              isSubmitted.current = true;
            }}
          >
            Yes
          </Button>
        </div>
      </BaseModal>
    </Wrapper>
  );
};
