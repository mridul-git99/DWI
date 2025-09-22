import { Button, StyledTabs, useDrawer } from '#components';
import { showNotification } from '#components/Notification/actions';
import { NotificationType } from '#components/Notification/types';
import { apiAssignUsersForChecklist, apiBulkAssignUsers } from '#utils/apiUrls';
import { getErrorMsg, request } from '#utils/request';
import React, { FC, useCallback, useEffect, useMemo, useState } from 'react';
import { useFieldArray, useForm } from 'react-hook-form';
import { useDispatch } from 'react-redux';
import styled from 'styled-components';
import { ChecklistUserGroupAssign } from './ChecklistUserGroupAssign';
import { openOverlayAction } from '#components/OverlayContainer/actions';
import { OverlayNames } from '#components/OverlayContainer/types';

export const AssignTaskWrapper = styled.form`
  display: flex;
  flex-direction: column;
  flex: 1;
  height: 100%;

  .assignment-tabs-container {
    height: 100%;
  }

  .assignment-tabs-panel {
    padding: 0;
    height: calc(100% - 49px);
  }

  .user-groups-contents {
    overflow: hidden scroll;
    padding: 0 16px;
  }

  .filters {
    display: flex;
    justify-content: space-between;
    margin-block: 16px;

    .input-wrapper {
      max-width: 300px;
    }
  }

  .no-data-found {
    text-align: center;
    padding: 16px;
    font-size: 16px;
    color: #ccc;
    margin-top: 16px;
  }
`;

export const AssignTask: FC<{
  onCloseDrawer: React.Dispatch<React.SetStateAction<any>>;
  checklistId: string;
  selectedTasks: any[];
  source: string;
  jobId?: string;
  renderAssignedUsers?: any;
}> = ({ onCloseDrawer, checklistId, selectedTasks, source, jobId, renderAssignedUsers }) => {
  const dispatch = useDispatch();
  const [isDataFetched, setIsDataFetched] = useState({ users: false, userGroups: false });
  const [activeStep, setActiveStep] = useState(0);
  const [deselectUsers, setDeselectUsers] = useState<any[]>([]);
  const [deselectGroups, setDeselectGroups] = useState<any[]>([]);
  const [userData, setUserData] = useState<any[]>([]);
  const [groupData, setGroupData] = useState<any[]>([]);

  const form = useForm<{
    users: any[];
    groups: any[];
  }>({
    mode: 'onChange',
    criteriaMode: 'all',
    defaultValues: {
      users: [],
      groups: [],
    },
  });

  const {
    handleSubmit,
    control,
    formState: { isDirty, isValid },
  } = form;

  const {
    fields: usersFields,
    append: usersAppend,
    remove: usersRemove,
  } = useFieldArray({
    control,
    name: 'users',
  });

  const {
    fields: groupsFields,
    append: groupsAppend,
    remove: groupsRemove,
  } = useFieldArray({
    control,
    name: 'groups',
  });

  const newGroups = useMemo(
    () =>
      groupsFields.filter(
        (item: any) =>
          !groupData.some(
            (oldItem: any) => (oldItem.userGroupId || oldItem.id) === (item.userGroupId || item.id),
          ),
      ),
    [groupData, groupsFields],
  );

  const deletedGroups = useMemo(
    () =>
      groupData.filter(
        (oldItem: any) =>
          !groupsFields.some(
            (item: any) => (item.userGroupId || item.id) === (oldItem.userGroupId || oldItem.id),
          ),
      ),
    [groupData, groupsFields],
  );

  const newUsers = useMemo(
    () =>
      usersFields.filter(
        (item: any) =>
          !userData.some(
            (oldItem: any) => (oldItem.userId || oldItem.id) === (item.userId || item.id),
          ),
      ),
    [userData, usersFields],
  );

  const deletedUsers = useMemo(
    () =>
      userData.filter(
        (oldItem: any) =>
          !usersFields.some(
            (item: any) => (item.userId || item.id) === (oldItem.userId || oldItem.id),
          ),
      ),
    [userData, usersFields],
  );

  useEffect(() => {
    setDrawerOpen(true);
    return () => {
      handleCloseDrawer();
    };
  }, [onCloseDrawer]);

  const onTabChange = useCallback(
    (count) => {
      setActiveStep(count);
    },
    [setActiveStep],
  );

  const checkIsDirty = useCallback(() => {
    if (
      newGroups.length === 0 &&
      deletedGroups.length === 0 &&
      newUsers.length === 0 &&
      deletedUsers.length === 0
    ) {
      return false;
    }
    if (isDirty) {
      return true;
    }
    return false;
  }, [isDirty, newGroups, deletedGroups, newUsers, deletedUsers]);

  const handleCloseDrawer = useCallback(() => {
    setDrawerOpen && setDrawerOpen(false);
    setTimeout(() => {
      onCloseDrawer(false);
    }, 200);
  }, [onCloseDrawer, setDrawerOpen]);

  const handleSubmitHandler = useCallback(
    async (notify) => {
      const payload = {
        assignedUserIds: newUsers.map((el) => el.userId || el.id) || [],
        unassignedUserIds: deletedUsers.map((el) => el.userId || el.id),
        assignedUserGroupIds: newGroups.map((el) => el.userGroupId || el.id) || [],
        unassignedUserGroupIds: deletedGroups.map((el) => el.userGroupId || el.id),
        ...(source === 'job'
          ? {
              taskExecutionIds: (
                selectedTasks?.map((task) => task.length === 2 && task[0]) || []
              )?.map((el) => el),
            }
          : {
              taskIds: selectedTasks,
            }),
      };
      if (source === 'job') {
        const { data, errors } = await request('PATCH', apiBulkAssignUsers(jobId as string), {
          data: payload,
          params: { notify },
        });
        if (data && renderAssignedUsers) {
          dispatch(
            showNotification({
              type: NotificationType.SUCCESS,
              msg: 'Successfully updated assignments.',
            }),
          );
          renderAssignedUsers();
        } else if (errors) {
          dispatch(
            openOverlayAction({
              type: OverlayNames.ASSIGNMENT_INFO,
              props: {
                errors,
                assignedUsers: newUsers,
                unassignedUsers: deletedUsers,
                assignedUserGroups: newGroups,
                unassignedUserGroups: deletedGroups,
              },
            }),
          );
        }
      } else {
        const { data, errors } = await request(
          'PATCH',
          apiAssignUsersForChecklist(checklistId as string),
          {
            data: payload,
            params: { notify },
          },
        );
        if (data && renderAssignedUsers) {
          dispatch(
            showNotification({
              type: NotificationType.SUCCESS,
              msg: 'Successfully updated assignments.',
            }),
          );
          renderAssignedUsers();
        } else if (errors) {
          dispatch(
            showNotification({
              type: NotificationType.ERROR,
              msg: getErrorMsg(errors),
            }),
          );
        }
      }
      handleCloseDrawer && handleCloseDrawer();
    },
    [
      selectedTasks,
      handleCloseDrawer,
      newUsers,
      deletedUsers,
      newGroups,
      deletedGroups,
      source,
      jobId,
    ],
  );

  const sections = useMemo(
    () => [
      {
        value: '0',
        label: 'Users',
        panelContent: (
          <ChecklistUserGroupAssign
            form={form}
            usersFields={usersFields}
            groupsFields={groupsFields}
            usersAppend={usersAppend}
            usersRemove={usersRemove}
            activeStep={activeStep}
            source={source}
            checklistId={checklistId}
            selectedTasks={selectedTasks}
            isDataFetched={isDataFetched}
            setIsDataFetched={setIsDataFetched}
            setUserData={setUserData}
            setGroupData={setGroupData}
            deselectUsers={deselectUsers}
            setDeselectUsers={setDeselectUsers}
            deselectGroups={deselectGroups}
            setDeselectGroups={setDeselectGroups}
            section="users"
          />
        ),
      },
      {
        value: '1',
        label: 'User Groups',
        panelContent: (
          <ChecklistUserGroupAssign
            form={form}
            usersFields={usersFields}
            groupsFields={groupsFields}
            groupsAppend={groupsAppend}
            groupsRemove={groupsRemove}
            activeStep={activeStep}
            source={source}
            checklistId={checklistId}
            selectedTasks={selectedTasks}
            isDataFetched={isDataFetched}
            setIsDataFetched={setIsDataFetched}
            setUserData={setUserData}
            setGroupData={setGroupData}
            deselectUsers={deselectUsers}
            setDeselectUsers={setDeselectUsers}
            deselectGroups={deselectGroups}
            setDeselectGroups={setDeselectGroups}
            section="userGroups"
          />
        ),
      },
    ],
    [checklistId, selectedTasks, form, usersFields, groupsFields],
  );

  const { StyledDrawer, setDrawerOpen } = useDrawer({
    title: 'Assign Users',
    hideCloseIcon: true,
    bodyContent: (
      <AssignTaskWrapper
        onSubmit={(e) => {
          e.preventDefault();
          handleSubmit(handleSubmitHandler);
        }}
      >
        <StyledTabs
          containerProps={{
            className: 'assignment-tabs-container',
          }}
          panelsProps={{
            className: 'assignment-tabs-panel',
          }}
          activeTab={activeStep.toString()}
          onChange={onTabChange}
          tabs={sections}
        />
      </AssignTaskWrapper>
    ),
    footerContent: (
      <>
        <Button
          variant="textOnly"
          color="blue"
          disabled={!isValid || !checkIsDirty?.()}
          onClick={() => handleSubmitHandler(false)}
        >
          Confirm Without Notifying
        </Button>
        <Button
          variant="secondary"
          style={{ marginLeft: 'auto' }}
          onClick={() => {
            handleCloseDrawer();
          }}
        >
          Cancel
        </Button>
        <Button disabled={!isValid || !checkIsDirty?.()} onClick={() => handleSubmitHandler(true)}>
          Confirm
        </Button>
      </>
    ),
    footerProps: {
      style: {
        justifyContent: 'flex-start',
      },
    },
  });

  return StyledDrawer;
};
