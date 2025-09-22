import { Checkbox, FilterProp, InfiniteListView } from '#components';
import { useTypedSelector } from '#store';
import { clearAuditLogFilters, setAuditLogFilters } from '#store/audit-log-filters/action';
import { fetchUsers } from '#store/users/actions';
import { User, UsersListType } from '#store/users/types';
import { openLinkInNewTab } from '#utils';
import {
  DEFAULT_PAGE_NUMBER,
  DEFAULT_PAGE_SIZE,
  EXPORT_LIMIT_LEVELS,
  MAX_PAGE_SIZE,
} from '#utils/constants';
import { FilterOperators, InputTypes, fetchDataParams } from '#utils/globalTypes';
import { getInitials } from '#utils/stringUtils';
import { formatDateTime } from '#utils/timeUtils';
import { usePrevious } from '#utils/usePrevious';
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
import { endOfToday, getHours, getMinutes, getUnixTime, set, startOfDay, subDays } from 'date-fns';
import { groupBy } from 'lodash';
import React, { FC, useEffect, useState } from 'react';
import { useDispatch } from 'react-redux';
import { TabViewProps } from '../types';
import { fetchSessionActivities } from './actions';
import { Composer } from './styles';
import {
  SessionActivitySeverity,
  SessionActivityState,
  SessionActivity as SessionActivityType,
} from './types';
import { UserFilterWrapper } from '#styles/UserFilterWrapper';
import { downloadPdf } from '#utils/downloadPdf';
import { apiPrintUserAudits } from '#utils/apiUrls';
import { showNotification } from '#components/Notification/actions';
import { NotificationType } from '#components/Notification/types';

type initialState = {
  dateRange: DateRange<Date>;
  appliedFilters: Record<string, boolean>;
  startTime: Date | null;
  endTime: Date | null;
  searchQuery: string;
  selectedUsers: User[];
  unSelectedUsers: User[];
  appliedUsers: User[];
};

// TODO Change appliedUsers, selectedUsers, unSelectedUsers to HashMap as we only need the id's of the users SO we can keep it like { userId : boolean }.
const currentDate = startOfDay(new Date());
const initialState: initialState = {
  dateRange: [null, null],
  appliedFilters: {},
  startTime: currentDate,
  endTime: endOfToday(),
  searchQuery: '',
  selectedUsers: [],
  unSelectedUsers: [],
  appliedUsers: [],
};

const SessionActivity: FC<TabViewProps> = () => {
  const { logs, loading, pageable }: SessionActivityState = useTypedSelector(
    (state) => state.sessionActivity,
  );
  const {
    list,
    pageable: { last, page },
  } = useTypedSelector((state) => state.users.all);

  const { filters: sessionAuditLogFilters } = useTypedSelector((state) => state.auditLogFilters);
  const selectedFacility = useTypedSelector((state) => state.auth.selectedFacility);

  const dispatch = useDispatch();
  const [state, setState] = useState(initialState);
  const { searchQuery, selectedUsers, unSelectedUsers, appliedUsers } = state;

  const prevSearch = usePrevious(searchQuery);

  const resetFilter = () => {
    setState(initialState);
    dispatch(clearAuditLogFilters());
  };

  const onCheckChanged = (user: User, checked: boolean) => {
    if (checked) {
      const newSelected = selectedUsers.filter((u) => user.id !== u.id);
      setState({
        ...state,
        selectedUsers: newSelected,
        unSelectedUsers: [...unSelectedUsers, user],
      });
    } else {
      const newUnSelected = unSelectedUsers.filter((u) => user.id !== u.id);
      setState({
        ...state,
        unSelectedUsers: newUnSelected,
        selectedUsers: [...selectedUsers, user],
      });
    }
  };

  const userRow = (user: User, checked: boolean) => {
    return (
      <div className="item" key={`user_${user.id}`}>
        <div className="right">
          <Checkbox checked={checked} onClick={() => onCheckChanged(user, checked)} />
        </div>
        <div className="thumb">{getInitials(`${user.firstName} ${user.lastName}`)}</div>
        <div className="middle">
          <span className="userId">{user.employeeId}</span>
          <span className="userName">{`${user.firstName} ${user.lastName}`}</span>
        </div>
      </div>
    );
  };

  const handleOnScroll = (e: React.UIEvent<HTMLElement>) => {
    e.stopPropagation();
    const { scrollHeight, scrollTop, clientHeight } = e.currentTarget;
    if (scrollTop + clientHeight >= scrollHeight - clientHeight * 0.7 && !last)
      fetchUsersData({ page: page + 1, size: DEFAULT_PAGE_SIZE });
  };

  const handleUnselectAll = () => {
    setState({
      ...state,
      unSelectedUsers: [],
      selectedUsers: [],
      appliedUsers: [],
    });
  };

  const filterProp: FilterProp = {
    filters: [
      {
        label: 'Date/Time Range',
        onApply: () => {
          setState({
            ...state,
            appliedFilters: {
              ...state.appliedFilters,
              'Date/Time Range': true,
            },
          });
        },
        content: (
          <LocalizationProvider dateAdapter={DateFnsUtils}>
            <StaticDateRangePicker
              displayStaticWrapperAs="desktop"
              value={state.dateRange}
              calendars={1}
              onChange={(newValue) => setState({ ...state, dateRange: newValue })}
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
                onChange={(newValue) => setState({ ...state, startTime: newValue })}
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
                onChange={(newValue) => setState({ ...state, endTime: newValue })}
              />
            </div>
          </LocalizationProvider>
        ),
      },
      {
        label: 'Users',
        onApply: () => {
          const applicableUsers = [
            ...selectedUsers,
            ...appliedUsers.filter((user) => !unSelectedUsers.some((item) => item.id === user.id)),
          ];
          if (!!applicableUsers.length) {
            setState({
              ...state,
              appliedFilters: {
                ...state.appliedFilters,
                Users: true,
              },
              appliedUsers: applicableUsers,
              selectedUsers: [],
              unSelectedUsers: [],
            });
          } else {
            setState({
              ...state,
              appliedFilters: {
                ...state.appliedFilters,
                Users: false,
              },
              appliedUsers: [],
              selectedUsers: [],
              unSelectedUsers: [],
            });
          }
        },
        content: function template() {
          const bodyView: JSX.Element[] = [];

          if (list) {
            if (searchQuery === '') {
              appliedUsers.forEach((user) => {
                const isUnSelected = !unSelectedUsers.some((item) => item.id === user.id);
                bodyView.push(userRow(user, isUnSelected));
              });
            }

            (list as unknown as Array<User>).forEach((user) => {
              const isSelected = selectedUsers.some((item) => item.id === user.id);
              const inApplied = appliedUsers.some((item) => item.id === user.id);
              if (!inApplied) {
                bodyView.push(userRow(user, isSelected));
              }
            });
          }
          return (
            <UserFilterWrapper>
              <div className="top-content">
                <div className="searchboxwrapper">
                  <Search className="searchsubmit" />
                  <input
                    className="searchbox"
                    type="text"
                    onChange={(e) => setState({ ...state, searchQuery: e.target.value })}
                    defaultValue={searchQuery}
                    placeholder="First Name"
                  />
                </div>
                <span onClick={handleUnselectAll}>Unselect All</span>
              </div>
              <div className="scrollable-content" onScroll={handleOnScroll}>
                {bodyView}
              </div>
            </UserFilterWrapper>
          );
        },
      },
    ],
    onReset: () => resetFilter(),
    activeCount: Object.keys(state.appliedFilters).filter((key) => !!state.appliedFilters[key])
      .length,
  };

  const handleDownload = async () => {
    if (pageable.totalElements > EXPORT_LIMIT_LEVELS.HIGH) {
      dispatch(
        showNotification({
          type: NotificationType.ERROR,
          msg: `Cannot export ${pageable.totalElements.toLocaleString()} session records. Maximum export limit is ${EXPORT_LIMIT_LEVELS.HIGH.toLocaleString()} records. Please apply filters to reduce the dataset.`,
        }),
      );
      return;
    }

    const timestamp = Date.now();
    await downloadPdf({
      url: apiPrintUserAudits(),
      method: 'GET',
      params: { filters: sessionAuditLogFilters },
      filename: `SessionActivity_${selectedFacility?.name}_${timestamp}`,
    });
  };
  useEffect(() => {
    return () => {
      dispatch(clearAuditLogFilters());
    };
  }, []);

  useEffect(() => {
    fetchLogs();
  }, [state.appliedFilters]);

  useEffect(() => {
    if (prevSearch !== searchQuery) {
      fetchUsersData({ page: DEFAULT_PAGE_NUMBER, size: DEFAULT_PAGE_SIZE });
    }
  }, [searchQuery]);

  const fetchUsersData = (params: fetchDataParams = {}) => {
    const { page, size } = params;
    const filters = JSON.stringify({
      op: FilterOperators.AND,
      fields: [{ field: 'firstName', op: FilterOperators.LIKE, values: [searchQuery] }],
    });
    dispatch(fetchUsers({ page, size, filters }, UsersListType.ALL));
  };

  const fetchLogs = (params: fetchDataParams = {}) => {
    const { page = DEFAULT_PAGE_NUMBER, size = MAX_PAGE_SIZE } = params;
    const { dateRange, startTime, endTime } = state;
    let greaterDate = subDays(currentDate, 7);
    let lowerDate = endOfToday();
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

      const userFilter = appliedUsers.map((u) => u.id);
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

      dispatch(
        fetchSessionActivities({
          size,
          filters,
          sort: 'triggeredAt,desc',
          page,
        }),
      );
    }
  };

  if (!logs || !pageable) {
    return <div>{loading && 'Loading...'}</div>;
  }

  const grouped = groupBy(logs, 'triggeredOn');
  const data = [] as Record<string, string | SessionActivityType[]>[];

  Object.keys(grouped)
    .reverse()
    .forEach((item) => {
      data.push({
        [item]: grouped[item],
        id: item,
      });
    });

  return (
    <Composer>
      <InfiniteListView
        isSearchable={false}
        fetchData={fetchLogs}
        pageable={pageable}
        data={data}
        onPrimaryClick={() => {
          handleDownload();
        }}
        primaryButtonText="Export"
        primaryButtonDisable={pageable.empty}
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
              (item[itemId] as SessionActivityType[]).forEach((element) => {
                if (element.severity === SessionActivitySeverity.CRITICAL) criticalCount++;
              });
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
                      {(item[itemId] as SessionActivityType[]).map((log) => (
                        <div className="log-item" key={`${log.id}`}>
                          <div className="circle" />
                          <div className="content">
                            <div className="content-items" style={{ whiteSpace: 'nowrap' }}>
                              {formatDateTime({ value: log.triggeredAt, type: InputTypes.TIME })}
                            </div>
                            {log.severity === SessionActivitySeverity.CRITICAL && (
                              <div className="content-items">
                                <ReportProblemOutlinedIcon className="icon" />
                              </div>
                            )}
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
    </Composer>
  );
};

export default SessionActivity;
