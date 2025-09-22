import { FormGroup } from '#components';
import { ParameterProps } from '#PrototypeComposer/Activity/types';
import { InputTypes } from '#utils/globalTypes';
import React, { FC } from 'react';
import { FormatOptionLabelContext } from 'react-select';

const SingleSelectTaskView: FC<Pick<ParameterProps, 'parameter'>> = ({ parameter }) => {
  return (
    <FormGroup
      inputs={[
        {
          type: InputTypes.SINGLE_SELECT,
          props: {
            id: parameter.id,
            options: parameter.data.map((option: any) => ({
              label: option.name,
              value: option.id,
            })),
            formatOptionLabel: (
              option: any,
              { context }: { context: FormatOptionLabelContext },
            ) => {
              if (context === 'menu') {
                return option.label;
              }
              return <div />;
            },
            placeholder: '',
          },
        },
      ]}
    />
  );
};

export default SingleSelectTaskView;
