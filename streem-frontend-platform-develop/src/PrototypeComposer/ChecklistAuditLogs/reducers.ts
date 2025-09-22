import { DEFAULT_PAGINATION } from '#utils/constants';
import { Pageable } from '#utils/globalTypes';
import {
  ChecklistAuditLogActions,
  ChecklistAuditLogActionType,
  ChecklistAuditLogsState,
  ChecklistAuditLogsType,
} from './types';

export const initialState: ChecklistAuditLogsState = {
  logs: [],
  loading: false,
  pageable: DEFAULT_PAGINATION,
};

const reducer = (
  state = initialState,
  action: ChecklistAuditLogActionType,
): ChecklistAuditLogsState => {
  switch (action.type) {
    case ChecklistAuditLogActions.FETCH_CHECKLIST_AUDIT_LOGS_ONGOING:
      return { ...state, loading: true };

    case ChecklistAuditLogActions.FETCH_CHECKLIST_AUDIT_LOGS_SUCCESS:
      const { data, pageable } = action.payload;
      return {
        ...state,
        loading: false,
        pageable: pageable as Pageable,
        logs:
          pageable && pageable.page === 0
            ? (data as Array<ChecklistAuditLogsType>)
            : [...state.logs, ...(data as ChecklistAuditLogsType[])],
      };

    case ChecklistAuditLogActions.FETCH_CHECKLIST_AUDIT_LOGS_ERROR:
      return { ...state, loading: false, error: action.payload?.error };

    default:
      return { ...state };
  }
};

export { reducer as checklistAuditLogsReducer };
