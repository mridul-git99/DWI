import { Button, StyledTabs } from '#components';
import { createFetchList } from '#hooks/useFetchData';
import { defaultParams } from '#services/users';
import { useTypedSelector } from '#store';
import { User } from '#store/users/types';
import { DEFAULT_PAGE_NUMBER, DEFAULT_PAGE_SIZE } from '#utils/constants';
import { FilterField, FilterOperators } from '#utils/globalTypes';
import { AssignTaskWrapper } from '#views/Checklists/UserGroups/AssignTask';
import { PeerVerificationUserGroupAssign } from '#views/Job/components/Task/Parameters/Verification/PeerVerificationUserGroupAssign';
import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { useFieldArray, useForm } from 'react-hook-form';

const urlParams = {
  page: DEFAULT_PAGE_NUMBER,
  size: DEFAULT_PAGE_SIZE,
  filters: {
    op: FilterOperators.AND,
    fields: [{ field: 'archived', op: FilterOperators.EQ, values: [false] }],
  },
};

export enum ShowSectionUsersAndUserGroupTabs {
  USERS = 'USERS',
  GROUPS = 'GROUPS',
  BOTH = 'BOTH',
}

interface Props {
  onSubmit?: (payload: any, handleClose: () => void) => void;
  handleCloseDrawer?: () => void;
  apiUrlUserGroup: () => string;
  apiUrlUser: () => string;
  showRoleTag?: boolean;
  showSections?: ShowSectionUsersAndUserGroupTabs;
  shouldFilterList?: boolean;
}

export const useUserAndUserGroupTabs = ({
  onSubmit,
  handleCloseDrawer,
  apiUrlUserGroup,
  apiUrlUser,
  showRoleTag = true,
  showSections = ShowSectionUsersAndUserGroupTabs.BOTH,
  shouldFilterList = false,
}: Props) => {
  const [activeStep, setActiveStep] = useState(0);

  const {
    auth: { userId },
  } = useTypedSelector((state) => state);

  const [selectedKey, setSelectedKey] = useState<string>('firstName');

  const {
    list: userGroupsList,
    reset: userGroupsReset,
    fetchNext: userGroupsFetchNext,
  } = createFetchList(apiUrlUserGroup(), {}, false);

  const {
    list: assigneeList,
    reset: assigneeReset,
    fetchNext: assigneeFetchNext,
  } = createFetchList<User[]>(apiUrlUser(), urlParams, true);

  const form = useForm({
    mode: 'onChange',
    criteriaMode: 'all',
    defaultValues: {
      users: [],
      groups: [],
    },
  });

  const {
    formState: { isDirty },
    control,
    setValue,
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

  const deselectAll = () => {
    if (selectedKey === 'groupName') {
      setValue('groups', []);
    } else {
      setValue('users', []);
    }
  };

  const handleClickHandler = (data: any, checked: boolean) => {
    if (selectedKey === 'groupName') {
      if (checked) {
        groupsAppend(data);
      } else {
        const groupIndex = groupsFields.findIndex((g) => g.id === data.id);
        groupsRemove(groupIndex);
      }
    } else {
      if (checked) {
        usersAppend(data);
      } else {
        const userIndex = usersFields.findIndex((g) => g.id === data.id);
        usersRemove(userIndex);
      }
    }
  };

  const disableButton = () => {
    if (groupsFields.length === 0 && usersFields.length === 0) {
      return true;
    } else {
      return !isDirty || !isDirty;
    }
  };

  const handleScroll = (event: any, fetchNext: () => void) => {
    const { scrollHeight, scrollTop, clientHeight } = event.currentTarget;
    if (scrollHeight - Math.ceil(scrollTop) <= clientHeight) {
      fetchNext();
    }
  };

  const assigneeItemScroll = (event: any) => handleScroll(event, assigneeFetchNext);

  const groupItemScroll = (event: any) => handleScroll(event, userGroupsFetchNext);

  const fetchUserAndUserGroups = (fields: FilterField[], key?: string) => {
    if (key === 'groupName') {
      userGroupsReset({
        params: {
          filters: {
            op: FilterOperators.AND,
            fields: [
              { field: 'name', op: FilterOperators.LIKE, values: fields?.[0]?.values },
              { field: 'active', op: FilterOperators.EQ, values: [true] },
            ],
          },
          query: fields?.[0]?.values?.[0],
        },
      });
    } else {
      assigneeReset({
        params: {
          ...defaultParams(),
          filters: {
            op: FilterOperators.AND,
            fields: fields,
          },
        },
      });
    }
    setSelectedKey(key!);
  };

  const onSubmitModal = async () => {
    let payload = {
      userId: usersFields.map((user) => user.id),
      userGroupId: groupsFields.map((group) => group.id),
    };
    onSubmit?.(payload, handleCloseDrawer ? handleCloseDrawer : () => {});
  };

  const filteredUsersList = useMemo(() => {
    return shouldFilterList
      ? assigneeList?.filter((currentUser) => currentUser?.id !== userId) || []
      : assigneeList;
  }, [assigneeList, userId, shouldFilterList]);

  useEffect(() => {
    return () => {
      handleCloseDrawer?.();
    };
  }, [handleCloseDrawer]);

  const onTabChange = useCallback(
    (count) => {
      setActiveStep(count);
      fetchUserAndUserGroups(
        [
          {
            field: count == 0 ? 'firstName' : 'groupName',
            op: FilterOperators.LIKE,
            values: [''],
          },
        ],
        count == 0 ? 'firstName' : 'groupName',
      );
    },
    [setActiveStep],
  );

  const bodyContent = () => {
    if (showSections === ShowSectionUsersAndUserGroupTabs.BOTH) {
      const sections = [
        {
          value: '0',
          label: 'Users',
          panelContent: (
            <PeerVerificationUserGroupAssign
              section={'users'}
              handleDeselectAll={deselectAll}
              handleClickHandler={handleClickHandler}
              filteredList={filteredUsersList}
              assigneeItemScroll={assigneeItemScroll}
              groupItemScroll={groupItemScroll}
              groupsFields={groupsFields}
              usersFields={usersFields}
              fetchVerificationAssignees={fetchUserAndUserGroups}
              showRoleTag={showRoleTag}
            />
          ),
        },
        {
          value: '1',
          label: 'User Groups',
          panelContent: (
            <PeerVerificationUserGroupAssign
              section={'groups'}
              handleDeselectAll={deselectAll}
              handleClickHandler={handleClickHandler}
              filteredList={userGroupsList}
              assigneeItemScroll={assigneeItemScroll}
              groupItemScroll={groupItemScroll}
              groupsFields={groupsFields}
              usersFields={usersFields}
              fetchVerificationAssignees={fetchUserAndUserGroups}
            />
          ),
        },
      ];

      return (
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
      );
    } else if (showSections === ShowSectionUsersAndUserGroupTabs.USERS) {
      return (
        <PeerVerificationUserGroupAssign
          section={'users'}
          handleDeselectAll={deselectAll}
          handleClickHandler={handleClickHandler}
          filteredList={filteredUsersList}
          assigneeItemScroll={assigneeItemScroll}
          groupItemScroll={groupItemScroll}
          groupsFields={groupsFields}
          usersFields={usersFields}
          fetchVerificationAssignees={fetchUserAndUserGroups}
          showRoleTag={showRoleTag}
        />
      );
    } else if (showSections === ShowSectionUsersAndUserGroupTabs.GROUPS) {
      return (
        <PeerVerificationUserGroupAssign
          section={'groups'}
          handleDeselectAll={deselectAll}
          handleClickHandler={handleClickHandler}
          filteredList={userGroupsList}
          assigneeItemScroll={assigneeItemScroll}
          groupItemScroll={groupItemScroll}
          groupsFields={groupsFields}
          usersFields={usersFields}
          fetchVerificationAssignees={fetchUserAndUserGroups}
        />
      );
    }
    return null;
  };

  return {
    bodyContent: () => (
      <AssignTaskWrapper
        onSubmit={(e) => {
          e.preventDefault();
        }}
      >
        {bodyContent()}{' '}
      </AssignTaskWrapper>
    ),
    footerContent: () => (
      <>
        <Button
          variant="textOnly"
          color="blue"
          disabled={disableButton ? disableButton() : false}
          onClick={() => onSubmitModal()}
        >
          Confirm Without Notifying
        </Button>
        <Button
          variant="secondary"
          style={{ marginLeft: 'auto' }}
          onClick={() => {
            handleCloseDrawer?.();
          }}
        >
          Cancel
        </Button>
        <Button disabled={disableButton ? disableButton() : false} onClick={() => onSubmitModal()}>
          Confirm
        </Button>
      </>
    ),
    selectedUsers: usersFields,
    selectedUserGroups: groupsFields,
    resetAll: deselectAll,
  };
};
