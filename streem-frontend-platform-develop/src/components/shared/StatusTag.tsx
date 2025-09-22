import { JobStatus } from '#views/Jobs/ListView/types';
import React, { FC } from 'react';
import styled from 'styled-components';
import ClearIcon from '@material-ui/icons/Clear';
import { STATUS_COLOR_MAP } from '#utils/jobMethods';

const Tag = styled.div<{ color: string; background: string }>`
  display: flex;
  gap: 4px;
  align-items: center;
  padding: 4px 8px;
  font-size: 12px;
  color: ${(props) => props.color};
  background-color: ${(props) => props.background};
  cursor: pointer;

  .MuiSvgIcon-root {
    font-size: 16px;
  }
`;

type StatusTagProps = {
  status: JobStatus | null;
  isClearable?: boolean;
  onClear?: () => void;
};

export const StatusTag: FC<StatusTagProps> = ({ status, isClearable = false, onClear }) => {
  if (!status) {
    return null;
  }
  const { statusText, color, backgroundColor } = STATUS_COLOR_MAP[status];
  return (
    <Tag color={color} background={backgroundColor} onClick={onClear}>
      {statusText}
      {isClearable && <ClearIcon />}
    </Tag>
  );
};
