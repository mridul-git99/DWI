import React, { FC } from 'react';
import styled from 'styled-components';

const StyledComponent = styled.div<{ as?: string }>`
  /* Define styles for an anchor tag */
  ${(props) =>
    props.as === 'a' &&
    `
    text-decoration: none;
    color: #1D84FF;
    cursor: pointer;
  `}
`;

interface CustomComponentProps {
  as?: string;
  children: React.ReactNode;
}

export const CustomTag: FC<any> = ({ as, children, ...rest }) => {
  return (
    <StyledComponent as={as} {...rest}>
      {children}
    </StyledComponent>
  );
};
