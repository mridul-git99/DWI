import { BaseModal } from '#components';
import { CommonOverlayProps } from '#components/OverlayContainer/types';
import React, { FC } from 'react';
import styled from 'styled-components';

export interface RecurrenceExecutionModalProps {
  onPrimary: () => void;
  onSecondary: () => void;
}

const Wrapper = styled.div.attrs({})`
  .modal {
    min-width: 600px !important;
    max-width: 600px !important;

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

const RecurrenceExecutionModal: FC<CommonOverlayProps<RecurrenceExecutionModalProps>> = ({
  closeAllOverlays,
  closeOverlay,
  props: { onPrimary, onSecondary },
}) => {
  return (
    <Wrapper>
      <BaseModal
        closeAllModals={closeAllOverlays}
        title="Task Recurrence"
        primaryText="Create a Recurring Task"
        secondaryText="End Recurrence"
        closeModal={closeOverlay}
        onPrimary={onPrimary}
        onSecondary={onSecondary}
      >
        <div className="task-recurrence">
          This completed task has “Recurrence enabled”, which means a repeated version of the task
          can be created.
          <br /> <br />
          Do you want to create a recurring task or do you want to end the recurrence ?
          <br /> <br />
          Please note, the task will be executed with the same resource parameters as used in the
          master task.
        </div>
      </BaseModal>
    </Wrapper>
  );
};

export default RecurrenceExecutionModal;
