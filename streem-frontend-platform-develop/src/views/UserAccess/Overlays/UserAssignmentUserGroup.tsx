import { BaseModal, TextInput } from '#components';
import { createFetchList } from '#hooks/useFetchData';
import { defaultParams } from '#services/users';
import { apiGetUsers } from '#utils/apiUrls';
import { FilterOperators } from '#utils/globalTypes';
import { generateUserSearchFilters } from '#utils/smartFilterUtils';
import { ListItem } from '#views/Checklists/TrainedUser/ListItem';
import { Search } from '@material-ui/icons';
import { debounce } from 'lodash';
import React, { useCallback, useState } from 'react';
import styled from 'styled-components';

const Wrapper = styled.div`
  .modal {
    padding: 0;

    &-body {
      padding: 0 !important;

      .body {
        height: 60dvh;
        min-width: 60dvw;
        .header {
          display: flex;
          align-items: center;
          padding: 16px;
          border-bottom: 1px solid #eeeeee;
        }

        .header-title {
          display: flex;
          align-items: center;
          justify-content: center;
          gap: 16px;
        }
        .heading {
          font-size: 16px !important;
        }

        .content {
          display: flex;
          flex-direction: column;
          height: 92%;
          overflow: auto;

          .filter-bar {
            display: flex;
            height: min-content;
            width: 90%;
            margin: 16px auto;
            gap: 16px;

            .search-user-filter {
              .input-wrapper {
                max-width: 300px;
              }
            }

            .deselect-btn {
              margin-left: auto;
            }
          }

          .users-list {
            max-height: 100%;
            width: 95%;
            margin: 16px auto;
            overflow-y: scroll;
            padding: 0 24px 24px;
          }
        }
      }
    }
  }
`;

const UserAssignmentUserGroup = ({
  closeAllOverlays,
  closeOverlay,
  props: { addUser, removeUser, usersId },
}: any) => {
  const usersIdByKey = usersId.reduce((acc, curr) => {
    acc[curr] = true;
    return acc;
  }, {});
  const [selectedUsers, setSelectedUsers] = useState([] as any[]);
  const { list: users, reset: resetUsers, fetchNext } = createFetchList(apiGetUsers(), {}, true);

  const handleOnScroll = useCallback(
    (e) => {
      e.stopPropagation();
      const { scrollHeight, scrollTop, clientHeight } = e.currentTarget;
      if (scrollTop + clientHeight >= scrollHeight - clientHeight * 0.7) {
        fetchNext();
      }
    },
    [fetchNext, usersId],
  );

  return (
    <Wrapper>
      <BaseModal
        closeAllModals={closeAllOverlays}
        closeModal={() => {
          closeOverlay();
        }}
        showFooter={false}
        title="Add Users"
      >
        <div className="body">
          <div className="content">
            <div className="filter-bar">
              <TextInput
                className="search-user-filter"
                AfterElement={Search}
                afterElementWithoutError
                afterElementClass="search"
                name="search-filter"
                onChange={debounce(({ value }) => {
                  const filters =
                    generateUserSearchFilters(FilterOperators.LIKE, value ? value : '') || {};
                  resetUsers({
                    params: {
                      ...defaultParams(),
                      filters,
                    },
                  });
                }, 500)}
                placeholder="Search Users"
              />
            </div>
            <div className="users-list" onScroll={handleOnScroll}>
              {users.map((user) => {
                return (
                  <ListItem
                    user={user}
                    key={user.id}
                    checkboxDisabled={usersIdByKey?.[user.id]}
                    selected={selectedUsers.includes(user.id) || usersIdByKey?.[user.id]}
                    onClick={(checked) => {
                      if (checked) {
                        setSelectedUsers((prev) => [...prev, user.id]);
                        addUser(user.id, user);
                      } else {
                        setSelectedUsers((prev) => prev.filter((id) => id !== user.id));
                        removeUser(user.id);
                      }
                    }}
                  />
                );
              })}
            </div>
          </div>
        </div>
      </BaseModal>
    </Wrapper>
  );
};

export default UserAssignmentUserGroup;
