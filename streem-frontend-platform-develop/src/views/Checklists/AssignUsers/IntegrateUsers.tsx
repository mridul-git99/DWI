import { Button, TextInput, useDrawer } from '#components';
import { showNotification } from '#components/Notification/actions';
import { NotificationType } from '#components/Notification/types';
import { createFetchList } from '#hooks/useFetchData';
import { apiGetUntrainedUsersAndUserGroups, apiMapUsersGroupsForChecklist } from '#utils/apiUrls';
import { DEFAULT_PAGE_SIZE } from '#utils/constants';
import { getErrorMsg, request } from '#utils/request';
import React, { FC, useCallback, useEffect } from 'react';
import { useFieldArray, useForm } from 'react-hook-form';
import { useDispatch } from 'react-redux';
import styled from 'styled-components';
// import { JobPermissionContainer } from '../TrainedUser/JobPermission';
import { defaultParams } from '#services/users';
import { FilterOperators } from '#utils/globalTypes';
import { debounce } from 'lodash';
import { ListItem } from '../TrainedUser/ListItem';
import { Search } from '@material-ui/icons';

const IntegrateUserWrapper = styled.form`
  display: flex;
  flex-direction: column;
  flex: 1;
  width: 100%;
  position: relative;

  .filters {
    display: flex;
    justify-content: space-between;
    width: 100%;
    margin: 16px 0;

    .trained-user-filter-search {
      max-width: 300px;

      .input-wrapper {
        height: 42px;
      }
    }
  }

  .user-groups-contents {
    overflow-y: auto;
    padding-left: 12px;
  }
`;

const UserContent = ({
  fields,
  reset,
  tab,
  list = [],
  previouslySelected = [],
  handleOnScroll,
  resetForm,
  handleClickHandler,
}: any) => {
  return (
    <>
      <div className="filters">
        <TextInput
          className="trained-user-filter-search"
          placeholder={`${tab === 'users' ? 'Search by User Name' : 'Search by Group Name'}`}
          AfterElement={Search}
          afterElementWithoutError
          afterElementClass="search"
          onChange={debounce(({ value }) => {
            if (tab === 'users') {
              if (value === '') {
                reset({
                  params: {
                    ...defaultParams(),
                    query: value,
                    filters: {
                      op: FilterOperators.AND,
                      fields: [
                        {
                          field: 'id',
                          op: FilterOperators.NOT_IN,
                          values: previouslySelected.map((user: any) => user.userId),
                        },
                      ],
                    },
                  },
                });
                return;
              }
              reset({
                params: {
                  ...defaultParams(),
                  size: DEFAULT_PAGE_SIZE,
                  query: value,
                },
              });
            } else if (tab === 'groups') {
              if (value === '') {
                reset({
                  params: {
                    ...defaultParams(false),
                    query: value,
                    filters: {
                      op: FilterOperators.AND,
                      fields: [
                        {
                          field: 'id',
                          op: FilterOperators.NIN,
                          values: previouslySelected.map((group: any) => group.userGroupId),
                        },
                      ],
                    },
                  },
                });
                return;
              }
              reset({
                params: {
                  ...defaultParams(false),
                  query: value,
                  filters: {
                    op: 'AND',
                    fields: [
                      {
                        field: 'active',
                        op: 'EQ',
                        values: [true],
                      },
                      {
                        field: 'id',
                        op: FilterOperators.NIN,
                        values: previouslySelected.map((group: any) => group.userGroupId),
                      },
                    ],
                  },
                },
              });
            }
          }, 500)}
        />
        <Button variant="textOnly" color="blue" disabled={!fields.length} onClick={resetForm}>
          Deselect All
        </Button>
      </div>
      <div
        className="user-groups-contents"
        onScroll={(e) => {
          handleOnScroll(e);
        }}
      >
        {list.map((data: any) => {
          if (tab === 'users') {
            const isChecked = fields.some((_data: any) => _data.userId === data.userId);
            return (
              <ListItem
                key={data.id}
                user={data}
                onClick={(checked) => handleClickHandler(data, checked)}
                selected={isChecked}
              />
            );
          } else if (tab === 'groups') {
            const isChecked = fields.some((_data: any) => _data.userGroupId === data.userGroupId);
            return (
              <ListItem
                key={data.id}
                user={data}
                onClick={(checked) => handleClickHandler(data, checked)}
                selected={isChecked}
                isGroup={true}
              />
            );
          }
        })}
      </div>
      {/* <div>
        <JobPermissionContainer
          permissionField={permissionField}
          permissionAppend={permissionAppend}
          permissionRemove={permissionRemove}
        />
      </div> */}
    </>
  );
};

const IntegrateUser: FC<{
  onCloseDrawer: React.Dispatch<React.SetStateAction<any>>;
  checklistId: string;
  list: any;
  fetchData: any;
  tab: 'users' | 'groups';
}> = ({ onCloseDrawer, checklistId, list, fetchData, tab }) => {
  const dispatch = useDispatch();
  const {
    list: usersAndGroupsList,
    reset: usersAndGroupsListReset,
    fetchNext: usersAndGroupsFetchNext,
  } = createFetchList(
    apiGetUntrainedUsersAndUserGroups(checklistId),
    {
      ...defaultParams(false),
      archived: false,
      users: true,
      filters: {
        op: FilterOperators.AND,
        fields: [],
      },
    },
    false,
  );

  useEffect(() => {
    const tabParamsMap = {
      users: {
        users: true,
        userGroups: false,
      },
      groups: {
        users: false,
        userGroups: true,
      },
    };

    usersAndGroupsListReset({
      params: {
        ...defaultParams(false),
        archived: false,
        ...tabParamsMap[tab],
      },
    });
  }, [tab]);

  const form = useForm({
    mode: 'onChange',
    criteriaMode: 'all',
    defaultValues: {
      users: [],
      groups: [],
      permission: [],
    },
  });

  const {
    formState: { isDirty, isValid },
    control,
    reset: resetForm,
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

  // const {
  //   fields: permissionField,
  //   append: permissionAppend,
  //   remove: permissionRemove,
  // } = useFieldArray({
  //   control,
  //   name: 'permission',
  // });

  useEffect(() => {
    setDrawerOpen(true);
    return () => {
      handleCloseDrawer();
    };
  }, []);

  const handleOnScroll = useCallback(
    (e) => {
      const { scrollHeight, scrollTop, clientHeight } = e.currentTarget;
      if (scrollHeight - Math.ceil(scrollTop) <= clientHeight) {
        usersAndGroupsFetchNext();
      }
    },
    [usersAndGroupsFetchNext],
  );

  const handleCloseDrawer = useCallback(() => {
    setDrawerOpen(false);
    setTimeout(() => {
      onCloseDrawer(false);
    }, 200);
  }, []);

  const handleClickHandler = useCallback(
    (user, checked) => {
      if (tab === 'users') {
        if (checked) {
          usersAppend(user);
        } else {
          usersRemove(
            usersFields.findIndex(
              (field) => (field.userId || field.id) === (user.userId || user.id),
            ),
          );
        }
      } else if (tab === 'groups') {
        if (checked) {
          groupsAppend(user);
        } else {
          groupsRemove(
            groupsFields.findIndex(
              (field) => (field.userGroupId || field.id) === (user.userGroupId || user.id),
            ),
          );
        }
      }
    },
    [usersFields, groupsFields],
  );

  const handleSubmitHandler = useCallback(async () => {
    const { status, errors } = await request(
      'PATCH',
      apiMapUsersGroupsForChecklist(checklistId as string),
      {
        data: {
          assignedUserIds: usersFields.map((user) => user.userId),
          assignedUserGroupIds: groupsFields.map((group) => group.userGroupId),
        },
      },
    );
    if (status === 'OK') {
      dispatch(
        showNotification({
          type: NotificationType.SUCCESS,
          msg: `${tab === 'users' ? 'User Added to the Process' : 'Group Added to the Process'}`,
          detail: `${
            tab === 'users'
              ? 'You have successfully added user as trained user.'
              : 'You have successfully added group as trained group.'
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
    handleCloseDrawer();
  }, [usersFields, groupsFields]);

  const { StyledDrawer, setDrawerOpen } = useDrawer({
    title: `${tab === 'users' ? 'Assign User' : 'Add User Group'}`,
    hideCloseIcon: true,
    bodyContent: (
      <IntegrateUserWrapper
        onSubmit={(e) => {
          e.preventDefault();
        }}
      >
        <UserContent
          fields={tab === 'users' ? usersFields : groupsFields}
          reset={usersAndGroupsListReset}
          list={usersAndGroupsList}
          previouslySelected={list}
          handleOnScroll={handleOnScroll}
          resetForm={resetForm}
          handleClickHandler={handleClickHandler}
          tab={tab}
          // permissionField={permissionField}
          // permissionAppend={permissionAppend}
          // permissionRemove={permissionRemove}
        />
      </IntegrateUserWrapper>
    ),
    footerContent: (
      <>
        <Button
          variant="secondary"
          style={{ marginLeft: 'auto' }}
          onClick={() => {
            handleCloseDrawer();
          }}
        >
          Cancel
        </Button>
        <Button
          type="submit"
          disabled={
            !isDirty || !isValid || (tab === 'users' ? !usersFields.length : !groupsFields.length)
          }
          onClick={handleSubmitHandler}
        >
          Save
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

export default IntegrateUser;
