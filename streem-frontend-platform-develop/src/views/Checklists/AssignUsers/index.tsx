import { createFetchList } from '#hooks/useFetchData';
import { defaultParams } from '#services/users';
import { apiGetAllTrainedUsersAssignedToChecklist } from '#utils/apiUrls';
import { DEFAULT_PAGE_NUMBER, DEFAULT_PAGE_SIZE } from '#utils/constants';
import { TabContentWrapper } from '#views/Jobs/ListView/styles';
import React, { useCallback } from 'react';
import { FilterComponent } from './FilterComponent';
import { TableComponent } from './TableComponent';

const renderFilterParams = (tab: string) => {
  return {
    ...defaultParams(),
    users: tab === 'users',
    userGroups: tab === 'groups',
  };
};

export const TrainedUsers = ({ values }: any) => {
  const { id, tab } = values;
  const { list, reset, pagination, status }: any = createFetchList(
    apiGetAllTrainedUsersAssignedToChecklist(id),
    renderFilterParams(tab),
    true,
  );

  const fetchData = useCallback(
    ({ page = DEFAULT_PAGE_NUMBER, size = DEFAULT_PAGE_SIZE }: any) => {
      reset({
        params: {
          page,
          size,
        },
      });
    },
    [reset],
  );

  return (
    <TabContentWrapper>
      <FilterComponent
        id={id}
        reset={reset}
        list={list}
        fetchData={fetchData}
        tab={tab}
        status={status}
      />
      <TableComponent
        currentPageData={list}
        pageable={pagination}
        fetchData={fetchData}
        checklistId={id}
        tab={tab}
        status={status}
      />
    </TabContentWrapper>
  );
};
