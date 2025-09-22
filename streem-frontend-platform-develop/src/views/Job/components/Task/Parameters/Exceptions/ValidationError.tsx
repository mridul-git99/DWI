import React, { FC } from 'react';
import styled from 'styled-components';

const ValidationError: FC<{ error: string }> = ({ error }) => {
  const Wrapper = styled.div`
    display: flex;
    padding: 4px 8px;
    font-size: 14px;
    background-color: rgba(255, 107, 107, 0.16);
    border: 1px solid #ff6b6b;
    width: max-content;
  `;

  if (!error) return null;

  return <Wrapper>{error}</Wrapper>;
};

export default ValidationError;
