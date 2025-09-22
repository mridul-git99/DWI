import { Button, LoadingContainer, SearchFilter } from '#components';
import { openOverlayAction } from '#components/OverlayContainer/actions';
import { OverlayNames } from '#components/OverlayContainer/types';
import { getFullName } from '#utils/stringUtils';
import { ListItem } from '#views/Checklists/TrainedUser/ListItem';
import React, { useCallback } from 'react';
import { useDispatch } from 'react-redux';

const SearchBar = ({
  readOnly,
  handleUpdateQuery,
  deselectUsers,
  handleUserSelectionModal,
}: any) => (
  <div className="filter-bar">
    <div>
      <SearchFilter
        showDropdown={false}
        updateFilterFields={(fields) => {
          handleUpdateQuery(fields.length > 0 ? fields[0]?.values?.[0] : '');
        }}
      />
    </div>
    <div>
      <Button variant="textOnly" color="blue" disabled={readOnly} onClick={deselectUsers}>
        Deselect All
      </Button>
      <Button
        variant="secondary"
        color="gray"
        disabled={readOnly}
        onClick={handleUserSelectionModal}
      >
        Add Users
      </Button>
    </div>
  </div>
);

const UserList = ({
  usersId,
  usersMap,
  userListById,
  readOnly,
  handleOnScroll,
  removeUser,
}: any) => {
  const dispatch = useDispatch();
  const handleRemoveUser = useCallback(
    (user) => {
      if (!userListById?.[user.id]) {
        return removeUser(user.id, '');
      }
      dispatch(
        openOverlayAction({
          type: OverlayNames.REASON_MODAL,
          props: {
            modalTitle: 'Remove User',
            modalDesc: `Are you sure you want to remove "${getFullName(user)} (ID: ${
              user.employeeId
            })"?`,
            onSubmitHandler: (reason, getError) => {
              removeUser(user.id, reason);
              getError && getError();
            },
            onSubmitModalText: 'Confirm',
          },
        }),
      );
    },
    [dispatch, removeUser, userListById],
  );
  return (
    <div className="users-list" onScroll={handleOnScroll}>
      {usersId.map((userId) => {
        const user = usersMap[userId];
        return user ? (
          <ListItem
            key={user.id}
            user={user}
            selected={false}
            buttonDisabled={readOnly}
            onRemove={() => handleRemoveUser(user)}
          />
        ) : null;
      })}
    </div>
  );
};

const NoUserFound = () => <div className="no-user-found">No User Found</div>;

export const UserDetails = ({
  usersId,
  readOnly,
  deselectUsers,
  handleUpdateQuery,
  handleUserSelectionModal,
  removeUser,
  loadingStatus,
  handleOnScroll,
  usersMap,
  userListById,
  isUserPresent,
}: any) => {
  return (
    <div className="user-container">
      {!!usersId?.length && (
        <SearchBar
          readOnly={readOnly}
          handleUpdateQuery={handleUpdateQuery}
          deselectUsers={deselectUsers}
          handleUserSelectionModal={handleUserSelectionModal}
        />
      )}
      <LoadingContainer
        loading={loadingStatus === 'loading'}
        component={
          <div className="user-selection">
            {usersId?.length > 0 ? (
              <UserList
                usersId={usersId}
                usersMap={usersMap}
                userListById={userListById}
                readOnly={readOnly}
                handleOnScroll={handleOnScroll}
                removeUser={removeUser}
              />
            ) : (
              <div className="adduser-btn">
                <Button
                  variant="secondary"
                  color="gray"
                  onClick={handleUserSelectionModal}
                  disabled={readOnly}
                >
                  Add Users
                </Button>
              </div>
            )}
            {usersId?.length > 0 && !isUserPresent && <NoUserFound />}
          </div>
        }
      />
    </div>
  );
};
