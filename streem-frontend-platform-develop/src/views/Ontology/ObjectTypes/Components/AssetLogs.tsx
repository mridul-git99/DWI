import {
  Button,
  DataTable,
  ListActionMenu,
  LoadingContainer,
  Pagination,
  TextInput,
  ToggleSwitch,
} from '#components';
import checkPermission from '#services/uiPermissions';
import { TabContentWrapper } from '#views/Jobs/ListView/styles';
import { MenuItem } from '@material-ui/core';
import { Search } from '@material-ui/icons';
import { debounce } from 'lodash';
import React, { FC, useEffect, useMemo, useState } from 'react';
import EditOutlinedIcon from '@material-ui/icons/EditOutlined';
import ArrowDropDown from '@material-ui/icons/ArrowDropDown';
import VisibilityOutlinedIcon from '@material-ui/icons/VisibilityOutlined';
import ArchiveIcon from '#assets/svg/archiveIcon.svg';
import { useTypedSelector } from '#store';
import { createFetchList } from '#hooks/useFetchData';
import {
  apiGetObjectTypeJobLogsCustomViews,
  apiToggleArchiveObjectTypeJobLogsCustomView,
} from '#utils/apiUrls';
import { DEFAULT_PAGE_NUMBER, DEFAULT_PAGE_SIZE } from '#utils/constants';
import { FilterField, FilterOperators } from '#utils/globalTypes';
import CreateLogViewDrawer from './CreateLogViewDrawer';
import { useDispatch } from 'react-redux';
import { openOverlayAction } from '#components/OverlayContainer/actions';
import { OverlayNames } from '#components/OverlayContainer/types';
import { showNotification } from '#components/Notification/actions';
import { NotificationType } from '#components/Notification/types';
import { getErrorMsg, request } from '#utils/request';
import { JobLogViewProvider } from '#hooks/useCreateJobLogView';

const AssetLogs: FC = () => {
  const dispatch = useDispatch();
  const urlParams = useMemo(
    () => ({
      page: DEFAULT_PAGE_NUMBER,
      size: DEFAULT_PAGE_SIZE,
      sort: 'createdAt,desc',
    }),
    [],
  );

  const objectTypeId = useTypedSelector((state) => state.ontology?.objectTypes?.active?.id || '');
  const externalId = useTypedSelector(
    (state) => state.ontology?.objectTypes?.active?.externalId || '',
  );
  const facilityId = useTypedSelector((state) => state.auth?.selectedFacility?.id || '');

  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const [selectedView, setSelectedView] = useState<any>(null);
  const [reRender, setReRender] = useState<boolean>(false);
  const [createLogViewDrawer, setCreateLogViewDrawer] = useState(false);
  const [showArchived, setShowArchived] = useState(false);
  const [filterFields, setFilterFields] = useState<FilterField[]>([
    { field: 'facilityId', op: FilterOperators.EQ, values: [facilityId] },
    {
      field: 'objectTypeId',
      op: FilterOperators.EQ,
      values: [objectTypeId],
    },
    {
      field: 'archived',
      op: FilterOperators.EQ,
      values: [showArchived],
    },
  ]);

  const { list, reset, pagination, status } = createFetchList(
    apiGetObjectTypeJobLogsCustomViews(),
    urlParams,
    false,
  );

  const handleClose = () => {
    setAnchorEl(null);
  };

  const toggleArchiveLogView = async ({
    id,
    isArchiving,
    reason,
  }: {
    id: string;
    isArchiving: boolean;
    reason: string;
  }) => {
    const url = isArchiving
      ? apiToggleArchiveObjectTypeJobLogsCustomView(id, 'archive')
      : apiToggleArchiveObjectTypeJobLogsCustomView(id, 'unarchive');

    const { data, errors } = await request('PATCH', url, {
      data: {
        reason,
      },
    });

    if (data) {
      dispatch(
        showNotification({
          type: NotificationType.SUCCESS,
          msg: `Log View ${isArchiving ? 'archived' : 'unarchived'} successfully`,
        }),
      );
      setReRender((prev) => !prev);
    } else if (errors) {
      dispatch(
        showNotification({
          type: NotificationType.ERROR,
          msg: getErrorMsg(errors),
        }),
      );
    }
  };

  useEffect(() => {
    reset({
      params: {
        ...urlParams,
        filters: {
          op: FilterOperators.AND,
          fields: filterFields,
        },
      },
    });
  }, [filterFields, reRender]);

  return (
    <JobLogViewProvider>
      <TabContentWrapper>
        <div className="before-table-wrapper">
          <div className="filters">
            <TextInput
              afterElementWithoutError
              AfterElement={Search}
              afterElementClass=""
              placeholder={`Search with Log View Name `}
              onChange={debounce(({ value }) => {
                setFilterFields([
                  ...filterFields.filter((field) => field.field !== 'label'),
                  {
                    field: 'label',
                    op: FilterOperators.LIKE,
                    values: [value],
                  },
                ]);
              }, 500)}
            />
            <ToggleSwitch
              checkedIcon={false}
              uncheckedIcon={false}
              offLabel="Show Archived"
              onLabel="Showing Archived"
              checked={showArchived}
              onChange={(value) => {
                setShowArchived(value);
                setFilterFields([
                  ...filterFields.filter((field) => field.field !== 'archived'),
                  {
                    field: 'archived',
                    op: FilterOperators.EQ,
                    values: [value],
                  },
                ]);
              }}
            />
          </div>
          {checkPermission(['ontology', 'createObject']) && (
            <div className="actions">
              <Button
                onClick={() => {
                  setCreateLogViewDrawer(true);
                }}
              >
                Create new
              </Button>
            </div>
          )}
        </div>
        <LoadingContainer
          loading={status === 'loading'}
          component={
            <DataTable
              columns={[
                {
                  id: 'logViewName',
                  label: 'Log View Name',
                  minWidth: 400,
                  format: (item) => {
                    return item?.label;
                  },
                },

                ...(checkPermission(['ontology', 'createObject'])
                  ? [
                      {
                        id: 'actions',
                        label: 'Actions',
                        minWidth: 100,
                        format: function renderComp(item) {
                          return (
                            <div style={{ display: 'flex', gap: 16 }}>
                              <div
                                id="more-actions"
                                onClick={(event: any) => {
                                  setAnchorEl(event.currentTarget);
                                  setSelectedView(item);
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
                                onClose={() => {
                                  setSelectedView(null);
                                  handleClose();
                                }}
                              >
                                {!showArchived && (
                                  <>
                                    <MenuItem
                                      onClick={() => {
                                        handleClose();
                                        dispatch(
                                          openOverlayAction({
                                            type: OverlayNames.OBJECT_PREVIEW_MODAL,
                                            props: {
                                              collection: externalId,
                                              customView: selectedView,
                                              setSelectedView,
                                            },
                                          }),
                                        );
                                      }}
                                    >
                                      <div className="list-item">
                                        <VisibilityOutlinedIcon />
                                        <span>View</span>
                                      </div>
                                    </MenuItem>
                                    <MenuItem
                                      onClick={() => {
                                        handleClose();
                                        setCreateLogViewDrawer(true);
                                      }}
                                    >
                                      <div className="list-item">
                                        <EditOutlinedIcon />
                                        <span>Edit</span>
                                      </div>
                                    </MenuItem>
                                  </>
                                )}
                                <MenuItem
                                  onClick={() => {
                                    handleClose();
                                    dispatch(
                                      openOverlayAction({
                                        type: OverlayNames.REASON_MODAL,
                                        props: {
                                          modalTitle: `${
                                            showArchived ? 'Unarchive' : 'Archive'
                                          } Asset Log`,
                                          modalDesc: `Enter your reason to ${
                                            showArchived ? 'unarchive' : 'archive'
                                          }`,
                                          onSubmitHandler: (
                                            reason: string,
                                            closeModal: () => void,
                                          ) => {
                                            toggleArchiveLogView({
                                              id: selectedView?.id,
                                              isArchiving: !showArchived,
                                              reason,
                                            });
                                            setSelectedView(null);
                                            closeModal();
                                          },
                                        },
                                      }),
                                    );
                                  }}
                                >
                                  <div className="list-item">
                                    {!showArchived && <img src={ArchiveIcon} alt="Archive Icon" />}
                                    <span style={{ color: showArchived ? '#333333' : '#DA1E28' }}>
                                      {showArchived ? 'Unarchive' : 'Archive'}
                                    </span>
                                  </div>
                                </MenuItem>
                              </ListActionMenu>
                            </div>
                          );
                        },
                      },
                    ]
                  : []),
              ]}
              emptyTitle="No asset log configured"
              rows={list}
            />
          }
        />
        <Pagination
          pageable={pagination}
          fetchData={(p) =>
            reset({
              params: {
                ...urlParams,
                filters: {
                  op: FilterOperators.AND,
                  fields: filterFields,
                },
                page: p.page,
                size: p.size,
              },
            })
          }
        />
        {createLogViewDrawer && (
          <CreateLogViewDrawer
            onCloseDrawer={setCreateLogViewDrawer}
            selectedView={selectedView}
            setReRender={setReRender}
            setSelectedView={setSelectedView}
          />
        )}
      </TabContentWrapper>
    </JobLogViewProvider>
  );
};

export default AssetLogs;
