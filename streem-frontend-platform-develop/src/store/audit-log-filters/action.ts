import { actionSpreader } from '#store/helpers';
import { AuditLogsFiltersAction } from './types';

export const setAuditLogFilters = (payload: string) =>
  actionSpreader(AuditLogsFiltersAction.SET_FILTERS, payload);

export const setPdfColumns = (payload: any[]) =>
  actionSpreader(AuditLogsFiltersAction.SET_COLUMNS, payload);

export const clearAuditLogFilters = () => actionSpreader(AuditLogsFiltersAction.CLEAR_FILTRES);

export const setPdfMetaData = (payload: Record<string, string>) =>
  actionSpreader(AuditLogsFiltersAction.SET_PDF_META_DATA, payload);

export const setQueryParams = (payload: {
  userId: string;
  filters: Record<string, string | undefined>;
}) => actionSpreader(AuditLogsFiltersAction.SET_QUERY_PARAMS, payload);
