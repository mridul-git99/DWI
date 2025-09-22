import React, { FC } from 'react';
import styled, { css } from 'styled-components';

type Props = {
  height?: number;
  percentage: number;
  whiteBackground?: boolean;
  danger?: boolean;
};

const Wrapper = styled.div.attrs({
  className: 'progress-bar',
})<Props>`
  border-radius: 4px;
  position: relative;
  width: 100%;
  background-color: ${({ whiteBackground }) => (whiteBackground ? '#ffffff' : '#eee')};
  height: ${({ height }) => `${height}px`};
  box-shadow: inset 0 2px 8px 0 rgba(0, 0, 0, 0.08);
  text-align: center;

  .filler {
    border-radius: inherit;
    height: 100%;
    transition: width 0.2s ease-in;
    width: ${({ percentage }) => `${percentage}%`};

    ${({ percentage, danger }) => {
      if (percentage === 100) {
        return css`
          background-color: ${danger ? '#da1e28' : '#5aa700'};
        `;
      } else if (percentage === 0) {
        return css`
          background-color: ${danger ? '#da1e28' : '#666666'};
          width: 6%;
        `;
      } else {
        return css`
          background-color: ${danger ? '#da1e28' : '#1d84ff'};
        `;
      }
    }}
  }

  .text {
    font-weight: 800;
    font-size: 13px;
    margin-top: 16px;
  }
`;

const ProgressBar: FC<Props> = ({
  percentage = 0,
  height = 8,
  whiteBackground = false,
  danger = false,
}) => (
  <Wrapper
    percentage={percentage}
    height={height}
    whiteBackground={whiteBackground}
    danger={danger}
  >
    <div className="filler"></div>
    <span className="text">{Math.ceil(percentage)} %</span>
  </Wrapper>
);

export default ProgressBar;
