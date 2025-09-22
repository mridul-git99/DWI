import FullScreenIcon from '#assets/svg/FullScreen';
import { BaseModal, Button, ImageAuth, Textarea, TextInput } from '#components';
import { CommonOverlayProps } from '#components/OverlayContainer/types';
import { Checklist, EnabledStates } from '#PrototypeComposer/checklist.types';
import { CollaboratorType } from '#PrototypeComposer/reviewer.types';
import { useTypedSelector } from '#store';
import { Task } from '#types';
import { getFileExtension } from '#utils/stringUtils';
import { DeleteOutlined } from '@material-ui/icons';
import { debounce, omit } from 'lodash';
import React, { FC, useEffect, useMemo, useState } from 'react';
import { useDispatch } from 'react-redux';
import styled, { css } from 'styled-components';
import { Parameter } from '../Activity/types';
import { addTaskMedia, removeTaskMedia, updateTaskMedia } from '../Tasks/actions';
import { MediaDetails } from '../Tasks/types';

const Wrapper = styled.div<{
  fullScreeen: boolean;
  disableDescInput: boolean;
  disableNameInput: boolean;
}>`
  #modal-container {
    z-index: 1400 !important;

    ${({ fullScreeen }) => {
      return css`
        .modal {
          max-height: ${fullScreeen && '100dvh !important'};
        }
      `;
    }}

    ${({ fullScreeen, disableDescInput, disableNameInput }) => {
      return css`
        .modal {
          min-width: ${fullScreeen ? '100% !important' : '300px !important'};
          max-width: 80dvw !important;
          height: ${fullScreeen ? '100%' : 'max-content'};
          background: ${fullScreeen ? 'transparent !important' : 'white'};
          padding: ${fullScreeen ? '10px !important' : 'unset'};

          .close-icon {
            display: ${fullScreeen ? 'none' : 'inline-block'};
          }

          &-body {
            height: ${fullScreeen ? '100%' : 'inherit'};
            padding: 0 !important;

            .wrapper {
              display: flex;
              height: inherit;
              align-items: ${fullScreeen ? 'center' : 'unset'};

              .left-side {
                align-items: center;
                display: flex;
                flex: 2;
                justify-content: center;
                max-width: ${fullScreeen ? '100%' : '600px'};
                max-height: ${fullScreeen ? 'unset' : '90%'};
                min-width: 280px;
                height: ${fullScreeen ? '100%' : 'unset'};
                position: relative;

                img {
                  max-width: 100%;
                  max-height: 100%;
                }

                .full-screen-action {
                  position: absolute;
                  right: 16px;
                  top: 24px;
                  width: 24px;
                  height: 24px;
                  cursor: pointer;
                  border-radius: 50%;
                  background-color: ${fullScreeen ? 'rgba(0, 0, 0, 1)' : 'rgba(0, 0, 0, 0.2)'};
                }
              }

              .right-side {
                border-left: 1px solid #eeeeee;
                display: ${fullScreeen ? 'none' : 'flex'};
                flex: 1;
                flex-direction: column;
                justify-content: center;

                .media-details {
                  padding: 24px;

                  .title-text {
                    color: #ccc;
                    font-size: 12px;
                  }
                  .title-container {
                    height: 60px;
                    overflow: auto;
                    overflow-x: hidden;
                    margin: 10px 0;

                    div {
                      position: static;
                      color: #ccc;
                    }

                    span {
                      font-size: 14px;
                    }
                  }

                  ${disableDescInput
                    ? css`
                        .input-label {
                          color: #999999;
                          font-size: 12px;

                          .optional-badge {
                            display: none;
                          }
                        }
                      `
                    : null}

                  .input {
                    margin-bottom: 40px;

                    .input-wrapper {
                      ${disableDescInput || disableNameInput
                        ? css`
                            background: none;
                            border: none;
                            padding: 10px 0px;
                            pointer-events: none;

                            input {
                              font-size: 14px;
                              font-weight: bold;
                              ::placeholder {
                                visibility: hidden;
                              }
                            }
                          `
                        : null}
                    }
                  }

                  .textarea {
                    margin-bottom: 15px;

                    .textarea-wrapper {
                      textarea:disabled {
                        background: none;
                        font-size: 14px;
                        color: #000;
                        border-radius: 4px;
                        border: solid 1px #eeeeee;
                        ::placeholder {
                          visibility: hidden;
                        }
                      }
                    }
                  }

                  button#save-details {
                    margin-left: auto;
                  }
                }

                .delete-media {
                  display: flex;
                  align-items: center;
                  padding: 16px 24px;
                  margin-top: auto;
                  border-top: 1px solid #eeeeee;
                  cursor: pointer;
                }
              }
            }
          }
        }
      `;
    }}
  }
`;

type Props = {
  mediaDetails: MediaDetails;
  taskId?: Task['id'];
  parameterId?: Parameter['id'];
  isParameter?: boolean;
  disableNameInput?: boolean;
  execute: (data: MediaDetails, isDeleting?: boolean) => void;
  disableDescInput?: boolean;
  isCorrectingError?: boolean;
  uploadedMedia?: any;
  isExecution?: boolean;
};

const TaskMediaModal: FC<CommonOverlayProps<Props>> = ({
  closeAllOverlays,
  closeOverlay,
  props: {
    mediaDetails,
    taskId,
    parameterId,
    isParameter = false,
    execute,
    disableNameInput = false,
    disableDescInput = false,
    uploadedMedia,
    isCorrectingError,
    isExecution = false,
  } = {},
}) => {
  const dispatch = useDispatch();
  const { state, collaborators, userId, parameters } = useTypedSelector((state) => ({
    userId: state.auth.userId,
    state: state.prototypeComposer?.data?.state as Checklist['state'],
    collaborators: state.prototypeComposer?.data?.collaborators as Checklist['collaborators'],
    parameters: state.job.activeTask.parameters,
  }));

  const [stateMediaDetails, setStateMediaDetails] = useState<MediaDetails>(mediaDetails);
  const [fullScreeen, setFullScreeen] = useState(false);
  const [isAuthor, setIsAuthor] = useState(false);
  const [errors, setErrors] = useState({ name: '' });

  useEffect(() => {
    setIsAuthor(
      collaborators?.some(
        (collaborator) =>
          (collaborator.type === CollaboratorType.PRIMARY_AUTHOR ||
            collaborator.type === CollaboratorType.AUTHOR) &&
          collaborator.id === userId,
      ),
    );
  }, []);

  if (!isExecution && collaborators && (!isAuthor || !(state in EnabledStates))) {
    disableNameInput = true;
    disableDescInput = true;
  }

  const isImage = ['png', 'jpg', 'jpeg'].includes(
    getFileExtension(stateMediaDetails?.filename!) || '',
  );

  // From array of responses of a parameter, get the response belonging to the current taskExectionId
  const parameterResponseMedia = useMemo(() => {
    if (isCorrectingError) {
      return uploadedMedia;
    } else {
      return parameterId ? parameters.get(parameterId)?.response?.medias : [];
    }
  }, [isCorrectingError, uploadedMedia, parameters, parameterId, taskId]);

  return (
    <Wrapper
      fullScreeen={fullScreeen}
      disableDescInput={disableDescInput}
      disableNameInput={disableNameInput}
    >
      <BaseModal
        closeAllModals={closeAllOverlays}
        closeModal={closeOverlay}
        showHeader={false}
        showFooter={false}
      >
        <div className="wrapper">
          {isImage && (
            <div className="left-side">
              <ImageAuth
                key={stateMediaDetails.id}
                src={stateMediaDetails?.link}
                alt={stateMediaDetails.filename}
                showDownload
              />
              <div className="full-screen-action" onClick={() => setFullScreeen(!fullScreeen)}>
                <FullScreenIcon />
              </div>
            </div>
          )}

          <div className="right-side">
            <div className="media-details">
              {(!disableNameInput || (disableNameInput && stateMediaDetails.name)) &&
              (disableNameInput || disableDescInput) ? (
                <div>
                  <div className="title-text">{isImage ? 'Photo name' : 'File name'}</div>
                  <div className="title-container">
                    <span>{stateMediaDetails.name}</span>
                  </div>
                </div>
              ) : (
                <TextInput
                  defaultValue={stateMediaDetails.name}
                  error={errors.name}
                  label={isImage ? 'Photo name' : 'File name'}
                  name="name"
                  onBlur={(event) => {
                    setStateMediaDetails({
                      ...stateMediaDetails,
                      name: event.target.value,
                    });

                    if (!!errors.name) {
                      setErrors({ ...errors, name: '' });
                    }
                  }}
                />
              )}

              {(!disableDescInput || (disableDescInput && stateMediaDetails.description)) && (
                <Textarea
                  optional
                  defaultValue={stateMediaDetails.description}
                  label="Description"
                  name="description"
                  disabled={disableDescInput}
                  onChange={debounce(({ name, value }) => {
                    setStateMediaDetails({
                      ...stateMediaDetails,
                      [name]: value,
                    });
                  }, 500)}
                  rows={4}
                />
              )}

              {!disableDescInput && (
                <Button
                  id="save-details"
                  onClick={() => {
                    if (stateMediaDetails.name && stateMediaDetails.name.length > 250) {
                      setErrors({
                        name: "Name field can't be more than 250 characters. Please try again",
                      });
                      return;
                    }
                    if (disableNameInput || !!stateMediaDetails.name) {
                      if (isParameter && execute) {
                        execute(
                          omit(
                            {
                              ...stateMediaDetails,
                              mediaId: stateMediaDetails?.mediaId || stateMediaDetails?.id || '',
                            },
                            'id',
                          ),
                        );
                        closeOverlay();
                      } else {
                        if (mediaDetails?.id) {
                          dispatch(
                            updateTaskMedia({
                              taskId,
                              mediaId: mediaDetails?.id,
                              parameterId,
                              mediaDetails: {
                                name: stateMediaDetails.name,
                                description: stateMediaDetails.description,
                              },
                            }),
                          );
                        } else {
                          dispatch(
                            addTaskMedia({
                              taskId,
                              mediaDetails: { ...stateMediaDetails },
                            }),
                          );
                        }
                      }
                    } else {
                      setErrors({ name: 'Name is required' });
                    }
                  }}
                >
                  Save
                </Button>
              )}
            </div>

            {!disableDescInput && mediaDetails?.id && taskId && (
              <div
                className="delete-media"
                onClick={() => {
                  if (isParameter && parameterId && execute) {
                    const updatedMedias = (parameterResponseMedia || []).map((currMedia) =>
                      omit(
                        {
                          ...currMedia,
                          mediaId: currMedia?.id,
                          ...(currMedia?.id === mediaDetails.id && {
                            archived: true,
                            reason: '.',
                          }),
                        },
                        'id',
                      ),
                    );
                    execute(updatedMedias, true);
                    closeOverlay();
                  } else {
                    dispatch(removeTaskMedia(taskId, mediaDetails.id));
                  }
                }}
              >
                <DeleteOutlined className="icon" />
                Delete
              </div>
            )}
          </div>
        </div>
      </BaseModal>
    </Wrapper>
  );
};

export default TaskMediaModal;
