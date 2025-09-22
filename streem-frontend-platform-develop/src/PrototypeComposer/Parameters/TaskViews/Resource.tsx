import { FormGroup } from '#components';
import { ParameterProps } from '#PrototypeComposer/Activity/types';
import { InputTypes } from '#utils/globalTypes';
import React, { FC } from 'react';

const ResourceTaskView: FC<Pick<ParameterProps, 'parameter'>> = ({ parameter }) => {
  return (
    <FormGroup
      inputs={[
        {
          type: InputTypes.SINGLE_SELECT,
          props: {
            id: parameter.id,
            options: [],
            placeholder: '',
          },
        },
      ]}
    />
  );
};

export default ResourceTaskView;
