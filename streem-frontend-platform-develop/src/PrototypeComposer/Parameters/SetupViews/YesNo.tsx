import { TextInput } from '#components';
import { capitalize } from 'lodash';
import React, { FC } from 'react';
import { useFormContext } from 'react-hook-form';
import { v4 as uuidv4 } from 'uuid';
import { CommonWrapper } from './styles';

const fields = [
  {
    type: 'yes',
    id: uuidv4(),
    name: '',
  },
  {
    type: 'no',
    id: uuidv4(),
    name: '',
  },
];

const YesNoParameter: FC<{ isReadOnly: boolean }> = ({ isReadOnly }) => {
  const { register } = useFormContext();

  return (
    <CommonWrapper>
      <ul className="list" style={{ marginTop: 0 }}>
        {fields.map((item, index) => (
          <li className="list-item" key={item.id} style={{ marginBlock: 8 }}>
            <input
              type="hidden"
              name={`data.${index}.id`}
              ref={register({
                required: true,
              })}
              defaultValue={item.id}
            />
            <input
              type="hidden"
              name={`data.${index}.type`}
              ref={register({
                required: true,
              })}
              defaultValue={item.type}
            />
            <TextInput
              name={`data.${index}.name`}
              label={`Label for ${capitalize(item.type)}`}
              ref={register({
                required: true,
              })}
              defaultValue={item.name}
              disabled={isReadOnly}
            />
          </li>
        ))}
      </ul>
    </CommonWrapper>
  );
};

export default YesNoParameter;
