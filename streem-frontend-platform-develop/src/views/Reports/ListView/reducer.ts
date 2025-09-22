import { DEFAULT_PAGINATION } from '#utils/constants';
import { Pageable } from '#utils/globalTypes';
import { ReportsAction, ReportsList, ReportState } from './types';

const initialState = {
  loading: false,
  error: undefined,
  selectedState: ReportState.Reports,
  reports: {
    list: [],
    pageable: DEFAULT_PAGINATION,
  },
  report: {},
};

const reducer = (state = initialState, action) => {
  switch (action.type) {
    case ReportsAction.FETCH_REPORTS:
      return { ...state, loading: true };
    case ReportsAction.FETCH_REPORTS_ONGOING:
      return { ...state, loading: true };

    case ReportsAction.FETCH_REPORTS_SUCCESS:
      return {
        ...state,
        loading: false,
        reports: {
          ...state.reports,
          list: action.payload.data as Array<ReportsList>,
          pageable: action.payload.pageable as Pageable,
        },
      };

    case ReportsAction.FETCH_REPORTS_ERROR:
      return { ...state, loading: false, error: action.payload?.error };
    case ReportsAction.FETCH_REPORT:
      return { ...state, loading: true };
    case ReportsAction.FETCH_REPORT_SUCCESS:
      return {
        ...state,
        loading: false,
        report: action.payload.data,
      };
    case ReportsAction.FETCH_REPORT_ERROR:
      return { ...state, loading: false, error: action.payload?.error };
    default:
      return { ...state };
  }
};

export { reducer as ReportsListViewReducer };
