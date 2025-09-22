import React from 'react';
import { useFormContext } from 'react-hook-form';
import { FormGroup } from '#components';
import { InputTypes } from '#utils/globalTypes';

export const GroupDetails = ({ readOnly }: any) => {
  const { register } = useFormContext();

  return (
    <div>
      <FormGroup
        key="basic-info-section"
        inputs={[
          {
            type: InputTypes.SINGLE_LINE,
            props: {
              placeholder: 'Enter Group Name',
              label: 'Group Name',
              id: 'groupName',
              name: 'groupName',
              ref: register({ required: true }),
              disabled: readOnly,
            },
          },
          {
            type: InputTypes.MULTI_LINE,
            props: {
              placeholder: 'Enter Group Description',
              label: 'Group Description',
              id: 'groupDescription',
              name: 'groupDescription',
              rows: 3,
              ref: register({ required: true }),
              disabled: readOnly,
            },
          },
        ]}
      />
    </div>
  );
};
