import { GeneralHeader } from '#components';
import useTabs from '#components/shared/useTabs';
import { useTypedSelector } from '#store';
import { FilterOperators } from '#utils/globalTypes';
import { getQuickFiltersForCompleteJobs, getQuickFiltersForJobs } from '#utils/smartFilterUtils';
import React, { FC } from 'react';
import JobsContent from './JobsContent';
import { ViewWrapper } from './styles';
import { AssignedJobStates, CompletedJobStates, ListViewProps, UnassignedJobStates } from './types';

const JobListView: FC<ListViewProps> = ({ location }) => {
  const { selectedUseCase } = useTypedSelector((state) => state.auth);
  const processFilter = location?.state?.processFilter
    ? location?.state?.processFilter?.schedulerId
      ? [
          {
            field: 'checklist.id',
            op: FilterOperators.EQ,
            values: [location.state.processFilter.processId],
          },
          {
            field: 'schedulerId',
            op: FilterOperators.EQ,
            values: [location.state.processFilter.schedulerId],
          },
        ]
      : [
          {
            field: 'checklist.id',
            op: FilterOperators.EQ,
            values: [location.state.processFilter.id],
          },
        ]
    : [];

  const { renderTabHeader, renderTabContent } = useTabs({
    tabs: [
      {
        id: '0',
        label: 'Created',
        values: {
          baseFilters: [
            {
              field: 'state',
              op: FilterOperators.ANY,
              values: [UnassignedJobStates.UNASSIGNED, AssignedJobStates.ASSIGNED],
            },
            ...processFilter,
          ],
          quickFilters: getQuickFiltersForJobs(),
          processFilter: location?.state?.processFilter,
          activeTabValue: '0',
        },
        tabContent: JobsContent,
      },
      {
        id: '1',
        label: 'On Going',
        values: {
          baseFilters: [
            {
              field: 'state',
              op: FilterOperators.ANY,
              values: [AssignedJobStates.IN_PROGRESS, AssignedJobStates.BLOCKED],
            },
            ...processFilter,
          ],
          quickFilters: getQuickFiltersForJobs(),
          processFilter: location?.state?.processFilter,
          activeTabValue: '1',
        },
        tabContent: JobsContent,
      },
      {
        id: '2',
        label: 'Completed',
        values: {
          baseFilters: [
            {
              field: 'state',
              op: FilterOperators.ANY,
              values: [CompletedJobStates.COMPLETED, CompletedJobStates.COMPLETED_WITH_EXCEPTION],
            },
            ...processFilter,
          ],
          quickFilters: getQuickFiltersForCompleteJobs(),
          processFilter: location?.state?.processFilter,
          activeTabValue: '2',
        },
        tabContent: JobsContent,
      },
    ],
    useTabIndexFromQuery: true,
  });

  const selectedProcessName = location?.state?.processFilter?.processName;

  return (
    <ViewWrapper>
      <GeneralHeader
        heading={`${selectedUseCase?.label} - Jobs ${
          selectedProcessName ? `for ${selectedProcessName}` : ''
        }`}
      />

      <div className="list-table">
        {renderTabHeader()}
        {renderTabContent()}
      </div>
    </ViewWrapper>
  );
};
export default JobListView;
