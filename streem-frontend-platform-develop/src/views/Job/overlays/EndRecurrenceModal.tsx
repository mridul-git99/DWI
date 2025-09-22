import { BaseModal } from '#components';
import { CommonOverlayProps } from '#components/OverlayContainer/types';
import React, { FC } from 'react';
import styled from 'styled-components';

export interface EndRecurrenceModalProps {
  onPrimary: () => void;
}

const Wrapper = styled.div.attrs({})`
  .modal {
    min-width: 406px !important;
    max-width: 406px !important;

    .modal-body {
      padding: 24px !important;
      overflow: auto;

      .task-recurrence {
        color: #525252;
        font-size: 14px;
        line-height: 16px;
      }
    }

    .modal-footer {
      flex-direction: row-reverse !important;
    }
  }
`;

const EndRecurrenceModal: FC<CommonOverlayProps<EndRecurrenceModalProps>> = ({
  closeAllOverlays,
  closeOverlay,
  props: { onPrimary },
}) => {
  return (
    <Wrapper>
      <BaseModal
        closeAllModals={closeAllOverlays}
        title="End Recurrence"
        primaryText="Confirm"
        secondaryText="Cancel"
        closeModal={closeOverlay}
        onPrimary={onPrimary}
      >
        <div className="task-recurrence">
          Ending the recurrence will stop the automatic creation of new instances of this task. Are
          you sure you want to proceed?
        </div>
      </BaseModal>
    </Wrapper>
  );
};

export default EndRecurrenceModal;
