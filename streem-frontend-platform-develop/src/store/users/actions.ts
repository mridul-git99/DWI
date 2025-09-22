import { actionSpreader } from '#store/helpers';
import { ResponseObj } from '#utils/globalTypes';

import { User, UsersAction, UsersListType } from './types';

export const fetchUsers = (
  params: Record<string, string | number | boolean | undefined>,
  type: UsersListType,
) => actionSpreader(UsersAction.FETCH_USERS, { params, type });

export const fetchUsersOngoing = () => actionSpreader(UsersAction.FETCH_USERS_ONGOING);

export const fetchUsersSuccess = (
  { data, pageable }: Partial<ResponseObj<User[]>>,
  type: UsersListType,
) =>
  actionSpreader(UsersAction.FETCH_USERS_SUCCESS, {
    data,
    pageable,
    type,
  });

export const fetchUsersError = (error: string) =>
  actionSpreader(UsersAction.FETCH_USERS_ERROR, { error });

export const setSelectedState = (state: UsersListType) =>
  actionSpreader(UsersAction.SET_SELECTED_STATE, { state });

export const fetchSelectedUser = (id?: User['id']) =>
  actionSpreader(UsersAction.FETCH_SELECTED_USER, { id });

export const fetchSelectedUserSuccess = ({ data }: Partial<ResponseObj<User>>) =>
  actionSpreader(UsersAction.FETCH_SELECTED_USER_SUCCESS, {
    data,
  });

export const fetchSelectedUserError = (error: string) =>
  actionSpreader(UsersAction.FETCH_SELECTED_USER_ERROR, { error });
