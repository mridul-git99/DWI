import React, { FC } from 'react';
import { Checkbox } from './Checkbox';
import styled from 'styled-components';

const Wrapper = styled.div`
  .container {
    font-size: 14px;
  }

  .checkbox-input {
    padding: 12px 8px;
    border-bottom: 1px solid #e0e0e0;
  }

  .nested-options {
    .checkbox-input {
      padding-left: 32px;
    }
  }
`;

type Option = {
  label: string;
  value: string;
  options?: Option[];
};

interface NestedCheckboxListProps {
  options: Option[];
  checkedOptions: any;
  onStageCheckboxChange: (value: string, checked: boolean) => void;
  onTaskCheckboxChange: (value: string, checked: boolean) => void;
}

export const NestedCheckboxList: FC<NestedCheckboxListProps> = ({
  options,
  checkedOptions,
  onStageCheckboxChange,
  onTaskCheckboxChange,
}) => {
  return (
    <Wrapper>
      {options?.map((option) => {
        return (
          <div key={option.value}>
            <Checkbox
              label={option.label}
              checked={checkedOptions[option.value] || false}
              onClick={() =>
                option.options
                  ? onStageCheckboxChange(option.value, !checkedOptions[option.value])
                  : onTaskCheckboxChange(option.value, !checkedOptions[option.value])
              }
            />
            {option.options && (
              <div className="nested-options">
                <NestedCheckboxList
                  options={option.options}
                  checkedOptions={checkedOptions}
                  onStageCheckboxChange={onStageCheckboxChange}
                  onTaskCheckboxChange={onTaskCheckboxChange}
                />
              </div>
            )}
          </div>
        );
      })}
    </Wrapper>
  );
};

export default NestedCheckboxList;
