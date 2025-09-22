import { PhotoCamera } from '@material-ui/icons';
import React, { FC } from 'react';
import styled from 'styled-components';

const MediaTaskViewWrapper = styled.div`
  align-items: center;
  background-color: #f4f4f4;
  display: flex;
  flex: 1;
  flex-direction: column;
  justify-content: center;
  padding: 16px;
  gap: 8px;

  .icon {
    color: #1d84ff;
  }

  span {
    font-size: 14px;
    line-height: 1.14;
    letter-spacing: 0.16px;
    color: #1d84ff;
  }
`;

const MediaTaskView: FC = () => {
  return (
    <MediaTaskViewWrapper>
      <PhotoCamera className="icon" />
      <span>User can capture photos</span>
    </MediaTaskViewWrapper>
  );
};

export default MediaTaskView;
