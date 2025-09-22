import { FormGroup } from '#components';
import { InputTypes } from '#utils/globalTypes';
import React from 'react';
import { useFormContext } from 'react-hook-form';

export const ReasonDetails = ({ readOnly }: any) => {
  const { register } = useFormContext();

  return (
    <div>
      <FormGroup
        key="basic-info-section"
        inputs={[
          {
            type: InputTypes.MULTI_LINE,
            props: {
              placeholder: 'Enter Group Reason',
              label: 'Group Reason',
              id: 'groupReason',
              name: 'groupReason',
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
