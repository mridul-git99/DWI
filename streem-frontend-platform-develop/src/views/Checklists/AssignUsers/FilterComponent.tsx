import React, { useCallback, useState } from 'react';
import { Button, TextInput } from '#components';
import IntegrateUser from './IntegrateUsers';
import { defaultParams } from '#services/users';
import { debounce } from 'lodash';
import { Search } from '@material-ui/icons';
import checkPermission from '#services/uiPermissions';

export const FilterComponent = ({ id, list, reset, fetchData, tab, status }: any) => {
  const [createJobDrawerVisible, setCreateJobDrawerVisible] = useState(false);

  const handleClick = useCallback(() => {
    setCreateJobDrawerVisible(true);
  }, [setCreateJobDrawerVisible]);

  return (
    <div className="before-table-wrapper">
      <div className="filters">
        <TextInput
          AfterElement={Search}
          afterElementWithoutError
          afterElementClass=""
          placeholder={`${tab === 'users' ? ' Search Users Name' : 'Search Group Name'}`}
          onChange={debounce(({ value }) => {
            if (tab === 'users') {
              reset({
                params: {
                  ...defaultParams(false),
                  query: value,
                },
              });
            } else {
              reset({
                params: {
                  page: 0,
                  query: value,
                },
              });
            }
          }, 500)}
        />
      </div>
      {checkPermission(['trainedUsers', 'edit']) && (
        <div className="actions">
          <Button onClick={handleClick} disabled={status !== 'success'}>
            {tab === 'users' ? 'Assign User' : 'Add User Group'}
          </Button>
        </div>
      )}
      {createJobDrawerVisible && (
        <IntegrateUser
          onCloseDrawer={setCreateJobDrawerVisible}
          checklistId={id}
          list={list}
          fetchData={fetchData}
          tab={tab}
        />
      )}
    </div>
  );
};
