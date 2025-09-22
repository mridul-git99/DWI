import { BaseModal, FormGroup } from '#components';
import { CommonOverlayProps } from '#components/OverlayContainer/types';
import { InputTypes } from '#utils/globalTypes';
import React, { FC } from 'react';
import { useForm } from 'react-hook-form';
import { useDispatch } from 'react-redux';
import styled from 'styled-components';
import { updateJob } from '../ListView/actions';

export interface SetDateModalProps {
  jobId: string;
}

const Wrapper = styled.div.attrs({})`
  .modal {
    min-width: 700px !important;
    max-width: 720px !important;
    max-height: 80dvh;

    .modal-body {
      padding: 24px !important;
      overflow: auto;

      .custom-label {
        align-items: center;
        color: #525252;
        display: flex;
        font-size: 12px;
        justify-content: flex-start;
        letter-spacing: 0.32px;
        line-height: 1.33;
        margin: 0px;
        margin-bottom: 8px;
      }

      .form-group {
        padding: 0;
        margin-bottom: 16px;

        :last-of-type {
          margin-bottom: 0;
        }
      }

      .due-after-section {
        display: flex;
        margin-bottom: 16px;
        .form-group {
          flex-direction: row;
          gap: 0.8%;
          width: 100%;
          > div {
            margin-bottom: 0;
            width: 16%;
            input {
              width: calc(100% - 32px);
            }
          }
        }
      }

      .scheduler-summary {
        border-top: 1.5px solid #e0e0e0;
        h4 {
          font-size: 14px;
          font-weight: bold;
          line-height: 1.14;
          letter-spacing: 0.16px;
          color: #161616;
          margin-block: 16px;
        }
        .read-only-group {
          padding: 0;
          .read-only {
            margin-bottom: 16px;
          }
        }
      }
    }

    .modal-footer {
      flex-direction: row-reverse;
    }
  }
`;

const SetDateModal: FC<CommonOverlayProps<SetDateModalProps>> = ({
  closeAllOverlays,
  closeOverlay,
  props: { jobId },
}) => {
  const dispatch = useDispatch();
  const form = useForm({
    mode: 'onChange',
    criteriaMode: 'all',
  });

  const {
    handleSubmit,
    register,
    formState: { isDirty, isValid },
    setValue,
  } = form;

  const validateEpoch = (value: number) => {
    if (isNaN(value)) {
      return false;
    }
    return true;
  };

  register('expectedStartDate', {
    required: true,
    validate: validateEpoch,
  });
  register('expectedEndDate', {
    required: true,
    validate: validateEpoch,
  });

  const onSubmit = (data: any) => {
    dispatch(
      updateJob({
        job: {
          id: jobId,
          ...data,
        },
      }),
    );
  };

  const onCloseHandler = () => {
    closeOverlay();
  };

  return (
    <Wrapper>
      <BaseModal
        closeAllModals={closeAllOverlays}
        closeModal={onCloseHandler}
        title="Schedule"
        primaryText="Save"
        secondaryText="Cancel"
        disabledPrimary={!isValid || !isDirty}
        onPrimary={handleSubmit((data) => onSubmit(data))}
      >
        <form>
          <FormGroup
            key="basic-info-section"
            inputs={[
              {
                type: InputTypes.DATE_TIME,
                props: {
                  placeholder: 'Start Date & Time',
                  label: 'Start Date & Time',
                  id: 'expectedStartDate',
                  name: 'expectedStartDate',
                  onChange: ({ value }: { value: string }) => {
                    setValue('expectedStartDate', Number(value), {
                      shouldValidate: true,
                      shouldDirty: true,
                    });
                  },
                },
              },
              {
                type: InputTypes.DATE_TIME,
                props: {
                  placeholder: 'End Date & Time',
                  label: 'End Date & Time',
                  id: 'expectedEndDate',
                  name: 'expectedEndDate',
                  onChange: ({ value }: { value: string }) => {
                    setValue('expectedEndDate', Number(value), {
                      shouldValidate: true,
                      shouldDirty: true,
                    });
                  },
                },
              },
            ]}
          />
        </form>
      </BaseModal>
    </Wrapper>
  );
};

export default SetDateModal;
