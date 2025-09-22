import reset from '#assets/svg/reset-filter.svg';
import {
  LoadingContainer,
  Pagination,
  ResourceFilter,
  SearchFilter,
  Select,
  ToggleSwitch,
} from '#components';
import DateRangeFilter from '#components/shared/DateRangeFilter';
import Option from '#components/shared/DropdownCheckboxOption';
import { createFetchList } from '#hooks/useFetchData';
import { User } from '#services/users';
import { useTypedSelector } from '#store';
import { apiGetChecklists, apiGetUsers, apiInboxAutoSuggest } from '#utils/apiUrls';
import {
  ALL_FACILITY_ID,
  DEFAULT_PAGE_NUMBER,
  DEFAULT_PAGE_SIZE,
  SYSTEM_USER,
} from '#utils/constants';
import { FilterOperators } from '#utils/globalTypes';
import { quickFilterStyles } from '#utils/selectStyleUtils';
import { generateUserSearchFilters, getQuickFilter } from '#utils/smartFilterUtils';
import { getFullName } from '#utils/stringUtils';
import JobInfoDrawer from '#views/Jobs/Components/JobInfo';
import JobList from '#views/Jobs/Components/JobList';
import { TabContentWrapper } from '#views/Jobs/ListView/styles';
import ArrowDropDownIcon from '@material-ui/icons/ArrowDropDown';
import { debounce } from 'lodash';
import React, { FC, useCallback, useMemo } from 'react';
import { InboxState, QuickFilter } from './types';

const DropdownIcon = () => <ArrowDropDownIcon fontSize="small" />;

const InboxContent: FC<any> = ({
  label,
  searchUrlParamsForJobCodeFilter,
  searchUrlParamsForProcessNameFilter,
  filterValue,
  filterField,
  applyFilter,
  handleRemoveFilter,
  isKeyPresentInFilters,
  getJobStatusFilter,
  quickFilters,
  setSelectedJob,
  selectedJob,
  jobs,
  pageable,
  loading,
  fetchData,
  resetFilters,
  inboxFilters,
}) => {
  const isFilterApplied = inboxFilters ? Object.keys(JSON.parse(inboxFilters)).length > 0 : false;
  const facilityId = useTypedSelector((state) => state.auth.selectedFacility?.id);
  const authDetails = useTypedSelector((state) => state.auth);

  const userState = facilityId === ALL_FACILITY_ID ? 'authors/global' : 'authors';
  const {
    list: usersList,
    reset: resetUsers,
    fetchNext: fetchNextUsers,
  } = createFetchList<User[]>(
    apiGetUsers(userState),
    { page: DEFAULT_PAGE_NUMBER, size: DEFAULT_PAGE_SIZE },
    true,
  );

  const filterUsers = useMemo(() => {
    return (users: User[]) => {
      return [
        SYSTEM_USER,
        ...users.map((user) => ({
          ...user,
          label: `${getFullName(user)}, ID : ${user.employeeId}`,
          value: user.id,
        })),
      ];
    };
  }, [usersList]);

  const isFilterByCurrentUser = useMemo(() => {
    return filterValue('createdBy')?.value === authDetails.userId;
  }, [filterValue, authDetails]);

  const handleFilterUpdate = useCallback(
    ({ data, filterKey, fieldKey, isToggle = false }: any) => {
      if (isToggle && data) {
        applyFilter(filterKey, {
          label: {
            label: `${authDetails.firstName} ${authDetails.lastName}, ID: ${authDetails.employeeId}`,
            value: authDetails.userId,
          },
          value: {
            field: fieldKey,
            op: 'ANY',
            values: [authDetails.userId],
          },
        });
      } else if (data) {
        applyFilter(filterKey, {
          label: {
            label: data.label,
            value: data.value,
          },
          value: {
            field: fieldKey,
            op: 'ANY',
            values: [data.value],
          },
        });
      } else {
        handleRemoveFilter(filterKey);
      }
    },
    [authDetails, applyFilter, handleRemoveFilter],
  );

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
                url: apiInboxAutoSuggest(),
                urlFilterField: 'code',
                urlParams: searchUrlParamsForJobCodeFilter,
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
            updateFilterFields={(fields: any[]) => {
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
          <ResourceFilter
            onChange={(data: any) => {
              applyFilter('resource', {
                label: {
                  label: data.label,
                  value: data.id,
                },
              });
            }}
            onClear={() => {
              handleRemoveFilter('resource');
            }}
            DropdownIcon={DropdownIcon}
            defaultValue={filterValue('resource')}
          />
          <DateRangeFilter
            filterKey={'rangeFilter'}
            filterValue={filterValue}
            applyFilter={applyFilter}
            removeFilter={handleRemoveFilter}
            isDisabled={isKeyPresentInFilters('quickFilter')}
          />
          <Select
            placeholder="Quick Filters"
            floatingLabel={true}
            styles={quickFilterStyles(filterValue('quickFilter')?.value)}
            components={{ DropdownIndicator: DropdownIcon, IndicatorSeparator: null }}
            options={quickFilters}
            value={filterValue('quickFilter')}
            onChange={(data) => {
              if (data) {
                applyFilter('quickFilter', {
                  label: {
                    label: data.label,
                    value: data.value,
                  },
                  value: getQuickFilter(data.value, true),
                });
              } else {
                handleRemoveFilter('quickFilter');
              }
            }}
            isClearable={true}
            isDisabled={isKeyPresentInFilters('rangeFilter') || isKeyPresentInFilters('jobStatus')}
          />

          {label === InboxState.ALL_JOBS && (
            <Select
              placeholder="Job Status"
              floatingLabel={true}
              options={[QuickFilter.NOT_STARTED, QuickFilter.ONGOING, QuickFilter.COMPLETED].map(
                (value) => ({ label: value, value }),
              )}
              components={{ Option, DropdownIndicator: DropdownIcon, IndicatorSeparator: null }}
              closeMenuOnSelect={false}
              hideSelectedOptions={false}
              value={filterValue('jobStatus')}
              onChange={(options) => {
                if (options.length > 0) {
                  applyFilter('jobStatus', {
                    label: options.map((option: any) => {
                      return {
                        label: option.label,
                        value: option.value,
                      };
                    }),
                    value: getJobStatusFilter(options),
                  });
                } else {
                  handleRemoveFilter('jobStatus');
                }
              }}
              isClearable={true}
              isMulti={true}
              isDisabled={isKeyPresentInFilters('quickFilter')}
            />
          )}
          <Select
            placeholder="Job Created By"
            floatingLabel={true}
            styles={quickFilterStyles(filterValue('createdBy')?.value)}
            components={{ DropdownIndicator: DropdownIcon, IndicatorSeparator: null }}
            options={filterUsers(usersList)}
            value={filterValue('createdBy')}
            onChange={(data) =>
              handleFilterUpdate({ data, filterKey: 'createdBy', fieldKey: 'createdBy.id' })
            }
            onInputChange={debounce((value, actionMeta) => {
              if (value !== actionMeta.prevInputValue)
                resetUsers({
                  params: {
                    page: DEFAULT_PAGE_NUMBER,
                    size: DEFAULT_PAGE_SIZE,
                    ...(value && {
                      filters: generateUserSearchFilters(FilterOperators.LIKE, value),
                    }),
                  },
                });
            }, 500)}
            onMenuScrollToBottom={fetchNextUsers}
            isClearable={true}
          />
          <ToggleSwitch
            checkedIcon={false}
            uncheckedIcon={false}
            offLabel="Created by me"
            onLabel="Created by me"
            checked={isFilterByCurrentUser}
            onChange={(checked) =>
              handleFilterUpdate({
                data: checked,
                filterKey: 'createdBy',
                fieldKey: 'createdBy.id',
                isToggle: true,
              })
            }
          />
        </div>

        {isFilterApplied && (
          <div className="actions">
            <div
              className="icon-filter"
              onClick={() => {
                resetFilters(['filters']);
              }}
            >
              <img src={reset} />
            </div>
          </div>
        )}
      </div>
      <LoadingContainer
        loading={loading}
        component={
          <>
            <JobList jobs={jobs} setSelectedJob={setSelectedJob} view="Inbox" label={label} />
          </>
        }
      />
      <Pagination pageable={pageable} fetchData={fetchData} />
      {selectedJob && (
        <JobInfoDrawer job={selectedJob} onCloseDrawer={setSelectedJob} isInboxView={true} />
      )}
    </TabContentWrapper>
  );
};

export default InboxContent;
