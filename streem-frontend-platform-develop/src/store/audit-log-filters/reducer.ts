import { AuditLogsFiltersState, AuditLogFiltersActionType, AuditLogsFiltersAction } from './types';

const initialState: AuditLogsFiltersState = {
  filters: '',
  columns: [],
  pdfMetaData: {},
  queryParams: {},
};

const reducer = (
  state = initialState,
  action: AuditLogFiltersActionType,
): AuditLogsFiltersState => {
  switch (action.type) {
    case AuditLogsFiltersAction.SET_FILTERS:
      return { ...state, filters: action.payload };

    case AuditLogsFiltersAction.SET_COLUMNS:
      return { ...state, columns: action.payload };

    case AuditLogsFiltersAction.SET_PDF_META_DATA:
      return { ...state, pdfMetaData: action.payload };

    case AuditLogsFiltersAction.CLEAR_FILTRES:
      return initialState;

    case AuditLogsFiltersAction.SET_QUERY_PARAMS:
      return {
        ...state,
        queryParams: {
          ...state.queryParams,
          [action.payload.userId]: action.payload.filters
            ? {
                ...state.queryParams?.[action.payload.userId],
                ...action.payload.filters,
              }
            : {},
        },
      };

    default:
      return state;
  }
};

export { reducer as AuditLogFiltersReducer };
