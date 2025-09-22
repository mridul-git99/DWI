import { debounce } from 'lodash';
import React, { FC } from 'react';
import { Select } from '#components';
import { useTypedSelector } from '#store/helpers';

type TSelectApproversFieldProps = {
  selectedApprovers: any[];
  list: any[];
  loading: boolean;
  fetchData: (params?: Record<string, any>) => void;
  fetchNext: () => void;
  onChange: (value: any[]) => void;
  isDisabled?: boolean;
};

const SelectApproversField: FC<TSelectApproversFieldProps> = ({
  list,
  loading,
  fetchData,
  fetchNext,
  onChange,
  selectedApprovers,
  isDisabled = false,
}) => {
  const userId = useTypedSelector((state) => state.auth.userId || '');
  return (
    <Select
      label="Approver"
      isLoading={loading}
      options={list.reduce((options, user) => {
        if (user.id !== userId) {
          options.push({
            ...user,
            value: user.id,
            label: user?.firstName + ' ' + user?.lastName,
            externalId: <div>&nbsp;(ID: {user?.employeeId})</div>,
          });
        }
        return options;
      }, [])}
      onInputChange={debounce((searchedValue: string, actionMeta) => {
        if (searchedValue !== actionMeta.prevInputValue) {
          fetchData({
            customQueryParams: { query: searchedValue },
          });
        }
      }, 500)}
      onChange={(value) => {
        onChange(value);
      }}
      value={selectedApprovers?.length ? selectedApprovers : null}
      onMenuScrollToBottom={fetchNext}
      isMulti={true}
      onMenuOpen={fetchData}
      placeholder="Select from list of approvers"
      isDisabled={isDisabled}
    />
  );
};

export default SelectApproversField;
