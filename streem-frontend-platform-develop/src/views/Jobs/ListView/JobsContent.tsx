import { ComposerEntity } from '#PrototypeComposer/types';
import DownloadIcon from '#assets/svg/DownloadIcon.svg';
import {
  Button,
  LoadingContainer,
  Pagination,
  ResourceFilter,
  SearchFilter,
  Select,
  TabContentProps,
} from '#components';
import { showNotification } from '#components/Notification/actions';
import { NotificationType } from '#components/Notification/types';
import { openOverlayAction } from '#components/OverlayContainer/actions';
import { OverlayNames } from '#components/OverlayContainer/types';
import Tooltip from '#components/shared/Tooltip';
import checkPermission, { isFeatureAllowed } from '#services/uiPermissions';
import { useTypedSelector } from '#store/helpers';
import { Checklist } from '#types';
import {
  ALL_FACILITY_ID,
  DEFAULT_PAGE_NUMBER,
  DEFAULT_PAGE_SIZE,
  EXPORT_LIMIT_LEVELS,
  SYSTEM_USER,
} from '#utils/constants';
import { FilterField, FilterOperators, fetchDataParams } from '#utils/globalTypes';
import { quickFilterStyles } from '#utils/selectStyleUtils';
import { generateUserSearchFilters, getQuickFilter } from '#utils/smartFilterUtils';
import { fetchChecklists } from '#views/Checklists/ListView/actions';
import { QuickFilter } from '#views/Inbox/ListView/types';
import JobList from '#views/Jobs/Components/JobList';
import { debounce } from 'lodash';
import React, { FC, useEffect, useMemo, useState } from 'react';
import { useDispatch } from 'react-redux';
import CreateJob from '../Components/CreateJob';
import JobInfoDrawer from '../Components/JobInfo';
import { fetchJobs, fetchJobsExcel } from './actions';
import { TabContentWrapper } from './styles';
import { CompletedJobStates } from './types';
import { createFetchList } from '#hooks/useFetchData';
import { apiGetUsers } from '#utils/apiUrls';
import { getFullName } from '#utils/stringUtils';
import { User } from '#services/users/types';
import DateRangeFilter from '#components/shared/DateRangeFilter';

const JobsContent: FC<TabContentProps> = ({
  values: { baseFilters, quickFilters, processFilter, activeTabValue },
}) => {
  const dispatch = useDispatch();
  const {
    jobListView: { jobs, pageable, loading, reRender },
    auth: { selectedFacility, selectedUseCase },
    checklistListView: { checklists, pageable: checklistPageable, loading: checklistsLoading },
  } = useTypedSelector((state) => state);

  const facilityId = useTypedSelector((state) => state.auth.selectedFacility?.id);

  const jobsBaseFilterFields = useMemo(() => {
    return [
      ...(baseFilters || []),
      {
        field: 'useCaseId',
        op: FilterOperators.EQ,
        values: [selectedUseCase?.id],
      },
    ];
  }, [baseFilters, selectedUseCase]);

  const [createJobDrawerVisible, setCreateJobDrawerVisible] = useState(false);
  const [filterFields, setFilterFields] = useState<FilterField[]>(jobsBaseFilterFields);
  const [searchFilterFields, setSearchFilterFields] = useState<FilterField[]>([]);
  const [selectedJob, setSelectedJob] = useState<any>();
  const [resourceFilter, setResourceFilter] = useState<string>('');
  const [quickFilter, setQuickFilter] = useState<QuickFilter>();
  const [dateFilter, setDateFilter] = useState<any>();

  const userState = useMemo(() => {
    return facilityId === ALL_FACILITY_ID ? 'authors/global' : 'authors';
  }, [facilityId]);

  const {
    list: usersList,
    reset: resetUsers,
    fetchNext: fetchNextUsers,
  } = createFetchList(
    apiGetUsers(userState),
    {
      page: DEFAULT_PAGE_NUMBER,
      size: DEFAULT_PAGE_SIZE,
    },
    false,
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

  const fetchChecklistData = ({
    page = DEFAULT_PAGE_NUMBER,
    size = DEFAULT_PAGE_SIZE,
    query = '',
  }) => {
    const filters = JSON.stringify({
      op: FilterOperators.AND,
      fields: [
        { field: 'state', op: FilterOperators.EQ, values: ['PUBLISHED'] },
        { field: 'archived', op: FilterOperators.EQ, values: [false] },
        ...(query ? [{ field: 'name', op: FilterOperators.LIKE, values: [query] }] : []),
        {
          field: 'useCaseId',
          op: FilterOperators.EQ,
          values: [selectedUseCase?.id],
        },
      ],
    });
    dispatch(fetchChecklists({ page, size, filters, sort: 'id' }, page === 0));
  };

  const fetchData = (params: fetchDataParams = {}) => {
    const {
      page = DEFAULT_PAGE_NUMBER,
      size = DEFAULT_PAGE_SIZE,
      filters = filterFields,
      objectId = resourceFilter,
    } = params;
    dispatch(
      fetchJobs({
        page,
        size,
        sort: 'createdAt,desc',
        objectId,
        filters: {
          op: FilterOperators.AND,
          fields: [...filters, ...searchFilterFields],
        },
      }),
    );
  };

  useEffect(() => {
    fetchData({ filters: jobsBaseFilterFields });
  }, [selectedUseCase]);

  useEffect(() => {
    fetchData({ filters: filterFields });
  }, [JSON.stringify(filterFields), reRender, resourceFilter, searchFilterFields]);

  useEffect(() => {
    if (activeTabValue === '2') {
      if (
        quickFilter === QuickFilter.COMPLETED ||
        quickFilter === QuickFilter.COMPLETED_WITH_EXCEPTION
      ) {
        setFilterFields((currentFields) => {
          const filteredFields = currentFields
            ? currentFields.filter((field) => field.field !== 'state')
            : [];
          return [...filteredFields, ...getQuickFilter(quickFilter, true)];
        });
      } else {
        setFilterFields((currentFields) => {
          const filteredFields = currentFields
            ? currentFields.filter((field) => field.field !== 'state')
            : [];
          return [
            ...filteredFields,
            {
              field: 'state',
              op: FilterOperators.ANY,
              values: [CompletedJobStates.COMPLETED, CompletedJobStates.COMPLETED_WITH_EXCEPTION],
            },
          ];
        });
      }
    } else {
      if (quickFilter) {
        setFilterFields((currentFields) => {
          const filteredFields = currentFields
            ? currentFields.filter(
                (field) => field.field !== 'expectedStartDate' && field.field !== 'expectedEndDate',
              )
            : [];
          return [...filteredFields, ...getQuickFilter(quickFilter, true)];
        });
      } else {
        setFilterFields((currentFields) =>
          currentFields.filter(
            (field) => field.field !== 'expectedStartDate' && field.field !== 'expectedEndDate',
          ),
        );
      }
    }
  }, [quickFilter]);

  const handleOnCreateJob = () => {
    if (selectedFacility?.id === ALL_FACILITY_ID) {
      dispatch(
        openOverlayAction({
          type: OverlayNames.ENTITY_START_ERROR_MODAL,
          props: {
            entity: ComposerEntity.JOB,
          },
        }),
      );
    } else {
      setCreateJobDrawerVisible(true);
    }
  };

  const onSelectUpdate = (option: Checklist) => {
    if (option) {
      const selectedFilterField = {
        field: 'checklistAncestorId',
        op: FilterOperators.EQ,
        values: [option.ancestorId],
      };
      setFilterFields((currentFields) => {
        const updatedFilterFields = [
          ...currentFields.filter((field) => field.field !== selectedFilterField?.field),
          selectedFilterField,
        ];
        return updatedFilterFields;
      });
    } else {
      setFilterFields((currentFields) =>
        currentFields.filter((curr) => curr.field !== 'checklistAncestorId'),
      );
    }
  };

  const onSetDate = (jobId: string) => {
    dispatch(
      openOverlayAction({
        type: OverlayNames.SET_DATE,
        props: {
          jobId,
        },
      }),
    );
  };

  const handleMenuScrollToBottom = () => {
    if (!checklistPageable.last) fetchChecklistData({ page: checklistPageable.page + 1 });
  };

  const onChildChange = (option: any) => {
    setResourceFilter(option.id);
  };

  const applyDateRangeFilter = (_: string, data: any) => {
    setDateFilter(data.label);
    setFilterFields((currentFields) => {
      const updatedFilterFields = [
        ...currentFields.filter((field) => field.field !== 'startedAt'),
        ...data.value,
      ];
      return updatedFilterFields;
    });
  };

  const removeDateRangeFilter = (_: string) => {
    setDateFilter(null);
    setFilterFields((currentFields) =>
      currentFields.filter((field) => field.field !== 'startedAt'),
    );
  };

  const handleDownload = () => {
    if (pageable.totalElements > EXPORT_LIMIT_LEVELS.HIGH) {
      dispatch(
        showNotification({
          type: NotificationType.ERROR,
          msg: `Cannot export ${pageable.totalElements.toLocaleString()} jobs. Maximum export limit is ${EXPORT_LIMIT_LEVELS.HIGH.toLocaleString()} jobs. Please apply filters to reduce the dataset.`,
        }),
      );
      return;
    }

    const filters = {
      op: FilterOperators.AND,
      fields: [...filterFields, ...searchFilterFields],
    };

    const tabName =
      activeTabValue === '0' ? 'created' : activeTabValue === '1' ? 'on-going' : 'completed';
    const timestamp = Date.now();
    const filename = `Jobs_${selectedFacility?.name}_${selectedUseCase?.label}_${tabName}_${timestamp}.xlsx`;

    dispatch(
      fetchJobsExcel({
        filters: JSON.stringify(filters),
        filename: filename,
        objectId: resourceFilter,
      }),
    );
  };

  const hasJobsToExport = pageable.totalElements > 0;
  const exportTooltipText = hasJobsToExport ? 'Export Jobs' : 'No jobs to export';

  return (
    <TabContentWrapper>
      <div className="before-table-wrapper">
        <div className="filters">
          <SearchFilter
            showDropdown
            dropdownOptions={[
              {
                label: 'Process Name',
                value: 'checklist.name',
                field: 'checklist.name',
                operator: FilterOperators.LIKE,
              },
              {
                label: 'Job ID',
                value: 'code',
                field: 'code',
                operator: FilterOperators.LIKE,
              },
            ]}
            updateFilterFields={(fields) => setSearchFilterFields(fields)}
          />
          <Select
            backspaceRemovesValue={false}
            hideSelectedOptions={false}
            onMenuOpen={() => {
              fetchChecklistData({ page: DEFAULT_PAGE_NUMBER });
            }}
            onChange={(newValue) => {
              onSelectUpdate(newValue);
            }}
            isLoading={checklistsLoading}
            onInputChange={debounce((searchedValue: string, actionMeta) => {
              if (searchedValue !== actionMeta.prevInputValue)
                fetchChecklistData({ page: DEFAULT_PAGE_NUMBER, query: searchedValue });
            }, 500)}
            options={checklists.map((currList) => ({ ...currList, label: currList.name }))}
            placeholder="Processes"
            tabSelectsValue={false}
            onMenuScrollToBottom={handleMenuScrollToBottom}
            optional
            isDisabled={!!processFilter}
            {...(processFilter && {
              defaultValue: [{ label: processFilter.processName, value: processFilter.id }],
            })}
            showTooltip={true}
          />
          <ResourceFilter onChange={onChildChange} onClear={() => setResourceFilter('')} />
          <DateRangeFilter
            filterKey="rangeFilter"
            filterValue={() => {
              return dateFilter;
            }}
            applyFilter={applyDateRangeFilter}
            removeFilter={removeDateRangeFilter}
            isDisabled={!!quickFilter}
          />
          <Select
            placeholder="Quick Filters"
            styles={quickFilterStyles(quickFilter)}
            options={quickFilters}
            onChange={(data) => {
              if (data) {
                setQuickFilter(data.value);
              } else {
                setQuickFilter(undefined);
              }
            }}
            isClearable={true}
            isDisabled={!!dateFilter}
          />
          <Select
            placeholder="Job Created By"
            floatingLabel={true}
            options={filterUsers(usersList)}
            onChange={(data) => {
              if (data) {
                setFilterFields((currentFields) => {
                  const updatedFilterFields = [
                    ...currentFields.filter((field) => field.field !== 'createdBy.id'),
                    {
                      field: 'createdBy.id',
                      op: FilterOperators.EQ,
                      values: [data.value],
                    },
                  ];
                  return updatedFilterFields;
                });
              } else {
                setFilterFields((currentFields) =>
                  currentFields.filter((curr) => curr.field !== 'createdBy.id'),
                );
              }
            }}
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
            onMenuOpen={() => {
              resetUsers({
                params: {
                  page: DEFAULT_PAGE_NUMBER,
                  size: DEFAULT_PAGE_SIZE,
                },
              });
            }}
            onMenuScrollToBottom={fetchNextUsers}
            isClearable={true}
          />

          {processFilter?.schedulerId && (
            <Select
              isDisabled={!!processFilter?.schedulerId}
              {...(processFilter && {
                defaultValue: [
                  { label: processFilter.schedulerName, value: processFilter.schedulerId },
                ],
              })}
            />
          )}
          {isFeatureAllowed('jobsDownload') && (
            <Tooltip title={exportTooltipText} arrow>
              <span>
                <Button variant="textOnly" onClick={handleDownload} disabled={!hasJobsToExport}>
                  <img src={DownloadIcon} alt="Download icon" className="icon" />
                </Button>
              </span>
            </Tooltip>
          )}
        </div>
        {!processFilter &&
          !(baseFilters[0] in CompletedJobStates) &&
          checkPermission(['checklists', 'createJob']) && (
            <div className="actions">
              <Button onClick={handleOnCreateJob}>Create a Job</Button>
            </div>
          )}
      </div>
      <LoadingContainer
        loading={loading}
        component={
          <>
            <JobList
              jobs={jobs}
              setSelectedJob={setSelectedJob}
              view="Jobs"
              onSetDate={onSetDate}
            />
            <Pagination pageable={pageable} fetchData={fetchData} />
          </>
        }
      />
      {createJobDrawerVisible && <CreateJob onCloseDrawer={setCreateJobDrawerVisible} />}
      {selectedJob && <JobInfoDrawer job={selectedJob} onCloseDrawer={setSelectedJob} />}
    </TabContentWrapper>
  );
};

export default JobsContent;
