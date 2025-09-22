import ArchiveIcon from '#assets/svg/archiveIcon.svg';
import AuditLogsIcon from '#assets/svg/auditLogIcons.svg';
import { AssigneeList, ListActionMenu } from '#components';
import { showNotification } from '#components/Notification/actions';
import { NotificationType } from '#components/Notification/types';
import { openOverlayAction } from '#components/OverlayContainer/actions';
import { OverlayNames } from '#components/OverlayContainer/types';
import { createFetchList } from '#hooks/useFetchData';
import { useQueryParams } from '#hooks/useQueryParams';
import { apiUserGroupArchived, apiUserGroups } from '#utils/apiUrls';
import { DEFAULT_PAGE_NUMBER, DEFAULT_PAGE_SIZE } from '#utils/constants';
import { FilterOperators } from '#utils/globalTypes';
import { getErrorMsg, request } from '#utils/request';
import { TabContentWrapper } from '#views/Jobs/ListView/styles';
import { MenuItem } from '@material-ui/core';
import { ArrowDropDown } from '@material-ui/icons';
import EditOutlinedIcon from '@material-ui/icons/EditOutlined';
import { navigate } from '@reach/router';
import React, { MouseEvent, useCallback, useEffect, useMemo } from 'react';
import { useDispatch } from 'react-redux';
import { UserGroupTable } from '../UserGroups/UserGroupTable';
import { UserGroupsFilter } from './UserGroupsFilter';
import { useTypedSelector } from '#store';

const isJSONString = (str: string) => {
  try {
    JSON.parse(str);
    return true;
  } catch (e) {
    return false;
  }
};

export const UserGroups = () => {
  const dispatch = useDispatch();
  const [anchorEl, setAnchorEl] = React.useState(null);
  const [selectedGroup, setSelectedGroup] = React.useState();
  const { list, reset, pagination } = createFetchList(apiUserGroups(), {}, false);
  const { id: selectedFacilityId } = useTypedSelector((state) => state.auth.selectedFacility);
  const [showAddNewUserGroup, setShowAddNewUserGroup] = React.useState(true);
  const { getQueryParam } = useQueryParams();
  const fields = getQueryParam('fields', []);
  const archive = getQueryParam('archive') === 'true';
  const [filterParams, setFilterParams] = React.useState({
    tab: 1,
    archive: archive,
    fields: isJSONString(fields) ? JSON.parse(fields) : fields || [],
  });

  const fetchUserGroups = useCallback(
    (params = {}, fields, archive) => {
      const { page = DEFAULT_PAGE_NUMBER, size = DEFAULT_PAGE_SIZE } = params;
      reset({
        params: {
          page,
          size,
          filters: {
            op: FilterOperators.AND,
            fields: [
              ...(fields || []),
              { field: 'active', op: FilterOperators.EQ, values: [!archive] },
            ],
          },
          sort: 'createdAt,desc',
        },
      });
    },
    [reset],
  );

  const handleClose = useCallback(() => {
    setAnchorEl(null);
  }, []);

  const onArchiveGroup = useCallback(
    async (group: any) => {
      const response = await request('PATCH', apiUserGroupArchived(group?.id, group.active), {
        params: { reason: group.reason },
      });
      if (response.status === 'OK') {
        dispatch(
          showNotification({
            type: NotificationType.SUCCESS,
            msg: `Group has been ${group.active ? 'Unarchived' : 'Archived'}`,
            detail: `${group.name} has been ${
              group.active ? 'unarchived' : 'archived'
            } successfully.`,
          }),
        );
        setFilterParams({ ...filterParams, fields: [] });
      } else {
        dispatch(
          showNotification({
            type: NotificationType.ERROR,
            msg: getErrorMsg(response?.errors),
          }),
        );
      }
    },
    [request, apiUserGroups, filterParams, setFilterParams],
  );

  const onEditGroup = useCallback(
    (group: any) => {
      navigate(`/users/group/edit/${group.id}`);
    },
    [navigate],
  );

  const onAuditLogs = useCallback(
    (group: any) => {
      navigate(`/users/group/audit-logs/${group.id}`);
    },
    [navigate],
  );

  useEffect(() => {
    navigate(
      `?tab=1&archive=${filterParams.archive}&fields=${
        isJSONString(filterParams.fields)
          ? filterParams.fields
          : JSON.stringify(filterParams.fields) || []
      }`,
    );
    fetchUserGroups(
      {},
      isJSONString(filterParams.fields) ? JSON.parse(filterParams.fields) : filterParams.fields,
      filterParams.archive,
    );
    setShowAddNewUserGroup(selectedFacilityId !== '-1');
  }, [filterParams]);

  const renderButtons = useCallback(
    (item: any) => {
      return (
        <>
          <div
            className="list-card-columns"
            id="more-actions"
            onClick={(event: MouseEvent<HTMLDivElement>) => {
              setAnchorEl(event.currentTarget);
              setSelectedGroup(item);
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
            style={{ marginTop: 30 }}
          >
            {!filterParams.archive && (
              <MenuItem
                onClick={() => {
                  setAnchorEl(null);
                  onEditGroup(selectedGroup);
                }}
              >
                <div className="list-item">
                  <EditOutlinedIcon />
                  <span>Edit</span>
                </div>
              </MenuItem>
            )}
            <MenuItem
              onClick={() => {
                setAnchorEl(null);
                onAuditLogs(selectedGroup);
              }}
            >
              <div className="list-item">
                <img src={AuditLogsIcon} alt="Audit Logs Icon" />
                <span>Audit Logs</span>
              </div>
            </MenuItem>
            <MenuItem
              onClick={() => {
                setAnchorEl(null);
                dispatch(
                  openOverlayAction({
                    type: OverlayNames.REASON_MODAL,
                    props: {
                      modalTitle: `${!filterParams.archive ? 'Archive' : 'Unarchive'} Group`,
                      modalDesc: `Are you sure you want to ${
                        !filterParams.archive ? 'archive' : 'unarchive'
                      } this group?`,
                      onSubmitHandler: (reason: string, _, callback) => {
                        onArchiveGroup({ ...selectedGroup, reason, active: filterParams.archive });
                        callback && callback();
                      },
                      onSubmitModalText: 'Confirm',
                    },
                  }),
                );
              }}
            >
              <div className="list-item">
                <img src={ArchiveIcon} alt="Archive Icon" />
                <span>{!filterParams.archive ? 'Archive' : 'Unarchive'}</span>
              </div>
            </MenuItem>
          </ListActionMenu>
        </>
      );
    },
    [
      anchorEl,
      setAnchorEl,
      onEditGroup,
      dispatch,
      openOverlayAction,
      ArchiveIcon,
      AuditLogsIcon,
      filterParams,
      selectedGroup,
    ],
  );

  const columns = useMemo(
    () => [
      {
        id: 'groupName',
        label: 'Group Name',
        minWidth: 240,
        format: function renderComp(item: any) {
          return (
            <span
              className="primary"
              style={{ textTransform: 'capitalize' }}
              onClick={() => {
                navigate(`/users/group/edit/${item.id}?readOnly=true`);
              }}
              title={item.description}
            >
              {item.name}
            </span>
          );
        },
      },
      {
        id: 'users',
        label: 'Users',
        minWidth: 360,
        format: function renderComp(item: any) {
          return (
            <div className="primary" style={{ textTransform: 'capitalize' }}>
              <AssigneeList users={item.users} count={10} userCount={item.userCount} />
            </div>
          );
        },
      },
      {
        id: 'actions',
        label: 'Actions',
        minWidth: 152,
        format: function renderComp(item: any) {
          return renderButtons(item);
        },
      },
    ],
    [navigate, renderButtons],
  );

  return (
    <TabContentWrapper>
      <UserGroupsFilter
        filterParams={filterParams}
        setFilterParams={setFilterParams}
        showAddNewUserGroup={showAddNewUserGroup}
      />
      <UserGroupTable
        columns={columns}
        currentPageData={list}
        pageable={pagination}
        fetchData={fetchUserGroups}
      />
    </TabContentWrapper>
  );
};
