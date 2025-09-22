import { showNotification } from '#components/Notification/actions';
import { NotificationType } from '#components/Notification/types';
import { openOverlayAction } from '#components/OverlayContainer/actions';
import { OverlayNames } from '#components/OverlayContainer/types';
import {
  apiBranchingRuleExecute,
  apiCopyEntities,
  apiGetChecklist,
  apiJobLogColumns,
  apiQuickPublishProcess,
  apiValidatePrototype,
} from '#utils/apiUrls';
import { Error, ResponseObj } from '#utils/globalTypes';
import { getErrorMsg, handleCatch, request } from '#utils/request';
import { groupBy } from 'lodash';
import { all, call, fork, put, takeLatest, takeLeading } from 'redux-saga/effects';
import { setValidationError as setParameterValidationError } from './Activity/actions';
import { ParameterSaga } from './Activity/saga';
import { ChecklistAuditLogsSaga } from './ChecklistAuditLogs/saga';
import { setValidationError as setStageValidationError } from './Stages/actions';
import { StageListSaga } from './Stages/saga';
import { setValidationError as setTaskValidationError } from './Tasks/actions';
import { TaskListSaga } from './Tasks/saga';
import {
  copyEntities,
  copyEntitiesSuccess,
  executeBranchingRulesParameter,
  fetchComposerData,
  fetchComposerDataOngoing,
  fetchComposerDataSuccess,
  fetchJobLogColumns,
  fetchJobLogColumnsSuccess,
  quickPublishSuccess,
  setChecklistValidationErrors,
  updateHiddenParameterIds,
  validatePrototype,
} from './actions';
import { ComposerAction } from './reducer.types';
import { ReviewerSaga } from './reviewer.saga';
import { groupErrors } from './utils';

function* fetchComposerDataSaga({ payload }: ReturnType<typeof fetchComposerData>) {
  try {
    const { id, setLoading } = payload;

    if (setLoading) {
      yield put(fetchComposerDataOngoing());
    }

    const { data, errors } = yield call(request, 'GET', apiGetChecklist(id));

    if (data) {
      yield put(fetchComposerDataSuccess({ data }));
    } else {
      console.info('Handle getChecklist API error');
      console.error(errors);
    }
  } catch (error: unknown) {
    console.info('ERROR in fetchComposerDataSaga');
    console.error(error);
  }
}

function* validatePrototypeSaga({ payload }: ReturnType<typeof validatePrototype>) {
  try {
    const { id, isCustomPublish } = payload;
    const { data, errors } = yield call(request, 'GET', apiValidatePrototype(id));

    if ((errors as Array<Error>)?.length) {
      const { stagesErrors, tasksErrors, parametersErrors, otherErrors, errorsWithEntity } =
        groupErrors(errors);
      if (stagesErrors.length) {
        yield all(stagesErrors.map((error) => put(setStageValidationError(error))));
      }

      if (tasksErrors.length) {
        yield all(tasksErrors.map((error) => put(setTaskValidationError(error))));
      }

      if (parametersErrors.length) {
        const errorsById = groupBy(parametersErrors, 'id');
        if (Object.keys(errorsById).length) {
          yield all(
            Object.entries(errorsById).map(([parameterId, errors]) =>
              put(setParameterValidationError(parameterId, errors)),
            ),
          );
        }
      }

      if (errorsWithEntity.length) {
        yield put(setChecklistValidationErrors(errorsWithEntity));
      }

      if (otherErrors.length) {
        yield all(
          otherErrors.map((error) =>
            put(
              showNotification({
                type: NotificationType.ERROR,
                msg: error.message,
              }),
            ),
          ),
        );
      }
    } else if (data && !isCustomPublish) {
      yield put(setChecklistValidationErrors([]));
      yield put(
        openOverlayAction({
          type: OverlayNames.CHECKLIST_REVIEWER_ASSIGNMENT,
          props: {
            checklistId: id,
          },
        }),
      );
    } else if (data && isCustomPublish) {
      yield call(quickpublishsaga, { payload: { id } });
    }
  } catch (error) {
    console.error('error came in apiValidatePrototype :: ', error);
  }
}

function* executeBranchingRulesSaga({
  payload,
}: ReturnType<typeof executeBranchingRulesParameter>) {
  try {
    const { parameterValues, checklistId = undefined } = payload;
    const { data } = yield call(request, 'PATCH', apiBranchingRuleExecute(), {
      data: { parameterValues, checklistId },
    });
    yield put(updateHiddenParameterIds(data));
  } catch (error) {
    console.error('error from executeBranchingRules function in Composer Saga :: ', error);
  }
}

function* copyEntitySaga({ payload }: ReturnType<typeof copyEntities>) {
  try {
    const { checklistId, stageId, taskId, type } = payload;
    const { data, errors }: ResponseObj<any[]> = yield call(
      request,
      'POST',
      apiCopyEntities(checklistId),
      {
        data: payload,
      },
    );

    if (errors) {
      throw getErrorMsg(errors);
    }

    yield put(copyEntitiesSuccess(data, data, stageId, taskId, type));
    yield put(
      showNotification({
        type: NotificationType.SUCCESS,
        msg: `${type.toLocaleLowerCase()} has been successfully duplicated`,
      }),
    );
  } catch (error) {
    yield* handleCatch('Parameter', 'copyEntitySaga', error, true);
  }
}
function* quickpublishsaga({ payload }: { payload: { id: string } }) {
  try {
    const { id } = payload;
    const { data: apiData, errors } = yield call(request, 'POST', apiQuickPublishProcess(id));
    if (apiData) {
      yield put(quickPublishSuccess(apiData));
      yield put(
        showNotification({
          type: NotificationType.SUCCESS,
          msg: 'This process has been auto-published successfully.',
        }),
      );
    }
    if (errors) {
      throw getErrorMsg(errors);
    }
  } catch (error) {
    yield* handleCatch('Prototype', 'quickpublishsaga', error, true);
  }
}

function* fetchJobLogColumnsSaga({ payload }: ReturnType<typeof fetchJobLogColumns>) {
  try {
    const { id } = payload;

    const { data, errors } = yield call(request, 'GET', apiJobLogColumns(id));

    if (data) {
      yield put(fetchJobLogColumnsSuccess({ data }));
    } else {
      throw errors;
    }
  } catch (error: unknown) {
    handleCatch('Prototype', 'fetchJobLogColumnsSaga', error);
  }
}

export function* ComposerSaga() {
  yield takeLeading(ComposerAction.FETCH_COMPOSER_DATA, fetchComposerDataSaga);
  yield takeLeading(ComposerAction.VALIDATE_PROTOTYPE, validatePrototypeSaga);
  yield takeLatest(ComposerAction.EXECUTE_LATEST_BRANCHING_RULES, executeBranchingRulesSaga);
  yield takeLeading(ComposerAction.COPY_ENTITY, copyEntitySaga);
  yield takeLeading(ComposerAction.FETCH_JOB_LOG_COLUMNS, fetchJobLogColumnsSaga);
  yield all([
    fork(StageListSaga),
    fork(TaskListSaga),
    fork(ParameterSaga),
    fork(ReviewerSaga),
    fork(ChecklistAuditLogsSaga),
  ]);
}
