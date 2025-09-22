import { BaseModal, ImageUploadButton, Textarea } from '#components';
import { CommonOverlayProps } from '#components/OverlayContainer/types';
import { ExceptionValues } from '#types';
import {
  Add,
  DeleteOutlined,
  Error as ErrorIcon,
  RadioButtonChecked,
  RadioButtonUnchecked,
} from '@material-ui/icons';
import { debounce } from 'lodash';
import React, { FC, useState } from 'react';
import { useDispatch } from 'react-redux';
import styled from 'styled-components';
import { jobActions } from '../jobStore';

const Wrapper = styled.div`
  .modal-body {
    max-height: 500px;
    overflow: auto;
  }

  .reason-error {
    align-items: center;
    background-color: rgba(255, 107, 107, 0.16);
    border: solid 1px #ff6b6b;
    border-radius: 4px;
    color: #ff6b6b;
    display: flex;
    font-size: 12px;
    justify-content: center;
    padding: 4px 0;

    .icon {
      color: #ff6b6b;
      font-size: 16px;
      margin-right: 5px;
    }
  }

  .details {
    align-items: flex-start;
    display: flex;
    flex-direction: column;
    margin-bottom: 24px;

    span {
      color: #666666;
      font-size: 16px;
      line-height: 1;

      :first-child {
        margin-bottom: 16px;
      }

      :last-child {
        color: #333333;
        font-weight: 600;
      }
    }
  }

  .reason {
    &-container {
      label {
        color: #666666;
        display: flex;
        font-size: 16px;
        margin-bottom: 8px;
      }
    }

    &-list {
      display: flex;
      flex-wrap: wrap;
    }

    &-item {
      align-content: center;
      box-sizing: bordr-box;
      color: #666666;
      cursor: pointer;
      display: flex;
      flex: 0 50%;
      font-size: 16px;
      margin-bottom: 8px;

      > .icon {
        margin-right: 5px;

        &.selected {
          color: #1d84ff;
        }
      }

      span {
        align-items: center;
        display: flex;
      }
    }
  }

  .textarea {
    margin-top: 8px;
  }

  .media-upload {
    display: flex;
    flex-direction: column;
    margin-top: 24px;

    .header {
      align-items: center;
      display: flex;
      justify-content: space-between;

      .upload-image {
        > div {
          color: #1d84ff;

          .icon {
            color: #1d84ff;
          }
        }
      }
    }

    .body {
      counter-reset: item;
      list-style-type: none;
      padding: 0;

      .item {
        display: flex;
        align-items: center;
        list-style-position: inside;

        :before {
          color: #000000;
          content: counter(item) ' ';
          counter-increment: item;
          font-size: 14px;
          margin-right: 12px;
        }

        .remove {
          align-items: center;
          color: #ff6b6b;
          display: flex;
          margin-left: auto;
          cursor: pointer;

          .icon {
            color: #ff6b6b;
            margin-right: 5px;
          }
        }
      }
    }
  }
`;

export const ExceptionReason = [
  { label: 'Job got cancelled', value: 'JOB_GOT_CANCELLED' },
  { label: 'Job created by mistake', value: 'JOB_CREATED_BY_MISTAKE' },
  { label: 'Job completed offline', value: 'JOB_COMPLETED_OFFLINE' },
  { label: 'Other', value: 'OTHER' },
];

const CompleteJobWithExceptionModal: FC<CommonOverlayProps<any>> = ({
  closeAllOverlays,
  closeOverlay,
  props: { jobId, name, code },
}) => {
  const dispatch = useDispatch();

  const [values, setValues] = useState<ExceptionValues>({
    comment: '',
    medias: [],
    reason: '',
  });

  const [errors, setErrors] = useState<{ reason: boolean; comment: boolean }>({
    reason: false,
    comment: false,
  });

  const validateAndSubmit = () => {
    if (!values.comment || !values.reason) {
      setErrors((prevErrors) => ({
        ...prevErrors,
        reason: !values.reason,
        comment: !values.comment,
      }));
    } else {
      dispatch(
        jobActions.completeJob({
          jobId,
          withException: true,
          values,
          details: { name, code },
        }),
      );
    }
  };

  return (
    <Wrapper>
      <BaseModal
        animated={false}
        closeAllModals={closeAllOverlays}
        closeModal={closeOverlay}
        onPrimary={() => validateAndSubmit()}
        onSecondary={() => closeOverlay()}
        primaryText="Complete Job"
        secondaryText="Go Back"
        title="Completing a Job With Exceptions"
      >
        {errors.reason ? (
          <div className="reason-error">
            <ErrorIcon className="icon" />
            Reason not selected for Completeing the Job with Exception
          </div>
        ) : null}
        <div className="details">
          <span>You’re about to complete the following job with exceptions:</span>
          <span>
            {code} {name}
          </span>
        </div>

        <div className="reason-container">
          <label>Please select a reason below:</label>

          <div className="reason-list">
            {ExceptionReason.map((reason, idx) => (
              <div
                className="reason-item"
                key={idx}
                onClick={() => {
                  setValues((val) => ({ ...val, reason: reason.value }));
                  setErrors((prrevErrors) => ({
                    ...prrevErrors,
                    reason: false,
                  }));
                }}
              >
                {values.reason === reason.value ? (
                  <RadioButtonChecked className="icon selected" />
                ) : (
                  <RadioButtonUnchecked className="icon" />
                )}
                <span>{reason.label}</span>
              </div>
            ))}
          </div>
        </div>

        <Textarea
          defaultValue={values.comment}
          error={
            errors.comment ? 'You Need to provide additional Remarks before submitting' : false
          }
          label="Additional Remarks"
          onChange={debounce(({ value }) => {
            setValues((val) => ({ ...val, comment: value }));
            setErrors((prrevErrors) => ({
              ...prrevErrors,
              comment: false,
            }));
          }, 500)}
          rows={3}
        />

        <div className="media-upload">
          <div className="header">
            <label>Document Upload</label>
            <ImageUploadButton
              icon={Add}
              label="Add New File"
              onUploadError={(error) => console.error('error on media upload :: ', error)}
              onUploadSuccess={(file) => {
                setValues((val) => ({ ...val, medias: [...val.medias, file] }));
              }}
              acceptedTypes={['image/*', '.pdf']}
            />
          </div>

          <ol className="body">
            {values.medias.map((media, index) => (
              <li className="item" key={index}>
                <div>{media.originalFilename}</div>
                <div
                  className="remove"
                  onClick={() => {
                    setValues((val) => {
                      const medias = val.medias.filter((m) => m.mediaId !== media.mediaId);
                      return { ...val, medias };
                    });
                  }}
                >
                  <DeleteOutlined className="icon" />
                  Delete
                </div>
              </li>
            ))}
          </ol>
        </div>
      </BaseModal>
    </Wrapper>
  );
};

export default CompleteJobWithExceptionModal;
