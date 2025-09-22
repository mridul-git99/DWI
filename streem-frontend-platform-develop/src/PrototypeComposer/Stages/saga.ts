import { resetChecklistValidationErrors } from '#PrototypeComposer/actions';
import { setValidationError as setParameterValidationError } from '#PrototypeComposer/Activity/actions';
import { groupErrors } from '#PrototypeComposer/utils';
import { RootState } from '#store/types';
import { Stage } from '#types';
import { apiCreateStage, apiDeleteStage } from '#utils/apiUrls';
import { findTaskAndStage } from '#utils/parameterUtils';
import { getErrorMsg, handleCatch, request } from '#utils/request';
import { navigate } from '@reach/router';
import { groupBy } from 'lodash';
import { all, call, put, select, takeEvery, takeLeading } from 'redux-saga/effects';
import { apiReorderStages, apiUpdateStage } from '../../utils/apiUrls';
import {
  addNewStageError,
  addNewStageSuccess,
  deleteStage,
  deleteStageSuccess,
  reOrderStage,
  reOrderStageError,
  reOrderStageSuccess,
  updateStageName,
  updateStageNameError,
  updateStageNameSuccess,
} from './actions';
import { StageListActions } from './reducer.types';

function* addNewStageSaga() {
  try {
    const {
      stages: { listOrder, listById },
      data: { id: checklistId },
    } = yield select((state: RootState) => state.prototypeComposer);

    const newStage: Pick<Stage, 'name' | 'orderTree'> = {
      name: '',
      orderTree: listOrder.length ? listById[listOrder[listOrder.length - 1]].orderTree + 1 : 1,
    };

    const { data, errors } = yield call(request, 'POST', apiCreateStage(checklistId), {
      data: { ...newStage },
    });

    if (data) {
      yield put(addNewStageSuccess(data));
    } else {
      yield put(addNewStageError(errors));
    }
  } catch (error) {
    console.error('error came in addNewStageSaga :: ', error);
  }
}

function* deleteStageSaga({ payload }: ReturnType<typeof deleteStage>) {
  try {
    const { id } = payload;

    const { data, errors } = yield call(request, 'PATCH', apiDeleteStage(id));

    if (data) {
      const { listById, listOrder }: RootState['prototypeComposer']['stages'] = yield select(
        (state: RootState) => state.prototypeComposer.stages,
      );

      const deletedStageIndex = listOrder.indexOf(id);
      const stagesToReorder = listOrder.slice(deletedStageIndex + 1);

      if (stagesToReorder.length) {
        const reOrderMap = stagesToReorder.reduce<Record<string, number>>((acc, stageId) => {
          acc[stageId] = listById[stageId].orderTree - 1;
          return acc;
        }, {});

        const { data: reorderData, errors: reorderErrors } = yield call(
          request,
          'PATCH',
          apiReorderStages(),
          { data: { stagesOrder: reOrderMap } },
        );

        if (reorderData) {
          yield put(deleteStageSuccess({ id, newOrderMap: reOrderMap }));
        } else {
          throw new Error(reorderErrors);
        }
      } else {
        yield put(deleteStageSuccess({ id }));
      }
    } else {
      if ((errors as Array<Error>)?.length) {
        const { parameterOrderInTaskInStage }: RootState['prototypeComposer']['parameters'] =
          yield select((state: RootState) => state.prototypeComposer.parameters);
        const { parametersErrors } = groupErrors(errors);

        if (parametersErrors.length) {
          const parameterId = parametersErrors[0].id;
          const {
            stageId,
            taskId,
            taskParameters = [],
          } = findTaskAndStage(parameterOrderInTaskInStage, parameterId);

          if (taskParameters.length) {
            yield all(
              taskParameters.map((parameterId) =>
                put(setParameterValidationError(parameterId, [])),
              ),
            );
          }

          if (stageId && taskId) {
            navigate(`?stageId=${stageId}&taskId=${taskId}`, { replace: true });
          }

          const errorsById = groupBy(parametersErrors, 'id');
          if (Object.keys(errorsById).length) {
            yield all(
              Object.entries(errorsById).map(([parameterId, errors]) =>
                put(setParameterValidationError(parameterId, errors)),
              ),
            );
          }
        }
        throw getErrorMsg(errors);
      }
    }
  } catch (error) {
    yield* handleCatch('Stage', 'deleteStageSaga', error, true);
  }
}

function* reOrderStageSaga({ payload }: ReturnType<typeof reOrderStage>) {
  try {
    const listOrder: Stage['id'][] = yield select(
      (state: RootState) => state.prototypeComposer.stages.listOrder,
    );
    const toStageId = listOrder[payload.to];
    const { data: reorderData, errors: reorderErrors } = yield call(
      request,
      'PATCH',
      apiReorderStages(),
      {
        data: {
          stagesOrder: { [toStageId]: payload.from + 1, [payload.id]: payload.to + 1 },
        },
      },
    );
    if (reorderData) {
      yield put(reOrderStageSuccess(payload));
    } else {
      yield put(reOrderStageError(reorderErrors));
    }
  } catch (error) {
    console.error('error came in reOrderStageSaga :: ', error);
  }
}

function* updateStageNameSaga({ payload }: ReturnType<typeof updateStageName>) {
  try {
    const { id, name, orderTree } = payload.stage;

    const { data, errors } = yield call(request, 'PATCH', apiUpdateStage(id), {
      data: { name, orderTree },
    });

    if (data) {
      yield put(updateStageNameSuccess(data));
      //E303 = 'STAGE_NAME_CANNOT_BE_EMPTY',
      yield put(resetChecklistValidationErrors(id, 'E303'));
    } else {
      yield put(updateStageNameError(errors));
    }
  } catch (error) {
    console.error('error came in updateStageNameSaga :: ', error);
  }
}

export function* StageListSaga() {
  yield takeLeading(StageListActions.ADD_NEW_STAGE, addNewStageSaga);
  yield takeEvery(StageListActions.DELETE_STAGE, deleteStageSaga);
  // yield takeLeading(StageListActions.DUPLICATE_STAGE, duplicateStageSaga);
  // TODO: when enabling this reorder saga, connect with BE to make sure the API works as per the need
  yield takeLeading(StageListActions.REORDER_STAGE, reOrderStageSaga);
  yield takeEvery(StageListActions.UPDATE_STAGE_NAME, updateStageNameSaga);
}
