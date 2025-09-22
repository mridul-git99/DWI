import TaskMedias from '#PrototypeComposer/Tasks/TaskMedias';
import { Media } from '#PrototypeComposer/checklist.types';
import { ImageUploadButton } from '#components';
import { openOverlayAction } from '#components/OverlayContainer/actions';
import { OverlayNames } from '#components/OverlayContainer/types';
import { useTypedSelector } from '#store';
import { FileUploadData } from '#utils/globalTypes';
import { getVideoDevices } from '#utils/inputUtils';
import { jobActions } from '#views/Job/jobStore';
import { LinearProgress } from '@material-ui/core';
import { PhotoCamera } from '@material-ui/icons';
import React, { FC, useEffect, useState } from 'react';
import { useDispatch } from 'react-redux';
import styled from 'styled-components';
import { ParameterProps } from './Parameter';

const ImageCaptureWrapper = styled.div.attrs({
  className: 'parameter-media',
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

const filterMedia = (parameterMediaData: Media[], newMediaData: Media[]) => {
  const parameterMediaMap = new Map(parameterMediaData.map((media) => [media.id, media]));
  const result = [];
  //on correction we should send new medias only and if archived that data with flag true.

  for (const newMedia of newMediaData) {
    const matchingMedia = parameterMediaMap.get(newMedia.mediaId);
    if (matchingMedia && newMedia.archived) {
      result.push({ ...newMedia, id: newMedia.mediaId });
    } else if (!matchingMedia) {
      result.push({ ...newMedia, id: newMedia.mediaId });
    }
  }
  return result;
};

const ImageCapture: FC<ParameterProps> = ({
  parameter,
  isCorrectingError,
  isTaskCompleted,
  isLoggedInUserAssigned,
  setCorrectedParameterValues,
}) => {
  const dispatch = useDispatch();
  const { activeTask } = useTypedSelector((state) => state.job);
  const taskExecutionId = activeTask?.taskExecution.id;
  const [isUploading, setIsUploading] = useState(false);
  const [videoDevices, setVideoDevices] = useState<MediaDeviceInfo[]>([]);
  const [uploadedMedia, setUploadedMedia] = useState<Media[]>([]);

  const onExecute = (data: any, isDeleting?: boolean) => {
    if (isCorrectingError) {
      if (isDeleting) {
        if (setCorrectedParameterValues) {
          const filteredMedia = filterMedia(parameter.response.medias, data);
          setCorrectedParameterValues((prev) => ({
            ...prev,
            medias: filteredMedia,
          }));
        }
        setUploadedMedia((data || []).map((el) => ({ ...el, id: el.mediaId })));
      } else {
        if (setCorrectedParameterValues) {
          setCorrectedParameterValues((prev) => {
            const existingIndex = prev.medias.findIndex((media) => media.id === data.mediaId);
            if (existingIndex !== -1) {
              const updatedMedias = [...prev.medias];
              updatedMedias[existingIndex] = { ...data, id: data.mediaId };
              return { ...prev, medias: updatedMedias };
            } else {
              return { ...prev, medias: [...prev.medias, { ...data, id: data.mediaId }] };
            }
          });
        }
        setUploadedMedia((prev) => {
          const existingIndex = prev.findIndex((media) => media.id === data.mediaId);
          if (existingIndex !== -1) {
            const updatedMedias = [...prev];
            updatedMedias[existingIndex] = { ...data, id: data.mediaId };
            return updatedMedias;
          } else {
            return [...prev, { ...data, id: data.mediaId }];
          }
        });
      }
    } else {
      dispatch(
        jobActions.executeParameter({
          parameter: {
            ...parameter,
            data: {
              medias: isDeleting ? data : [...(parameter.data?.medias ?? []), { ...data }],
            },
          },
        }),
      );
    }
  };

  const onUploaded = (fileData: FileUploadData) => {
    dispatch(
      openOverlayAction({
        type: OverlayNames.TASK_MEDIA,
        props: {
          uploadedMedia,
          mediaDetails: {
            ...fileData,
            name: '',
            description: '',
          },
          isParameter: true,
          isCorrectingError,
          taskId: taskExecutionId,
          execute: onExecute,
          isExecution: true,
        },
      }),
    );
    setIsUploading(false);
  };

  const fetchVideoDevices = async () => {
    const devices = await getVideoDevices();
    setVideoDevices(devices);
  };

  const isDisabled =
    videoDevices.length === 0 || (isCorrectingError && !setCorrectedParameterValues);

  useEffect(() => {
    setUploadedMedia(parameter.response!.medias!);
  }, [parameter?.response?.audit?.modifiedAt]);

  useEffect(() => {
    fetchVideoDevices();
  }, []);

  return (
    <ImageCaptureWrapper data-id={parameter.id} data-type={parameter.type}>
      {uploadedMedia.length > 0 && (
        <TaskMedias
          taskId={taskExecutionId}
          medias={uploadedMedia ?? []}
          parameterId={parameter.id}
          isTaskCompleted={isTaskCompleted || !isLoggedInUserAssigned}
          isCorrectingError={isCorrectingError}
          isParameter
          execute={onExecute}
          uploadedMedia={uploadedMedia}
          setCorrectedParameterValues={setCorrectedParameterValues}
        />
      )}
      {(!isTaskCompleted || isCorrectingError) && (
        <div style={{ display: 'flex' }}>
          {isUploading ? (
            <LinearProgress style={{ height: 8, width: '100%', color: '#1d84ff' }} />
          ) : (
            <div
              className="card"
              style={videoDevices.length === 0 ? { cursor: 'not-allowed' } : {}}
            >
              <ImageUploadButton
                label="User can capture photos"
                onUploadStart={() => {
                  setIsUploading(true);
                }}
                onUploadSuccess={(fileData) => {
                  onUploaded(fileData);
                }}
                icon={PhotoCamera}
                allowCapture
                disabled={isDisabled}
                onUploadError={() => {
                  setIsUploading(false);
                }}
                showErrorNotification={true}
              />
            </div>
          )}
        </div>
      )}
    </ImageCaptureWrapper>
  );
};

export default ImageCapture;
