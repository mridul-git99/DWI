import { ImageGallery, ImageUploadButton } from '#components';
import { openOverlayAction } from '#components/OverlayContainer/actions';
import { OverlayNames } from '#components/OverlayContainer/types';
import { EnabledStates } from '#PrototypeComposer/checklist.types';
import { useTypedSelector } from '#store/helpers';
import { PermMediaOutlined } from '@material-ui/icons';
import React, { FC } from 'react';
import { useDispatch } from 'react-redux';
import { TaskMediasWrapper } from './styles';
import { TaskMediasProps } from './types';

const TaskMedias: FC<TaskMediasProps> = ({
  medias,
  taskId,
  parameterId,
  isParameter = false,
  isTaskCompleted,
  isCorrectingError,
  execute,
  uploadedMedia,
  setCorrectedParameterValues,
}) => {
  const dispatch = useDispatch();

  const {
    data,
    tasks: { activeTaskId },
  } = useTypedSelector((state) => state.prototypeComposer);

  if (taskId === activeTaskId || isParameter) {
    return (
      <TaskMediasWrapper>
        <div className="container">
          <ImageGallery
            medias={medias}
            onClickHandler={(media) => {
              dispatch(
                openOverlayAction({
                  type: OverlayNames.TASK_MEDIA,
                  props: {
                    taskId,
                    isParameter,
                    parameterId,
                    mediaDetails: media,
                    disableNameInput: isCorrectingError
                      ? setCorrectedParameterValues
                        ? false
                        : isTaskCompleted
                      : isTaskCompleted,
                    disableDescInput: isCorrectingError
                      ? setCorrectedParameterValues
                        ? false
                        : isTaskCompleted
                      : isTaskCompleted,
                    execute,
                    uploadedMedia,
                    isCorrectingError,
                  },
                }),
              );
            }}
          />

          {!isParameter ? (
            <ImageUploadButton
              onUploadSuccess={(fileData) => {
                dispatch(
                  openOverlayAction({
                    type: OverlayNames.TASK_MEDIA,
                    props: {
                      mediaDetails: {
                        ...fileData,
                        name: '',
                        description: '',
                      },
                      taskId: taskId,
                    },
                  }),
                );
              }}
              onUploadError={(error) => console.error('handle image upload error :: ', error)}
              label="Upload Media"
              icon={PermMediaOutlined}
              disabled={data?.state && !(data.state in EnabledStates) && !data?.archived}
            />
          ) : null}
        </div>
      </TaskMediasWrapper>
    );
  } else {
    return <TaskMediasWrapper />;
  }
};

export default TaskMedias;
