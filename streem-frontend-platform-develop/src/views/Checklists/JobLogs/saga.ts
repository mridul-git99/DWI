import { apiGetJobLogsExcel } from '#utils/apiUrls';
import { request } from '#utils/request';
import { call, put, takeLatest } from 'redux-saga/effects';
import { fetchJobLogsExcel, fetchJobLogsExcelError } from './actions';
import { DownloadAction } from './types';

function* fetchJobLogsExcelSaga({ payload }: ReturnType<typeof fetchJobLogsExcel>) {
  try {
    const { params } = payload;
    const res = yield call(request, 'GET', apiGetJobLogsExcel(), {
      params,
      responseType: 'blob',
    });
    if (res) {
      const url = window.URL.createObjectURL(new Blob([res]));
      const link = document.createElement('a');
      link.href = url;
      const timestamp = Date.now();
      const baseFilename = `View_${params.customViewId}_${params.checklistId}_${timestamp}`;
      link.setAttribute('download', `${baseFilename}.xlsx`);
      document.body.appendChild(link);
      link.click();
    }
  } catch (error) {
    console.error('error from fetchJobLogsExcel function in JobLogsSaga :: ', error);
    yield put(fetchJobLogsExcelError(error));
  }
}

export function* JobLogsSaga() {
  yield takeLatest(DownloadAction.FETCH_JOB_LOGS_EXCEL, fetchJobLogsExcelSaga);
}
