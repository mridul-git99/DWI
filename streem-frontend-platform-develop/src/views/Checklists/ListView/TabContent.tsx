import {
  Checklist,
  ChecklistStates,
  ChecklistStatesColors,
  ChecklistStatesContent,
  DisabledStates,
} from '#PrototypeComposer/checklist.types';
import { CollaboratorType } from '#PrototypeComposer/reviewer.types';
import { ComposerEntity } from '#PrototypeComposer/types';
import MemoArchive from '#assets/svg/Archive';
import DownloadIcon from '#assets/svg/DownloadIcon';
import FilterIcon from '#assets/svg/FilterIcon.svg';
import MemoStartRevision from '#assets/svg/StartRevision';
import MemoViewInfo from '#assets/svg/ViewInfo';
import {
  Button,
  CustomMenu,
  DataTable,
  ImageUploadButton,
  LoadingContainer,
  Pagination,
  SearchFilter,
  ToggleSwitch,
} from '#components';
import { showNotification } from '#components/Notification/actions';
import { NotificationType } from '#components/Notification/types';
import { openOverlayAction } from '#components/OverlayContainer/actions';
import { OverlayNames } from '#components/OverlayContainer/types';
import checkPermission, { roles, isFeatureAllowed } from '#services/uiPermissions';
import { useTypedSelector } from '#store';
import { apiImportChecklist, apiPrintProcessPdf, apiValidateReviseProcess } from '#utils/apiUrls';
import { downloadPdf } from '#utils/downloadPdf';
import { ALL_FACILITY_ID, DEFAULT_PAGE_NUMBER, DEFAULT_PAGE_SIZE } from '#utils/constants';
import {
  Error,
  FilterField,
  FilterOperators,
  ResponseError,
  fetchDataParams,
} from '#utils/globalTypes';
import { getErrorMsg, request } from '#utils/request';
import CreateJob from '#views/Jobs/Components/CreateJob';
import { TabContentWrapper } from '#views/Jobs/ListView/styles';
import { Chip } from '@material-ui/core';
import {
  FiberManualRecord,
  GetAppOutlined,
  PublishOutlined,
  TimelineOutlined,
} from '@material-ui/icons';
import { navigate } from '@reach/router';
import React, { FC, useEffect, useMemo, useState } from 'react';
import { useDispatch } from 'react-redux';
import styled from 'styled-components';
import { addRevisionPrototype } from '../NewPrototype/actions';
import { FormMode } from '../NewPrototype/types';
import FiltersDrawer from '../Overlays/FilterDrawer';
import {
  archiveChecklist,
  clearData,
  exportChecklist,
  fetchChecklistsForListView,
  unarchiveChecklist,
} from './actions';
import { ListViewProps } from './types';

const getBaseFilter = (label: string): FilterField[] => [
  { field: 'archived', op: FilterOperators.EQ, values: [false] },
  ...(label === 'prototype'
    ? ([
        {
          field: 'state',
          op: FilterOperators.NE,
          values: [DisabledStates.DEPRECATED],
        },
        {
          field: 'state',
          op: FilterOperators.NE,
          values: [DisabledStates.PUBLISHED],
        },
      ] as FilterField[])
    : [
        {
          field: 'state',
          op: FilterOperators.EQ,
          values: [DisabledStates.PUBLISHED],
        },
      ]),
];

const TypeChip = styled(Chip)<{ $fontColor: string; $backGroundColor: string }>`
  height: 24px !important;
  background-color: ${({ $backGroundColor }) => $backGroundColor} !important;
  color: ${({ $fontColor }) => $fontColor} !important;
  line-height: 1.33;
  letter-spacing: 0.32px;
  font-size: 12px !important;
`;

const ActionItem: FC<{ item: Checklist; label: string }> = ({ item, label }) => {
  const dispatch = useDispatch();

  const facilityId = useTypedSelector((state) => state.auth.selectedFacility?.id);
  const userRoles = useTypedSelector((state) => state.auth.roles);
  const userId = useTypedSelector((state) => state.auth.userId);

  const [createJobDrawerVisible, setCreateJobDrawerVisible] = useState(false);

  const checkArchiveAndRevisionPermission = (action: 'archive' | 'revision') => {
    if (item.global) {
      if (facilityId === ALL_FACILITY_ID) return true;
    } else if (checkPermission(['checklists', action])) {
      return true;
    }
    return false;
  };

  const handleOnCreateJob = () => {
    if (userRoles?.some((role) => role === roles.ACCOUNT_OWNER) && facilityId === ALL_FACILITY_ID) {
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

  const handleReviseProcess = async () => {
    const { data, errors } = await request('PATCH', apiValidateReviseProcess(item.id));
    if (data) {
      dispatch(
        openOverlayAction({
          type: OverlayNames.CONFIRMATION_MODAL,
          props: {
            onPrimary: () => dispatch(addRevisionPrototype(item.id, item.code, item.name)),
            title: 'Start Revision',
            body: (
              <>
                <span>Are you sure you want to start a Revision on this Process?</span>
                <span style={{ color: '#999999' }}>
                  This will Deprecate the current Process and create a new Prototype as a revision.
                </span>
              </>
            ),
          },
        }),
      );
    } else if (errors) {
      dispatch(
        showNotification({
          type: NotificationType.ERROR,
          msg: getErrorMsg(errors),
        }),
      );
    }
  };

  const onArchiveOrUnarchive = () => {
    const type = label === 'published' ? 'Process' : 'Protoype';
    const action = item.archived ? 'Unarchive' : 'Archive';

    dispatch(
      openOverlayAction({
        type: OverlayNames.REASON_MODAL,
        props: {
          modalTitle: `${action} ${type}`,
          modalDesc: `Provide details for ${
            item.archived ? 'unarchiving' : 'archiving'
          } the ${type.toLowerCase()}`,
          onSubmitHandler: (reason: string, setFormErrors: (errors?: Error[]) => void) =>
            item.archived
              ? dispatch(unarchiveChecklist(item.id, reason, setFormErrors))
              : dispatch(archiveChecklist(item.id, reason, setFormErrors)),
          onSubmitModalText: action,
        },
      }),
    );
  };

  const handleDownloadProcess = async () => {
    const timestamp = Date.now();
    await downloadPdf({
      url: apiPrintProcessPdf(item.id),
      method: 'GET',
      filename: `${item.code}_${timestamp}`,
    });
  };

  const actionItems = useMemo(
    () => [
      {
        label: 'View Info',
        icon: <MemoViewInfo />,
        onClick: () => {
          dispatch(
            openOverlayAction({
              type: OverlayNames.CHECKLIST_INFO,
              props: { checklistId: item.id },
            }),
          );
        },
      },
      {
        label: 'View Jobs',
        icon: <MemoViewInfo />,
        onClick: () => {
          navigate(`/checklists/jobs`, {
            state: {
              processFilter: {
                processName: item.name,
                id: item.id,
              },
            },
          });
        },
      },
      {
        label: 'Scheduler',
        icon: <TimelineOutlined />,
        onClick: () => {
          navigate(`/checklists/${item.id}/scheduler`, {
            state: {
              processFilter: {
                processName: item.name,
                id: item.id,
                archived: item.archived,
              },
            },
          });
        },
      },
      ...(checkPermission(['checklists', 'importExport'])
        ? [
            {
              label: 'Export',
              icon: <PublishOutlined />,
              onClick: () => {
                dispatch(exportChecklist({ checklistId: item.id }));
              },
            },
          ]
        : []),
      ...(isFeatureAllowed('downloadProcess') && item.state !== ChecklistStates.BEING_BUILT
        ? [
            {
              label: 'Process Template',
              icon: <DownloadIcon />,
              onClick: handleDownloadProcess,
            },
          ]
        : []),
      ...(!item.archived && checkArchiveAndRevisionPermission('revision')
        ? [
            {
              label: 'Start a Revision',
              icon: <MemoStartRevision />,
              onClick: handleReviseProcess,
            },
          ]
        : []),
      ...(checkArchiveAndRevisionPermission('archive')
        ? [
            {
              label: item.archived ? 'Unarchive Process' : 'Archive Process',
              icon: <MemoArchive />,
              onClick: onArchiveOrUnarchive,
            },
          ]
        : []),
      ...(facilityId !== ALL_FACILITY_ID
        ? [
            {
              label: 'View Job Logs',
              icon: <MemoViewInfo />,
              onClick: () => navigate(`/checklists/${item.id}/logs`),
            },
            {
              label: 'Trained Users',
              icon: <MemoViewInfo />,
              onClick: () => navigate(`/checklists/${item.id}/trained-user?tab=0`),
            },
          ]
        : !item.archived
        ? [
            {
              label: 'Sharing with Units',
              icon: <MemoViewInfo />,
              onClick: () => {
                dispatch(
                  openOverlayAction({
                    type: OverlayNames.PROCESS_SHARING,
                    props: { checklistId: item.id },
                  }),
                );
              },
            },
          ]
        : []),
    ],
    [item.id, item.name, item.archived],
  );

  const renderActions = () => {
    if (label === 'published') {
      return (
        <CustomMenu
          items={actionItems}
          type="list-menu"
          BeforeComponent={
            !item.archived &&
            checkPermission(['checklists', 'createJob']) && (
              <div className="primary" onClick={handleOnCreateJob}>
                <span>Create Job</span>
              </div>
            )
          }
        />
      );
    } else if (
      item.audit.createdBy.id === userId ||
      checkPermission(['checklists', 'archivePrototype'])
    ) {
      return (
        <div id="archive-unarchive" onClick={onArchiveOrUnarchive}>
          <MemoArchive style={{ marginRight: '8px' }} />
          {item.archived ? 'Unarchive' : 'Archive'}
        </div>
      );
    } else {
      return <div style={{ display: 'flex', justifyContent: 'center' }}>-N/A-</div>;
    }
  };

  return (
    <>
      {renderActions()}
      {createJobDrawerVisible && (
        <CreateJob
          checklist={{ label: item.name, value: item.id }}
          onCloseDrawer={setCreateJobDrawerVisible}
        />
      )}
    </>
  );
};

const ListView: FC<ListViewProps & { label: string }> = ({ label }) => {
  const dispatch = useDispatch();
  const checklistProperties = useTypedSelector(
    (state) => state.properties[ComposerEntity.CHECKLIST].list,
  );
  const checklistPropertiesLoading = useTypedSelector(
    (state) => state.properties[ComposerEntity.CHECKLIST].loading,
  );
  const checklistDataLoading = useTypedSelector((state) => state.checklistListView.loading);
  const pageable = useTypedSelector((state) => state.checklistListView.pageable);
  const currentPageData = useTypedSelector((state) => state.checklistListView.currentPageData);
  const userId = useTypedSelector((state) => state.auth.userId)!;
  const selectedUseCase = useTypedSelector((state) => state.auth.selectedUseCase);
  const facilityId = useTypedSelector((state) => state.auth.selectedFacility?.id);

  const [filterFields, setFilterFields] = useState<FilterField[]>(getBaseFilter(label));
  const [searchFilterFields, setSearchFilterFields] = useState<FilterField[]>([]);
  const [showPrototypeFilterDrawer, setPrototypeFilterDrawer] = useState<boolean>(false);

  const fetchData = (params: fetchDataParams = {}) => {
    const { page = DEFAULT_PAGE_NUMBER, size = DEFAULT_PAGE_SIZE, filters = filterFields } = params;
    dispatch(
      fetchChecklistsForListView({
        facilityId,
        page,
        size,
        sort: 'createdAt,desc',
        filters: JSON.stringify({
          op: FilterOperators.AND,
          fields: [
            ...filters,
            ...searchFilterFields,
            {
              field: 'useCaseId',
              op: FilterOperators.EQ,
              values: [selectedUseCase?.id],
            },
            ...(facilityId === ALL_FACILITY_ID
              ? [
                  {
                    field: 'isGlobal',
                    op: FilterOperators.EQ,
                    values: [true],
                  },
                ]
              : label === 'prototype'
              ? [
                  {
                    field: 'isGlobal',
                    op: FilterOperators.EQ,
                    values: [false],
                  },
                ]
              : []),
          ],
        }),
      }),
    );
  };

  useEffect(() => {
    dispatch(clearData());
  }, []);

  useEffect(() => {
    fetchData({ filters: filterFields });
  }, [filterFields, searchFilterFields, selectedUseCase]);

  const checkStartPrototypePermission = () => {
    if (facilityId === ALL_FACILITY_ID) {
      return checkPermission(['checklists', 'createGlobal']);
    }
    return checkPermission(['checklists', 'create']);
  };
  const onApplyMoreFilters = (filters: any) => {
    const stateFilter = filters?.find((filter: any) => filter?.field === 'state');
    const collaboratorFilter = filters?.find((filter: any) => filter?.field !== 'state');
    setFilterFields((currentFields) => [
      ...currentFields.filter((field) => field?.field !== 'state'),
      ...(stateFilter?.values
        ? [
            {
              id: stateFilter?.id,
              field: 'state',
              op: FilterOperators.ANY,
              values: stateFilter?.values,
            },
          ]
        : getBaseFilter(label).filter((field) => field?.field === 'state')),
    ]);

    setFilterFields((currentFields) => [
      ...currentFields.filter(
        (field) =>
          field?.field !== 'collaborators.type' &&
          field?.field !== 'collaborators.user.id' &&
          field?.field !== 'not.a.collaborator',
      ),
      ...(collaboratorFilter?.id
        ? collaboratorFilter?.values?.[0] === CollaboratorType.AUTHOR
          ? [
              {
                field: 'collaborators.user.id',
                op: FilterOperators.EQ,
                values: [userId],
              },
              {
                id: collaboratorFilter.id,
                field: 'collaborators.type',
                op: FilterOperators.ANY,
                values: [CollaboratorType.AUTHOR, CollaboratorType.PRIMARY_AUTHOR],
              },
            ]
          : collaboratorFilter?.values?.[0] === CollaboratorType.REVIEWER
          ? [
              {
                field: 'collaborators.user.id',
                op: FilterOperators.EQ,
                values: [userId],
              },
              {
                id: collaboratorFilter.id,
                field: 'collaborators.type',
                op: FilterOperators.ANY,
                values: [CollaboratorType.REVIEWER],
              },
            ]
          : [
              {
                id: collaboratorFilter.id,
                field: 'not.a.collaborator',
                op: FilterOperators.NE,
                values: [userId],
              },
            ]
        : []),
    ]);
  };

  const onSubmitDrawer = (data: any, callback: any) => {
    onApplyMoreFilters(data.filters);
    callback();
  };

  const onUploadError = (error: ResponseError[] | string) => {
    dispatch(
      showNotification({
        type: NotificationType.ERROR,
        msg: getErrorMsg(error) as string,
      }),
    );
  };

  const searchDropdownOptions = useMemo(
    () => [
      {
        label: 'Process Name',
        value: 'name',
        field: 'name',
        operator: FilterOperators.LIKE,
      },
      {
        label: 'Process ID',
        value: 'code',
        field: 'code',
        operator: FilterOperators.LIKE,
      },
      ...checklistProperties.map((property) => ({
        label: property.label,
        value: property.id,
        field: `properties.${property.id}`,
        operator: FilterOperators.LIKE,
      })),
    ],
    [checklistProperties],
  );

  const columns = useMemo(
    () => [
      ...(label === 'prototype'
        ? [
            {
              id: 'state',
              label: 'State',
              minWidth: 166,
              format(item: Checklist) {
                return (
                  <div style={{ display: 'flex', alignItems: 'center' }}>
                    <FiberManualRecord
                      className="icon"
                      style={{ color: ChecklistStatesColors[item.state] }}
                    />
                    <span title={ChecklistStatesContent[item.state]}>
                      {ChecklistStatesContent[item.state]}
                    </span>
                  </div>
                );
              },
            },
          ]
        : []),
      {
        id: 'name',
        label: 'Name',
        minWidth: 240,
        format(item: Checklist) {
          return (
            <span
              className="primary"
              onClick={() => navigate(`/checklists/${item.id}`)}
              title={item.name}
            >
              {item.name}
            </span>
          );
        },
      },
      {
        id: 'isGlobal',
        label: 'Local/Global',
        minWidth: 120,
        format(item: Checklist) {
          const tagTitle = item?.global ? 'Global' : 'Local';
          return (
            <span title={tagTitle}>
              <TypeChip
                label={tagTitle}
                $backGroundColor={item?.global ? '#d0e2ff' : '#a7f0ba'}
                $fontColor={item?.global ? '#0043ce' : '#0e6027'}
              />
            </span>
          );
        },
      },
      {
        id: 'checklist-id',
        label: 'Process ID',
        minWidth: 152,
        format(item: Checklist) {
          return <div key={item.id}>{item.code}</div>;
        },
      },
      ...checklistProperties.map((checklistProperty) => {
        return {
          id: checklistProperty.id,
          label: checklistProperty.label,
          minWidth: 125,
          maxWidth: 180,
        };
      }),
      {
        id: 'actions',
        label: 'Actions',
        minWidth: label === 'published' ? 170 : 100,
        format: (item: Checklist) => <ActionItem item={item} label={label} />,
      },
      ...(label === 'published' && currentPageData.some((item) => item.state === 'BEING_REVISED')
        ? [
            {
              id: 'revised',
              label: '',
              minWidth: 240,
              format(item: Checklist) {
                if (item.state === 'BEING_REVISED') {
                  return (
                    <div style={{ display: 'flex', alignItems: 'center' }}>
                      <FiberManualRecord
                        className="icon"
                        style={{
                          color: ChecklistStatesColors[item.state],
                        }}
                      />
                      {ChecklistStatesContent[item.state]}
                    </div>
                  );
                } else {
                  return <div />;
                }
              },
            },
          ]
        : []),
    ],
    [checklistProperties, currentPageData, label],
  );

  return (
    <TabContentWrapper>
      <div className="before-table-wrapper">
        <div className="filters">
          <SearchFilter
            showDropdown
            dropdownOptions={searchDropdownOptions}
            updateFilterFields={(fields, option) => {
              if (option && option.field.startsWith('properties.')) {
                const propertyId = option.field.replace('properties.', '');
                const searchValue = fields[0]?.values?.[0];

                if (searchValue) {
                  setSearchFilterFields([
                    {
                      field: 'checklistPropertyValues.facilityUseCasePropertyMapping.propertiesId',
                      op: FilterOperators.EQ,
                      values: [propertyId],
                    },
                    {
                      field: 'checklistPropertyValues.value',
                      op: FilterOperators.LIKE,
                      values: [searchValue],
                    },
                  ]);
                } else {
                  setSearchFilterFields([]);
                }
              } else {
                setSearchFilterFields(fields);
              }
            }}
          />
          {label === 'prototype' && (
            <div className="icon-filter" onClick={() => setPrototypeFilterDrawer(true)}>
              <img className="icon" src={FilterIcon} alt="filter icon" />
              {filterFields.filter((field) => field?.hasOwnProperty('id')).length > 0 && (
                <span>{`(${
                  filterFields.filter((field) => field?.hasOwnProperty('id')).length
                })`}</span>
              )}
            </div>
          )}
          <ToggleSwitch
            checkedIcon={false}
            offLabel="Show Archived"
            onLabel="Showing Archived"
            checked={!!filterFields.find((field) => field.field === 'archived')?.values[0]}
            onChange={(isChecked) =>
              setFilterFields((currentFields) =>
                currentFields.map((field) => ({
                  ...field,
                  ...(field.field === 'archived'
                    ? { values: [isChecked] }
                    : { values: field.values }),
                })),
              )
            }
            uncheckedIcon={false}
          />
          {label === 'prototype' && checkPermission(['checklists', 'importExport']) && (
            <ImageUploadButton
              icon={GetAppOutlined}
              label="Import"
              onUploadSuccess={() => {
                dispatch(
                  showNotification({
                    type: NotificationType.SUCCESS,
                    msg: 'Process Imported',
                  }),
                );
              }}
              onUploadError={onUploadError}
              apiCall={apiImportChecklist}
              acceptedTypes={['.zip']}
              useCaseId={selectedUseCase!.id}
            />
          )}
        </div>
        {checkStartPrototypePermission() && (
          <div className="actions">
            <Button
              onClick={() => {
                navigate('/checklists/prototype', {
                  state: { mode: FormMode.ADD },
                });
              }}
            >
              Start a Prototype
            </Button>
          </div>
        )}
      </div>
      <LoadingContainer
        loading={checklistDataLoading || checklistPropertiesLoading}
        component={
          <>
            <DataTable
              columns={columns}
              rows={currentPageData.map((item) => {
                return {
                  ...item,
                  ...item.properties.reduce<Record<string, string>>((obj, checklistProperty) => {
                    obj[checklistProperty.id] = checklistProperty.value;
                    return obj;
                  }, {}),
                };
              })}
              emptyTitle="No Processes Found"
            />
            <Pagination pageable={pageable} fetchData={fetchData} />
          </>
        }
      />
      {showPrototypeFilterDrawer && (
        <FiltersDrawer
          setState={setPrototypeFilterDrawer}
          onSubmit={onSubmitDrawer}
          filters={filterFields.filter((field) => field?.hasOwnProperty('id'))}
        />
      )}
    </TabContentWrapper>
  );
};

export default ListView;
