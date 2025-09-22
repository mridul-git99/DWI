import { LoadingContainer, Pagination, SearchFilter } from '#components';
import { apiGetChecklists, apiGetJobsByResource } from '#utils/apiUrls';
import { FilterOperators, Pageable } from '#utils/globalTypes';
import JobInfoDrawer from '#views/Jobs/Components/JobInfo';
import JobList from '#views/Jobs/Components/JobList';
import React, { Dispatch, FC, SetStateAction } from 'react';
import DateRangeFilter from '#components/shared/DateRangeFilter';
import { TabContentWrapper } from '#views/Jobs/ListView/styles';
import { Job } from '#views/Jobs/ListView/types';

type TJobsListContentProps = {
  jobs: Job[];
  pagination: Pageable;
  fetchJobs: (additionalParams?: { page?: any }) => void;
  selectedObjectId: string;
  selectedJob: Job;
  setSelectedJob: Dispatch<SetStateAction<any>>;
  loading: boolean;
  applyFilter: (key: string, data: Record<string, any>) => void;
  handleRemoveFilter: (key: string) => void;
  filterValue: (key: string) => Record<string, any>;
  filterField: (key: string) => Record<string, any>;
  searchUrlParamsForProcessNameFilter: Record<string, any>;
  urlParams: Record<string, any>;
};

const JobsListContent: FC<TJobsListContentProps> = ({
  jobs,
  pagination,
  fetchJobs,
  selectedObjectId,
  selectedJob,
  setSelectedJob,
  loading,
  applyFilter,
  handleRemoveFilter,
  filterValue,
  filterField,
  searchUrlParamsForProcessNameFilter,
  urlParams,
}) => {
  return (
    <TabContentWrapper>
      <div className="before-table-wrapper">
        <div className="filters">
          <SearchFilter
            showDropdown
            defaultValue={filterValue('searchQuery')}
            dropdownOptions={[
              {
                label: 'Process Name',
                value: 'name',
                field: 'checklistAncestorId',
                operator: FilterOperators.EQ,
                url: apiGetChecklists(),
                urlFilterField: 'name',
                urlParams: searchUrlParamsForProcessNameFilter,
                labelKey: ['name'],
                valueKey: ['ancestorId'],
              },
              {
                label: 'Job ID',
                value: 'code',
                field: 'code',
                operator: FilterOperators.EQ,
                url: apiGetJobsByResource(selectedObjectId),
                urlFilterField: 'code',
                urlParams,
                labelKey: ['code'],
                valueKey: ['code'],
              },
            ]}
            prefilledSearch={
              filterField('searchQuery')
                ? {
                    field: filterField('searchQuery')?.[0]?.field,
                    value: filterField('searchQuery')?.[0]?.values?.[0],
                  }
                : {}
            }
            showSelectDropdown={true}
            updateFilterFields={(fields) => {
              if (fields.length > 0) {
                const filterFields = fields.map((item) => {
                  const { label, ...rest } = item;
                  return rest;
                });

                applyFilter('searchQuery', {
                  label: fields[0].label,
                  value: filterFields,
                });
              } else {
                handleRemoveFilter('searchQuery');
              }
            }}
          />
          <DateRangeFilter
            filterKey={'rangeFilter'}
            filterValue={filterValue}
            applyFilter={applyFilter}
            removeFilter={handleRemoveFilter}
          />
        </div>
      </div>

      <LoadingContainer
        loading={loading}
        component={
          <>
            <JobList jobs={jobs} view={'Object View'} setSelectedJob={setSelectedJob} />
            <Pagination
              pageable={pagination}
              fetchData={(p) =>
                fetchJobs({
                  page: p.page,
                })
              }
            />
          </>
        }
      />
      {selectedJob && <JobInfoDrawer job={selectedJob} onCloseDrawer={setSelectedJob} />}
    </TabContentWrapper>
  );
};

export default JobsListContent;
