import { User, UsersAction, UsersActionType, UsersListType, UsersState } from '#store/users/types';
import { DEFAULT_PAGINATION } from '#utils/constants';

const initialTabState = {
  list: [],
  pageable: DEFAULT_PAGINATION,
};

const initialState: UsersState = {
  active: initialTabState,
  archived: initialTabState,
  all: initialTabState,
  loading: true,
  selectedState: UsersListType.ACTIVE,
  selectedUser: undefined,
  currentPageData: [],
  selectedUserLoading: true,
};

const reducer = (state = initialState, action: UsersActionType): UsersState => {
  switch (action.type) {
    case UsersAction.FETCH_USERS_ONGOING:
      return { ...state, loading: true };

    case UsersAction.FETCH_SELECTED_USER:
      return { ...state, selectedUserLoading: true };

    case UsersAction.FETCH_USERS_SUCCESS:
      const { data, pageable, type } = action.payload;

      return {
        ...state,
        loading: false,
        [type]: {
          pageable,
          list:
            pageable && pageable.page === 0
              ? (data as User[])
              : [...state[type].list, ...(data as User[])],
        },
        currentPageData: data as User[],
      };

    case UsersAction.SET_SELECTED_STATE:
      return {
        ...state,
        selectedState: action.payload?.state || state.selectedState,
      };

    case UsersAction.FETCH_SELECTED_USER_SUCCESS:
      return {
        ...state,
        selectedUser: action.payload.data,
        selectedUserLoading: false,
      };

    case UsersAction.FETCH_SELECTED_USER_ERROR:
      return { ...state, selectedUserLoading: false };

    case UsersAction.FETCH_USERS_ERROR:
      return { ...state, loading: false, error: action.payload?.error };

    default:
      return { ...state };
  }
};

export { reducer as UsersReducer };
