import { FormGroup } from '#components';
import { ParameterProps } from '#PrototypeComposer/Activity/types';
import { PARAMETER_OPERATORS } from '#PrototypeComposer/constants';
import { InputTypes } from '#utils/globalTypes';
import React, { FC } from 'react';
import { FormatOptionLabelContext } from 'react-select';

const ShouldBeTaskView: FC<Pick<ParameterProps, 'parameter'>> = ({ parameter }) => {
  const selectedOperator = PARAMETER_OPERATORS.filter((o) => parameter.data.operator === o.value);
  const taskValue = (parameter) => {
    if (parameter.data.value) {
      return parameter.data.value;
    } else {
      return `${parameter.data.lowerValue} <-> ${parameter.data.upperValue}`;
    }
  };

  const uom = parameter.data.uom.trim() !== '' ? parameter.data.uom.trim() : null;

  return (
    <FormGroup
      inputs={[
        ...(uom
          ? [
              {
                type: InputTypes.SINGLE_LINE,
                props: {
                  id: 'uom',
                  label: 'Unit of Measurement',
                  placeholder: '',
                  value: uom,
                  disabled: true,
                },
              },
            ]
          : []),

        {
          type: InputTypes.SINGLE_SELECT,
          props: {
            id: 'operator',
            label: 'Criteria',
            options: PARAMETER_OPERATORS,
            value: selectedOperator,
            placeholder: 'Choose an option',
            formatOptionLabel: (
              option: any,
              { context }: { context: FormatOptionLabelContext },
            ) => {
              if (context === 'menu') {
                return option.label;
              }
              return selectedOperator[0].label;
            },
          },
        },
        {
          type: InputTypes.SINGLE_LINE,
          props: {
            id: 'value',
            label: 'Value',
            placeholder: '',
            value: taskValue(parameter),
            disabled: true,
          },
        },
      ]}
    />
  );
};

export default ShouldBeTaskView;
