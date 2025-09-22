import { Media } from '#PrototypeComposer/checklist.types';
import { openOverlayAction } from '#components/OverlayContainer/actions';
import { OverlayNames } from '#components/OverlayContainer/types';
import { Parameter } from '#types';
import { openLinkInNewTab } from '#utils';
import { jobActions } from '#views/Job/jobStore';
import { omit } from 'lodash';
import React, { FC } from 'react';
import { useDispatch } from 'react-redux';
import styled from 'styled-components';
import closeIcon from '../../assets/svg/close.svg';
import FileIcon from '../../assets/svg/file.svg';
import { CustomTag } from './CustomTag';
import { ImageAuth } from './ImageAuth';

const FileGalleryWrapper = styled.div.attrs({
  className: 'file-gallery-wrapper',
})<Pick<FileGalleryProps, 'isTaskCompleted' | 'isCorrectingError'>>`
  display: flex;
  justify-content: space-between;
  align-items: center;
  .media-list {
    display: flex;
    flex-wrap: nowrap;

    flex: 1;
    flex-direction: column;
    gap: 10px;
  }

  .media-list-item {
    display: flex;
    justify-content: space-between;
    cursor: pointer;
    align-items: center;
  }

  .media-list-item-head {
    display: flex;
    gap: 8px;
  }

  .media-list-item-img {
    border: 1px solid #eeeeee;
    border-radius: 5px;
    box-sizing: border-box;
    cursor: pointer;
    height: 20px;
    width: 20px;
    fit-content: cover;
  }

  .media-list-item-name {
    color: #1d84ff;
    font-size: 14px;
  }

  .media-list-item-remove-icon {
    pointer-events: ${({ isTaskCompleted, isCorrectingError }) =>
      isTaskCompleted && !isCorrectingError ? 'none' : 'unset'};
  }
`;

export type FileGalleryProps = {
  medias: Media[];
  parameter: Parameter;
  isCorrectingError?: boolean;
  isTaskCompleted?: boolean;
  setCorrectedParameterValues?: React.Dispatch<React.SetStateAction<any>>;
  setUploadedMedia: any;
};

enum FileTypes {
  PDF = 'application/pdf',
  DOC = 'application/doc',
  DOCX = 'application/docx',
  JPEG = 'image/jpeg',
  JPG = 'image/jpg',
  PNG = 'image/png',
}

export const FileGallery: FC<FileGalleryProps> = ({
  medias,
  parameter,
  setUploadedMedia,
  isCorrectingError,
  isTaskCompleted,
  setCorrectedParameterValues,
}) => {
  const sectionIcon = (type: string, mediaLink: string) => {
    switch (type) {
      case FileTypes.PDF:
      case FileTypes.DOC:
      case FileTypes.DOCX:
        return <img src={FileIcon} alt="file icon" />;
      case FileTypes.JPEG:
      case FileTypes.JPG:
      case FileTypes.PNG:
        return <ImageAuth className="media-list-item-img" src={mediaLink} alt="file icon" />;
      default:
        return <img src={FileIcon} alt="file icon" />;
    }
  };

  const dispatch = useDispatch();
  const handleDelete = (
    media: Media,
    reason: string,
    setFormErrors: (errors?: Error[]) => void,
  ) => {
    const updatedMedias = (parameter?.response?.medias || [])
      .map((currMedia) =>
        omit(
          {
            ...currMedia,
            mediaId: currMedia?.id,
            ...(currMedia?.id === media?.id && {
              archived: true,
              reason,
            }),
          },
          'id',
        ),
      )
      .filter((media: any) => media.archived === true);

    dispatch(
      jobActions.executeParameter({
        parameter: { ...parameter, data: { medias: updatedMedias } },
      }),
    );
    setFormErrors(undefined);
  };

  return (
    <FileGalleryWrapper isTaskCompleted={isTaskCompleted} isCorrectingError={isCorrectingError}>
      <div className="media-list">
        {medias.map((media, index) => {
          if (media?.archived === false) {
            return (
              <div className="media-list-item" key={index}>
                <CustomTag
                  style={{ pointerEvents: 'auto' }}
                  as={'div'}
                  onClick={() => openLinkInNewTab(`/media?link=${media.link}`)}
                  children={
                    <div className="media-list-item-head">
                      {sectionIcon(media.type, media.link)}
                      <div className="media-list-item-name">{`${media?.name}.${
                        media?.filename?.split('.')?.[1]
                      }`}</div>
                    </div>
                  }
                />
                <img
                  src={closeIcon}
                  className="media-list-item-remove-icon"
                  onClick={() => {
                    if (isCorrectingError) {
                      if (setCorrectedParameterValues) {
                        setCorrectedParameterValues((prev) => ({
                          ...prev,
                          medias: [...prev.medias, { ...media, archived: true }],
                        }));
                        setUploadedMedia((prev) => prev.filter((m) => m.id !== media.id));
                      }
                    } else {
                      dispatch(
                        openOverlayAction({
                          type: OverlayNames.REASON_MODAL,
                          props: {
                            modalTitle: 'Remove File',
                            modalDesc: `Are you sure you want to remove the updated file?`,
                            onSubmitHandler: (
                              reason: string,
                              setFormErrors: (errors?: Error[]) => void,
                            ) => {
                              handleDelete(media, reason, setFormErrors);
                            },
                          },
                        }),
                      );
                    }
                  }}
                />
              </div>
            );
          } else {
            return null;
          }
        })}
      </div>
    </FileGalleryWrapper>
  );
};
