import { DEFAULT_PAGINATION } from '#utils/constants';
import { Pageable } from '#utils/globalTypes';
import { keyBy } from 'lodash';
import { Checklist } from '../types';
import { ListViewAction, ListViewActionType, ListViewState } from './types';

const initialState: ListViewState = {
  checklists: [],
  currentPageData: [],
  loading: false,
  pageable: DEFAULT_PAGINATION,
  jobLogs: {
    list: [],
    loading: true,
    pageable: DEFAULT_PAGINATION,
    processName: '',
  },
  customViews: {
    loading: false,
    views: {},
  },
};

// TODO: optimize the reducer for Published and prototype tabs
const reducer = (state = initialState, action: ListViewActionType): ListViewState => {
  switch (action.type) {
    case ListViewAction.FETCH_CHECKLISTS_ONGOING:
      return { ...state, loading: true };

    case ListViewAction.FETCH_PROCESS_LOGS:
      return { ...state, jobLogs: { ...state.jobLogs, loading: true } };

    case ListViewAction.CLEAR_DATA:
      return { ...state, checklists: [], pageable: DEFAULT_PAGINATION };

    case ListViewAction.FETCH_CHECKLISTS_SUCCESS:
      const { data, pageable } = action.payload;
      return {
        ...state,
        loading: false,
        pageable: pageable as Pageable,
        checklists:
          pageable && pageable.page === 0
            ? (data as Array<Checklist>)
            : [...state.checklists, ...(data as Array<Checklist>)],
        currentPageData: data as Checklist[],
      };

    case ListViewAction.FETCH_CHECKLISTS_ERROR:
      return { ...state, loading: false, error: action.payload?.error };

    case ListViewAction.FETCH_PROCESS_LOGS_ERROR:
      return {
        ...state,
        jobLogs: { ...state.jobLogs, loading: false, error: action.payload?.error },
      };

    case ListViewAction.UPDATE_LIST:
      return {
        ...state,
        currentPageData: state.currentPageData.filter(
          (checklist) => checklist.id !== action.payload.id,
        ),
      };

    case ListViewAction.FETCH_PROCESS_LOGS_SUCCESS:
      return {
        ...state,
        jobLogs: {
          ...state.jobLogs,
          loading: false,
          list: action.payload.data!,
          pageable: action.payload.pageable!,
          processName: action.payload.processName,
        },
      };

    case ListViewAction.SAVE_CUSTOM_VIEW:
    case ListViewAction.GET_CUSTOM_VIEWS:
    case ListViewAction.ADD_CUSTOM_VIEW:
      return {
        ...state,
        customViews: {
          ...state.customViews,
          loading: true,
        },
      };

    case ListViewAction.ADD_CUSTOM_VIEW_SUCCESS:
      return {
        ...state,
        customViews: {
          ...state.customViews,
          loading: false,
          views: { ...state.customViews.views, [action.payload.data.id]: action.payload.data },
        },
      };

    case ListViewAction.DELETE_CUSTOM_VIEW_ERROR:
    case ListViewAction.SAVE_CUSTOM_VIEW_ERROR:
    case ListViewAction.GET_CUSTOM_VIEWS_ERROR:
    case ListViewAction.ADD_CUSTOM_VIEW_ERROR:
      return {
        ...state,
        error: action.payload?.error,
        customViews: {
          ...state.customViews,
          loading: false,
        },
      };

    case ListViewAction.GET_CUSTOM_VIEWS_SUCCESS:
      return {
        ...state,
        customViews: {
          ...state.customViews,
          loading: false,
          views: { ...keyBy(action.payload.data, 'id') },
        },
      };

    case ListViewAction.SAVE_CUSTOM_VIEW_SUCCESS:
      return {
        ...state,
        customViews: {
          ...state.customViews,
          loading: false,
          views: { ...state.customViews.views, [action.payload.data.id]: action.payload.data },
        },
      };

    case ListViewAction.DELETE_CUSTOM_VIEW_SUCCESS:
      const updatedViews = { ...state.customViews.views };
      delete updatedViews[action.payload.data.id];
      return {
        ...state,
        customViews: {
          ...state.customViews,
          views: updatedViews,
        },
      };

    default:
      return { ...state };
  }
};

export { reducer as ChecklistListViewReducer };
