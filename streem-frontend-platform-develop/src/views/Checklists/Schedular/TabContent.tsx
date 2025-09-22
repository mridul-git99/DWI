import { DisabledStates } from '#PrototypeComposer/checklist.types';
import MemoArchive from '#assets/svg/Archive';
import MemoViewInfo from '#assets/svg/ViewInfo';
import {
  Button,
  DataTable,
  ListActionMenu,
  Pagination,
  TextInput,
  ToggleSwitch,
} from '#components';
import { openOverlayAction } from '#components/OverlayContainer/actions';
import { OverlayNames } from '#components/OverlayContainer/types';
import checkPermission from '#services/uiPermissions';
import { useTypedSelector } from '#store';
import { DEFAULT_PAGE_NUMBER, DEFAULT_PAGE_SIZE } from '#utils/constants';
import { FilterField, FilterOperators } from '#utils/globalTypes';
import { getFullName } from '#utils/stringUtils';
import { formatDateTimeToHumanReadable } from '#utils/timeUtils';
import { TabContentWrapper } from '#views/Jobs/ListView/styles';
import { MenuItem } from '@material-ui/core';
import { ArrowDropDown, EditOutlined, Search, WatchLaterOutlined } from '@material-ui/icons';
import { navigate } from '@reach/router';
import { capitalize, debounce } from 'lodash';
import React, { FC, MouseEvent, useEffect, useState } from 'react';
import { useDispatch } from 'react-redux';
import { Frequency, RRule } from 'rrule';
import CreateSchedulerDrawer from './Components/CreateSchedularDrawer';
import VersionHistoryDrawer from './Components/VersionHistoryDrawer';
import schedulersActionObjects from './schedulerStore';

const getBaseFilter = (
  label: string,
  selectedChecklist: Record<string, string>,
  checklistVersionIds: string[] = [],
): FilterField[] => [
  {
    field: 'archived',
    op: FilterOperators.EQ,
    values: [false],
  },
  {
    field: 'state',
    op: FilterOperators.EQ,
    values: [label === 'Active' ? DisabledStates.PUBLISHED : DisabledStates.DEPRECATED],
  },

  {
    field: 'checklistId',
    op: label === 'Active' ? FilterOperators.EQ : FilterOperators.ANY,
    values:
      label === 'Active'
        ? [selectedChecklist?.id]
        : [selectedChecklist?.id, ...checklistVersionIds],
  },
];

const ListView: FC<any & { label: string }> = ({ label, values }) => {
  const dispatch = useDispatch();
  const selectedChecklist = { ...values, name: values?.processName };
  const [createSchedulerDrawer, setCreateSchedulerDrawer] = useState(false);
  const [versionHistoryDrawer, setVersionHistoryDrawer] = useState(false);
  const [readOnlyMode, setReadOnlyMode] = useState(false);
  const [searchFilterFields, setSearchFilterFields] = useState<FilterField[]>([]);
  const { schedulerActions } = schedulersActionObjects;
  const { list, pageable, checklistInfo } = useTypedSelector((state) => state.schedular);

  const [filterFields, setFilterFields] = useState<FilterField[]>(
    getBaseFilter(
      label,
      selectedChecklist,
      checklistInfo?.versions?.map((curr) => curr.id),
    ),
  );

  const fetchData = (params = {}) => {
    const { page = DEFAULT_PAGE_NUMBER, size = DEFAULT_PAGE_SIZE, filters = filterFields } = params;
    dispatch(
      schedulerActions.fetchSchedulers({
        page,
        size,
        sort: 'id,desc',
        filters: JSON.stringify({
          op: FilterOperators.AND,
          fields: [...filters, ...searchFilterFields],
        }),
      }),
    );
  };

  useEffect(() => {
    fetchData();
  }, [filterFields, searchFilterFields]);

  useEffect(() => {
    if (label === 'Active') {
      dispatch(schedulerActions.fetchChecklistInfo({ checklistId: selectedChecklist?.id }));
    }
  }, []);

  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const [selectedScheduler, setSelectedScheduler] = useState<Record<string, any> | null>(null);

  const handleClose = () => {
    setAnchorEl(null);
    setTimeout(() => setSelectedScheduler(null), 200);
  };

  const columns = [
    {
      id: 'name',
      label: 'Name',
      minWidth: 100,
      format: function renderComp(item: any) {
        return (
          <span
            className="primary"
            onClick={() => {
              setCreateSchedulerDrawer(true);
              setReadOnlyMode(true);
              setSelectedScheduler(item);
            }}
          >
            {item.name}
          </span>
        );
      },
    },
    ...(label === 'Deprecated'
      ? [
          {
            id: 'processVersion',
            label: '# Process Version',
            minWidth: 100,
            format: function renderComp(item: any) {
              const versionNumber = checklistInfo?.versions?.find(
                (currVersion: any) => currVersion?.id === item?.checklistId,
              )?.versionNumber;
              return <div key={item?.id}>{versionNumber}</div>;
            },
          },
        ]
      : []),
    {
      id: 'recurrence',
      label: 'Recurrence',
      minWidth: 100,
      format: function renderComp(item: any) {
        const rule = RRule.fromString(item?.recurrenceRule);
        const rRuleOptions = rule?.origOptions;
        const frequency =
          Object?.keys(Frequency)[Object?.values(Frequency)?.indexOf(rRuleOptions?.freq)];
        return <div key={item?.id}>{capitalize(frequency)}</div>;
      },
    },
    {
      id: 'id',
      label: 'ID',
      minWidth: 100,
      format: function renderComp(item: any) {
        return <div key={item?.id}>{item?.code}</div>;
      },
    },
    {
      id: 'createdBy',
      label: 'Created By',
      minWidth: 100,
      format: function renderComp(item: any) {
        return <div key={item?.id}>{getFullName(item?.audit?.createdBy)}</div>;
      },
    },
    {
      id: 'createdDate',
      label: 'Created Date',
      minWidth: 100,
      format: function renderComp(item: any) {
        return <div key={item?.id}>{formatDateTimeToHumanReadable(item?.audit?.createdAt)}</div>;
      },
    },

    ...(label === 'Active'
      ? [
          {
            id: 'actions',
            label: 'Actions',
            minWidth: 100,
            format: function renderComp(item: any) {
              return (
                !item.archived &&
                (checkPermission(['scheduler', 'actions']) ? (
                  <div style={{ display: 'flex', gap: 16 }}>
                    <div
                      id="more-actions"
                      onClick={(event: MouseEvent<HTMLDivElement>) => {
                        setAnchorEl(event.currentTarget);
                        setSelectedScheduler(item);
                      }}
                    >
                      More <ArrowDropDown className="icon" fontSize="small" />
                    </div>

                    <ListActionMenu
                      id="row-more-actions"
                      anchorEl={anchorEl}
                      keepMounted
                      disableEnforceFocus
                      open={Boolean(anchorEl)}
                      onClose={handleClose}
                    >
                      <MenuItem
                        onClick={() => {
                          setAnchorEl(null);
                          setCreateSchedulerDrawer(true);
                        }}
                      >
                        <div className="list-item">
                          <EditOutlined />
                          <span>Revise</span>
                        </div>
                      </MenuItem>
                      <MenuItem
                        onClick={() => {
                          handleClose();
                          setVersionHistoryDrawer(true);
                        }}
                      >
                        <div className="list-item">
                          <WatchLaterOutlined />
                          <span>Version History</span>
                        </div>
                      </MenuItem>
                      <MenuItem
                        onClick={() =>
                          navigate(`/checklists/jobs`, {
                            state: {
                              processFilter: {
                                processName: selectedChecklist?.name,
                                processId: selectedChecklist?.id,
                                schedulerId: selectedScheduler?.id,
                                schedulerName: selectedScheduler?.name,
                              },
                            },
                          })
                        }
                      >
                        <div className="list-item">
                          <MemoViewInfo />
                          <span>View Jobs</span>
                        </div>
                      </MenuItem>
                      <MenuItem
                        onClick={() => {
                          handleClose();
                          dispatch(
                            openOverlayAction({
                              type: OverlayNames.REASON_MODAL,
                              props: {
                                modalTitle: 'Archive Scheduler',
                                modalDesc: `Provide reason for archiving ${selectedScheduler?.name} schedular`,
                                onSubmitHandler: (
                                  reason: string,
                                  setFormErrors: (errors?: Error[]) => void,
                                ) => {
                                  dispatch(
                                    schedulerActions.archiveScheduler({
                                      schedularId: selectedScheduler?.id,
                                      reason,
                                      setFormErrors,
                                    }),
                                  );
                                },
                                onSubmitModalText: 'Confirm',
                              },
                            }),
                          );
                        }}
                      >
                        <div className="list-item">
                          <MemoArchive style={{ marginRight: '8px' }} />
                          <span>Archive</span>
                        </div>
                      </MenuItem>
                    </ListActionMenu>
                  </div>
                ) : (
                  <div style={{ display: 'flex', gap: 16, color: '#bbbbbb' }}>
                    <div id="more-actions-disabled">
                      More <ArrowDropDown className="icon" fontSize="small" />
                    </div>
                  </div>
                ))
              );
            },
          },
        ]
      : [
          {
            id: 'deprecatedDate',
            label: 'Deprecated On',
            minWidth: 100,
            format: function renderComp(item: any) {
              return (
                <div key={item?.id}>
                  {item?.deprecatedAt ? formatDateTimeToHumanReadable(item.deprecatedAt) : '-'}
                </div>
              );
            },
          },
        ]),
  ];

  return (
    <TabContentWrapper>
      <div className="before-table-wrapper">
        <div className="filters">
          <TextInput
            afterElementWithoutError
            AfterElement={Search}
            afterElementClass=""
            placeholder="Search..."
            onChange={debounce(
              ({ value }) =>
                setSearchFilterFields([
                  { field: 'name', op: FilterOperators.LIKE, values: [value] },
                ]),
              500,
            )}
          />
          {label === 'Active' && (
            <ToggleSwitch
              checkedIcon={false}
              offLabel="Show Archived"
              onLabel="Showing Archived"
              uncheckedIcon={false}
              checked={!!filterFields.find((field) => field.field === 'archived')?.values[0]}
              onChange={(isChecked) =>
                setFilterFields((currentFields) => {
                  const updatedFilterFields = currentFields.map((field) => ({
                    ...field,
                    ...(field.field === 'archived'
                      ? { values: [isChecked] }
                      : { values: field.values }),
                  })) as FilterField[];

                  return updatedFilterFields;
                })
              }
            />
          )}
        </div>
        {label === 'Active' && (
          <div className="actions">
            <Button
              onClick={() => {
                setCreateSchedulerDrawer(true);
              }}
              disabled={!checkPermission(['scheduler', 'create']) || selectedChecklist?.archived}
            >
              Create Scheduler
            </Button>
          </div>
        )}
      </div>
      <div style={{ display: 'contents' }}>
        <DataTable columns={columns} rows={list} emptyTitle="No Schedulers Found" />
        <Pagination pageable={pageable} fetchData={fetchData} />
      </div>
      {createSchedulerDrawer && (
        <CreateSchedulerDrawer
          checklist={{ label: selectedChecklist?.name, value: selectedChecklist.id }}
          onCloseDrawer={setCreateSchedulerDrawer}
          schedular={{
            label: selectedScheduler?.name,
            value: selectedScheduler?.id,
            checklistId: selectedScheduler?.checklistId,
            checklistName: selectedScheduler?.checklistName,
          }}
          readOnly={readOnlyMode}
          setReadOnly={setReadOnlyMode}
          handleClose={handleClose}
        />
      )}
      {versionHistoryDrawer && (
        <VersionHistoryDrawer
          schedular={{ label: selectedScheduler?.name, value: selectedScheduler?.id }}
          onCloseDrawer={setVersionHistoryDrawer}
          setReadOnly={setReadOnlyMode}
          setCreateSchedulerDrawer={setCreateSchedulerDrawer}
          setSelectedScheduler={setSelectedScheduler}
          selectedChecklist={selectedChecklist}
        />
      )}
    </TabContentWrapper>
  );
};

export default ListView;
