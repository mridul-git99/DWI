import { apiGetReports, apiGetReport } from '../../../utils/apiUrls';
import { ResponseObj } from '../../../utils/globalTypes';
import { getErrorMsg, handleCatch, request } from '../../../utils/request';
import { call, put, takeLatest } from 'redux-saga/effects';
import {
  fetchReports,
  fetchReportsOngoing,
  fetchReportsSuccess,
  fetchReportsError,
  fetchReportError,
  fetchReportSuccess,
  fetchReport,
} from './action';
import { ReportsAction } from './types';

function* fetchReportsSaga({ payload }: ReturnType<typeof fetchReports>) {
  try {
    const { params } = payload;

    if (params.page === 0) {
      yield put(fetchReportsOngoing());
    }

    const { data, pageable, errors }: ResponseObj<any> = yield call(
      request,
      'GET',
      apiGetReports(),
      {
        params,
      },
    );
    if (errors) {
      throw getErrorMsg(errors);
    }
    yield put(fetchReportsSuccess({ data, pageable }));
  } catch (e) {
    const error = yield* handleCatch('ReportsListView', 'fetchReportsSaga', e);
    yield put(fetchReportsError(error));
  }
}

function* fetchReportSaga({ payload }: ReturnType<typeof fetchReport>) {
  try {
    const { params } = payload;
    const { data } = yield call(request, 'GET', apiGetReport(params.id), {
      params: { useCaseId: params.useCaseId },
    });

    if (data) {
      yield put(fetchReportSuccess({ data }));
    }
  } catch (error) {
    console.error('error from fetchReport function in Report Saga :: ', error);
    yield put(fetchReportError(error));
  }
}

export function* ReportsListViewSaga() {
  yield takeLatest(ReportsAction.FETCH_REPORTS, fetchReportsSaga);
  yield takeLatest(ReportsAction.FETCH_REPORT, fetchReportSaga);
}
