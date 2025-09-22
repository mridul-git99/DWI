import { actionSpreader } from '#store/helpers';
import { ResponseObj } from '#utils/globalTypes';
import { ChecklistAuditLogActions, ChecklistAuditLogsType } from './types';

export const fetchChecklistAuditLogs = (payload: {
  checklistId: string;
  params: {
    size: number;
    filters: string;
    sort: string;
    page: number;
  };
}) => actionSpreader(ChecklistAuditLogActions.FETCH_CHECKLIST_AUDIT_LOGS, payload);

export const fetchChecklistAuditLogsOngoing = () =>
  actionSpreader(ChecklistAuditLogActions.FETCH_CHECKLIST_AUDIT_LOGS_ONGOING);

export const fetchChecklistAuditLogsSuccess = ({
  data,
  pageable,
}: Partial<ResponseObj<ChecklistAuditLogsType[]>>) =>
  actionSpreader(ChecklistAuditLogActions.FETCH_CHECKLIST_AUDIT_LOGS_SUCCESS, {
    data,
    pageable,
  });

export const fetchChecklistAuditLogsError = (error: any) =>
  actionSpreader(ChecklistAuditLogActions.FETCH_CHECKLIST_AUDIT_LOGS_ERROR, {
    error,
  });
