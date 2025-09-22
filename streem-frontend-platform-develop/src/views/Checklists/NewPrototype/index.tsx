import { RouteComponentProps } from '@reach/router';
import React, { FC } from 'react';

import PrototypeForm from './PrototypeForm';
import { Wrapper } from './styles';
import { FormMode, Props } from './types';

const NewPrototype: FC<RouteComponentProps<Props>> = (props) => {
  return (
    <Wrapper>
      <PrototypeForm
        formData={props.location?.state?.formData ?? {}}
        formMode={props.location?.state?.mode ?? FormMode.ADD}
      />
    </Wrapper>
  );
};

export default NewPrototype;
