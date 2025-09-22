import {
  clearAuditLogFilters,
  setAuditLogFilters,
  setPdfColumns,
  setPdfMetaData,
  setQueryParams,
} from './action';

export interface AuditLogsFiltersState {
  readonly filters: string;
  readonly columns: any[];
  readonly pdfMetaData: Record<string, string>;
  readonly queryParams: Record<string, Record<string, string | undefined>>;
}

export enum AuditLogsFiltersAction {
  SET_FILTERS = '@@auditLogsFilters/SET_FILTERS',
  CLEAR_FILTRES = '@@auditLogsFilters/CLEAR_FILTRES',
  SET_PDF_META_DATA = '@@auditLogsFilters/SET_PDF_META_DATA',
  SET_COLUMNS = '@@auditLogsFilters/SET_COLUMNS',
  SET_QUERY_PARAMS = '@@auditLogsFilters/SET_QUERY_PARAMS',
}

export type AuditLogFiltersActionType = ReturnType<
  | typeof setAuditLogFilters
  | typeof clearAuditLogFilters
  | typeof setPdfColumns
  | typeof setPdfMetaData
  | typeof setQueryParams
>;
