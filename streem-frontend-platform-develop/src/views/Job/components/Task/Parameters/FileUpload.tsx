import { TaskMediasWrapper } from '#PrototypeComposer/Tasks/styles';
import { FileGallery, FileGalleryProps, ImageUploadButton } from '#components';
import { openOverlayAction } from '#components/OverlayContainer/actions';
import { OverlayNames } from '#components/OverlayContainer/types';
import { FileUploadData } from '#utils/globalTypes';
import { jobActions } from '#views/Job/jobStore';
import { LinearProgress } from '@material-ui/core';
import { PublishOutlined } from '@material-ui/icons';
import { isEqual } from 'lodash';
import React, { FC, useEffect, useState } from 'react';
import { useDispatch } from 'react-redux';
import styled from 'styled-components';
import { ParameterProps } from './Parameter';
import { Media } from '#PrototypeComposer/checklist.types';

const FileUploadMedias: FC<FileGalleryProps> = ({
  medias,
  parameter,
  setUploadedMedia,
  isCorrectingError,
  isTaskCompleted,
  setCorrectedParameterValues,
}) => {
  return (
    <TaskMediasWrapper>
      <div className="container">
        <FileGallery
          medias={medias}
          parameter={parameter}
          setUploadedMedia={setUploadedMedia}
          isCorrectingError={isCorrectingError}
          isTaskCompleted={isTaskCompleted}
          setCorrectedParameterValues={setCorrectedParameterValues}
        />
      </div>
    </TaskMediasWrapper>
  );
};

const FileUploadWrapper = styled.div.attrs({
  className: 'file-upload-parameter',
})`
  display: flex;
  flex-direction: column;

  .card {
    align-items: center;
    background-color: #ffffff;
    border: 1px solid #e0e0e0;
    display: flex;
    flex: 1;
    flex-direction: column;
    justify-content: center;
    margin-right: 24px;
    padding: 24px;

    :last-child {
      margin-right: 0;
    }

    .icon {
      font-size: 48px;
      cursor: not-allowed;
    }

    span {
      color: #1d84ff;
      font-size: 14px;
      margin-top: 8px;
    }

    .upload-image {
      > div {
        display: flex;
        flex-direction: column;
      }

      .icon {
        cursor: pointer;
      }
    }
  }
`;

const FileUploadParameter: FC<ParameterProps> = ({
  parameter,
  isCorrectingError,
  isTaskCompleted,
  setCorrectedParameterValues,
}) => {
  const dispatch = useDispatch();
  const [isUploading, setIsUploading] = useState(false);
  const [uploadedMedia, setUploadedMedia] = useState<Media[]>([]);

  const onUploaded = (fileData: FileUploadData) => {
    dispatch(
      openOverlayAction({
        type: OverlayNames.TASK_MEDIA,
        props: {
          mediaDetails: {
            ...fileData,
            name: '',
            description: '',
          },
          isParameter: true,
          isExecution: true,
          execute: (data: Media) => {
            if (isCorrectingError) {
              if (setCorrectedParameterValues) {
                setCorrectedParameterValues((prev) => ({
                  ...prev,
                  medias: [...prev.medias, { ...data, id: data.mediaId }],
                }));
                setUploadedMedia((prev) => [...prev, { ...data, id: data.mediaId }]);
              }
            } else {
              dispatch(
                jobActions.executeParameter({
                  parameter: {
                    ...parameter,
                    data: {
                      medias: [...(parameter.data?.medias ?? []), { ...data }],
                    },
                  },
                }),
              );
            }
          },
        },
      }),
    );
    setIsUploading(false);
  };

  const isDisabled = isCorrectingError && !setCorrectedParameterValues;

  useEffect(() => {
    if (!isEqual(parameter?.response?.medias, uploadedMedia)) {
      setUploadedMedia(parameter.response!.medias!);
    }
  }, [parameter?.response?.medias]);

  return (
    <FileUploadWrapper>
      {uploadedMedia.length > 0 && (
        <FileUploadMedias
          medias={uploadedMedia}
          parameter={parameter}
          setUploadedMedia={setUploadedMedia}
          isCorrectingError={isCorrectingError}
          isTaskCompleted={isTaskCompleted}
          setCorrectedParameterValues={setCorrectedParameterValues}
        />
      )}
      {(!isTaskCompleted || isCorrectingError) && (
        <div style={{ display: 'flex' }}>
          {isUploading ? (
            <LinearProgress style={{ height: 8, width: '100%', color: '#1d84ff' }} />
          ) : (
            <>
              <div className="card">
                <ImageUploadButton
                  label="Click Here To Upload Files"
                  onUploadStart={() => {
                    setIsUploading(true);
                  }}
                  onUploadSuccess={(fileData) => {
                    onUploaded(fileData);
                  }}
                  icon={PublishOutlined}
                  onUploadError={() => {
                    setIsUploading(false);
                  }}
                  disabled={isDisabled}
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
                    '.zip',
                  ]}
                  showErrorNotification={true}
                />
              </div>
            </>
          )}
        </div>
      )}
    </FileUploadWrapper>
  );
};

export default FileUploadParameter;
