import { Media } from '#PrototypeComposer/checklist.types';
import AddCircleOutlineIcon from '@material-ui/icons/AddCircleOutline';
import closeIcon from '#assets/svg/close.svg';
import { BaseModal, CustomTag, ImageUploadButton, Textarea } from '#components';
import { showNotification } from '#components/Notification/actions';
import { NotificationType } from '#components/Notification/types';
import { CommonOverlayProps } from '#components/OverlayContainer/types';
import { openLinkInNewTab } from '#utils';
import { apiPostPatchJobAnnotation } from '#utils/apiUrls';
import { nonEmptyStringRegex } from '#utils/constants';
import { getErrorMsg, request } from '#utils/request';
import React, { useState, Dispatch, SetStateAction } from 'react';
import { useForm } from 'react-hook-form';
import { useDispatch } from 'react-redux';
import styled from 'styled-components';
import { LinearProgress, makeStyles } from '@material-ui/core';

const Wrapper = styled.div`
  .modal {
    .modal-body {
      width: 560px;
      color: #525252;
      display: flex;
      flex-direction: column;
      gap: 16px;

      .remark-media-section {
        display: flex;
        flex-direction: column;
        gap: 8px;
      }

      .upload-image {
        color: #1d84ff;
        border: 1px solid #1d84ff;
        width: max-content;
        padding: 6px 16px;
      }

      .media-list-item {
        display: flex;
        justify-content: space-between;
        cursor: pointer;
        align-items: center;
        color: #1d84ff;
        font-size: 14px;
      }
    }

    .modal-footer {
      flex-direction: row-reverse !important;
    }
  }
`;

type Props = {
  jobId: string;
  setRefetchAnnotations: Dispatch<SetStateAction<boolean>>;
};

const useStyles = makeStyles({
  colorPrimary: {
    backgroundColor: '#d3d3d3',
  },
  barColorPrimary: {
    backgroundColor: '#1d84ff',
  },
});

const AddRemark = (props: CommonOverlayProps<Props>) => {
  const classes = useStyles();
  const {
    props: { jobId, setRefetchAnnotations },
    closeOverlay,
    closeAllOverlays,
  } = props;
  const [mediaData, setMediaData] = useState<Media[]>([]);
  const [isFileUploading, setIsFileUploading] = useState<boolean>(false);
  const { register, formState, watch } = useForm<any>({
    mode: 'onChange',
    criteriaMode: 'all',
    defaultValues: { remarks: '' },
  });
  const { remarks } = watch(['remarks']);
  const { isDirty, isValid } = formState;

  const dispatch = useDispatch();

  const onSubmitModal = async () => {
    const payload = {
      jobId: jobId,
      remarks: remarks.trim(),
      ids: mediaData.map((el: Media) => el.mediaId || el.id),
    };

    const { data, errors } = await request('POST', apiPostPatchJobAnnotation(), {
      data: payload,
    });

    if (data) {
      dispatch(
        showNotification({
          type: NotificationType.SUCCESS,
          msg: `${data?.code || 'Annotation'} added successfully!`,
        }),
      );
      closeOverlay();
      setRefetchAnnotations((prev: boolean) => !prev);
    } else if (errors) {
      dispatch(
        showNotification({
          type: NotificationType.ERROR,
          msg: getErrorMsg(errors),
        }),
      );
    }
  };

  const onUploadedFile = (fileData: Media) => {
    setMediaData((prev) => [...prev, fileData]);
    setIsFileUploading(false);
  };

  const onDeleteFile = (index: number) => {
    setMediaData((prev) => {
      const arr = [...prev];
      arr.splice(index, 1);
      return arr;
    });
  };

  return (
    <Wrapper>
      <BaseModal
        closeModal={closeOverlay}
        closeAllModals={closeAllOverlays}
        title={'Add Annotations'}
        primaryText="Submit"
        onPrimary={onSubmitModal}
        disabledPrimary={!isValid || !isDirty || isFileUploading}
        secondaryText="Close"
        onSecondary={closeOverlay}
      >
        <>
          <Textarea
            placeholder="Write here"
            rows={4}
            maxRows={8}
            ref={register({
              required: true,
              pattern: nonEmptyStringRegex,
            })}
            name="remarks"
            label="Remarks"
          />

          <div className="remark-media-section">
            {mediaData.length > 0 &&
              mediaData.map((media: Media, index: number) => {
                if (media?.archived === false) {
                  return (
                    <div className="media-list-item" key={index}>
                      <CustomTag
                        as={'div'}
                        onClick={() => openLinkInNewTab(`/media?link=${media.link}`)}
                      >
                        <span>{media?.originalFilename}</span>
                      </CustomTag>
                      <img
                        src={closeIcon}
                        onClick={() => {
                          onDeleteFile(index);
                        }}
                      />
                    </div>
                  );
                } else {
                  return null;
                }
              })}

            {isFileUploading && (
              <LinearProgress
                classes={{
                  colorPrimary: classes.colorPrimary,
                  barColorPrimary: classes.barColorPrimary,
                }}
                style={{ height: 4, width: '100%' }}
              />
            )}

            <div
              style={{
                fontSize: '12px',
              }}
            >
              Upload relevant document (optional)
            </div>

            <ImageUploadButton
              label="Attach documents"
              onUploadStart={() => {
                setIsFileUploading(true);
              }}
              onUploadSuccess={(fileData) => {
                onUploadedFile(fileData);
              }}
              icon={() => {
                return <AddCircleOutlineIcon fontSize="small" style={{ marginRight: '8px' }} />;
              }}
              onUploadError={() => {
                setIsFileUploading(false);
              }}
              acceptedTypes={[
                'image/*',
                '.pdf',
                '.doc',
                '.docx',
                '.png',
                '.jpg',
                '.jpeg',
                '.xlsx',
                '.xls',
                '.ppt',
                '.pptx',
                '.csv',
              ]}
              showErrorNotification={true}
            />
          </div>
        </>
      </BaseModal>
    </Wrapper>
  );
};

export default AddRemark;
