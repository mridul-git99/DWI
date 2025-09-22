import { Button, TextInput } from '#components';
import { FilterOperators } from '#utils/globalTypes';
import { ListItem } from '#views/Checklists/TrainedUser/ListItem';
import { AssignGroupsWrapper } from '#views/Checklists/UserGroups/ChecklistUserGroupAssign';
import { Search } from '@material-ui/icons';
import { debounce } from 'lodash';
import React from 'react';

export const PeerVerificationUserGroupAssign = ({
  section,
  handleDeselectAll,
  handleClickHandler,
  filteredList,
  usersFields,
  groupsFields,
  assigneeItemScroll,
  groupItemScroll,
  fetchVerificationAssignees,
  showRoleTag,
}: any) => {
  const handleOnScroll = (e: any) => {
    if (section === 'users') {
      assigneeItemScroll(e);
    } else {
      groupItemScroll(e);
    }
  };

  return (
    <AssignGroupsWrapper>
      <>
        <div className="filters">
          <TextInput
            AfterElement={Search}
            afterElementWithoutError
            afterElementClass="search"
            name="search-filter"
            onChange={debounce(({ value }) => {
              if (section === 'users') {
                fetchVerificationAssignees(
                  [{ field: 'firstName', op: FilterOperators.LIKE, values: [value] }],
                  'firstName',
                );
              } else {
                fetchVerificationAssignees(
                  [{ field: 'groups', op: FilterOperators.LIKE, values: [value] }],
                  'groupName',
                );
              }
            }, 500)}
            placeholder={`Search by ${section === 'users' ? 'User' : 'Group'} Name`}
          />
          <Button
            variant="textOnly"
            color="blue"
            disabled={section === 'users' ? usersFields.length === 0 : groupsFields.length === 0}
            onClick={handleDeselectAll}
          >
            Deselect All
          </Button>
        </div>
        {section === 'users' && (
          <div className="user-groups-contents" onScroll={handleOnScroll}>
            {filteredList.length > 0 ? (
              filteredList?.map((user) => {
                return (
                  <ListItem
                    key={user.id}
                    user={user}
                    onClick={(checked) => handleClickHandler(user, checked)}
                    selected={!usersFields.every((el) => (el.id === user.id ? false : true))}
                    isGroup={false}
                    showRoleTag={showRoleTag}
                  />
                );
              })
            ) : (
              <div className="no-data-found">No User Found</div>
            )}
          </div>
        )}
        {section === 'groups' && (
          <div className="user-groups-contents" onScroll={handleOnScroll}>
            {filteredList.length > 0 ? (
              filteredList?.map((group) => {
                return (
                  <ListItem
                    key={group.id}
                    user={group}
                    onClick={(checked) => handleClickHandler(group, checked)}
                    selected={!groupsFields.every((el) => (el.id === group.id ? false : true))}
                    isGroup={true}
                  />
                );
              })
            ) : (
              <div className="no-data-found">No Group Found</div>
            )}
          </div>
        )}
      </>
    </AssignGroupsWrapper>
  );
};
