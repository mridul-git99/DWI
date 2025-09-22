import React from 'react';
import { Checkbox } from '#components/shared/Checkbox';
import { components } from 'react-select';

const Option = (props: any) => {
  return (
    <components.Option {...props}>
      <Checkbox label={props.label} checked={props.isSelected} />
    </components.Option>
  );
};

export default Option;
