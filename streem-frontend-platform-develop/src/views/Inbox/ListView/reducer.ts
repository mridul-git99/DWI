import { DEFAULT_PAGINATION } from '#utils/constants';

import { InboxState, ListViewAction, ListViewActionType, ListViewState } from './types';

const initialState: ListViewState = {
  loading: false,
  error: undefined,
  selectedState: InboxState.PENDING_ON_ME,
  jobs: [],
  pageable: DEFAULT_PAGINATION,
  verifications: {
    loading: false,
    list: [],
    pageable: DEFAULT_PAGINATION,
  },
  approvals: {
    loading: false,
    list: [],
    pageable: DEFAULT_PAGINATION,
  },
};

const reducer = (state = initialState, action: ListViewActionType): ListViewState => {
  switch (action.type) {
    case ListViewAction.FETCH_INBOX_ONGOING:
      return { ...state, loading: true };

    case ListViewAction.FETCH_INBOX_SUCCESS:
      const { data, pageable } = action.payload;
      return {
        ...state,
        loading: false,
        jobs: data,
        pageable: pageable,
      };

    case ListViewAction.FETCH_INBOX_ERROR:
      return { ...state, loading: false, error: action.payload?.error };

    case ListViewAction.SET_SELECTED_STATE:
      return {
        ...state,
        selectedState: action.payload?.state || state.selectedState,
      };

    case ListViewAction.RESET_INBOX:
      return { ...initialState };

    case ListViewAction.FETCH_VERIFICATIONS:
      return {
        ...state,
        verifications: {
          ...state.verifications,
          loading: true,
        },
      };

    case ListViewAction.FETCH_VERIFICATIONS_SUCCESS:
      return {
        ...state,
        verifications: {
          ...state.verifications,
          loading: false,
          list: action.payload?.data || [],
          pageable: action.payload?.pageable || DEFAULT_PAGINATION,
        },
      };

    case ListViewAction.FETCH_APPROVALS:
      return {
        ...state,
        approvals: {
          ...state.approvals,
          loading: true,
        },
      };

    case ListViewAction.FETCH_APPROVALS_SUCCESS:
      return {
        ...state,
        approvals: {
          ...state.approvals,
          loading: false,
          list: action.payload?.data || [],
          pageable: action.payload?.pageable || DEFAULT_PAGINATION,
        },
      };

    case ListViewAction.UPDATE_APPROVALS_LIST:
      const { rulesId } = action.payload;
      return {
        ...state,
        approvals: {
          ...state.approvals,
          list: state.approvals.list.filter((item) => item.rulesId !== rulesId),
        },
      };

    default:
      return { ...state };
  }
};

export { reducer as InboxListViewReducer };
