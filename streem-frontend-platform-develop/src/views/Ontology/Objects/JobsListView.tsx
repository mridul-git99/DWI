import React, { FC } from 'react';
import JobsListContainer from './JobsListContainer';
import { StyledTabs } from '#components/shared/StyledTabs';
import { FilterOperators } from '#utils/globalTypes';
import { AssignedJobStates, UnassignedJobStates } from '#views/Jobs/ListView/types';
import { endOfToday, getUnixTime } from 'date-fns';
import styled from 'styled-components';
import { getQuickFilter } from '#utils/smartFilterUtils';
import { QuickFilter } from '#views/Inbox/ListView/types';

const JobsWrapper = styled.div`
  background-color: #ffffff;
  padding: 8px;
  height: 100%;
  display: flex;

  .job-list {
    padding: 0;
  }

  .MuiTabs-root {
    min-height: 40px;
  }

  .MuiTabs-indicator {
    height: 2px;
  }

  .jobs-tabs-list {
    .MuiTab-root {
      max-width: 160px;
      padding: 0px 12px;
      min-height: 40px;
    }
  }

  .jobs-tabs {
    width: 100%;

    .MuiTabs-flexContainer {
      border-bottom: 1px solid #e0e0e0 !important;
      button {
        border-bottom: none !important;

        &.Mui-selected {
          background: #ffffff !important;
          .MuiTab-wrapper {
            font-weight: 600 !important;
          }
        }
      }
    }
  }

  .jobs-tabs-panel {
    padding: 8px 0px 0px;
    height: calc(100% - 49px);
  }
`;

const jobTabsList = [
  {
    label: 'Not Started',
    filters: [
      {
        field: 'state',
        op: FilterOperators.ANY,
        values: [UnassignedJobStates.UNASSIGNED, AssignedJobStates.ASSIGNED],
      },
    ],
  },
  {
    label: 'Ongoing',
    filters: getQuickFilter(QuickFilter.ONGOING),
  },
  {
    label: 'Due Today',
    filters: [
      {
        field: 'state',
        op: FilterOperators.ANY,
        values: [
          AssignedJobStates.IN_PROGRESS,
          AssignedJobStates.BLOCKED,
          AssignedJobStates.ASSIGNED,
          UnassignedJobStates.UNASSIGNED,
        ],
      },
      {
        field: 'expectedEndDate',
        op: FilterOperators.LT,
        values: [getUnixTime(endOfToday()).toString()],
      },
    ],
  },
  {
    label: 'Overdue',
    filters: [
      {
        field: 'state',
        op: FilterOperators.ANY,
        values: [
          AssignedJobStates.IN_PROGRESS,
          AssignedJobStates.BLOCKED,
          AssignedJobStates.ASSIGNED,
          UnassignedJobStates.UNASSIGNED,
        ],
      },
      {
        field: 'expectedStartDate',
        op: FilterOperators.GT,
        values: [getUnixTime(endOfToday()).toString()],
      },
    ],
  },
  {
    label: 'Completed',
    filters: getQuickFilter(QuickFilter.COMPLETED),
  },
];

const JobsListView: FC = () => {
  const jobsTabs = () => {
    return (
      <JobsWrapper>
        <StyledTabs
          containerProps={{
            className: 'jobs-tabs',
          }}
          tabListProps={{
            className: 'jobs-tabs-list',
          }}
          panelsProps={{
            className: 'jobs-tabs-panel',
          }}
          tabs={jobTabsList.map((tab, index) => ({
            value: index.toString(),
            label: tab.label,
            panelContent: <JobsListContainer filters={tab.filters} />,
          }))}
          queryString="objectJobsTab"
        />
      </JobsWrapper>
    );
  };

  return <div style={{ overflow: 'auto', flex: 1 }}>{jobsTabs()}</div>;
};

export default JobsListView;
