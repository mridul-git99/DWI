import { showNotification } from '#components/Notification/actions';
import { NotificationType } from '#components/Notification/types';
import { openOverlayAction } from '#components/OverlayContainer/actions';
import { OverlayNames } from '#components/OverlayContainer/types';
import { createFetchList } from '#hooks/useFetchData';
import { getDataSetById } from '#hooks/useFetchDataById';
import { apiGetUsers, apiGetUsersFromGroup } from '#utils/apiUrls';
import { DEFAULT_PAGE_NUMBER } from '#utils/constants';
import { FilterOperators } from '#utils/globalTypes';
import { generateUserSearchFilters } from '#utils/smartFilterUtils';
import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { useDispatch } from 'react-redux';
import { findUsersFromQuery } from '../utils';
import { UserDetails } from './UserDetails';

export const UserDetailsContainer = ({
  userList = [],
  id,
  isDirtyForm,
  usersId,
  setUsersId,
  readOnly,
  setUserRemovalReason,
}: any) => {
  const dispatch = useDispatch();
  const [searchQuery, setSearchQuery] = useState('');
  const [addedUsers, setAddedUsers] = useState<Array<string>>([]);
  const [removeUsers, setRemoveUsers] = useState<Array<string>>([]);
  const [upsertUserMap, setUpsertUserMap] = useState<Record<string, any>>({});

  const {
    listById,
    fetchData: fetchUsersData,
    fetchNext,
    status,
  } = getDataSetById(apiGetUsers(), {});

  const {
    listById: groupsUserListById,
    reset: fetchUsersFromUserGroup,
    fetchNext: fetchMoreUsersFromUserGroup,
  } = createFetchList(apiGetUsersFromGroup(id), {}, false);

  const calculateUserDetails = useCallback(() => {
    const addedUsers = usersId.filter((item) => !userList.includes(item));
    const deletedUsers = userList.filter((item) => !usersId.includes(item));
    return { addedUsers, deletedUsers };
  }, [usersId, userList]);

  useEffect(() => {
    fetchUsersData({ ids: usersId });
    let { addedUsers, deletedUsers } = calculateUserDetails();
    setAddedUsers(() => addedUsers);
    setRemoveUsers(() => deletedUsers);
  }, [usersId.length]);

  const fetchUsersQuery = useCallback(
    (searchQueryStr) => {
      if (searchQueryStr) {
        fetchUsersFromUserGroup({
          params: {
            page: DEFAULT_PAGE_NUMBER,
            size: 10,
            filters: generateUserSearchFilters(FilterOperators.LIKE, searchQueryStr, 'user'),
          },
        });
      }
    },
    [fetchUsersFromUserGroup],
  );

  const addUser = useCallback(
    (userId, user) => {
      isDirtyForm.current = true;
      setUsersId((prev) => [...prev, userId]);
      setUpsertUserMap((prev) => ({ ...prev, [userId]: user }));
    },
    [isDirtyForm, setUsersId, setUpsertUserMap],
  );

  const removeUser = useCallback(
    (userId, reason) => {
      isDirtyForm.current = true;
      setUsersId((prev) => prev.filter((_userId) => _userId !== userId));
      setUserRemovalReason((prev) => ({ ...prev, [userId]: reason }));
      setUpsertUserMap((prev) => {
        const updatedUserMap = { ...prev };
        delete updatedUserMap[userId];
        return updatedUserMap;
      });
      if (reason) {
        dispatch(
          showNotification({
            type: NotificationType.SUCCESS,
            msg: 'User has been removed from the group',
          }),
        );
      }
    },
    [dispatch, isDirtyForm, setUsersId, setUserRemovalReason, setUpsertUserMap, showNotification],
  );

  const handleUpdateQuery = useCallback(
    (query) => {
      setSearchQuery(query);
      fetchUsersQuery(query);
    },
    [setSearchQuery, fetchUsersQuery],
  );

  const fetchMoreUsers = useCallback(() => {
    if (searchQuery) {
      fetchMoreUsersFromUserGroup();
    } else {
      fetchNext({ ids: usersId });
    }
  }, [searchQuery, fetchNext, usersId, fetchMoreUsersFromUserGroup]);

  const handleUserSelectionModal = useCallback(() => {
    dispatch(
      openOverlayAction({
        type: OverlayNames.USER_ASSIGN_USER_GROUP,
        props: {
          addUser,
          removeUser,
          usersId,
        },
      }),
    );
  }, [addUser, removeUser, usersId]);

  const filteredUsers = useCallback(() => {
    const data = { ...groupsUserListById };
    removeUsers.forEach((userId) => {
      delete data[userId];
    });
    addedUsers.forEach((userId) => {
      const userStatusFound = findUsersFromQuery(upsertUserMap, userId, searchQuery);
      if (userStatusFound) {
        data[userId] = upsertUserMap[userId];
      }
    });
    return data || {};
  }, [groupsUserListById, removeUsers, addedUsers, upsertUserMap, searchQuery]);

  const isUserPresent = useMemo(() => {
    return searchQuery ? Object.values(filteredUsers())?.length : Object.values(listById).length;
  }, [filteredUsers, listById, searchQuery]);

  const deselectUsers = useCallback(() => {
    isDirtyForm.current = true;
    usersId.forEach((userId) => {
      setUserRemovalReason((prev) => ({ ...prev, [userId]: '' }));
    });
    setUsersId(() => []);
    setUpsertUserMap(() => {});
  }, [isDirtyForm, setUserRemovalReason, setUsersId, setUpsertUserMap, usersId]);

  const userListsById = useMemo(
    () => (searchQuery ? filteredUsers() : listById),
    [filteredUsers, listById, searchQuery],
  );

  return (
    <div className="user-container">
      <UserDetails
        usersId={usersId}
        handleUpdateQuery={handleUpdateQuery}
        handleUserSelectionModal={handleUserSelectionModal}
        addUser={addUser}
        removeUser={removeUser}
        loadingStatus={status}
        handleOnScroll={fetchMoreUsers}
        usersMap={userListsById}
        userListById={userList.reduce((acc, id) => {
          acc[id] = true;
          return acc;
        }, {})}
        isUserPresent={isUserPresent}
        deselectUsers={deselectUsers}
        readOnly={readOnly}
      />
    </div>
  );
};
