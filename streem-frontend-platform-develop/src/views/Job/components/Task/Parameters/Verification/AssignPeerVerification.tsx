import { Button, StyledTabs, useDrawer } from '#components';
import { createFetchList } from '#hooks/useFetchData';
import { defaultParams } from '#services/users';
import { useTypedSelector } from '#store';
import { User } from '#store/users/types';
import { apiParameterVerificationGroupAssignees, apiVerificationAssignees } from '#utils/apiUrls';
import { DEFAULT_PAGE_NUMBER, DEFAULT_PAGE_SIZE } from '#utils/constants';
import { FilterField, FilterOperators } from '#utils/globalTypes';
import React, { useMemo, useState, useCallback, useEffect } from 'react';
import { useFieldArray, useForm } from 'react-hook-form';
import { PeerVerificationUserGroupAssign } from './PeerVerificationUserGroupAssign';
import { AssignTaskWrapper } from '#views/Checklists/UserGroups/AssignTask';

const urlParams = {
  page: DEFAULT_PAGE_NUMBER,
  size: DEFAULT_PAGE_SIZE,
  filters: {
    op: FilterOperators.AND,
    fields: [{ field: 'archived', op: FilterOperators.EQ, values: [false] }],
  },
};

export const AssignPeerVerification = ({ onSubmit, handleCloseDrawer }: any) => {
  const [activeStep, setActiveStep] = useState(0);

  const {
    auth: { userId },
    job: { id: jobId },
  } = useTypedSelector((state) => state);
  const [selectedKey, setSelectedKey] = useState<string>('firstName');
  const {
    list: userGroupsList,
    reset: userGroupsReset,
    fetchNext: userGroupsFetchNext,
  } = createFetchList(apiParameterVerificationGroupAssignees(jobId), {}, false);

  const {
    list: assigneeList,
    reset: assigneeReset,
    fetchNext: assigneeFetchNext,
  } = createFetchList<User[]>(apiVerificationAssignees(jobId!), urlParams, true);

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

  const assigneeItemScroll = (event: any) => {
    const { scrollHeight, scrollTop, clientHeight } = event.currentTarget;
    if (scrollHeight - Math.ceil(scrollTop) <= clientHeight) {
      assigneeFetchNext();
    }
  };

  const groupItemScroll = (event: any) => {
    const { scrollHeight, scrollTop, clientHeight } = event.currentTarget;
    if (scrollHeight - Math.ceil(scrollTop) <= clientHeight) {
      userGroupsFetchNext();
    }
  };

  const fetchVerificationAssignees = (fields: FilterField[], key?: string) => {
    if (key === 'groupName') {
      userGroupsReset({
        params: {
          //   ...defaultParams(),
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
    onSubmit(payload, handleCloseDrawer);
  };

  const filteredUsersList = useMemo(() => {
    return assigneeList?.filter((currentUser) => currentUser?.id !== userId) || [];
  }, [assigneeList, userId]);

  useEffect(() => {
    return () => {
      handleCloseDrawer();
    };
  }, []);

  const onTabChange = useCallback(
    (count) => {
      setActiveStep(count);
      fetchVerificationAssignees(
        [{ field: count == 0 ? 'firstName' : 'groupName', op: FilterOperators.LIKE, values: [''] }],
        count == 0 ? 'firstName' : 'groupName',
      );
    },
    [setActiveStep],
  );

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
          fetchVerificationAssignees={fetchVerificationAssignees}
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
          fetchVerificationAssignees={fetchVerificationAssignees}
        />
      ),
    },
  ];

  return {
    bodyContent: () => (
      <AssignTaskWrapper>
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
            handleCloseDrawer();
          }}
        >
          Cancel
        </Button>
        <Button disabled={disableButton ? disableButton() : false} onClick={() => onSubmitModal()}>
          Confirm
        </Button>
      </>
    ),
  };
};
