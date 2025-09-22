import { BaseModal, FormGroup } from '#components';
import { CommonOverlayProps } from '#components/OverlayContainer/types';
import { useTypedSelector } from '#store';
import { InputTypes } from '#utils/globalTypes';
import { formatDateTime } from '#utils/timeUtils';
import { ReadOnlyGroup } from '#views/Ontology/ObjectTypes';
import { getUnixTime } from 'date-fns';
import { zonedTimeToUtc } from 'date-fns-tz';
import React, { FC } from 'react';
import { useForm } from 'react-hook-form';
import styled from 'styled-components';

export interface RangeFilterModalProps {
  onSubmit: (data: { expectedStartDate: number; expectedEndDate: number }) => void;
  onCancel: () => void;
}

const Wrapper = styled.div.attrs({})`
  .modal {
    width: 560px !important;

    .modal-body {
      padding: 24px !important;
      overflow: auto;

      .title {
        font-size: 14px;
        font-weight: bold;
        color: #161616;
        margin-bottom: 16px;
      }

      .form-group {
        padding: 0;
        margin-bottom: 16px;

        :last-of-type {
          margin-bottom: 0;
        }
      }

      .scheduler-summary {
        margin-top: 16px;
        border-top: 1px solid #e0e0e0;
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

          span {
            color: #161616;
            font-size: 14px;
            line-height: 1.33;
          }
        }
      }
    }

    .modal-footer {
      flex-direction: row-reverse;
    }
  }
`;

const RangeFilterModal: FC<CommonOverlayProps<RangeFilterModalProps>> = ({
  closeAllOverlays,
  closeOverlay,
  props: { onSubmit, onCancel, defaultStartDate, defaultEndDate },
}) => {
  const timeZone = useTypedSelector((state) => state.auth.selectedFacility?.timeZone);
  const form = useForm({
    mode: 'onChange',
    criteriaMode: 'all',
  });

  const {
    handleSubmit,
    register,
    formState: { isDirty, isValid },
    watch,
  } = form;

  const { expectedStartDate, expectedEndDate } = watch(['expectedStartDate', 'expectedEndDate']);

  const onCloseHandler = () => {
    closeOverlay();
  };

  return (
    <Wrapper>
      <BaseModal
        closeAllModals={closeAllOverlays}
        closeModal={onCloseHandler}
        title="Filter by Start Date & time"
        primaryText="Filter"
        secondaryText="Cancel"
        disabledPrimary={!isValid || !isDirty}
        onPrimary={handleSubmit((data) =>
          onSubmit({
            expectedStartDate: getUnixTime(zonedTimeToUtc(data.expectedStartDate, timeZone!)),
            expectedEndDate: getUnixTime(zonedTimeToUtc(data.expectedEndDate, timeZone!)),
          }),
        )}
        onSecondary={onCancel}
      >
        <div className="title">Start date & time</div>
        <form>
          <FormGroup
            key="basic-info-section"
            inputs={[
              {
                type: InputTypes.DATE_TIME,
                props: {
                  placeholder: 'From',
                  label: 'From',
                  id: 'expectedStartDate',
                  name: 'expectedStartDate',
                  ref: register({ required: true }),
                  ...(defaultStartDate && {
                    defaultValue: defaultStartDate,
                  }),
                },
              },
              {
                type: InputTypes.DATE_TIME,
                props: {
                  placeholder: 'To',
                  label: 'To',
                  id: 'expectedEndDate',
                  name: 'expectedEndDate',
                  ref: register({ required: true }),
                  ...(defaultEndDate && {
                    defaultValue: defaultEndDate,
                  }),
                },
              },
            ]}
          />
        </form>
        {expectedStartDate || expectedEndDate || defaultStartDate || defaultEndDate ? (
          <div className="scheduler-summary">
            <h4>Start date & time</h4>
            <ReadOnlyGroup
              className="read-only-group"
              items={[
                {
                  label: 'From',
                  value: formatDateTime({
                    value:
                      getUnixTime(zonedTimeToUtc(expectedStartDate, timeZone!)) || defaultStartDate,
                  }),
                },
                {
                  label: 'To',
                  value: formatDateTime({
                    value:
                      getUnixTime(zonedTimeToUtc(expectedEndDate, timeZone!)) || defaultEndDate,
                  }),
                },
              ]}
            />
          </div>
        ) : null}
      </BaseModal>
    </Wrapper>
  );
};

export default RangeFilterModal;
