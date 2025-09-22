import { apiGetSessionActivities } from '#utils/apiUrls';
import { ResponseObj } from '#utils/globalTypes';
import { getErrorMsg, handleCatch, request } from '#utils/request';
import { getStartOfDayEpochInTimezone } from '#utils/timeUtils';
import { call, put, takeLeading } from 'redux-saga/effects';
import {
  fetchSessionActivities,
  fetchSessionActivitiesError,
  fetchSessionActivitiesOngoing,
  fetchSessionActivitiesSuccess,
} from './actions';
import { SessionActivity, SessionActivityAction } from './types';

function* fetchSessionActivitiesSaga({ payload }: ReturnType<typeof fetchSessionActivities>) {
  try {
    const params = payload || {};
    if (params.page === 0) {
      yield put(fetchSessionActivitiesOngoing());
    }

    const { data, pageable, errors }: ResponseObj<SessionActivity[]> = yield call(
      request,
      'GET',
      apiGetSessionActivities(),
      { params },
    );

    if (errors) {
      throw getErrorMsg(errors);
    }

    const newData = data.map((el) => ({
      ...el,
      triggeredOn: getStartOfDayEpochInTimezone({ value: el.triggeredAt }),
    }));

    yield put(
      fetchSessionActivitiesSuccess({
        data: newData,
        pageable,
      }),
    );
  } catch (e) {
    const error = yield* handleCatch('SessionActivity', 'fetchSessionActivitiesSaga', e);
    yield put(fetchSessionActivitiesError(error as string));
  }
}

export function* SessionActivitySaga() {
  yield takeLeading(SessionActivityAction.FETCH_SESSION_ACTIVITY, fetchSessionActivitiesSaga);
}
