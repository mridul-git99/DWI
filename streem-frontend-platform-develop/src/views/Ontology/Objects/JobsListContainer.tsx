import React, { FC, useEffect, useMemo, useState } from 'react';
import { FilterField, FilterOperators } from '#utils/globalTypes';
import { useTypedSelector } from '#store';
import { DEFAULT_PAGE_NUMBER, DEFAULT_PAGE_SIZE } from '#utils/constants';
import { createFetchList } from '#hooks/useFetchData';
import { apiGetJobsByResource } from '#utils/apiUrls';
import JobsListContent from './JobsListContent';
import {
  filterFieldsFromQueryParams,
  getFilterInfoFromQueryParams,
  modifyFilters,
} from '#utils/filtersToQueryParams';
import { useQueryParams } from '#hooks/useQueryParams';

type TJobsListContainerProps = {
  filters: FilterField[];
};

const JobsListContainer: FC<TJobsListContainerProps> = ({ filters }) => {
  const reRender = useTypedSelector((state) => state.jobListView.reRender);
  const selectedObject = useTypedSelector((state) => state.ontology.objects.active);
  const useCaseId = useTypedSelector((state) => state.auth.selectedUseCase?.id || '');

  const { updateQueryParams, getQueryParam } = useQueryParams();

  const [selectedJob, setSelectedJob] = useState<any>();

  const urlParams = useMemo(
    () => ({
      page: DEFAULT_PAGE_NUMBER,
      size: DEFAULT_PAGE_SIZE,
      sort: 'createdAt,desc',
      filters: {
        op: FilterOperators.AND,
        fields: [
          {
            field: 'useCaseId',
            op: FilterOperators.EQ,
            values: [useCaseId],
          },
          ...filters,
        ],
      },
    }),
    [filters, useCaseId],
  );

  const searchUrlParamsForProcessNameFilter = useMemo(
    () => ({
      page: DEFAULT_PAGE_NUMBER,
      size: DEFAULT_PAGE_SIZE,
      sort: 'createdAt,desc',
      filters: {
        op: FilterOperators.AND,
        fields: [
          { field: 'archived', op: 'EQ', values: [false] },
          { field: 'state', op: 'EQ', values: ['PUBLISHED'] },
          { field: 'useCaseId', op: 'EQ', values: [useCaseId] },
        ],
      },
    }),
    [useCaseId],
  );

  const { list, reset, status, pagination } = createFetchList(
    apiGetJobsByResource(selectedObject!.id),
    urlParams,
    false,
  );

  const jobFilters = getQueryParam('filters');

  const jobFiltersFields = useMemo(
    () => (jobFilters ? filterFieldsFromQueryParams(jobFilters) : []),
    [jobFilters],
  );

  const handleFilter = ({
    key,
    action,
    data = {},
  }: {
    key: string;
    action: 'add' | 'remove';
    data?: Record<string, any>;
  }) => {
    const updatedFilters = modifyFilters(jobFilters, key, action, data);

    updateQueryParams({
      newParams: { filters: updatedFilters },
    });
  };

  const handleRemoveFilter = (key: string) => {
    handleFilter({ key, action: 'remove' });
  };

  const applyFilter = (key: string, data: Record<string, any>) => {
    handleFilter({ key, action: 'add', data });
  };

  const filterValue = (key: string) => {
    return getFilterInfoFromQueryParams(jobFilters, key, 'label');
  };

  const filterField = (key: string) => {
    return getFilterInfoFromQueryParams(jobFilters, key, 'value');
  };

  const fetchJobs = (additionalParams = {}) => {
    reset({
      params: {
        ...urlParams,
        filters: {
          op: FilterOperators.AND,
          fields: [...urlParams.filters.fields, ...jobFiltersFields],
        },
        ...additionalParams,
      },
    });
  };

  useEffect(() => {
    fetchJobs();
  }, [filters, reRender, jobFiltersFields]);

  return (
    <JobsListContent
      jobs={list}
      pagination={pagination}
      fetchJobs={fetchJobs}
      selectedObjectId={selectedObject!.id}
      selectedJob={selectedJob}
      setSelectedJob={setSelectedJob}
      loading={status === 'loading'}
      applyFilter={applyFilter}
      handleRemoveFilter={handleRemoveFilter}
      filterValue={filterValue}
      filterField={filterField}
      searchUrlParamsForProcessNameFilter={searchUrlParamsForProcessNameFilter}
      urlParams={urlParams}
    />
  );
};

export default JobsListContainer;
