import { resetPropertiesState } from '#store/properties/actions';
import { apiGetFacilities, apiSwitchFacility } from '#utils/apiUrls';
import { setAuthHeader } from '#utils/axiosClient';
import { ResponseObj } from '#utils/globalTypes';
import { getErrorMsg, request } from '#utils/request';
import { navigate } from '@reach/router';
import { call, put, takeEvery, takeLatest } from 'redux-saga/effects';

import {
  fetchFacilities,
  fetchFacilitiesError,
  fetchFacilitiesOngoing,
  fetchFacilitiesSuccess,
  switchFacility,
  switchFacilitySuccess,
} from './actions';
import { Facilities, FacilitiesAction } from './types';
import { MAX_PAGE_SIZE } from '#utils/constants';

function* fetchFacilitiesSaga({ payload }: ReturnType<typeof fetchFacilities>) {
  const { params } = payload || {};
  try {
    yield put(fetchFacilitiesOngoing());
    const { data }: ResponseObj<Facilities> = yield call(request, 'GET', apiGetFacilities(), {
      params: {
        ...params,
        size: MAX_PAGE_SIZE,
      },
    });
    yield put(fetchFacilitiesSuccess(data));
  } catch (error) {
    console.error('error from fetchFacilitiesSaga function in FacilitiesSaga :: ', error);
    yield put(fetchFacilitiesError(error));
  }
}

function* switchFacilitySaga({
  payload: { facilityId, loggedInUserId: userId },
}: ReturnType<typeof switchFacility>) {
  try {
    const { data, errors } = yield call(request, 'PATCH', apiSwitchFacility(userId, facilityId));

    if (data) {
      setAuthHeader(data.accessToken);
      yield put(switchFacilitySuccess(data.accessToken, facilityId));
      yield put(resetPropertiesState());
      navigate('/');
    } else {
      throw getErrorMsg(errors);
    }
  } catch (error) {
    console.error('error from switch facility saga :: ', error);
  }
}

export function* FacilitiesSaga() {
  yield takeEvery(FacilitiesAction.FETCH_FACILITIES, fetchFacilitiesSaga);
  yield takeLatest(FacilitiesAction.SWITCH_FACILITY, switchFacilitySaga);
}
