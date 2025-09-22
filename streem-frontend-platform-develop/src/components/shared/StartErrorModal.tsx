import CannotArchive from '#assets/svg/CannotArchive';
import { BaseModal } from '#components';
import { CommonOverlayProps } from '#components/OverlayContainer/types';
import { ComposerEntity } from '#PrototypeComposer/types';
import React, { FC } from 'react';
import styled from 'styled-components';

const Wrapper = styled.div`
  .modal {
    min-width: 420px !important;
    width: 420px !important;
  }

  .modal-body {
    padding: 0 !important;
  }

  .close-icon {
    top: 24px !important;
    right: 32px !important;
  }

  .body {
    padding: 80px;

    .editing-disabled-icon {
      font-size: 120px;
    }

    .text1 {
      color: #000000;
      font-size: 20px;
      font-weight: bold;
      margin-top: 16px;
    }

    .text2 {
      color: #666666;
      font-size: 14px;
      font-weight: normal;
      margin-top: 8px;
    }
  }
`;

const StartErrorModal: FC<CommonOverlayProps<{ entity: ComposerEntity }>> = ({
  props: { entity },
  closeAllOverlays,
  closeOverlay,
}) => (
  <Wrapper>
    <BaseModal
      closeAllModals={closeAllOverlays}
      closeModal={closeOverlay}
      showHeader={false}
      showFooter={false}
    >
      <div className="body">
        <CannotArchive fontSize="160px" />
        <div className="text1">{`Can't start ${
          entity === ComposerEntity.CHECKLIST ? 'Prototype' : 'Job'
        }`}</div>
        <div className="text2">
          {`Can't start a ${
            entity === ComposerEntity.CHECKLIST ? 'Prototype' : 'Job'
          } when in 'All Facility' view. Select a Facility
          first.`}
        </div>
      </div>
    </BaseModal>
  </Wrapper>
);

export default StartErrorModal;
