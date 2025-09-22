import EditingDisabledIcon from '#assets/svg/EditingDisabledIcon';
import { BaseModal } from '#components';
import { CommonOverlayProps } from '#components/OverlayContainer/types';
import React, { FC } from 'react';
import styled from 'styled-components';

import { DisabledStates, AllChecklistStates, EnabledStates } from '../checklist.types';

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

    .text {
      color: #000000;
      font-size: 20px;
      font-weight: bold;
      margin-top: 24px;
    }
  }
`;

const EditingDisabledModal: FC<
  CommonOverlayProps<{
    state: AllChecklistStates;
    archived: boolean;
  }>
> = ({ closeAllOverlays, closeOverlay, props: { state, archived } = {} }) => (
  <Wrapper>
    <BaseModal
      closeAllModals={closeAllOverlays}
      closeModal={closeOverlay}
      showHeader={false}
      showFooter={false}
    >
      <div className="body">
        <EditingDisabledIcon className="editing-disabled-icon" />
        <div className="text">
          {(() => {
            if (archived) {
              return "Prototype has been archived and can't be edited now";
            } else {
              switch (state) {
                case DisabledStates.SUBMITTED_FOR_REVIEW:
                case DisabledStates.BEING_REVIEWED:
                  return 'No edits can be done as the prototype is being reviewed.';
                case DisabledStates.READY_FOR_SIGNING:
                case DisabledStates.SIGN_OFF_INITIATED:
                case DisabledStates.SIGNING_IN_PROGRESS:
                  return 'No edits can be done as the prototype is under signing now';
                case DisabledStates.READY_FOR_RELEASE:
                  return 'No edits can be done as the prototype is now ready for release';
                case EnabledStates.REQUESTED_CHANGES:
                  return 'No edits can be done as the Prototype is with the Author for changes.';
                case EnabledStates.BEING_BUILT:
                  return 'You are not authorised to edit this Process.';
                case DisabledStates.PUBLISHED:
                  return 'You can start a revision of this Process to make changes';
                case DisabledStates.DEPRECATED:
                  return 'Cannot edit this prototype as it is deprecated';
                default:
                  return '';
              }
            }
          })()}
        </div>
      </div>
    </BaseModal>
  </Wrapper>
);

export default EditingDisabledModal;
