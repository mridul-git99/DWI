import { resetChecklistValidationErrors } from '#PrototypeComposer/actions';
import { showNotification } from '#components/Notification/actions';
import { NotificationType } from '#components/Notification/types';
import { MandatoryParameter, NonMandatoryParameter } from '#types';
import {
  apiAddNewParameter,
  apiGetParameters,
  apiSingleParameter,
  apiUnmapParameter,
} from '#utils/apiUrls';
import { ResponseObj } from '#utils/globalTypes';
import { getErrorMsg, handleCatch, request } from '#utils/request';
import { call, put, takeLatest, takeLeading } from 'redux-saga/effects';
import {
  addNewParameter,
  addNewParameterSuccess,
  deleteParameter,
  deleteParameterError,
  deleteParameterSuccess,
  fetchParameters,
  fetchParametersError,
  fetchParametersSuccess,
  toggleAddParameterRender,
  toggleNewParameter,
  updateParameterApi,
  updateParameterError,
  updateStoreParameter,
} from './actions';
import { ParameterListActions } from './reducer.types';

function* updateParameterSaga({ payload }: ReturnType<typeof updateParameterApi>) {
  try {
    const { setParameterSubmitting, ...parameter } = payload;
    const { data, errors } = yield call(request, 'PATCH', apiSingleParameter(parameter.id), {
      data: { ...parameter },
    });

    if (data) {
      yield put(updateStoreParameter(data, parameter.id));
      yield put(toggleNewParameter());
      yield put(
        showNotification({
          type: NotificationType.SUCCESS,
          msg: [NonMandatoryParameter.INSTRUCTION, NonMandatoryParameter.MATERIAL].includes(
            parameter?.type,
          )
            ? 'Instruction Updated Successfully'
            : parameter.type === MandatoryParameter.CHECKLIST
            ? 'Subtask Updated Successfully'
            : 'Parameter Updated Successfully',
          detail: parameter.label,
        }),
      );
    }
    if (setParameterSubmitting) setParameterSubmitting(false);
    if (errors) {
      yield put(updateParameterError(errors));
      throw getErrorMsg(errors);
    }
  } catch (error) {
    yield* handleCatch('Parameter', 'updateParameterSaga', error, true);
  }
}

function* addNewParameterSaga({ payload }: ReturnType<typeof addNewParameter>) {
  try {
    const { checklistId, stageId, setParameterSubmitting, taskId, ...parameter } = payload;

    const { data, errors } = yield call(
      request,
      'POST',
      apiAddNewParameter({ checklistId, stageId, taskId }),
      { data: parameter },
    );

    if (data) {
      if (stageId && taskId) {
        yield put(addNewParameterSuccess({ parameter: data, stageId, taskId }));
        //E211 = 'TASK_SHOULD_HAVE_ATLEAST_ONE_EXECUTABLE_PARAMETER',
        yield put(resetChecklistValidationErrors(taskId, 'E211'));
      } else {
        yield put(toggleAddParameterRender({ shouldReRender: true }));
      }
      yield put(toggleNewParameter());
      yield put(
        showNotification({
          type: NotificationType.SUCCESS,
          msg: [NonMandatoryParameter.INSTRUCTION, NonMandatoryParameter.MATERIAL].includes(
            parameter?.type,
          )
            ? 'New Instruction Created'
            : parameter.type === MandatoryParameter.CHECKLIST
            ? 'New Subtask Created'
            : 'New Parameter Created',
          detail: parameter.label,
        }),
      );
    } else {
      yield* handleCatch('Parameter', 'executeParameterSaga', getErrorMsg(errors), true);
    }
    setParameterSubmitting(false);
  } catch (errors) {
    yield* handleCatch('Parameter', 'executeParameterSaga', errors);
  }
}

function* deleteParameterSaga({ payload }: ReturnType<typeof deleteParameter>) {
  try {
    const { data, errors } = yield call(request, 'PATCH', apiUnmapParameter(payload.parameterId));

    if (data) {
      yield put(deleteParameterSuccess(payload));
    } else {
      yield put(deleteParameterError(errors));
      throw getErrorMsg(errors);
    }
  } catch (error) {
    yield* handleCatch('Parameter', 'deleteParameterSaga', error, true);
  }
}

function* fetchParametersSaga({ payload }: ReturnType<typeof fetchParameters>) {
  try {
    const { params, checklistId } = payload;
    const { data, pageable }: ResponseObj<any[]> = yield call(
      request,
      'GET',
      apiGetParameters(checklistId),
      {
        params,
      },
    );

    if (data) {
      yield put(fetchParametersSuccess({ data, pageable }));
    }
  } catch (error) {
    console.error('error from fetchObjectTypesSaga function in Ontology Saga :: ', error);
    yield put(fetchParametersError(error));
  }
}

export function* ParameterSaga() {
  yield takeLeading(ParameterListActions.ADD_NEW_PARAMETER, addNewParameterSaga);
  yield takeLatest(ParameterListActions.UPDATE_PARAMETER_API, updateParameterSaga);
  yield takeLeading(ParameterListActions.DELETE_PARAMETER, deleteParameterSaga);
  yield takeLatest(ParameterListActions.FETCH_PARAMETERS, fetchParametersSaga);
}
