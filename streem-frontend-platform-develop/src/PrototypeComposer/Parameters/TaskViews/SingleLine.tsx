import { FormGroup } from '#components';
import { ParameterProps } from '#PrototypeComposer/Activity/types';
import { MandatoryParameter } from '#types';
import { InputTypes } from '#utils/globalTypes';
import React, { FC } from 'react';

const SingleLineTaskView: FC<Pick<ParameterProps, 'parameter'>> = ({ parameter }) => {
  return (
    <FormGroup
      inputs={[
        {
          type: parameter.type as unknown as InputTypes,
          props: {
            id: parameter.label,
            disabled: true,
            ...(parameter.type === MandatoryParameter.MULTI_LINE && { rows: 3 }),
          },
        },
      ]}
    />
  );
};

export default SingleLineTaskView;
