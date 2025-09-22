import { BaseModal, StyledTabs } from '#components';
import { CommonOverlayProps } from '#components/OverlayContainer/types';
import React from 'react';
import styled from 'styled-components';
import AutomationInfo from '../components/Task/AutomationInfo';

const Wrapper = styled.div`
  .modal {
    width: 70%;

    .close-icon {
      color: #e0e0e0 !important;
      font-size: 16px !important;
    }

    .modal-header {
      border-bottom: 1px solid #f4f4f4 !important;
      h2 {
        color: #161616 !important;
        font-weight: bold !important;
        font-size: 14px !important;
      }
    }
  }
`;

type Props = {
  taskId: any;
  initialTab?: number;
  taskExecutionId?: any;
};

const AutomationTaskModal = (props: CommonOverlayProps<Props>) => {
  const { closeOverlay, closeAllOverlays } = props;

  return (
    <Wrapper>
      <BaseModal
        onSecondary={closeOverlay}
        closeModal={closeOverlay}
        closeAllModals={closeAllOverlays}
        title={'Automations'}
        showFooter={false}
      >
        <div className="automation-modal-form-body">
          <StyledTabs
            tabs={[
              {
                value: '0',
                label: 'Automations configured',
                panelContent: <AutomationInfo />,
              },
            ]}
            activeTab={'0'}
          />
        </div>
      </BaseModal>
    </Wrapper>
  );
};

export default AutomationTaskModal;
