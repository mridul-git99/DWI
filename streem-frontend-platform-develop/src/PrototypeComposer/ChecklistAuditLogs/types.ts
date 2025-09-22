import { ComposerActionType } from '#PrototypeComposer/reducer.types';
import { Pageable } from '#utils/globalTypes';
import {
  fetchChecklistAuditLogsError,
  fetchChecklistAuditLogsOngoing,
  fetchChecklistAuditLogsSuccess,
} from './actions';

export interface ChecklistAuditLogsType {
  action: string;
  details: string;
  id: string;
  checklistId: string;
  triggeredAt: number;
  triggeredBy: number;
}

export interface ChecklistAuditLogsState {
  readonly logs: ChecklistAuditLogsType[];
  readonly pageable: Pageable;
  readonly loading: boolean;
  readonly error?: any;
}

export enum ChecklistAuditLogActions {
  FETCH_CHECKLIST_AUDIT_LOGS = '@@checklistComposer/checklistAuditLogs/FETCH_CHECKLIST_AUDIT_LOGS',
  FETCH_CHECKLIST_AUDIT_LOGS_ERROR = '@@checklistComposer/checklistAuditLogs/FETCH_CHECKLIST_AUDIT_LOGS_ERROR',
  FETCH_CHECKLIST_AUDIT_LOGS_ONGOING = '@@checklistComposer/checklistAuditLogs/FETCH_CHECKLIST_AUDIT_LOGS_ONGOING',
  FETCH_CHECKLIST_AUDIT_LOGS_SUCCESS = '@@checklistComposer/checklistAuditLogs/FETCH_CHECKLIST_AUDIT_LOGS_SUCCESS',
}

export type ChecklistAuditLogActionType =
  | ReturnType<
      | typeof fetchChecklistAuditLogsError
      | typeof fetchChecklistAuditLogsOngoing
      | typeof fetchChecklistAuditLogsSuccess
    >
  | ComposerActionType;
