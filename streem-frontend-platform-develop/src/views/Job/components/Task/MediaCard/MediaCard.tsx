import React, { FC } from 'react';
import styled, { css } from 'styled-components';

import { ImageGallery } from '#components';
import { openOverlayAction } from '#components/OverlayContainer/actions';
import { OverlayNames } from '#components/OverlayContainer/types';
import { useDispatch } from 'react-redux';
import { Media } from '#PrototypeComposer/checklist.types';

export type MediaCardProps = {
  medias: Media[];
  isTaskActive: boolean;
};

const Wrapper = styled.div.attrs({
  className: 'task-media-card',
})<{ isTaskActive: boolean }>`
  grid-area: task-media-card;

  ${({ isTaskActive }) =>
    !isTaskActive
      ? css`
          display: none;
        `
      : null}

  .container {
    background-color: #ffffff;
    border: solid 1px #eeeeee;
    border-radius: 4px;
    box-shadow: 0 1px 4px 0 rgba(102, 102, 102, 0.08);
    display: flex;
    flex-direction: column;
    padding: 16px;

    .media-list {
      display: flex;
      flex-wrap: wrap;
      height: 110px;
      overflow-y: auto;
    }

    .active-media {
      border-radius: 4px;
      border: solid 2px #1d84ff;
      position: relative;
      height: 230px;
      cursor: pointer;

      .media-name {
        position: absolute;
        color: #ffffff;
        font-size: 12px;
        top: 32px;
        left: 12px;
        width: 56px;
      }

      img {
        height: 100%;
        width: 100%;
      }
    }
  }
`;

const MediaCard: FC<MediaCardProps> = ({ medias, isTaskActive }) => {
  const dispatch = useDispatch();

  return (
    <Wrapper isTaskActive={isTaskActive}>
      {medias.length ? (
        <div className="container">
          <ImageGallery
            medias={medias}
            onClickHandler={(media) =>
              dispatch(
                openOverlayAction({
                  type: OverlayNames.TASK_MEDIA,
                  props: {
                    mediaDetails: media,
                    disableDescInput: true,
                  },
                }),
              )
            }
          />
        </div>
      ) : null}
    </Wrapper>
  );
};

export default MediaCard;
