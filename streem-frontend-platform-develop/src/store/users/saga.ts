import { apiGetUser, apiGetUsers } from '#utils/apiUrls';
import { request } from '#utils/request';
import { ResponseObj } from '#utils/globalTypes';
import { call, put, takeLeading, takeLatest } from 'redux-saga/effects';

import {
  fetchUsers,
  fetchUsersError,
  fetchUsersSuccess,
  fetchUsersOngoing,
  fetchSelectedUser,
  fetchSelectedUserSuccess,
  fetchSelectedUserError,
} from './actions';
import { UsersAction, User, UsersListType } from './types';

function* fetchUsersSaga({ payload }: ReturnType<typeof fetchUsers>) {
  try {
    const { params, type } = payload;

    if (params.page === 0) {
      yield put(fetchUsersOngoing());
    }

    const { data, pageable }: ResponseObj<User[]> = yield call(
      request,
      'GET',
      apiGetUsers(type === UsersListType.ALL ? type : ''),
      { params },
    );
    yield put(fetchUsersSuccess({ data, pageable }, type));
  } catch (error) {
    console.error('error from fetchUsers function in UsersUsersSaga :: ', error);
    yield put(fetchUsersError(error));
  }
}

function* fetchSelectedUserSaga({ payload }: ReturnType<typeof fetchSelectedUser>) {
  try {
    const { id } = payload;

    if (!id) {
      yield put(fetchSelectedUserSuccess({ data: undefined }));
      return;
    }

    const { data, errors }: ResponseObj<User> = yield call(request, 'GET', apiGetUser(id));

    if (errors) {
      return false;
    }

    yield put(fetchSelectedUserSuccess({ data }));
  } catch (error) {
    console.error('error from fetchSelectedUserSaga function in Auth :: ', error);
    yield put(fetchSelectedUserError(error));
  }
}

export function* UsersSaga() {
  yield takeLatest(UsersAction.FETCH_USERS, fetchUsersSaga);
  yield takeLeading(UsersAction.FETCH_SELECTED_USER, fetchSelectedUserSaga);
}
