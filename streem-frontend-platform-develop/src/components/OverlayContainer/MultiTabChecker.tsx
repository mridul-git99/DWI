import { BaseModal } from '#components';
import { triggerPersist } from '#views/Auth/actions';
import React, { FC, useEffect, useRef } from 'react';
import { useDispatch } from 'react-redux';
import styled from 'styled-components';
import { closeOverlayAction, openOverlayAction } from './actions';
import { CommonOverlayProps, OverlayNames } from './types';

const Wrapper = styled.div`
  #modal-container {
    z-index: 1501 !important;
  }
  .modal-background {
    background: #787878 !important;

    .escape-overlay {
      display: none;
    }
    .modal {
      width: 400px;

      .close-icon {
        display: none;
      }

      &-header {
        border-bottom: none !important;

        h2 {
          font-size: 14px !important;
          line-height: 18px !important;
          font-weight: 600 !important;
          margin-bottom: 20px !important;
        }
      }

      &-body {
        padding: 0 !important;
      }

      &-footer {
        flex-direction: row-reverse;
      }
    }
  }
`;

type Props = {
  onUseHere: () => void;
  onClose: () => void;
};

export const MultiTabModal: FC<CommonOverlayProps<Props>> = ({
  closeAllOverlays,
  closeOverlay,
  props: { onUseHere, onClose },
}) => {
  return (
    <Wrapper>
      <BaseModal
        closeAllModals={closeAllOverlays}
        closeModal={closeOverlay}
        title={`Leucine is open in another window. Click "Use Here" to use Leucine in this window.`}
        primaryText="Use Here"
        secondaryText="Close"
        onPrimary={onUseHere}
        onSecondary={onClose}
      >
        <div />
      </BaseModal>
    </Wrapper>
  );
};

export const MultiTabChecker: FC = () => {
  const dispatch = useDispatch();
  const isModalOpen = useRef(false);
  const bc = useRef<BroadcastChannel | null>(null);

  const onUseHere = () => {
    if (bc.current) {
      isModalOpen.current = false;
      bc.current.postMessage('CLOSE_WINDOW');
      dispatch(triggerPersist());
      dispatch(closeOverlayAction(OverlayNames.MULTI_TAB_MODAL));
    }
  };

  const onClose = () => {
    if (bc.current) {
      bc.current.close();
    }
    window.location.replace('https://leucine.io');
  };

  const onAnotherOpen = () => {
    if (!isModalOpen.current) {
      isModalOpen.current = true;
      dispatch(
        openOverlayAction({
          type: OverlayNames.MULTI_TAB_MODAL,
          props: {
            onUseHere,
            onClose,
          },
        }),
      );
    }
  };

  useEffect(() => {
    if (!bc.current) {
      bc.current = new BroadcastChannel(window.location.hostname);
      bc.current.onmessage = (event) => {
        if (event.data === 'ON_OPEN') {
          onAnotherOpen();
        }
        if (event.data === 'CLOSE_WINDOW') {
          onClose();
        }
      };

      bc.current.postMessage('ON_OPEN');
    }
    return () => {
      if (bc.current) {
        bc.current.close();
      }
    };
  }, []);

  return null;
};
