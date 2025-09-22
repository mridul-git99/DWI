import { Facility, Organisation } from '#services/commonTypes';
import { Pageable } from '#utils/globalTypes';
import { UserType } from '#views/UserAccess/ManageUser/types';
import { RoleType } from '#views/UserAccess/types';

import {
  fetchSelectedUser,
  fetchSelectedUserError,
  fetchSelectedUserSuccess,
  fetchUsers,
  fetchUsersError,
  fetchUsersOngoing,
  fetchUsersSuccess,
  setSelectedState,
} from './actions';

export type ChallengeQuestion = {
  id: string;
  question: string;
};

export interface User {
  id: string;
  employeeId: string;
  firstName: string;
  lastName: string;
  email: string;
  username: string;
  archived: boolean;
  state: UserStates;
  token?: string;
  department?: string;
  assigned?: boolean;
  completelyAssigned?: boolean;
  roles?: Pick<RoleType, 'id' | 'name'>[];
  facilities?: Facility[];
  organisation?: Organisation;
  challengeQuestion?: ChallengeQuestion;
  userType: UserType;
}

export type Users = User[];

export type UsersGroup = {
  list: User[];
  pageable: Pageable;
};

export interface UsersState {
  readonly [UsersListType.ACTIVE]: UsersGroup;
  readonly [UsersListType.ARCHIVED]: UsersGroup;
  readonly [UsersListType.ALL]: UsersGroup;
  readonly loading: boolean;
  readonly error?: string;
  readonly selectedState: string;
  readonly selectedUser?: User;
  readonly currentPageData: User[];
  readonly selectedUserLoading: boolean;
}

export enum UserStates {
  ACCOUNT_LOCKED = 'ACCOUNT_LOCKED',
  INVITE_CANCELLED = 'INVITE_CANCELLED',
  INVITE_EXPIRED = 'INVITE_EXPIRED',
  PASSWORD_EXPIRED = 'PASSWORD_EXPIRED',
  REGISTERED = 'REGISTERED',
  REGISTERED_LOCKED = 'REGISTERED_LOCKED',
  UNREGISTERED = 'UNREGISTERED',
  UNREGISTERED_LOCKED = 'UNREGISTERED_LOCKED',
}

export enum UserStatesContent {
  ACCOUNT_LOCKED = 'Locked',
  INVITE_CANCELLED = 'Invitation Canceled',
  INVITE_EXPIRED = 'Invitation Expired',
  PASSWORD_EXPIRED = 'Active',
  REGISTERED = 'Active',
  REGISTERED_LOCKED = 'Locked',
  UNREGISTERED = 'Unregistered',
  UNREGISTERED_LOCKED = 'Locked',
}

export enum UserStatesColors {
  ACCOUNT_LOCKED = '#ff6b6b',
  INVITE_CANCELLED = '#f7b500',
  INVITE_EXPIRED = '#f7b500',
  PASSWORD_EXPIRED = '#5aa700',
  REGISTERED = '#5aa700',
  REGISTERED_LOCKED = '#ff6b6b',
  UNREGISTERED = '#f7b500',
  UNREGISTERED_LOCKED = '#ff6b6b',
}

export enum UsersListType {
  ACTIVE = 'active',
  ARCHIVED = 'archived',
  ALL = 'all',
}

export enum UsersAction {
  FETCH_SELECTED_USER = '@@users/FETCH_SELECTED_USER',
  FETCH_SELECTED_USER_ERROR = '@@users/FETCH_SELECTED_USER_ERROR',
  FETCH_SELECTED_USER_SUCCESS = '@@users/FETCH_SELECTED_USER_SUCCESS',
  FETCH_USERS = '@@users/FETCH_USERS',
  FETCH_USERS_ERROR = '@@users/FETCH_USERS_ERROR',
  FETCH_USERS_ONGOING = '@@users/FETCH_USERS_ONGOING',
  FETCH_USERS_SUCCESS = '@@users/FETCH_USERS_SUCCESS',
  SET_SELECTED_STATE = '@@users/SET_SELECTED_STATE',
}

export type UsersActionType = ReturnType<
  | typeof fetchUsersError
  | typeof fetchUsersSuccess
  | typeof setSelectedState
  | typeof fetchUsersOngoing
  | typeof fetchUsers
  | typeof fetchSelectedUser
  | typeof fetchSelectedUserSuccess
  | typeof fetchSelectedUserError
>;
