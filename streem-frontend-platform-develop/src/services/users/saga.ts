import { apiGetUsers } from '#utils/apiUrls';
import { request } from '#utils/request';
import { call, put, takeLatest } from 'redux-saga/effects';

import { fetch, fetchError, fetchMoreOngoing, fetchOngoing, fetchSuccess } from './actions';
import { OtherUserState, User, UsersAction } from './types';

function* fetchSaga({ payload }: ReturnType<typeof fetch>) {
  try {
    const { initialCall, params, type } = payload;

    if (initialCall) {
      yield put(fetchOngoing());
    } else {
      yield put(fetchMoreOngoing());
    }

    const { data, pageable, errors } = yield call(
      request,
      'GET',
      apiGetUsers(type.toUpperCase() in OtherUserState ? type.replaceAll('_', '/') : ''),
      { params },
    );

    if (data) {
      yield put(
        fetchSuccess({
          data: {
            list: (data as Array<User>).filter((el) => el.id !== '0'),
            pageable,
          },
          type,
          initialCall,
        }),
      );
    } else {
      yield put(fetchError(errors));
    }
  } catch (error) {
    console.error('error came in fetchSaga :: ', error);
  }
}

export function* UsersServiceSaga() {
  yield takeLatest(UsersAction.FETCH_USERS, fetchSaga);
}
