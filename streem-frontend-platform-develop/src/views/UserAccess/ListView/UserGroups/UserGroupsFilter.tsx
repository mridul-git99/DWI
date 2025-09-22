import { Button, SearchFilter, ToggleSwitch } from '#components';
import { FilterOperators } from '#utils/globalTypes';
import { navigate } from '@reach/router';
import React from 'react';

export const UserGroupsFilter = ({ filterParams, setFilterParams, showAddNewUserGroup }: any) => {
  return (
    <div className="before-table-wrapper">
      <div className="filters">
        <SearchFilter
          showDropdown
          prefilledSearch={
            filterParams?.fields?.length
              ? { value: filterParams.fields?.[0]?.values, field: filterParams.fields?.[0]?.field }
              : {}
          }
          dropdownOptions={[
            {
              label: 'Group Name',
              value: 'name',
              field: 'name',
              operator: FilterOperators.LIKE,
            },
            {
              label: 'User Employee Name',
              value: 'userGroupMembers.user.firstName',
              field: 'userGroupMembers.user.firstName',
              operator: FilterOperators.LIKE,
            },
          ]}
          updateFilterFields={(fields) => {
            setFilterParams({ ...filterParams, fields: fields });
          }}
        />
        <ToggleSwitch
          offLabel="Show Archived"
          onChange={(isChecked) => {
            setFilterParams({ ...filterParams, archive: isChecked });
          }}
          onLabel="Show Unarchived"
          checked={filterParams.archive}
        />
      </div>
      {showAddNewUserGroup && (
        <div className="actions">
          <Button onClick={() => navigate('/users/group/add')}>Add New Group</Button>
        </div>
      )}
    </div>
  );
};
