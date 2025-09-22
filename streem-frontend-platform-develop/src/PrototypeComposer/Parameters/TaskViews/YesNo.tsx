import { Button } from '#components';
import { ParameterProps } from '#PrototypeComposer/Activity/types';
import React, { FC } from 'react';
import styled from 'styled-components';

const YesNoTaskViewWrapper = styled.div`
  display: flex;
  .yes-label {
    color: #24a148;
    border-color: #24a148;
  }
  .no-label {
    color: #da1e28;
    border-color: #da1e28;
  }

  button {
    cursor: unset;
    :hover {
      background-color: unset;
    }
  }
`;

const YesNoTaskView: FC<Pick<ParameterProps, 'parameter'>> = ({ parameter }) => {
  return (
    <YesNoTaskViewWrapper>
      <Button variant="secondary" className="yes-label">
        {parameter.data[0].name}
      </Button>
      <Button variant="secondary" className="no-label">
        {parameter.data[1].name}
      </Button>
    </YesNoTaskViewWrapper>
  );
};

export default YesNoTaskView;
