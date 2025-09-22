import { BaseModal, Button, FormGroup, Textarea } from '#components';
import { CommonOverlayProps } from '#components/OverlayContainer/types';
import { TaskPauseReasons } from '#types';
import { InputTypes } from '#utils/globalTypes';
import { debounce } from 'lodash';
import React, { useState } from 'react';
import styled from 'styled-components';

const Wrapper = styled.div`
  #modal-container .modal-background .modal {
    min-width: 450px;
  }
  .modal {
    .close-icon {
      color: #e0e0e0 !important;
      font-size: 16px !important;
      top: 19px !important;
    }

    .modal-header {
      border-bottom: 1px solid #f4f4f4 !important;
      h2 {
        color: #161616 !important;
        font-weight: bold !important;
        font-size: 14px !important;
      }
    }
    .modal-body {
      padding: 0px !important;

      .reason-modal-form-body {
        padding: 16px 16px 16px 26px;

        .reason-modal-desc {
          color: #525252;
          font-size: 12px;
          text-align: start;
          margin-bottom: 8px;
        }

        .reason-list {
          display: flex;
          flex-direction: column;
          gap: 10px;
          .form-group {
            padding: unset;
            #pauseReason > label {
              span:last-child {
                color: #161616;
              }
            }
          }
        }
      }

      .reason-modal-footer {
        border-top: 1px solid #f4f4f4 !important;
        display: flex;
        align-items: center;
        padding: 16px;
        justify-content: flex-end;
      }
    }
  }
`;

type Props = {
  onSubmitHandler: (reason: string, comment: string) => void;
  modalTitle?: string;
  onSubmitModalText?: string;
};

const TaskPauseReasonModal = (props: CommonOverlayProps<Props>) => {
  const {
    props: { onSubmitHandler, modalTitle, onSubmitModalText },
    closeOverlay,
    closeAllOverlays,
  } = props;
  const [reasonTextIsEmpty, setReasonTextIsEmpty] = useState<boolean>(true);
  const [reason, setReason] = useState<string>('');
  const [comment, setComment] = useState<string>('');

  const onSubmitModal = async () => {
    if (!reasonTextIsEmpty) {
      await onSubmitHandler(reason, comment);
      closeOverlay();
    }
  };

  return (
    <Wrapper>
      <BaseModal
        onSecondary={closeOverlay}
        closeModal={closeOverlay}
        closeAllModals={closeAllOverlays}
        title={modalTitle ? modalTitle : 'Pause a Task'}
        disabledPrimary={reasonTextIsEmpty}
        showFooter={false}
      >
        <div className="reason-modal-form-body">
          <div className="reason-modal-desc">Select a reason to Pause task</div>
          <div className="reason-list">
            <FormGroup
              inputs={[
                {
                  type: InputTypes.RADIO,
                  props: {
                    groupProps: {
                      id: 'pauseReason',
                      name: 'pauseRea',
                      onChange: (e) => {
                        setReason(e.target.value);
                        if (e.target.value === 'OTHER') {
                          setReasonTextIsEmpty(true);
                        } else {
                          setReasonTextIsEmpty(false);
                        }
                      },
                    },
                    items: Object.entries(TaskPauseReasons).map((reason, index) => ({
                      key: index,
                      label: reason[1],
                      value: reason[0],
                    })),
                  },
                },
              ]}
            />
          </div>

          {reason === 'OTHER' && (
            <Textarea
              placeholder="Write here"
              rows={4}
              maxRows={8}
              onChange={debounce((e) => {
                if (e.value.trim() !== '') {
                  setComment(e.value);
                  setReasonTextIsEmpty(false);
                } else {
                  setReasonTextIsEmpty(true);
                }
              })}
            />
          )}
        </div>
        <div className="reason-modal-footer">
          <Button variant="secondary" onClick={() => closeOverlay()}>
            Cancel
          </Button>
          <Button color="red" onClick={onSubmitModal} disabled={reasonTextIsEmpty}>
            {onSubmitModalText ? onSubmitModalText : 'Submit'}
          </Button>
        </div>
      </BaseModal>
    </Wrapper>
  );
};

export default TaskPauseReasonModal;
