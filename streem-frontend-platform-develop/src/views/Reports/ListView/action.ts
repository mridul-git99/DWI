import { actionSpreader } from '#store/helpers';
import { ResponseObj } from '#utils/globalTypes';

import { ReportsAction } from './types';

export const fetchReports = (params: Record<string, string | number>) =>
  actionSpreader(ReportsAction.FETCH_REPORTS, { params });

export const fetchReportsOngoing = () => actionSpreader(ReportsAction.FETCH_REPORTS_ONGOING);

export const fetchReportsSuccess = ({ data, pageable }: Partial<ResponseObj<any>>) =>
  actionSpreader(ReportsAction.FETCH_REPORTS_SUCCESS, { data, pageable });

export const fetchReportsError = (error: any) =>
  actionSpreader(ReportsAction.FETCH_REPORTS_ERROR, { error });

export const fetchReport = (params: Record<string, string | number>) =>
  actionSpreader(ReportsAction.FETCH_REPORT, { params });

export const fetchReportSuccess = ({ data }: Partial<ResponseObj<any>>) =>
  actionSpreader(ReportsAction.FETCH_REPORT_SUCCESS, { data });

export const fetchReportError = (error: any) =>
  actionSpreader(ReportsAction.FETCH_REPORT_ERROR, { error });
