import {
  apiArchiveProcessScheduler,
  apiCreateScheduler,
  apiGetChecklistInfo,
  apiSingleProcessScheduler,
  apiVersionHistoryProcessScheduler,
} from '#utils/apiUrls';
import { ResponseObj } from '#utils/globalTypes';
import { getErrorMsg, request } from '#utils/request';
import { call, put, takeLatest } from 'redux-saga/effects';
import schedulerActionObjects from './schedulerStore';
import { showNotification } from '#components/Notification/actions';
import { NotificationType } from '#components/Notification/types';

const { schedulerActions, SchedulerActionsEnum } = schedulerActionObjects;

function* postCreateScheduler({ payload }: ReturnType<typeof schedulerActions.saveScheduler>) {
  try {
    const { data, handleClose } = payload;
    const response: ResponseObj<any> = yield call(request, 'POST', apiCreateScheduler(), {
      data,
    });
    if (response.data) {
      yield put(schedulerActions.saveSchedulerSuccess(response.data));
      yield put(
        showNotification({
          type: NotificationType.SUCCESS,
          msg: 'Scheduler created successfully',
        }),
      );
      handleClose();
    } else {
      yield put(schedulerActions.saveSchedulerError({ errors: response.errors }));
      throw getErrorMsg(response.errors);
    }
  } catch (error) {
    yield put(
      showNotification({
        type: NotificationType.ERROR,
        msg: typeof error !== 'string' ? 'Oops! Please Try Again.' : error,
      }),
    );
    console.error('error from postCreateScheduler function in SchedulerListViewSaga :: ', error);
  }
}

function* getSchedulerList({ payload }: ReturnType<typeof schedulerActions.fetchSchedulers>) {
  try {
    const { data, pageable }: ResponseObj<any> = yield call(request, 'GET', apiCreateScheduler(), {
      params: payload,
    });
    if (data) {
      yield put(schedulerActions.fetchSchedulersSuccess({ data, pageable }));
    }
  } catch (error) {
    console.error('error from getSchedulerList function in SchedulerListViewSaga ::  ', error);
  }
}

function* getSchedulerVersionHisoryList({
  payload,
}: ReturnType<typeof schedulerActions.fetchSchedulersVersionHistory>) {
  try {
    const { schedularId } = payload;
    const { data }: ResponseObj<any> = yield call(
      request,
      'GET',
      apiVersionHistoryProcessScheduler(schedularId),
    );
    if (data) {
      yield put(schedulerActions.fetchSchedulersVersionHistorySuccess({ data }));
    }
  } catch (error) {
    console.error(
      'error from getSchedulerVersionHisoryList function in SchedulerListViewSaga ::  ',
      error,
    );
  }
}

function* patchArchiveSchedular({ payload }: ReturnType<typeof schedulerActions.archiveScheduler>) {
  try {
    const { schedularId, reason, setFormErrors } = payload;
    const { data, errors }: ResponseObj<any> = yield call(
      request,
      'PATCH',
      apiArchiveProcessScheduler(schedularId),
      { data: { reason } },
    );
    if (data) {
      yield put(
        showNotification({
          type: NotificationType.SUCCESS,
          msg: 'Scheduler archived successfully',
        }),
      );
      yield put(schedulerActions.updateSchedulerList({ id: schedularId }));
      setFormErrors(errors);
    } else {
      setFormErrors(errors);
      console.error('error from apiArchiveSchedular :: ', errors);
    }
  } catch (error) {
    console.error('error from getSchedulerList function in SchedulerListViewSaga ::  ', error);
  }
}

function* patchEditScheduler({ payload }: ReturnType<typeof schedulerActions.modifyScheduler>) {
  try {
    const { schedularId, data, handleClose } = payload;
    const response: ResponseObj<any> = yield call(
      request,
      'PATCH',
      apiSingleProcessScheduler(schedularId),
      { data },
    );
    if (response.data) {
      handleClose();
      yield put(
        showNotification({
          type: NotificationType.SUCCESS,
          msg: 'Scheduler updated successfully',
        }),
      );
      yield put(schedulerActions.modifySchedulerSuccess({ id: schedularId, data: response.data }));
    } else {
      yield put(schedulerActions.modifySchedulerError({ errors: response.errors }));
      throw getErrorMsg(response.errors);
    }
  } catch (error) {
    console.error('error from getSingleScheduler function in SchedulerListViewSaga ::  ', error);
    yield put(
      showNotification({
        type: NotificationType.ERROR,
        msg: typeof error !== 'string' ? 'Oops! Please Try Again.' : error,
      }),
    );
  }
}

function* getChecklistInfo({ payload }: ReturnType<typeof schedulerActions.fetchChecklistInfo>) {
  try {
    const { checklistId } = payload;
    const { data, errors }: ResponseObj<any> = yield call(
      request,
      'GET',
      apiGetChecklistInfo(checklistId),
    );

    if (data) {
      yield put(schedulerActions.fetchChecklistInfoSuccess({ data }));
    } else {
      throw getErrorMsg(errors);
    }
  } catch (error) {
    console.error('error from getSingleScheduler function in SchedulerListViewSaga ::  ', error);
    yield put(
      showNotification({
        type: NotificationType.ERROR,
        msg: typeof error !== 'string' ? 'Oops! Please Try Again.' : error,
      }),
    );
  }
}

export function* SchedularListViewSaga() {
  yield takeLatest(SchedulerActionsEnum.saveScheduler, postCreateScheduler);
  yield takeLatest(SchedulerActionsEnum.fetchSchedulers, getSchedulerList);
  yield takeLatest(SchedulerActionsEnum.archiveScheduler, patchArchiveSchedular);
  yield takeLatest(
    SchedulerActionsEnum.fetchSchedulersVersionHistory,
    getSchedulerVersionHisoryList,
  );
  yield takeLatest(SchedulerActionsEnum.modifyScheduler, patchEditScheduler);
  yield takeLatest(SchedulerActionsEnum.fetchChecklistInfo, getChecklistInfo);
}
