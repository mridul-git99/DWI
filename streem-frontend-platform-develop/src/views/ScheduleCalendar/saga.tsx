import { apiProcessScheduler } from '#utils/apiUrls';
import { ResponseObj } from '#utils/globalTypes';
import { request } from '#utils/request';
import { call, put, takeLeading } from 'redux-saga/effects';
import { scheduleActions, ScheduleActionsEnum } from './scheduleStore';

function* fetchScheduleListSaga({ payload }: ReturnType<typeof scheduleActions.fetchScheduleList>) {
  try {
    const { data }: ResponseObj<any> = yield call(request, 'GET', apiProcessScheduler(), {
      params: payload,
    });
    yield put(
      scheduleActions.fetchScheduleListSuccess({
        scheduleList: data,
      }),
    );
  } catch (err) {
    console.log(err);
  }
}

export function* scheduleCalendarSaga() {
  yield takeLeading(ScheduleActionsEnum.fetchScheduleList, fetchScheduleListSaga);
}
