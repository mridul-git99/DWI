import { showNotification } from '#components/Notification/actions';
import { NotificationType } from '#components/Notification/types';
import { closeOverlayAction, openOverlayAction } from '#components/OverlayContainer/actions';
import { OverlayNames } from '#components/OverlayContainer/types';
import { navigate } from '@reach/router';
import {
  apiArchiveChecklist,
  apiArchiveValidate,
  apiCustomViews,
  apiCustomViewsArchive,
  apiExportChecklist,
  apiGetChecklists,
  apiGetJobLogs,
  apiProcessCustomViews,
  apiUnarchiveChecklist,
} from '#utils/apiUrls';
import { Error, ResponseObj } from '#utils/globalTypes';
import { getErrorMsg, handleCatch, request } from '#utils/request';
import { call, put, takeLatest, takeLeading } from 'redux-saga/effects';
import { Checklist } from '../types';
import {
  addCustomView,
  addCustomViewError,
  addCustomViewSuccess,
  archiveChecklist,
  deleteCustomView,
  deleteCustomViewError,
  deleteCustomViewSuccess,
  exportChecklist,
  fetchChecklists,
  fetchChecklistsError,
  fetchChecklistsOngoing,
  fetchChecklistsSuccess,
  fetchProcessLogs,
  fetchProcessLogsError,
  fetchProcessLogsSuccess,
  getCustomViews,
  getCustomViewsError,
  getCustomViewsSuccess,
  handlePublishedArchive,
  saveCustomView,
  saveCustomViewError,
  saveCustomViewSuccess,
  unarchiveChecklist,
  updateList,
} from './actions';
import { ListViewAction } from './types';

function* fetchChecklistsSaga({ payload }: ReturnType<typeof fetchChecklists>) {
  try {
    const { params, enableLoading } = payload;
    if (enableLoading) {
      yield put(fetchChecklistsOngoing());
    }

    const { data, pageable }: ResponseObj<Checklist[]> = yield call(
      request,
      'GET',
      apiGetChecklists(),
      { params },
    );

    yield put(fetchChecklistsSuccess({ data, pageable }));
  } catch (error) {
    console.error('error from fetchChecklist function in ChecklistListViewSaga :: ', error);
    yield put(fetchChecklistsError(error));
  }
}

function* archiveChecklistSaga({ payload }: ReturnType<typeof archiveChecklist>) {
  try {
    const { id, reason, setFormErrors } = payload;

    const { data, errors } = yield call(request, 'PATCH', apiArchiveChecklist(id), {
      data: { reason },
    });

    if (data) {
      yield put(updateList(id));
      yield put(
        openOverlayAction({
          type: OverlayNames.ARCHIVE_MODAL,
          props: { mode: 'archive' },
        }),
      );
      setFormErrors(errors);
    } else {
      const errorModes = {
        E120: 'cannotArchive',
        E145: 'cannotArchiveActiveSchedulers',
      };
      const error = (errors as Error[]).find((error) => errorModes.hasOwnProperty(error.code));
      if (error) {
        setFormErrors(undefined);
        yield put(
          openOverlayAction({
            type: OverlayNames.ARCHIVE_MODAL,
            props: { mode: errorModes[error.code as keyof typeof errorModes] },
          }),
        );
      } else {
        setFormErrors(errors);
        console.error('error from apiArchiveChecklist :: ', errors);
      }
    }
  } catch (error) {
    console.error('error in archiveChecklist saga :: ', error);
  }
}

function* unarchiveChecklistSaga({ payload }: ReturnType<typeof unarchiveChecklist>) {
  try {
    const { id, reason, setFormErrors } = payload;

    const { data, errors } = yield call(request, 'PATCH', apiUnarchiveChecklist(id), {
      data: { reason },
    });

    if (data) {
      yield put(updateList(id));
      yield put(
        openOverlayAction({
          type: OverlayNames.ARCHIVE_MODAL,
          props: { mode: 'unarchive' },
        }),
      );
    } else {
      console.error('error from apiUnarchiveChecklist :: ', errors);
    }

    setFormErrors(errors);
  } catch (error) {
    console.error('error in UnarchiveChecklist saga :: ', error);
  }
}

function* handlePublishedArchiveSaga({ payload }: ReturnType<typeof handlePublishedArchive>) {
  try {
    const { id } = payload;

    const { data } = yield call(request, 'GET', apiArchiveValidate(id));

    if (data) {
      yield put(
        openOverlayAction({
          type: OverlayNames.REASON_MODAL,
          props: {
            modalTitle: 'Archive Process',
            modalDesc: `Provide details for archiving the process`,
            onPrimaryClick: (reason: string, setFormErrors: (errors?: Error[]) => void) => {
              window.store.dispatch(archiveChecklist(id, reason, setFormErrors));
            },
          },
        }),
      );
    } else {
      yield put(
        openOverlayAction({
          type: OverlayNames.ARCHIVE_MODAL,
          props: { mode: 'cannotArchive' },
        }),
      );
    }
  } catch (error) {
    console.error('error came in handlePublishedArchiveSaga :: ', error);
  }
}

function parseJobLogsWorker(data: any) {
  return new Promise((resolve, reject) => {
    const worker = new Worker(new URL('./worker.ts', import.meta.url));

    worker.postMessage(data);

    worker.onmessage = function (e) {
      resolve(e.data);
    };

    worker.onerror = function (e) {
      reject(e.message);
    };
  });
}

function* fetchProcessLogsSaga({ payload }: ReturnType<typeof fetchProcessLogs>) {
  try {
    const { data, pageable } = yield call(request, 'GET', apiGetJobLogs(), {
      params: payload,
    });

    if (data) {
      const result = yield call(parseJobLogsWorker, data);
      yield put(
        fetchProcessLogsSuccess({ data: result, pageable, processName: data[0]?.checklistName }),
      );
    }
  } catch (error) {
    console.error('error from fetchProcessLogsSaga function in Checklist ListView Saga :: ', error);
    yield put(fetchProcessLogsError(error));
  }
}

function* addCustomViewSaga({ payload }: ReturnType<typeof addCustomView>) {
  try {
    const { data, errors } = yield call(
      request,
      'POST',
      payload?.checklistId ? apiProcessCustomViews(payload.checklistId) : apiCustomViews(),
      {
        data: payload.data,
      },
    );

    if (data) {
      yield put(addCustomViewSuccess(data));
      yield put(closeOverlayAction(OverlayNames.PUT_CUSTOM_VIEW));
      yield put(
        showNotification({
          type: NotificationType.SUCCESS,
          msg: `${data.label} view created successfully`,
        }),
      );
      payload.setActiveTab(data);
    } else if (errors) {
      yield* handleCatch('Job Logs Custom View', 'addCustomView', getErrorMsg(errors), true);
    }
  } catch (error) {
    console.error('error from addCustomViewSaga function in Checklist ListView Saga :: ', error);
    yield put(addCustomViewError(error));
  }
}

function* getCustomViewsSaga({ payload }: ReturnType<typeof getCustomViews>) {
  try {
    const { params } = payload;
    const { data } = yield call(request, 'GET', apiCustomViews(), {
      params,
    });

    if (data) {
      yield put(getCustomViewsSuccess(data));
    }
  } catch (error) {
    console.error('error from getCustomViewsSaga function in Checklist ListView Saga :: ', error);
    yield put(getCustomViewsError(error));
  }
}

function* saveCustomViewSaga({ payload }: ReturnType<typeof saveCustomView>) {
  try {
    const { data } = yield call(request, 'PATCH', apiCustomViews(payload.viewId), {
      data: payload.data,
    });

    if (data) {
      yield put(saveCustomViewSuccess(data));
      yield put(closeOverlayAction(OverlayNames.PUT_CUSTOM_VIEW));
      yield put(
        showNotification({
          type: NotificationType.SUCCESS,
          msg: `${data.label} view updated successfully`,
        }),
      );
    }
  } catch (error) {
    console.error('error from saveCustomViewSaga function in Checklist ListView Saga :: ', error);
    yield put(saveCustomViewError(error));
  }
}

function* deleteCustomViewSaga({ payload }: ReturnType<typeof deleteCustomView>) {
  try {
    const { view, tabIndex } = payload;
    const { data } = yield call(request, 'PATCH', apiCustomViewsArchive(view.id));

    if (data) {
      yield put(deleteCustomViewSuccess(view));
      yield put(
        showNotification({
          type: NotificationType.SUCCESS,
          msg: `“${view.label}” deleted successfully!`,
        }),
      );
      navigate(`?tab=${tabIndex}`, {
        replace: true,
      });
    }
  } catch (error) {
    console.error('error from deleteCustomViewSaga function in Checklist ListView Saga :: ', error);
    yield put(deleteCustomViewError(error));
  }
}

function* getExportChecklist({ payload }: ReturnType<typeof exportChecklist>) {
  try {
    const params = { ...payload, ids: payload.checklistId };
    const res = yield call(request, 'GET', apiExportChecklist(), { responseType: 'blob', params });

    const url = window.URL.createObjectURL(new Blob([res], { type: 'application/zip' }));
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', `${params?.ids || 'all'}.zip`);
    document.body.appendChild(link);
    link.click();
    if (res) {
      yield put(
        showNotification({
          type: NotificationType.SUCCESS,
          msg: `Process Exported`,
        }),
      );
    }
  } catch (error) {
    console.error('error from getExportChecklist function in Process ListView Saga :: ', error);
  }
}

export function* ChecklistListViewSaga() {
  yield takeLatest(ListViewAction.FETCH_CHECKLISTS, fetchChecklistsSaga);
  yield takeLatest(ListViewAction.FETCH_CHECKLISTS_FOR_LISTVIEW, fetchChecklistsSaga);
  yield takeLatest(ListViewAction.ARCHIVE, archiveChecklistSaga);
  yield takeLatest(ListViewAction.UNARCHIVE, unarchiveChecklistSaga);
  yield takeLatest(ListViewAction.HANDLE_PUBLISHED_ARCHIVE, handlePublishedArchiveSaga);
  yield takeLatest(ListViewAction.FETCH_PROCESS_LOGS, fetchProcessLogsSaga);
  yield takeLeading(ListViewAction.ADD_CUSTOM_VIEW, addCustomViewSaga);
  yield takeLeading(ListViewAction.GET_CUSTOM_VIEWS, getCustomViewsSaga);
  yield takeLeading(ListViewAction.SAVE_CUSTOM_VIEW, saveCustomViewSaga);
  yield takeLeading(ListViewAction.DELETE_CUSTOM_VIEW, deleteCustomViewSaga);
  yield takeLatest(ListViewAction.EXPORT_CHECKLIST, getExportChecklist);
}
