import { actionSpreader } from '#store/helpers';
import { DownloadAction } from './types';

export const fetchJobLogsExcel = (params: any) =>
  actionSpreader(DownloadAction.FETCH_JOB_LOGS_EXCEL, {
    params,
  });

export const fetchJobLogsExcelError = (error: any) =>
  actionSpreader(DownloadAction.FETCH_JOB_LOGS_EXCEL_ERROR, { error });
