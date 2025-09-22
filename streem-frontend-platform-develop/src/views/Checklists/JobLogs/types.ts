import { fetchJobLogsExcel, fetchJobLogsExcelError } from './actions';

export enum DownloadAction {
  FETCH_JOB_LOGS_EXCEL = '@@joblogs/Download/FETCH_JOB_LOGS_EXCEL',
  FETCH_JOB_LOGS_EXCEL_ERROR = '@@joblogs/Download/FETCH_JOB_LOGS_EXCEL_ERROR',
}

export type ListViewActionType = ReturnType<
  typeof fetchJobLogsExcel | typeof fetchJobLogsExcelError
>;
