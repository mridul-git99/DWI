import { CircularProgress, LinearProgress } from '@material-ui/core';
import React from 'react';
import styled from 'styled-components';

const LoadingContainerWrapper = styled.div.attrs({
  className: 'loading-container-wrapper',
})`
  display: flex;
  height: 100%;
  align-items: center;
  justify-content: center;
`;

export const LoadingContainer = ({
  loading,
  component,
  style = {},
  linear = false,
}: {
  loading: boolean;
  component?: JSX.Element;
  style?: Record<string, any>;
  linear?: boolean;
}) => {
  return loading ? (
    <LoadingContainerWrapper style={style}>
      {linear ? (
        <LinearProgress
          data-testid="loading-container-progress"
          style={{ height: 8, width: '100%', color: '#1d84ff' }}
        />
      ) : (
        <CircularProgress
          data-testid="loading-container-progress"
          style={{ color: 'rgb(29, 132, 255)' }}
        />
      )}
    </LoadingContainerWrapper>
  ) : (
    component || <></>
  );
};
