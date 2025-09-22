import { FilterProp, Link as GoBack, InfiniteListView } from '#components';
import NestedCheckboxList from '#components/shared/NestedCheckboxList';
import {
  ShowSectionUsersAndUserGroupTabs,
  useUserAndUserGroupTabs,
} from '#hooks/useUserAndUserGroupTabs';
import { fetchComposerData, resetComposer } from '#PrototypeComposer/actions';
import { ComposerEntity } from '#PrototypeComposer/types';
import { useTypedSelector } from '#store';
import { clearAuditLogFilters, setAuditLogFilters } from '#store/audit-log-filters/action';
import { UsersListType } from '#store/users/types';
import { UserFilterWrapper } from '#styles/UserFilterWrapper';
import { apiGetUsers, apiUserGroups } from '#utils/apiUrls';
import { DEFAULT_PAGE_NUMBER, MAX_PAGE_SIZE } from '#utils/constants';
import { FilterOperators, InputTypes, fetchDataParams } from '#utils/globalTypes';
import { getStageTaskOptions } from '#utils/stringUtils';
import { formatDateTime } from '#utils/timeUtils';
import TextField from '@material-ui/core/TextField';
import { Search } from '@material-ui/icons';
import AccessTimeIcon from '@material-ui/icons/AccessTime';
import ArrowDropDownIcon from '@material-ui/icons/ArrowDropDown';
import ReportProblemOutlinedIcon from '@material-ui/icons/ReportProblemOutlined';
import {
  DateRange,
  DateRangeDelimiter,
  LocalizationProvider,
  StaticDateRangePicker,
  TimePicker,
} from '@material-ui/pickers';
import DateFnsUtils from '@material-ui/pickers/adapter/date-fns';
import { RouteComponentProps } from '@reach/router';
import { endOfDay, getHours, getMinutes, getUnixTime, set, startOfDay, subDays } from 'date-fns';
import { groupBy } from 'lodash';
import React, { FC, useEffect, useMemo, useState } from 'react';
import { useDispatch } from 'react-redux';
import { fetchChecklistAuditLogs } from './actions';
import { Composer } from './styles';
import { ChecklistAuditLogsState, ChecklistAuditLogsType } from './types';

type initialState = {
  dateRange: DateRange<Date>;
  appliedFilters: Record<string, boolean>;
  startTime: Date | null;
  endTime: Date | null;
};

// TODO Change appliedUsers, selectedUsers, unSelectedUsers to HashMap as we only need the id's of the users So we can keep it like { userId : boolean }.
const currentDate = startOfDay(new Date());
const initialState: initialState = {
  dateRange: [null, null],
  appliedFilters: {},
  startTime: currentDate,
  endTime: endOfDay(new Date()),
};

type TValuesType = {
  isTrainedUserView: boolean;
  checklistId?: string;
};

type TProps = RouteComponentProps<{ id: string }> & {
  values: TValuesType;
};

const AuditLogs: FC<TProps> = ({ id, values }) => {
  const { isTrainedUserView = false, checklistId } = values;
  const { logs, loading, pageable }: ChecklistAuditLogsState = useTypedSelector(
    (state) => state.prototypeComposer.auditLogs,
  );
  const {
    stages: { listById: stagesListById },
    tasks: { listById: tasksListById, tasksOrderInStage },
  } = useTypedSelector((state) => state.prototypeComposer);
  const {
    bodyContent: bodyContentUserAndUserGroupFilter,
    selectedUserGroups: assigneeSelectedUserGroups,
    selectedUsers: assigneeSelectedUsers,
    resetAll: resetUserAndUserGroupFilter,
  } = useUserAndUserGroupTabs({
    apiUrlUser: () => apiGetUsers(UsersListType.ALL),
    apiUrlUserGroup: () => apiUserGroups(),
    showRoleTag: false,
    showSections: ShowSectionUsersAndUserGroupTabs.BOTH,
  });
  const {
    bodyContent: bodyContentUserFilter,
    selectedUsers: allocatedSelectedUsers,
    resetAll: resetUserFilter,
  } = useUserAndUserGroupTabs({
    apiUrlUser: () => apiGetUsers(UsersListType.ALL),
    apiUrlUserGroup: () => apiUserGroups(),
    showRoleTag: false,
    showSections: ShowSectionUsersAndUserGroupTabs.USERS,
  });

  const dispatch = useDispatch();
  const [state, setstate] = useState(initialState);

  const [checkedItems, setCheckedItems] = useState<Record<string, boolean>>({});
  const taskOptions = useMemo(() => {
    return getStageTaskOptions(stagesListById, tasksListById, tasksOrderInStage);
  }, [stagesListById, tasksListById, tasksOrderInStage]);

  const [selectedTasks, setSelectedTasks] = useState<Record<string, boolean>>({});
  const [filteredOptions, setFilteredOptions] = useState(taskOptions);

  const resetFilter = () => {
    setstate(initialState);
    setCheckedItems({});
    setSelectedTasks({});
    resetUserAndUserGroupFilter();
    resetUserFilter();
    state.dateRange[0] = null;
    dispatch(clearAuditLogFilters());
  };

  const handleTaskCheckboxChange = (taskValue, checked) => {
    const updatedCheckedItems: Record<string, boolean> = { ...checkedItems };
    const updatedSelectedTasks: Record<string, boolean> = { ...selectedTasks };

    filteredOptions.forEach((stage) => {
      stage.options.forEach((task) => {
        if (task.value === taskValue) {
          updatedCheckedItems[taskValue] = checked;
          const allTasksChecked = stage.options.every((t) => updatedCheckedItems[t.value]);
          updatedCheckedItems[stage.value] = allTasksChecked;
          if (checked) {
            updatedSelectedTasks[task.value] = true;
          } else {
            delete updatedSelectedTasks[task.value];
          }
        }
      });
    });

    setCheckedItems(updatedCheckedItems);
    setSelectedTasks(updatedSelectedTasks);
    if (Object.keys(updatedSelectedTasks).length === 0) {
      setstate({
        ...state,
        appliedFilters: {
          ...state.appliedFilters,
          stageTasks: false,
        },
      });
    }
  };

  const handleStageCheckboxChange = (stageValue, checked) => {
    const updatedCheckedItems: Record<string, boolean> = { ...checkedItems };
    const updatedSelectedTasks: Record<string, boolean> = { ...selectedTasks };

    filteredOptions.forEach((stage) => {
      if (stage.value === stageValue) {
        stage.options.forEach((task) => {
          if (checked && !updatedSelectedTasks[task.value]) {
            updatedCheckedItems[task.value] = true;
            updatedSelectedTasks[task.value] = true;
          } else if (!checked) {
            updatedCheckedItems[task.value] = false;
            delete updatedSelectedTasks[task.value];
          }
        });
      }
    });

    updatedCheckedItems[stageValue] = checked;
    setCheckedItems(updatedCheckedItems);
    setSelectedTasks(updatedSelectedTasks);

    if (Object.keys(updatedSelectedTasks).length === 0) {
      setstate({
        ...state,
        appliedFilters: {
          ...state.appliedFilters,
          stageTasks: false,
        },
      });
    }
  };
  const handleSearch = (term: string) => {
    const filtered = taskOptions.flatMap((option) => {
      const stageMatch = option.label.toLowerCase().includes(term.toLowerCase());
      const taskMatches = option.options.filter((task) =>
        task.label.toLowerCase().includes(term.toLowerCase()),
      );

      if (stageMatch) {
        return [
          {
            ...option,
            options: option.options,
          },
        ];
      }

      if (taskMatches.length > 0) {
        return [
          {
            ...option,
            label: option.label,
            options: taskMatches,
          },
        ];
      }

      return [];
    });

    setFilteredOptions(filtered);
  };

  const stageFilterBody = () => {
    return (
      <UserFilterWrapper>
        <div className="top-content">
          <div className="searchboxwrapper">
            <Search className="searchsubmit" />
            <input
              className="searchbox"
              type="text"
              onChange={(e) => handleSearch(e.target.value)}
              placeholder="Search for Tasks"
            />
          </div>
          <span
            onClick={() => {
              setCheckedItems({});
              setSelectedTasks({});
            }}
          >
            Deselect All
          </span>
        </div>
        <div className="scrollable-content">
          <NestedCheckboxList
            options={filteredOptions}
            checkedOptions={checkedItems}
            onStageCheckboxChange={handleStageCheckboxChange}
            onTaskCheckboxChange={handleTaskCheckboxChange}
          />
        </div>
      </UserFilterWrapper>
    );
  };

  const getDateRange = () => {
    if (state.dateRange[0] !== null) {
      return true;
    } else if (state.startTime !== null) {
      state.dateRange[0] = currentDate;
      return true;
    }
    return false;
  };

  const filterProp: FilterProp = {
    filters: [
      {
        label: 'Date/Time Range',
        onApply: () => {
          setstate({
            ...state,
            appliedFilters: {
              ...state.appliedFilters,
              stageTasks: Object.keys(selectedTasks).length > 0,
              assignees: assigneeSelectedUsers.length > 0 || assigneeSelectedUserGroups.length > 0,
              allocatorUsers: allocatedSelectedUsers.length > 0,
              'Date/Time Range': getDateRange(),
            },
          });
        },
        content: (
          <LocalizationProvider dateAdapter={DateFnsUtils}>
            <StaticDateRangePicker
              displayStaticWrapperAs="desktop"
              value={state.dateRange}
              calendars={1}
              onChange={(newValue) => setstate({ ...state, dateRange: newValue })}
              renderInput={(startProps, endProps) => (
                <>
                  <TextField {...startProps} />
                  <DateRangeDelimiter> to </DateRangeDelimiter>
                  <TextField {...endProps} />
                </>
              )}
            />
            <div className="timepicker-container">
              <TimePicker
                renderInput={(props) => <TextField {...props} />}
                ampm={false}
                showToolbar={false}
                label="Start Time"
                value={state.startTime}
                InputProps={{
                  startAdornment: <AccessTimeIcon />,
                }}
                openPickerIcon={<ArrowDropDownIcon />}
                onChange={(newValue) => setstate({ ...state, startTime: newValue })}
              />
              <TimePicker
                renderInput={(props) => <TextField {...props} />}
                ampm={false}
                label="End Time"
                value={state.endTime}
                InputProps={{
                  startAdornment: <AccessTimeIcon />,
                }}
                openPickerIcon={<ArrowDropDownIcon />}
                onChange={(newValue) => setstate({ ...state, endTime: newValue })}
              />
            </div>
          </LocalizationProvider>
        ),
      },
      {
        label: isTrainedUserView ? 'Allocator' : 'Users',
        onApply: () => {
          setstate({
            ...state,
            appliedFilters: {
              ...state.appliedFilters,
              stageTasks: Object.keys(selectedTasks).length > 0,
              assignees: assigneeSelectedUsers.length > 0 || assigneeSelectedUserGroups.length > 0,
              allocatorUsers: allocatedSelectedUsers.length > 0,
              'Date/Time Range': state.dateRange[0] !== null,
            },
          });
        },
        content: function template() {
          return (
            <UserFilterWrapper>
              <div className="scrollable-content scrollable-content-trained-users">
                {bodyContentUserFilter()}
              </div>
            </UserFilterWrapper>
          );
        },
      },
      ...(isTrainedUserView
        ? [
            {
              label: 'Task Assignees',
              onApply: () => {
                setstate({
                  ...state,
                  appliedFilters: {
                    ...state.appliedFilters,
                    stageTasks: Object.keys(selectedTasks).length > 0,
                    assignees:
                      assigneeSelectedUsers.length > 0 || assigneeSelectedUserGroups.length > 0,
                    allocatorUsers: allocatedSelectedUsers.length > 0,
                    'Date/Time Range': state.dateRange[0] !== null,
                  },
                });
              },
              content: function template() {
                return (
                  <UserFilterWrapper>
                    <div className="scrollable-content scrollable-content-trained-users">
                      {bodyContentUserAndUserGroupFilter()}
                    </div>
                  </UserFilterWrapper>
                );
              },
            },
            {
              label: 'Stage',
              onApply: () => {
                setstate({
                  ...state,
                  appliedFilters: {
                    ...state.appliedFilters,
                    stageTasks: Object.keys(selectedTasks).length > 0,
                    assignees:
                      assigneeSelectedUsers.length > 0 || assigneeSelectedUserGroups.length > 0,
                    allocatorUsers: allocatedSelectedUsers.length > 0,
                    'Date/Time Range': state.dateRange[0] !== null,
                  },
                });
              },
              content: stageFilterBody,
            },
          ]
        : []),
    ],
    onReset: () => resetFilter(),
    activeCount: Object.keys(state.appliedFilters).filter((key) => !!state.appliedFilters[key])
      .length,
  };

  useEffect(() => {
    if (checklistId) {
      dispatch(fetchComposerData({ entity: ComposerEntity.CHECKLIST, id: checklistId }));
    }

    return () => {
      dispatch(resetComposer());
    };
  }, [checklistId]);

  useEffect(() => {
    return () => {
      dispatch(clearAuditLogFilters());
    };
  }, []);

  useEffect(() => {
    fetchLogs();
  }, [state.appliedFilters]);

  useEffect(() => {
    if (taskOptions.length > 0) {
      setFilteredOptions(taskOptions);
    }
  }, [taskOptions]);

  const fetchLogs = (params: fetchDataParams = {}) => {
    const { page = DEFAULT_PAGE_NUMBER, size = MAX_PAGE_SIZE } = params;
    const { dateRange, startTime, endTime } = state;
    let greaterDate = subDays(currentDate, 7);
    let lowerDate = endOfDay(new Date());
    if (dateRange[0]) {
      greaterDate = dateRange[0];
      lowerDate = dateRange[0];
      if (dateRange[1]) {
        lowerDate = dateRange[1];
      }
    }
    if (greaterDate && lowerDate && startTime && endTime) {
      const greaterThan = getUnixTime(
        set(greaterDate, {
          hours: getHours(startTime),
          minutes: getMinutes(startTime),
        }),
      );
      const lowerThan = getUnixTime(
        set(lowerDate, {
          hours: getHours(endTime),
          minutes: getMinutes(endTime),
        }),
      );

      const userFilter = allocatedSelectedUsers.map((u) => u.id);
      const assigneeFilter = [...assigneeSelectedUsers, ...assigneeSelectedUserGroups].map(
        (u) => u.id,
      );
      const stageTaskFilter = Object.keys(selectedTasks).filter((id) => selectedTasks[id]);
      const fields = [
        {
          field: 'triggeredAt',
          op: FilterOperators.LOE,
          values: [lowerThan],
        },
        {
          field: 'triggeredBy',
          op: FilterOperators.ANY,
          values: userFilter,
        },
        {
          field: 'action',
          op: isTrainedUserView ? FilterOperators.EQ : FilterOperators.NE,
          values: ['TRAINED'],
        },
        ...(isTrainedUserView
          ? [
              {
                field: 'triggeredFor',
                op: FilterOperators.ANY,
                values: assigneeFilter,
              },
              {
                field: 'taskId',
                op: FilterOperators.ANY,
                values: stageTaskFilter,
              },
            ]
          : []),
      ];

      if (state.appliedFilters['Date/Time Range']) {
        fields.push({
          field: 'triggeredAt',
          op: FilterOperators.GOE,
          values: [greaterThan],
        });
      }

      const filters = JSON.stringify({
        op: FilterOperators.AND,
        fields,
      });

      dispatch(setAuditLogFilters(filters));
      if (id || checklistId) {
        dispatch(
          fetchChecklistAuditLogs({
            checklistId: id || checklistId!,
            params: { size, filters, sort: 'triggeredAt,desc', page },
          }),
        );
      }
    }
  };

  if (!logs || !pageable) {
    return <div>{loading && 'Loading...'}</div>;
  }

  const grouped: { [index: string]: ChecklistAuditLogsType[] } = groupBy(logs, 'triggeredOn');
  const data = Object.keys(grouped)
    .reverse()
    .map((item) => ({
      [item]: grouped[item],
      id: item,
    }));

  return (
    <Composer isTrainedUserView={isTrainedUserView}>
      {!isTrainedUserView && <GoBack label="Return to process" />}
      <div className="parameter-wrapper">
        <InfiniteListView
          isSearchable={false}
          fetchData={fetchLogs}
          pageable={pageable}
          data={data}
          filterProp={filterProp}
          beforeColumns={[
            {
              header: 'TIME',
              template: function renderComp(item) {
                const day = formatDateTime({
                  value: item.id as string,
                  type: InputTypes.DATE,
                });
                let criticalCount = 0;
                const itemId = item.id as string;

                return (
                  <div className="list-card-columns" key={`name_${itemId}`}>
                    <div style={{ padding: '0px 8px', flex: 1 }}>
                      <div className="log-header">
                        <div className="header-item">{day}</div>
                        <div className="header-item">{item[itemId].length} activities</div>
                        {criticalCount !== 0 && (
                          <>
                            <div className="header-item">
                              <ReportProblemOutlinedIcon className="icon" />
                            </div>
                            <div className="header-item">{criticalCount} Critical</div>
                          </>
                        )}
                      </div>
                      <div className="log-row">
                        {(item[itemId] as ChecklistAuditLogsType[]).map((log) => (
                          <div className="log-item" key={`${log.id}`}>
                            <div className="circle" />
                            <div className="content">
                              <div className="content-items" style={{ whiteSpace: 'nowrap' }}>
                                {formatDateTime({ value: log.triggeredAt, type: InputTypes.TIME })}
                              </div>
                              <div className="content-items">{log.details}</div>
                            </div>
                          </div>
                        ))}
                      </div>
                    </div>
                  </div>
                );
              },
            },
          ]}
        />
      </div>
    </Composer>
  );
};

export default AuditLogs;
