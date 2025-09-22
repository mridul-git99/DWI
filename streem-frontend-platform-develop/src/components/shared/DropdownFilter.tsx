import React, { FC } from 'react';
import styled from 'styled-components';
import { Option, Select } from './Select';

const Wrapper = styled.div`
  margin-left: 16px;
  min-width: 200px;
  display: none;

  @media (min-width: 1366px) {
    display: block;
  }

  .button {
    padding: 7px 16px;
  }
`;

type DropdownFilterProps = {
  options: Option[];
  updateFilter: (option: Option) => void;
  label?: string;
  placeholder?: string;
};

const DropdownFilter: FC<DropdownFilterProps> = ({ options, updateFilter, label }) => (
  <Wrapper>
    <Select label={label} options={options} onChange={updateFilter} isClearable />
  </Wrapper>
);

export default DropdownFilter;
