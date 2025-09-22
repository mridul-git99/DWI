import { StyledTabs } from '#components';
import PaginationSummary from '#components/shared/PaginationSummary';
import { useTypedSelector } from '#store/helpers';
import { FilterOperators } from '#utils/globalTypes';
import { getQuickFiltersForInbox } from '#utils/smartFilterUtils';
import { InboxJobsWrapper, InboxWrapper } from '#views/Inbox/styles';
import { AssignedJobStates } from '#views/Jobs/ListView/types';
import React, { FC, useEffect } from 'react';
import { useDispatch } from 'react-redux';
import InboxContentContainer from './InboxContentContainer';
import { resetInbox } from './actions';
import { InboxState, ListViewProps, QuickFilter } from './types';

const ListView: FC<ListViewProps> = () => {
  const {
    inboxListView: { pageable },
    auth: { selectedUseCase },
  } = useTypedSelector((state) => state);

  const dispatch = useDispatch();

  useEffect(() => {
    return () => {
      dispatch(resetInbox());
    };
  }, []);

  const jobTabs = () => {
    return (
      <InboxJobsWrapper>
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
          tabs={[
            {
              value: '0',
              label: InboxState.PENDING_ON_ME,
              panelContent: (
                <InboxContentContainer
                  label={InboxState.PENDING_ON_ME}
                  values={{
                    baseFilters: [
                      {
                        field: 'state',
                        op: FilterOperators.ANY,
                        values: [
                          AssignedJobStates.IN_PROGRESS,
                          AssignedJobStates.BLOCKED,
                          AssignedJobStates.ASSIGNED,
                        ],
                      },
                      {
                        field: 'useCaseId',
                        op: FilterOperators.EQ,
                        values: [selectedUseCase?.id],
                      },
                    ],
                    quickFilters: [
                      {
                        label: QuickFilter.NOT_STARTED,
                        value: QuickFilter.NOT_STARTED,
                      },
                      ...getQuickFiltersForInbox(),
                    ],
                  }}
                />
              ),
            },
            {
              value: '1',
              label: InboxState.ALL_JOBS,
              panelContent: (
                <InboxContentContainer
                  label={InboxState.ALL_JOBS}
                  values={{
                    baseFilters: [
                      {
                        field: 'useCaseId',
                        op: FilterOperators.EQ,
                        values: [selectedUseCase?.id],
                      },
                    ],
                    quickFilters: [
                      ...getQuickFiltersForInbox(),
                      ...[QuickFilter.COMPLETED, QuickFilter.COMPLETED_WITH_EXCEPTION].map(
                        (filter) => {
                          return {
                            label: filter,
                            value: filter,
                          };
                        },
                      ),
                    ],
                  }}
                />
              ),
            },
          ]}
          afterHeader={<PaginationSummary pageable={pageable} entityName="jobs" />}
          queryString="inboxTab"
        />
      </InboxJobsWrapper>
    );
  };

  return <InboxWrapper>{jobTabs()}</InboxWrapper>;
};

export default ListView;
