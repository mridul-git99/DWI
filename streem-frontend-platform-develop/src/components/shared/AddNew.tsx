import { Add } from '@material-ui/icons';
import React, { FC } from 'react';
import styled from 'styled-components';

const Wrapper = styled.div`
  align-items: center;
  color: #1d84ff;
  cursor: pointer;
  display: flex;
  font-size: 14px;
  width: max-content;
  margin-top: 16px;

  .icon {
    color: #1d84ff;
    margin-right: 8px;
  }
`;

type AddNewProps = {
  onClick: () => void;
  label?: string;
};

const AddNew: FC<AddNewProps> = ({ onClick, label = 'Add New' }) => (
  <Wrapper className="add-new-item" onClick={onClick} data-testid="add-new">
    <Add className="icon" fontSize="small" data-testid="add-new-icon" />
    {label}
  </Wrapper>
);

export default AddNew;
