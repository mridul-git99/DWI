import { TextInput } from '#components';
import { ParameterProps } from '#PrototypeComposer/Activity/types';
import React, { FC } from 'react';

const CalculationTaskView: FC<Pick<ParameterProps, 'parameter'>> = ({ parameter }) => {
  return <TextInput disabled placeholder="" value={parameter.data.expression} />;
};

export default CalculationTaskView;
