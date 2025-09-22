import { User } from '#services/users';
import { DEFAULT_PAGE_NUMBER, DEFAULT_PAGE_SIZE } from '#utils/constants';
import React from 'react';

export const urlParams = {
  page: DEFAULT_PAGE_NUMBER,
  size: DEFAULT_PAGE_SIZE,
  sort: 'createdAt,desc',
};

export const getFormattedUserOptions = (users: User[]) => {
  return (users || []).map((user) => ({
    value: user.id,
    label: user?.firstName + ' ' + user?.lastName,
    externalId: <div>&nbsp;(ID: {user?.employeeId})</div>,
  }));
};
