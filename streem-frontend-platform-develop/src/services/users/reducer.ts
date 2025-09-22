import { DEFAULT_PAGINATION } from '#utils/constants';
import { Reducer } from 'redux';

import {
  OtherUserState,
  UserGroup,
  UsersAction,
  UsersActionType,
  UsersById,
  UsersState,
  UserState,
} from './types';

const initialUserGroup: UserGroup = {
  pageable: DEFAULT_PAGINATION,
  users: [],
  usersById: {},
};

const initalState: UsersState = {
  loading: false,
  loadingMore: false,
  selectedState: UserState.ACTIVE,
  [UserState.ACTIVE]: initialUserGroup,
  [UserState.ARCHIVED]: initialUserGroup,
  [OtherUserState.TASKS]: initialUserGroup,
  [OtherUserState.REVIEWERS]: initialUserGroup,
  [OtherUserState.AUTHORS]: initialUserGroup,
  [OtherUserState.AUTHORS_GLOBAL]: initialUserGroup,
  [OtherUserState.REVIEWERS_GLOBAL]: initialUserGroup,
};

const reducer: Reducer<UsersState, UsersActionType> = (state = initalState, action) => {
  switch (action.type) {
    case UsersAction.FETCH_USERS_ONGOING:
      return { ...state, loading: true };

    case UsersAction.FETCH_USERS_ERROR:
      return { ...state, error: action.payload.error };

    case UsersAction.FETCH_USERS_SUCCESS:
      return {
        ...state,
        loading: false,
        loadingMore: false,
        [action.payload.type]: {
          ...state[action.payload.type],
          pageable: action.payload.pageable,
          users: !action.payload.initialCall
            ? [...state[action.payload.type].users, ...action.payload.list]
            : action.payload.list,
          usersById: !action.payload.initialCall
            ? {
                ...state[action.payload.type].usersById,
                ...action.payload.list.reduce<UsersById>((acc, user) => {
                  acc[user.id] = user;
                  return acc;
                }, {}),
              }
            : {
                ...action.payload.list.reduce<UsersById>((acc, user) => {
                  acc[user.id] = user;
                  return acc;
                }, {}),
              },
        },
      };

    case UsersAction.FETCH_MORE_USERS_ONGOING:
      return { ...state, loadingMore: true };

    case UsersAction.SET_SELECTED_STATE:
      return { ...state, selectedState: action.payload.state };

    default:
      return { ...state };
  }
};

export { reducer as UsersServiceReducer };
