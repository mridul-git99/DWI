import {
  Button,
  DataTable,
  ListActionMenu,
  LoadingContainer,
  Pagination,
  TabContentProps,
  TextInput,
  ToggleSwitch,
} from '#components';
import checkPermission, { roles } from '#services/uiPermissions';
import { useTypedSelector } from '#store/helpers';
import { fetchUsers } from '#store/users/actions';
import {
  User,
  UserStates,
  UserStatesColors,
  UserStatesContent,
  UsersListType,
} from '#store/users/types';
import { DEFAULT_PAGE_NUMBER, DEFAULT_PAGE_SIZE } from '#utils/constants';
import { FilterField, FilterOperators, fetchDataParams } from '#utils/globalTypes';
import { generateUserSearchFilters } from '#utils/smartFilterUtils';
import { getFullName } from '#utils/stringUtils';
import { TabContentWrapper } from '#views/Jobs/ListView/styles';
import { MenuItem } from '@material-ui/core';
import { ArrowDropDown, FiberManualRecord, Search } from '@material-ui/icons';
import { navigate } from '@reach/router';
import { debounce, startCase, toLower } from 'lodash';
import React, { MouseEvent, useEffect, useState } from 'react';
import { useDispatch } from 'react-redux';
import { removeUnderscore } from '../../../utils/stringUtils';
import { resendInvite, validateArchiveUser } from '../actions';
import { onCancelInvite, onUnArchiveUser, onUnlockUser, onValidateArchiveUser } from '../utils';

const TabContent: React.FC<TabContentProps> = (props) => {
  const [isArchived, setIsArchived] = useState(false);
  const {
    users: {
      [props.values[0] as UsersListType]: { pageable },
      currentPageData,
      loading,
    },
  } = useTypedSelector((state) => state);

  const dispatch = useDispatch();

  const [filterFields, setFilterFields] = useState<FilterField[]>([]);
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const [selectedUser, setSelectedUser] = useState<User | null>(null);

  const fetchData = (params: fetchDataParams = {}) => {
    const { page = DEFAULT_PAGE_NUMBER, size = DEFAULT_PAGE_SIZE } = params;
    dispatch(
      fetchUsers(
        {
          page,
          size,
          archived: isArchived,
          sort: 'createdAt,desc',
          filters:
            filterFields.length > 0
              ? {
                  op: FilterOperators.OR,
                  fields: filterFields,
                }
              : undefined,
        },
        isArchived ? UsersListType.ARCHIVED : UsersListType.ACTIVE,
      ),
    );
  };

  useEffect(() => {
    fetchData();
  }, [isArchived, filterFields]);

  const columns = [
    {
      id: 'name',
      label: 'Name',
      minWidth: 240,
      format: function renderComp(item: User) {
        const fullName = getFullName(item);
        return (
          <span
            className="primary"
            style={{ textTransform: 'capitalize' }}
            onClick={() => navigate(`/users/edit/${item.id}`)}
            title={fullName}
          >
            {fullName}
          </span>
        );
      },
    },
    {
      id: 'employeeId',
      label: 'Employee ID',
      minWidth: 152,
    },
    {
      id: 'email',
      label: 'Email',
      minWidth: 152,
      format: function renderComp({ email }: User) {
        return (
          <div title={email} style={{ textTransform: 'lowercase' }}>
            {email}
          </div>
        );
      },
    },
    {
      id: 'role',
      label: 'Role',
      minWidth: 152,
      format: function renderComp(item: User) {
        const userRolesString = removeUnderscore(
          item?.roles?.map((role) => startCase(toLower(role.name))).join(', ') || '',
        );
        return <span title={userRolesString}>{userRolesString}</span>;
      },
    },
    {
      id: 'status',
      label: 'Status',
      minWidth: 152,
      format: function renderComp(item: User) {
        return (() => {
          if (item.archived) {
            return <span title="Archived">Archived</span>;
          } else {
            return (
              <div
                title={UserStatesContent[item.state]}
                style={{ display: 'flex', alignItems: 'center' }}
              >
                <FiberManualRecord
                  className="icon"
                  style={{ color: UserStatesColors[item.state] }}
                />
                {UserStatesContent[item.state]}
              </div>
            );
          }
        })();
      },
    },
    ...(checkPermission(['usersAndAccess', 'listViewActions'])
      ? [
          {
            id: 'actions',
            label: 'Actions',
            minWidth: 152,
            format: function renderComp(item: User) {
              return showButtons(item);
            },
          },
        ]
      : []),
  ];
  const isUserLocked = (state: UserStates) =>
    [
      UserStates.UNREGISTERED_LOCKED,
      UserStates.REGISTERED_LOCKED,
      UserStates.ACCOUNT_LOCKED,
    ].includes(state);

  const handleClose = () => {
    setAnchorEl(null);
  };

  const ArchiveButton = () => (
    <MenuItem
      onClick={() => {
        setAnchorEl(null);
        dispatch(
          validateArchiveUser({
            user: selectedUser as User,
            onArchiveUser: onValidateArchiveUser,
            fetchData,
          }),
        );
      }}
    >
      <div className="list-item">
        <span>Archive</span>
      </div>
    </MenuItem>
  );

  const UnArchiveButton = () => (
    <MenuItem
      onClick={() => {
        setAnchorEl(null);
        onUnArchiveUser(selectedUser as User, fetchData);
      }}
    >
      <div className="list-item">
        <span>Unarchive</span>
      </div>
    </MenuItem>
  );

  const UnlockButton = () => (
    <MenuItem
      onClick={() => {
        setAnchorEl(null);
        onUnlockUser(selectedUser as User, fetchData);
      }}
    >
      <div className="list-item">
        <span>Unlock</span>
      </div>
    </MenuItem>
  );

  const ResendInviteButton = () => (
    <MenuItem
      onClick={() => {
        setAnchorEl(null);
        dispatch(resendInvite({ id: (selectedUser as User).id }));
      }}
    >
      <div className="list-item">
        <span>Reset Invite</span>
      </div>
    </MenuItem>
  );

  const CancelInviteButton = () => (
    <MenuItem
      onClick={() => {
        setAnchorEl(null);
        onCancelInvite(selectedUser as User, fetchData);
      }}
    >
      <div className="list-item">
        <span>Cancel Invite</span>
      </div>
    </MenuItem>
  );

  const GenerateNewSecretButton = () => (
    <MenuItem
      onClick={() => {
        setAnchorEl(null);
        dispatch(resendInvite({ id: (selectedUser as User).id }));
      }}
    >
      <div className="list-item">
        <span>Generate New Secret Key</span>
      </div>
    </MenuItem>
  );

  const showButtons = (item: User) => {
    const isItemAccountOwner = item?.roles?.some((i) => i?.name === roles.ACCOUNT_OWNER);

    if (isItemAccountOwner) {
      if (isUserLocked(item.state) && checkPermission(['usersAndAccess', 'editAccountOwner']))
        return (
          <>
            <div
              className="list-card-columns"
              id="more-actions"
              onClick={(event: MouseEvent<HTMLDivElement>) => {
                setAnchorEl(event.currentTarget);
                setSelectedUser(item);
              }}
            >
              More <ArrowDropDown className="icon" fontSize="small" />
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
              <UnlockButton />
            </ListActionMenu>
          </>
        );
      return null;
    }

    return (
      <>
        <div
          className="list-card-columns"
          id="more-actions"
          onClick={(event: MouseEvent<HTMLDivElement>) => {
            setAnchorEl(event.currentTarget);
            setSelectedUser(item);
          }}
        >
          More <ArrowDropDown className="icon" fontSize="small" />
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
          {(() => {
            if (selectedUser?.archived) {
              return <UnArchiveButton />;
            } else {
              return (
                <div>
                  {(() => {
                    switch (selectedUser?.state) {
                      case UserStates.ACCOUNT_LOCKED:
                        return <UnlockButton />;
                      case UserStates.INVITE_CANCELLED:
                        return (
                          <>
                            <ArchiveButton />
                            <ResendInviteButton />
                          </>
                        );
                      case UserStates.INVITE_EXPIRED:
                        return (
                          <>
                            <ArchiveButton />
                            <ResendInviteButton />
                          </>
                        );
                      case UserStates.REGISTERED_LOCKED:
                        return (
                          <>
                            <ArchiveButton />
                            <UnlockButton />
                          </>
                        );
                      case UserStates.UNREGISTERED:
                        return (
                          <>
                            <GenerateNewSecretButton />
                            <CancelInviteButton />
                          </>
                        );
                      case UserStates.UNREGISTERED_LOCKED:
                        return (
                          <>
                            <ArchiveButton />
                            <UnlockButton />
                          </>
                        );
                      default:
                        return <ArchiveButton />;
                    }
                  })()}
                </div>
              );
            }
          })()}
        </ListActionMenu>
      </>
    );
  };

  return (
    <TabContentWrapper>
      <div className="before-table-wrapper">
        <div className="filters">
          <TextInput
            afterElementWithoutError
            AfterElement={Search}
            afterElementClass=""
            placeholder="Search Users"
            onChange={debounce(({ value }) => {
              if (value) {
                const filters = generateUserSearchFilters(FilterOperators.LIKE, value);
                setFilterFields(filters.fields);
              } else {
                setFilterFields([]);
              }
            }, 500)}
          />
          <ToggleSwitch
            offLabel="Show archived users"
            onChange={(isChecked) => {
              setIsArchived(isChecked);
            }}
            onLabel="Show unarchived users"
            checked={isArchived}
          />
        </div>
        {checkPermission(['usersAndAccess', 'addNewUser']) && !isArchived ? (
          <div className="actions">
            <Button onClick={() => navigate('users/add')}>Add a new User</Button>
          </div>
        ) : null}
      </div>

      <LoadingContainer
        loading={loading}
        component={
          <>
            <DataTable columns={columns} rows={currentPageData} emptyTitle="No User Found" />
            <Pagination pageable={pageable} fetchData={fetchData} />
          </>
        }
      />
    </TabContentWrapper>
  );
};

export default TabContent;
