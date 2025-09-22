import React, { FC } from 'react';

import { ContentArea, Wrapper } from './styles';

const WorkArea: FC = ({ children, ...rest }) => {
  return (
    <Wrapper {...rest}>
      <ContentArea>{children}</ContentArea>
    </Wrapper>
  );
};

export default WorkArea;
