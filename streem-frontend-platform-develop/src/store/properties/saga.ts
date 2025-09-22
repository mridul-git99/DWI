import { apiGetProperties } from '#utils/apiUrls';
import { request } from '#utils/request';
import { call, put, takeEvery } from 'redux-saga/effects';
import { fetch, fetchError, fetchOngoing, fetchSuccess } from './actions';
import { PropertiesAction } from './types';

function* fetchSaga({ payload }: ReturnType<typeof fetch>) {
  const { entityArr, useCaseId } = payload;
  for (const entity of entityArr) {
    try {
      yield put(fetchOngoing(entity));
      const { data, errors } = yield call(request, 'GET', apiGetProperties(), {
        params: { type: entity, useCaseId },
      });
      if (data) {
        yield put(fetchSuccess({ data, entity }));
      } else {
        yield put(fetchError(errors));
      }
    } catch (error) {
      console.error('error in fetchSaga  :: ', error);
    }
  }
}

export function* PropertiesSaga() {
  yield takeEvery(PropertiesAction.FETCH_PROPERTIES, fetchSaga);
}
