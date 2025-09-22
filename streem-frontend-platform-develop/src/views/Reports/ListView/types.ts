export enum ReportState {
  Reports = 'Reports',
}

export type ReportsList = {
  id: string;
  name?: string;
  type?: string;
};

export enum ReportsAction {
  FETCH_REPORTS = '@@reports/ListView/FETCH_REPORTS',
  FETCH_REPORTS_ERROR = '@@reports/ListView/FETCH_REPORTS_ERROR',
  FETCH_REPORTS_ONGOING = '@@reports/ListView/FETCH_REPORTS_ONGOING',
  FETCH_REPORTS_SUCCESS = '@@reports/ListView/FETCH_REPORTS_SUCCESS',
  // SINGLE
  FETCH_REPORT = '@@reports/ListView/FETCH_REPORT',
  FETCH_REPORT_ERROR = '@@reports/ListView/FETCH_REPORT_ERROR',
  FETCH_REPORT_ONGOING = '@@reports/ListView/FETCH_REPORT_ONGOING',
  FETCH_REPORT_SUCCESS = '@@reports/ListView/FETCH_REPORT_SUCCESS',
}
