import { TabContentProps } from '#components';
import { useQueryParams } from '#hooks/useQueryParams';
import { setQueryParams } from '#store/audit-log-filters/action';
import { useTypedSelector } from '#store/helpers';
import { DEFAULT_PAGE_NUMBER, DEFAULT_PAGE_SIZE } from '#utils/constants';
import {
  filterFieldsFromQueryParams,
  getFilterInfoFromQueryParams,
  isKeyPresent,
  modifyFilters,
} from '#utils/filtersToQueryParams';
import { FilterOperators, fetchDataParams } from '#utils/globalTypes';
import { getQuickFilter } from '#utils/smartFilterUtils';
import React, { FC, useEffect, useMemo, useState } from 'react';
import { useDispatch } from 'react-redux';
import InboxContent from './InboxContent';
import { fetchInbox } from './actions';
import { InboxState } from './types';

const InboxContentContainer: FC<TabContentProps> = ({
  label,
  activeTabValue,
  values: { baseFilters = [], quickFilters },
}) => {
  const dispatch = useDispatch();
  const { updateQueryParams, getQueryParam } = useQueryParams();

  const {
    inboxListView: { jobs, pageable, loading },
    auth: {
      selectedFacility: { id: facilityId = '' } = {},
      selectedUseCase: { id: useCaseId = '' } = {},
      userId,
    },
    auditLogFilters: { queryParams },
    jobListView: { reRender },
  } = useTypedSelector((state) => state);
  const [selectedJob, setSelectedJob] = useState<any>();

  const inboxTabQueryParamValue = getQueryParam('inboxTab', '0');

  const getQueryFilters = () =>
    getQueryParam('filters') || new URLSearchParams(queryParams?.[userId!]?.[label]).get('filters');

  const inboxFilters = useMemo(
    () =>
      inboxTabQueryParamValue === '0'
        ? getQueryFilters() ||
          `{"quickFilter":{"label":{"label":"Unscheduled","value":"Unscheduled"},"value":[{"field":"expectedStartDate","op":"IS_NOT_SET","values":[]}]}}`
        : getQueryFilters(),
    [inboxTabQueryParamValue, queryParams, label],
  );

  const urlParams = useMemo(
    () => ({
      page: DEFAULT_PAGE_NUMBER,
      size: DEFAULT_PAGE_SIZE,
      sort: 'createdAt,desc',
    }),
    [],
  );

  const searchUrlParamsForJobCodeFilter = useMemo(
    () => ({
      ...urlParams,
      ...(label === InboxState.PENDING_ON_ME && { showPendingOnly: true }),
      filters: {
        op: FilterOperators.AND,
        fields: [...baseFilters],
      },
    }),
    [baseFilters, label, urlParams],
  );

  const searchUrlParamsForProcessNameFilter = useMemo(
    () => ({
      ...urlParams,
      filters: {
        op: FilterOperators.AND,
        fields: [
          { field: 'archived', op: 'EQ', values: [false] },
          { field: 'state', op: 'EQ', values: ['PUBLISHED'] },
          { field: 'useCaseId', op: 'EQ', values: [useCaseId] },
        ],
      },
    }),
    [useCaseId, urlParams],
  );

  const fetchData = (params: fetchDataParams = {}) => {
    const filterFields = inboxFilters ? filterFieldsFromQueryParams(inboxFilters) : [];
    const {
      page = DEFAULT_PAGE_NUMBER,
      filters = [...baseFilters, ...filterFields],
      objectId = filterValue('resource')?.value || '',
    } = params;
    dispatch(
      fetchInbox({
        facilityId,
        page,
        size: 10,
        objectId,
        ...(label === InboxState.PENDING_ON_ME && { showPendingOnly: true }),
        sort: 'createdAt,desc',
        filters: {
          op: FilterOperators.AND,
          fields: [...filters],
        },
      }),
    );
  };

  const getJobStatusFilter = (options: any[]) => {
    return [
      {
        field: 'state',
        op: FilterOperators.ANY,
        values: options.map((option) => getQuickFilter(option.value)[0].values).flat(),
      },
    ];
  };

  const saveQueryParams = (queryString: string) => {
    dispatch(
      setQueryParams({
        userId: userId!,
        filters: {
          [label]: queryString,
        },
      }),
    );
  };

  const handleFilter = ({
    key,
    action,
    data = {},
  }: {
    key: string;
    action: 'add' | 'remove';
    data?: Record<string, any>;
  }) => {
    const updatedFilters = modifyFilters(inboxFilters, key, action, data);

    updateQueryParams({
      newParams: { filters: updatedFilters },
      persistQueryParams: saveQueryParams,
    });
  };

  const handleRemoveFilter = (key: string) => {
    handleFilter({ key, action: 'remove' });
  };

  const applyFilter = (key: string, data: Record<string, any>) => {
    handleFilter({ key, action: 'add', data });
  };

  const filterValue = (key: string) => {
    return getFilterInfoFromQueryParams(inboxFilters, key, 'label');
  };

  const filterField = (key: string) => {
    return getFilterInfoFromQueryParams(inboxFilters, key, 'value');
  };

  const resetFilters = (keys: string[]) => {
    updateQueryParams({
      newParams: { filters: {} },
      paramsToRemove: keys,
      persistQueryParams: saveQueryParams,
    });
  };

  const isKeyPresentInFilters = (key: string) => {
    return isKeyPresent(inboxFilters, key);
  };

  useEffect(() => {
    if (activeTabValue === inboxTabQueryParamValue) {
      fetchData();
    }
  }, [inboxFilters, reRender, useCaseId]);

  return (
    <InboxContent
      label={label}
      searchUrlParamsForJobCodeFilter={searchUrlParamsForJobCodeFilter}
      searchUrlParamsForProcessNameFilter={searchUrlParamsForProcessNameFilter}
      filterValue={filterValue}
      filterField={filterField}
      applyFilter={applyFilter}
      handleRemoveFilter={handleRemoveFilter}
      isKeyPresentInFilters={isKeyPresentInFilters}
      getJobStatusFilter={getJobStatusFilter}
      quickFilters={quickFilters}
      loading={loading}
      jobs={jobs}
      pageable={pageable}
      selectedJob={selectedJob}
      setSelectedJob={setSelectedJob}
      fetchData={fetchData}
      resetFilters={resetFilters}
      inboxFilters={inboxFilters}
    />
  );
};

export default InboxContentContainer;
