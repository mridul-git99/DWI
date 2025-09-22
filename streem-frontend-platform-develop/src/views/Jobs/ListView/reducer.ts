import { DEFAULT_PAGINATION } from '#utils/constants';
import { ListViewAction, ListViewActionType, ListViewState } from './types';

const initialState: ListViewState = {
  loading: false,
  error: undefined,
  jobs: [],
  pageable: DEFAULT_PAGINATION,
  submitting: false,
  createdData: undefined,
  reRender: false,
};

// TODO: optimize the reducer for Unassigned, Assigned and completed tabs
const reducer = (state = initialState, action: ListViewActionType): ListViewState => {
  switch (action.type) {
    case ListViewAction.FETCH_JOBS_ONGOING:
      return { ...state, loading: true };

    case ListViewAction.FETCH_JOBS_SUCCESS:
      const { data, pageable } = action.payload;

      return {
        ...state,
        loading: false,
        jobs: data,
        pageable: pageable,
      };

    case ListViewAction.FETCH_JOBS_ERROR:
      return { ...state, loading: false, error: action.payload?.error };

    case ListViewAction.UPDATE_JOB:
    case ListViewAction.CREATE_JOB:
      return { ...state, submitting: true };

    case ListViewAction.CREATE_JOB_SUCCESS:
      return {
        ...state,
        submitting: false,
        createdData: action.payload.data,
        reRender: action.payload.shouldReRender ? !state.reRender : state.reRender,
      };

    case ListViewAction.UPDATE_JOB_SUCCESS:
      return {
        ...state,
        submitting: false,
        reRender: action.payload.shouldReRender ? !state.reRender : state.reRender,
      };

    case ListViewAction.CREATE_OR_UPDATE_JOB_ERROR:
      return {
        ...state,
        submitting: false,
      };

    default:
      return { ...state };
  }
};

export { reducer as NewJobListViewReducer };
