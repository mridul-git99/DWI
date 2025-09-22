import {
  Checkbox,
  FilterProp,
  Link as GoBack,
  InfiniteListView,
  LoadingContainer,
} from '#components';
import { User, UsersListType } from '#store/users/types';
import { DEFAULT_PAGE_NUMBER, DEFAULT_PAGE_SIZE } from '#utils/constants';
import { FilterOperators, InputTypes, fetchDataParams } from '#utils/globalTypes';
import { formatDateTime, getStartOfDayEpochInTimezone } from '#utils/timeUtils';
import TextField from '@material-ui/core/TextField';
import AccessTimeIcon from '@material-ui/icons/AccessTime';
import ArrowDropDownIcon from '@material-ui/icons/ArrowDropDown';
import {
  DateRange,
  DateRangeDelimiter,
  LocalizationProvider,
  StaticDateRangePicker,
  TimePicker,
} from '@material-ui/pickers';
import DateFnsUtils from '@material-ui/pickers/adapter/date-fns';
import { groupBy } from 'lodash';
import React, { FC, useEffect, useMemo, useState } from 'react';
import { endOfDay, getHours, getMinutes, getUnixTime, set, startOfDay, subDays } from 'date-fns';
import { createFetchList } from '#hooks/useFetchData';
import { apiUserGroups } from '#utils/apiUrls';
import { useTypedSelector } from '#store';
import { getInitials } from '#utils/stringUtils';
import { Search } from '@material-ui/icons';
import { useDispatch } from 'react-redux';
import { fetchUsers } from '#store/users/actions';
import { usePrevious } from '#utils/usePrevious';
import { UserFilterWrapper } from '#styles/UserFilterWrapper';
import { Composer } from '#PrototypeComposer/ChecklistAuditLogs/styles';

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

// TODO Change appliedUsers, selectedUsers, unSelectedUsers to HashMap as we only need the id's of the users So we can keep it like { userId : boolean }.
const currentDate = startOfDay(new Date());
const initialState: initialState = {
  dateRange: [null, null],
  appliedFilters: {},
  startTime: currentDate,
  endTime: endOfDay(new Date()),
  searchQuery: '',
  selectedUsers: [],
  unSelectedUsers: [],
  appliedUsers: [],
};

const AuditLogs: FC<any> = ({ id }: any) => {
  const {
    list: auditLogs,
    reset,
    status,
    pagination: auditLogsPagination,
    fetchNext,
  } = createFetchList(
    apiUserGroups('audits'),
    {
      sort: 'triggeredAt,desc',
      filters: {
        op: FilterOperators.AND,
        fields: [
          {
            field: 'userGroupId',
            op: FilterOperators.EQ,
            values: [id],
          },
        ],
      },
    },
    false,
  );

  const {
    list,
    pageable: { last, page },
  } = useTypedSelector((state) => state.users.all);

  const dispatch = useDispatch();

  const [state, setstate] = useState(initialState);
  const { appliedUsers, selectedUsers, unSelectedUsers, searchQuery } = state;

  const prevSearch = usePrevious(searchQuery);

  const resetFilter = () => {
    setstate(initialState);
  };

  const handleUnselectAll = () => {
    setstate({
      ...state,
      unSelectedUsers: [],
      selectedUsers: [],
      appliedUsers: [],
    });
  };

  const fetchUsersData = (params: fetchDataParams = {}) => {
    const { page, size } = params;
    const filters = JSON.stringify({
      op: FilterOperators.AND,
      fields: [{ field: 'firstName', op: FilterOperators.LIKE, values: [searchQuery] }],
    });
    dispatch(fetchUsers({ page, size, filters }, UsersListType.ALL));
  };

  const handleOnScroll = (e: React.UIEvent<HTMLElement>) => {
    e.stopPropagation();
    const { scrollHeight, scrollTop, clientHeight } = e.currentTarget;
    if (scrollTop + clientHeight >= scrollHeight - clientHeight * 0.7 && !last)
      fetchUsersData({ page: page + 1, size: DEFAULT_PAGE_SIZE });
  };

  const onCheckChanged = (user: User, checked: boolean) => {
    if (checked) {
      const newSelected = selectedUsers.filter((u) => user.id !== u.id);
      setstate({
        ...state,
        selectedUsers: newSelected,
        unSelectedUsers: [...unSelectedUsers, user],
      });
    } else {
      const newUnSelected = unSelectedUsers.filter((u) => user.id !== u.id);
      setstate({
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

  useEffect(() => {
    if (prevSearch !== searchQuery) {
      fetchUsersData({ page: DEFAULT_PAGE_NUMBER, size: DEFAULT_PAGE_SIZE });
    }
  }, [searchQuery]);

  const filterProp: FilterProp = {
    filters: [
      {
        label: 'Date/Time Range',
        onApply: () => {
          setstate({
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
        label: 'Users',
        onApply: () => {
          const applicableUsers = [
            ...selectedUsers,
            ...appliedUsers.filter((user) => !unSelectedUsers.some((item) => item.id === user.id)),
          ];
          if (!!applicableUsers.length) {
            setstate({
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
            setstate({
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
                    onChange={(e) => setstate({ ...state, searchQuery: e.target.value })}
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

  useEffect(() => {
    fetchLogs();
  }, [state.appliedFilters]);

  const fetchLogs = (params: fetchDataParams = {}) => {
    const { page = DEFAULT_PAGE_NUMBER, size = DEFAULT_PAGE_SIZE } = params;
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

      const userFilter = appliedUsers.map((u) => u.id);
      const nameOfTheUser = appliedUsers.map((u) => {
        return { id: u.id, firstName: u.firstName, lastName: u.lastName, employeeId: u.employeeId };
      });

      const fields = [
        {
          field: 'userGroupId',
          op: FilterOperators.EQ,
          values: [id],
        },
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

      const filters = {
        op: FilterOperators.AND,
        fields,
        names: nameOfTheUser,
      };

      if (id) {
        reset({
          sort: 'triggeredAt,desc',
          params: { size, filters, page },
        });
      }
    }
  };

  const grouped: { [index: string]: any[] } = useMemo(
    () =>
      groupBy(
        (auditLogs || []).map((log) => ({
          ...log,
          triggeredOn: getStartOfDayEpochInTimezone({ value: log.triggeredAt }),
        })),
        'triggeredOn',
      ),
    [auditLogs],
  );

  const data = useMemo(
    () =>
      Object.keys(grouped)
        .reverse()
        .map((item) => ({
          [item]: grouped[item],
          id: item,
        })),
    [grouped],
  );

  if (status === 'loading') {
    return <LoadingContainer loading={true} component={<></>} />;
  }

  return (
    <Composer>
      <GoBack label="Return to user groups" />
      <div className="audit-logs-wrapper">
        <InfiniteListView
          isSearchable={false}
          fetchData={fetchNext}
          pageable={auditLogsPagination}
          data={data}
          filterProp={filterProp}
          beforeColumns={[
            {
              header: 'TIME',
              template: function renderComp(item) {
                const day = formatDateTime({
                  value: item.id,
                  type: InputTypes.DATE,
                });
                const itemId = item.id as string;

                return (
                  <div className="list-card-columns" key={`name_${itemId}`}>
                    <div style={{ padding: '0px 8px', flex: 1 }}>
                      <div className="log-header">
                        <div className="header-item">{day}</div>
                        <div className="header-item">{item[itemId].length} activities</div>
                      </div>
                      <div className="log-row">
                        {(item[itemId] as any[]).map((log) => {
                          const details = log?.details;
                          return (
                            <div className="log-item" key={`${log.id}`}>
                              <div className="circle" />
                              <div className="content">
                                <div className="content-items" style={{ whiteSpace: 'nowrap' }}>
                                  {formatDateTime({
                                    value: log.triggeredAt,
                                    type: InputTypes.TIME,
                                  })}
                                </div>
                                <div className="content-items">{details}</div>
                              </div>
                            </div>
                          );
                        })}
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
