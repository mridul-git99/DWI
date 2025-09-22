import { Pageable } from '#utils/globalTypes';

import { Facility, Organisation } from '../commonTypes';
import {
  fetchError,
  fetchMoreOngoing,
  fetchOngoing,
  fetchSuccess,
  setSelectedState,
} from './actions';

type UserRole = {
  id: string;
  name: string;
};

export enum UserState {
  ACTIVE = 'active',
  ARCHIVED = 'archived',
}

export enum OtherUserState {
  TASKS = 'tasks',
  REVIEWERS = 'reviewers',
  AUTHORS = 'authors',
  AUTHORS_GLOBAL = 'authors_global',
  REVIEWERS_GLOBAL = 'reviewers_global',
}

export interface User {
  id: string;
  employeeId: string;
  firstName: string;
  lastName: string;
  email: string;
  username: string;
  verified: boolean;
  blocked: boolean;
  archived: boolean;
  active: boolean;
  department?: string;
  assigned?: boolean;
  completelyAssigned?: boolean;
  roles?: UserRole[];
  facilities?: Facility[];
  organisation?: Organisation;
  userId: string;
  userGroupId: string;
}

export type UsersById = Record<User['id'], User>;

export type UserGroup = {
  readonly pageable: Pageable;
  readonly users: User[];
  readonly usersById: UsersById;
};

export type UsersState = {
  readonly error?: unknown;
  readonly loading: boolean;
  readonly loadingMore: boolean;
  readonly selectedState: UserState;
  readonly selectedUserId?: User['id'];
  readonly [UserState.ACTIVE]: UserGroup;
  readonly [UserState.ARCHIVED]: UserGroup;
  readonly [OtherUserState.TASKS]: UserGroup;
  readonly [OtherUserState.REVIEWERS]: UserGroup;
  readonly [OtherUserState.AUTHORS]: UserGroup;
  readonly [OtherUserState.AUTHORS_GLOBAL]: UserGroup;
  readonly [OtherUserState.REVIEWERS_GLOBAL]: UserGroup;
};

export enum UsersAction {
  FETCH_MORE_USERS_ONGOING = '@@users-service/FETCH_MORE_USERS_ONGOING',
  FETCH_USERS = '@@users-service/FETCH_USERS',
  FETCH_USERS_ERROR = '@@users-service/FETCH_USERS_ERROR',
  FETCH_USERS_ONGOING = '@@users-service/FETCH_USERS_ONGOING',
  FETCH_USERS_SUCCESS = '@@users-service/FETCH_USERS_SUCCESS',
  SET_SELECTED_STATE = '@@users-service/SET_SELECTED_STATE',
}

export type UsersActionType = ReturnType<
  | typeof fetchError
  | typeof fetchOngoing
  | typeof fetchMoreOngoing
  | typeof fetchSuccess
  | typeof setSelectedState
>;

export type useUsersType = {
  userState?: UserState | OtherUserState;
  params?: fetchUsersParams;
};

export type useUsersReturnType = {
  loadMore: () => void;
  loadAgain: ({ newParams }: { newParams: fetchUsersParams }) => void;
  users: User[];
  usersById: UsersById;
};

export type fetchUsersParams = {
  filters?: string;
  page: number;
  size: number;
  sort?: string;
};

export type fetchType = {
  initialCall: boolean;
  params: fetchUsersParams;
  type: UserState | OtherUserState;
};

export type fetchSuccessType = {
  data: {
    list: User[];
    pageable: Pageable;
  };
  type: UserState | OtherUserState;
  initialCall: boolean;
};
