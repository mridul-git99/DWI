import React from 'react';
import { showNotification } from '#components/Notification/actions';
import { NotificationType } from '#components/Notification/types';
import { closeOverlayAction } from '#components/OverlayContainer/actions';
import { OverlayNames } from '#components/OverlayContainer/types';
import { apiGetJobs, apiGetSelectedJob, apiDownloadJobs } from '#utils/apiUrls';
import { ResponseObj } from '#utils/globalTypes';
import { getErrorMsg, handleCatch, request } from '#utils/request';
import { call, put, takeLatest } from 'redux-saga/effects';
import {
  createJob,
  createJobError,
  createJobSuccess,
  fetchJobs,
  fetchJobsError,
  fetchJobsOngoing,
  fetchJobsSuccess,
  fetchJobsExcel,
  fetchJobsExcelError,
  updateJob,
  updateJobSuccess,
} from './actions';
import { Job, ListViewAction } from './types';
import { navigate } from '@reach/router';
import { isFeatureAllowed } from '#services/uiPermissions';

function* fetchJobsSaga({ payload }: ReturnType<typeof fetchJobs>) {
  try {
    const { params } = payload;

    yield put(fetchJobsOngoing());

    const { data, pageable } = yield call(request, 'GET', apiGetJobs(), {
      params,
    });

    if (data) {
      yield put(fetchJobsSuccess({ data, pageable }));
    }
  } catch (error) {
    console.error('error from fetchJobsSaga function in JobListView Saga :: ', error);
    yield put(fetchJobsError(error));
  }
}

function* createJobSaga({ payload }: ReturnType<typeof createJob>) {
  try {
    const { errors, data }: ResponseObj<Job> = yield call(request, 'POST', apiGetJobs(), {
      data: {
        parameterValues: payload.parameterValues,
        selectedUseCaseId: payload.selectedUseCaseId,
        checklistId: payload.checklistId,
      },
      params: {
        validateUserRole: payload.validateUserRole,
      },
    });
    if (errors) {
      throw getErrorMsg(errors);
    }

    // NOTE: we are intentionally trying to navigate to inbox page rather than job. (even if there's no user assigned)
    yield put(
      showNotification({
        type: NotificationType.SUCCESS,
        autoClose: 15000,
        msg: (
          <div>
            <a className="clickable" onClick={() => navigate(`/inbox/${data.id}`)}>
              {data.code}
            </a>{' '}
            created successfully
          </div>
        ),
      }),
    );

    if (isFeatureAllowed('redirectToJobAfterCreation')) navigate(`/inbox/${data.id}`);

    yield put(
      createJobSuccess({
        parameterValues: payload.parameterValues,
        selectedUseCaseId: payload.selectedUseCaseId,
        checklistId: payload.checklistId,
        id: data.id,
      }),
    );
  } catch (e) {
    yield put(createJobError());
    yield* handleCatch('JobListView', 'createJobSaga', e, true);
  }
}

function* updateJobSaga({ payload }: ReturnType<typeof updateJob>) {
  try {
    const { job } = payload;
    const { errors, data }: ResponseObj<Job> = yield call(
      request,
      'PATCH',
      apiGetSelectedJob(job.id!),
      {
        data: job,
      },
    );
    if (errors) {
      throw getErrorMsg(errors);
    }

    yield put(closeOverlayAction(OverlayNames.SET_DATE));
    yield put(
      showNotification({
        type: NotificationType.SUCCESS,
        msg: 'Job Updated Successfully.',
      }),
    ),
      yield put(updateJobSuccess(data));
  } catch (e) {
    yield put(createJobError());
    yield* handleCatch('JobListView', 'updateJobSaga', e, true);
  }
}

function* fetchJobsExcelSaga({ payload }: ReturnType<typeof fetchJobsExcel>) {
  try {
    const { params } = payload;

    yield put(
      showNotification({
        type: NotificationType.WARNING,
        msg: 'Generating Excel file... This may take a few moments.',
      }),
    );

    const res = yield call(request, 'GET', apiDownloadJobs(), {
      params: { filters: params.filters, objectId: params.objectId },
      responseType: 'blob',
    });

    if (res && res.size > 0) {
      const url = window.URL.createObjectURL(new Blob([res]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', params.filename);
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);

      yield put(
        showNotification({
          type: NotificationType.SUCCESS,
          msg: 'Excel file downloaded successfully.',
        }),
      );
    } else {
      throw new Error('No data received from server or empty response');
    }
  } catch (error) {
    console.error('error from fetchJobsExcel function in JobsSaga :: ', error);
    yield put(fetchJobsExcelError(error));
    yield* handleCatch('JobListView', 'fetchJobsExcelSaga', error, true);
  }
}

export function* NewJobListViewSaga() {
  yield takeLatest(ListViewAction.CREATE_JOB, createJobSaga);
  yield takeLatest(ListViewAction.FETCH_JOBS, fetchJobsSaga);
  yield takeLatest(ListViewAction.UPDATE_JOB, updateJobSaga);
  yield takeLatest(ListViewAction.FETCH_JOBS_EXCEL, fetchJobsExcelSaga);
}
