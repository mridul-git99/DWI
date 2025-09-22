import { Button, TextInput } from '#components';
import { createFetchList } from '#hooks/useFetchData';
import {
  apiGetAllTrainedUsersAssignedToChecklist,
  apiGetAllUsersAssignedToChecklistTask,
  apiGetAllUsersAssignedToTask,
  apiGetUntrainedUsersAndUserGroups,
  apiUserGroups,
} from '#utils/apiUrls';
import { DEFAULT_PAGE_NUMBER, DEFAULT_PAGE_SIZE } from '#utils/constants';
import { request } from '#utils/request';
import { generateGroupSearchFilters } from '#utils/smartFilterUtils';
import { Search } from '@material-ui/icons';
import { debounce } from 'lodash';
import React, { useCallback, useEffect, useState } from 'react';
import styled from 'styled-components';
import { ListItem } from '../TrainedUser/ListItem';
import { RenderListData } from './RenderListData';

export const AssignGroupsWrapper = styled.div`
  display: grid;
  grid-template-rows: auto 1fr;
  height: 100%;
`;

export const ChecklistUserGroupAssign = ({
  form,
  usersFields = [],
  usersAppend,
  usersRemove,
  groupsFields = [],
  groupsAppend,
  groupsRemove,
  source,
  selectedTasks,
  checklistId,
  section,
  isDataFetched,
  setIsDataFetched,
  setUserData,
  deselectUsers,
  setDeselectUsers,
  deselectGroups,
  setDeselectGroups,
  setGroupData,
}: any) => {
  const { setValue, watch } = form;
  const [isTrainedUserPresent, setIsTrainedUserPresent] = useState(true);
  const [isTrainedGroupPresent, setIsTrainedGroupPresent] = useState(true);
  const { users, groups } = watch(['users', 'groups']);

  const {
    list: trainedUserList,
    reset: trainedUserReset,
    fetchNext: trainedUserFetchNext,
    pagination: trainedUserPagination,
    status: trainedUserStatus,
    triggerCount: trainedUserTriggerCount,
  } = createFetchList(
    apiGetAllTrainedUsersAssignedToChecklist(checklistId!),
    {
      page: DEFAULT_PAGE_NUMBER,
      size: DEFAULT_PAGE_SIZE,
      users: true,
      userGroups: false,
    },
    false,
  );

  const {
    list: trainedGroupList,
    reset: trainedGroupReset,
    fetchNext: trainedGroupFetchNext,
    pagination: trainedGroupPagination,
    status: trainedGroupStatus,
    triggerCount: trainedGroupTriggerCount,
  } = createFetchList(
    apiGetAllTrainedUsersAssignedToChecklist(checklistId!),
    {
      page: DEFAULT_PAGE_NUMBER,
      size: DEFAULT_PAGE_SIZE,
      users: false,
      userGroups: true,
    },
    false,
  );

  const {
    list: usersList,
    reset: resetUsersList,
    fetchNext: fetchNextUsersList,
    pagination: usersPagination,
  } = createFetchList(
    apiGetUntrainedUsersAndUserGroups(checklistId),
    {
      page: DEFAULT_PAGE_NUMBER,
      size: DEFAULT_PAGE_SIZE,
      archived: false,
      users: true,
    },
    false,
  );

  const {
    list: groupList,
    reset: resetGroupsList,
    fetchNext: fetchNextGroupsList,
    pagination: groupPagination,
  } = createFetchList(
    apiUserGroups(),
    {
      page: DEFAULT_PAGE_NUMBER,
      size: DEFAULT_PAGE_SIZE,
    },
    false,
  );

  useEffect(() => {
    if (section === 'users' && trainedUserStatus === 'init') {
      trainedUserReset({
        params: {
          page: DEFAULT_PAGE_NUMBER,
          size: DEFAULT_PAGE_SIZE,
          users: true,
          userGroups: false,
        },
      });
    } else if (section === 'userGroups' && trainedGroupStatus === 'init') {
      trainedGroupReset({
        params: {
          page: DEFAULT_PAGE_NUMBER,
          size: DEFAULT_PAGE_SIZE,
          users: false,
          userGroups: true,
        },
      });
    }
  }, [section]);

  useEffect(() => {
    if (section === 'users') {
      if (
        trainedUserList &&
        trainedUserList.length === 0 &&
        trainedUserStatus === 'success' &&
        isTrainedUserPresent &&
        source === 'job' &&
        trainedUserTriggerCount === 1
      ) {
        setIsTrainedUserPresent(false);
        resetUsersList({
          params: {
            page: DEFAULT_PAGE_NUMBER,
            size: DEFAULT_PAGE_SIZE,
            archived: false,
            users: true,
          },
        });
      }
    } else {
      if (
        trainedGroupList &&
        trainedGroupList.length === 0 &&
        trainedGroupStatus === 'success' &&
        isTrainedGroupPresent &&
        source === 'job' &&
        trainedGroupTriggerCount === 1
      ) {
        setIsTrainedGroupPresent(false);
        resetGroupsList({
          params: {
            page: DEFAULT_PAGE_NUMBER,
            size: DEFAULT_PAGE_SIZE,
          },
        });
      }
    }
  }, [trainedUserStatus, trainedGroupStatus, section]);

  useEffect(() => {
    (async () => {
      try {
        let data;
        if (isDataFetched?.[section]) {
          return;
        }
        if (source === 'job') {
          data = await request('POST', apiGetAllUsersAssignedToTask(), {
            data: {
              task: (selectedTasks?.map((task) => task.length === 2 && task[0]) || [])?.map(
                (taskId) => taskId,
              ),
              users: section === 'users',
              userGroups: section === 'userGroups',
            },
          });
        } else {
          data = await request('POST', apiGetAllUsersAssignedToChecklistTask(checklistId!), {
            data: {
              task: selectedTasks.map((taskId) => taskId),
              users: section === 'users',
              userGroups: section === 'userGroups',
            },
          });
        }
        if (section === 'users') {
          setIsDataFetched((prev) => ({ ...prev, users: true }));
          setUserData(data ? data.data : []);
          setValue('users', data ? data.data : []);
        } else {
          setIsDataFetched((prev) => ({ ...prev, userGroups: true }));
          setGroupData(data ? data.data : []);
          setValue('groups', data ? data.data : []);
        }
      } catch (error) {
        console.error('error came apiGetAllUsersAssignedToChecklistTask :: ', error);
      }
    })();
  }, [section]);

  const handleClickHandler = useCallback(
    (data) => {
      let selectedData;
      selectedData = section === 'users' ? usersFields : groupsFields;
      const index = selectedData.findIndex(
        (g) => (g.userId || g.userGroupId || g.id) === (data.userId || data.userGroupId || data.id),
      );
      if (index === -1) {
        section === 'users' ? usersAppend(data) : groupsAppend(data);
      } else {
        section === 'users' ? usersRemove(index) : groupsRemove(index);
      }
    },
    [usersFields, groupsFields, section, users, groups],
  );

  const handleOnScroll = useCallback(
    (e) => {
      const { scrollHeight, scrollTop, clientHeight } = e.currentTarget;
      if (section === 'users') {
        if (scrollTop + clientHeight >= scrollHeight - clientHeight * 0.7) {
          if (isTrainedUserPresent) {
            !trainedUserPagination.last && trainedUserFetchNext();
          } else {
            !usersPagination.last && fetchNextUsersList();
          }
        }
      } else {
        if (scrollTop + clientHeight >= scrollHeight - clientHeight * 0.7) {
          if (isTrainedGroupPresent) {
            !trainedGroupPagination.last && trainedGroupFetchNext();
          } else {
            !groupPagination.last && fetchNextGroupsList();
          }
        }
      }
    },
    [
      trainedGroupFetchNext,
      fetchNextGroupsList,
      trainedUserFetchNext,
      fetchNextUsersList,
      groupPagination,
      trainedGroupPagination,
      section,
      isTrainedUserPresent,
      isTrainedGroupPresent,
    ],
  );

  const handleSelectionFields = (data: any) => {
    if (section === 'users') {
      const deselectUserIndex = deselectUsers.findIndex(
        (user) => (user.userId || user.id) === (data.userId || data.id),
      );
      if (deselectUserIndex === -1) {
        return true;
      } else {
        return false;
      }
    } else {
      const deselectGroupsIndex = deselectGroups.findIndex(
        (group) => (group.userGroupId || group.id) === (data.userGroupId || data.id),
      );
      if (deselectGroupsIndex === -1) {
        return true;
      } else {
        return false;
      }
    }
  };

  const deselectAllUsers = useCallback(() => {
    setValue('users', [], { shouldDirty: true });
  }, [setValue]);

  const deselectAllGroups = useCallback(() => {
    setValue('groups', [], { shouldDirty: true });
  }, [setValue]);

  const handleDeselectAll = useCallback(() => {
    if (section === 'users') {
      deselectAllUsers();
      setDeselectUsers([]);
    } else {
      deselectAllGroups();
      setDeselectGroups([]);
    }
  }, []);

  const deselectButtonDisabled = () => {
    if (section === 'users') {
      return usersFields.length === 0;
    } else {
      return groupsFields.length === 0;
    }
  };

  return (
    <AssignGroupsWrapper>
      <div className="filters">
        <TextInput
          AfterElement={Search}
          afterElementWithoutError
          afterElementClass="search"
          name="search-filter"
          onChange={debounce(({ value }) => {
            if (section === 'users') {
              if (isTrainedUserPresent) {
                trainedUserReset({
                  params: {
                    page: DEFAULT_PAGE_NUMBER,
                    size: DEFAULT_PAGE_SIZE,
                    query: value,
                  },
                });
              } else {
                resetUsersList({
                  params: {
                    page: DEFAULT_PAGE_NUMBER,
                    size: DEFAULT_PAGE_SIZE,
                    archived: false,
                    users: true,
                    query: value,
                  },
                });
              }
            } else {
              if (isTrainedGroupPresent) {
                trainedGroupReset({
                  params: {
                    page: DEFAULT_PAGE_NUMBER,
                    size: DEFAULT_PAGE_SIZE,
                    query: value,
                  },
                });
              } else {
                const groupFilterQuery = generateGroupSearchFilters(value);
                resetGroupsList({
                  params: {
                    page: DEFAULT_PAGE_NUMBER,
                    size: DEFAULT_PAGE_SIZE,
                    filters: groupFilterQuery,
                  },
                });
              }
            }
          }, 500)}
          placeholder={`Search by ${section === 'users' ? 'User' : 'Group'} Name`}
        />
        <Button
          variant="textOnly"
          color="blue"
          disabled={deselectButtonDisabled()}
          onClick={handleDeselectAll}
        >
          Deselect All
        </Button>
      </div>
      <div className="user-groups-contents" onScroll={handleOnScroll}>
        {(section === 'users' ? usersFields : groupsFields)?.map((data) => {
          return (
            <ListItem
              key={data.userGroupId || data.userId || data.id}
              user={data}
              onClick={() => handleClickHandler(data)}
              selected={handleSelectionFields(data)}
              isGroup={section === 'userGroups'}
            />
          );
        })}
        <RenderListData
          fields={section === 'users' ? usersFields : groupsFields}
          section={section}
          trainedUserList={trainedUserList}
          trainedGroupList={trainedGroupList}
          usersList={usersList}
          groupList={groupList}
          deSelectedData={section === 'users' ? deselectUsers : deselectGroups}
          handleClickHandler={handleClickHandler}
        />
      </div>
    </AssignGroupsWrapper>
  );
};
