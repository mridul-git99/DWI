import { AssigneeList, DataTable, ListActionMenu, LoadingContainer, Pagination } from '#components';
import { openOverlayAction } from '#components/OverlayContainer/actions';
import { OverlayNames } from '#components/OverlayContainer/types';
import { MenuItem } from '@material-ui/core';
import { ArrowDropDown, FiberManualRecord } from '@material-ui/icons';
import React, { useCallback, useMemo, useState } from 'react';
import { useDispatch } from 'react-redux';
import RemoveCircleOutlineIcon from '@material-ui/icons/RemoveCircleOutline';
import { getFullName } from '#utils/stringUtils';
import { navigate } from '@reach/router';
import { apiMapUsersGroupsForChecklist } from '#utils/apiUrls';
import { getErrorMsg, request } from '#utils/request';
import { showNotification } from '#components/Notification/actions';
import { NotificationType } from '#components/Notification/types';
import checkPermission from '#services/uiPermissions';

export const TableComponent = ({
  currentPageData,
  pageable,
  fetchData,
  checklistId,
  tab,
  status,
}: any) => {
  const dispatch = useDispatch();
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const [selectedData, setSelectedData] = React.useState<any>({});
  // const [openPermissionDrawer, setOpenPermissionDrawer] = useState(false);

  const handleClose = useCallback(() => {
    setAnchorEl(null);
  }, []);

  const removeUserGroupPayload = useCallback(
    (item, reason) => {
      if (tab === 'users') {
        return {
          data: {
            unassignedUserIds: [item?.userId],
            reason: reason,
          },
          params: { notify: true },
        };
      } else {
        return {
          data: {
            unassignedUserGroupIds: [item?.userGroupId],
            reason: reason,
          },
          params: { notify: true },
        };
      }
    },
    [tab],
  );

  const onRemove = useCallback(
    async (reason) => {
      const { errors, status } = await request(
        'PATCH',
        apiMapUsersGroupsForChecklist(checklistId as string),
        removeUserGroupPayload(selectedData, reason),
      );
      if (status === 'OK') {
        dispatch(
          showNotification({
            type: NotificationType.SUCCESS,
            msg: `${
              tab === 'users'
                ? 'Trained user deleted from the checklist'
                : 'Trained user group deleted from the checklist'
            }`,
            detail: `${
              tab === 'users'
                ? 'You have successfully removed user as trained user.'
                : 'You have successfully removed as trained group.'
            }`,
          }),
        );
        fetchData && fetchData({});
      } else if (errors) {
        dispatch(
          showNotification({
            type: NotificationType.ERROR,
            msg: getErrorMsg(errors),
          }),
        );
      }
      setAnchorEl(null);
    },
    [selectedData, checklistId],
  );

  const modalDescription = useCallback(() => {
    if (tab === 'users') {
      return `Are you sure you want to Remove ${getFullName(selectedData)} (ID: ${
        selectedData.employeeId
      })`;
    } else {
      return `Are you sure you want to Remove ${selectedData.userGroupName}`;
    }
  }, [tab, getFullName, selectedData]);

  const renderButtons = useCallback(
    (item: any) => {
      return (
        <>
          <div
            id="more-actions"
            onClick={(event) => {
              setAnchorEl(event.currentTarget);
              setSelectedData(item);
            }}
          >
            <span>More</span> <ArrowDropDown className="icon" />
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
            {/* <MenuItem
              onClick={() => {
                setAnchorEl(null);
                setOpenPermissionDrawer(true);
              }}
            >
              <div className="list-item">
                <EditOutlined />
                <span>Edit Process Roles</span>
              </div>
            </MenuItem> */}
            <MenuItem
              onClick={() => {
                setAnchorEl(null);
                dispatch(
                  openOverlayAction({
                    type: OverlayNames.REASON_MODAL,
                    props: {
                      modalTitle: tab === 'users' ? 'Remove Trained User' : 'Remove User Group',
                      modalDesc: modalDescription(),
                      onSubmitHandler: (reason: string, callback: any) => {
                        onRemove(reason);
                        callback && callback();
                      },
                      onSubmitModalText: 'Confirm',
                    },
                  }),
                );
              }}
            >
              <div className="list-item">
                <RemoveCircleOutlineIcon style={{ color: 'red' }} />
                <span>Remove</span>
              </div>
            </MenuItem>
          </ListActionMenu>
        </>
      );
    },
    [anchorEl, dispatch, handleClose, onRemove, selectedData],
  );

  const usersColumns = useMemo(
    () => [
      {
        id: 'name',
        label: 'User Name',
        minWidth: 120,
        format: function renderComp(item: any) {
          return <span>{getFullName(item)}</span>;
        },
      },
      {
        id: 'id',
        label: 'Employee Id',
        minWidth: 120,
        format: function renderComp(item: any) {
          return <div>{item?.employeeId}</div>;
        },
      },
      {
        id: 'email',
        label: 'Email Id',
        minWidth: 120,
        format: function renderComp(item: any) {
          return <div>{item?.emailId}</div>;
        },
      },
      {
        id: 'status',
        label: 'Status',
        minWidth: 120,
        format: function renderComp(item: any) {
          return (
            <div key={item?.id}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
                <FiberManualRecord
                  className="icon"
                  style={{ color: item.status ? 'green' : 'red', width: 8, height: 8 }}
                />
                {item.status ? 'Assigned' : 'Unassigned'}
              </div>
            </div>
          );
        },
      },
      ...(checkPermission(['trainedUsers', 'edit'])
        ? [
            {
              id: 'actions',
              label: 'Actions',
              minWidth: 120,
              format: function renderComp(item: any) {
                return renderButtons(item);
              },
            },
          ]
        : []),
    ],
    [navigate, renderButtons],
  );

  const groupColumns = useMemo(
    () => [
      {
        id: 'name',
        label: 'Group Name',
        minWidth: 100,
        format: function renderComp(item: any) {
          return <span>{item.userGroupName}</span>;
        },
      },
      {
        id: 'id',
        label: 'Users',
        minWidth: 200,
        format: function renderComp(item: any) {
          return (
            <AssigneeList users={item.users || []} count={10} userCount={item.userCount || 0} />
          );
        },
      },
      {
        id: 'status',
        label: 'Status',
        minWidth: 100,
        format: function renderComp(item: any) {
          return (
            <div key={item?.id}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
                <FiberManualRecord
                  className="icon"
                  style={{ color: item.status ? 'green' : 'red', width: 8, height: 8 }}
                />
                {item.status ? 'Assigned' : 'Unassigned'}
              </div>
            </div>
          );
        },
      },
      ...(checkPermission(['trainedUsers', 'edit'])
        ? [
            {
              id: 'actions',
              label: 'Actions',
              minWidth: 152,
              format: function renderComp(item: any) {
                return renderButtons(item);
              },
            },
          ]
        : []),
    ],
    [dispatch, renderButtons],
  );

  return (
    <div style={{ display: 'contents' }}>
      <LoadingContainer
        component={
          <DataTable
            columns={tab === 'users' ? usersColumns : groupColumns}
            rows={currentPageData}
            emptyTitle={`${tab === 'users' ? 'No User Added' : 'No Group Added'}`}
          />
        }
        loading={status === 'loading'}
      />
      <Pagination pageable={pageable} fetchData={fetchData} />
      {/* {openPermissionDrawer && <EditPermission onCloseDrawer={setOpenPermissionDrawer} />} */}
    </div>
  );
};
