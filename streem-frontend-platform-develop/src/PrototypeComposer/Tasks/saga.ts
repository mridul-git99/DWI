import { closeOverlayAction } from '#components/OverlayContainer/actions';
import { OverlayNames } from '#components/OverlayContainer/types';
import { RootState } from '#store';
import {
  apiAddMediaToTask,
  apiAddStop,
  apiAddTaskAction,
  apiCreateTask,
  apiDeleteTask,
  apiRemoveStop,
  apiRemoveTaskMedia,
  apiRemoveTaskTimer,
  apiReOrderParameters,
  apiReorderTasks,
  apiSetTaskTimer,
  apiUpdateTask,
  apiUpdateTaskAction,
  apiUpdateTaskMedia,
  apiSetTaskRecurrence,
  apiRemoveTaskRecurrence,
  apiTaskSchedule,
  apiRemoveTaskSchedule,
  apiAddSoloTaskLock,
  apiRemoveSoloTaskLock,
  apiUpsertBulkVerification,
} from '#utils/apiUrls';
import { getErrorMsg, handleCatch, request } from '#utils/request';
import { call, put, select, takeEvery, takeLatest, takeLeading, all } from 'redux-saga/effects';
import { showNotification } from '#components/Notification/actions';
import { NotificationType } from '#components/Notification/types';
import {
  addNewTask,
  addNewTaskSuccess,
  addStop,
  addTaskAction,
  addTaskMedia,
  archiveTaskAction,
  deleteTask,
  deleteTaskSuccess,
  removeTaskMedia,
  removeTaskRecurrence,
  removeTaskTimer,
  reOrderParameters,
  reOrderTask,
  reOrderTaskError,
  reOrderTaskSuccess,
  setTaskError,
  setTaskRecurrence,
  setTaskTimer,
  setValidationError,
  updateTask,
  updateTaskAction,
  updateTaskMedia,
  updateTaskMediaSuccess,
  updateTaskName,
  setTaskSchedule,
  addSoloTaskLock,
  removeSoloTaskLock,
  addBulkVerification,
  removeBulkVerification,
} from './actions';
import { TaskListActions } from './reducer.types';
import { resetChecklistValidationErrors } from '#PrototypeComposer/actions';
import { setValidationError as setParameterValidationError } from '#PrototypeComposer/Activity/actions';
import { groupErrors } from '#PrototypeComposer/utils';
import { groupBy } from 'lodash';
import { findTaskAndStage } from '#utils/parameterUtils';
import { navigate } from '@reach/router';
import { updatedActions } from './utils';
import { AutomationActionConfigurationDataType } from '#PrototypeComposer/checklist.types';

function* addNewTaskSaga({ payload }: ReturnType<typeof addNewTask>) {
  try {
    const { checklistId, stageId, orderTree, type = 'STATIC', data, name = '' } = payload;
    let newTaskOrderTree = orderTree;

    if (!newTaskOrderTree) {
      const {
        tasks: { tasksOrderInStage, listById },
      } = yield select((state: RootState) => state.prototypeComposer);
      const activeStageTasksOrder = tasksOrderInStage[stageId];
      newTaskOrderTree = activeStageTasksOrder.length
        ? listById[activeStageTasksOrder[activeStageTasksOrder.length - 1]].orderTree + 1
        : 1;
    }

    const { data: _data, errors } = yield call(
      request,
      'POST',
      apiCreateTask({ checklistId, stageId }),
      {
        data: {
          name,
          orderTree: newTaskOrderTree,
          type,
          data,
        },
      },
    );

    if (_data) {
      yield put(addNewTaskSuccess({ ..._data }, stageId));
      // E128 = 'STAGE_MUST_CONTAIN_ATLEAST_ONE_TASK',
      yield put(resetChecklistValidationErrors(stageId, 'E128'));
    } else {
      yield put(setTaskError(errors));
    }
  } catch (error) {
    console.error('error came in addNewTaskSaga :: ', error);
  }
}

function* deleteTaskSaga({ payload }: ReturnType<typeof deleteTask>) {
  try {
    const activeStageId: RootState['prototypeComposer']['stages']['activeStageId'] = yield select(
      (state: RootState) => state.prototypeComposer.stages.activeStageId,
    );

    const { data, errors } = yield call(request, 'PATCH', apiDeleteTask(payload.taskId));

    if (data && activeStageId) {
      const {
        tasks: { listById, tasksOrderInStage },
        data: processData,
      }: RootState['prototypeComposer'] = yield select(
        (state: RootState) => state.prototypeComposer,
      );

      const deletedTaskIndex = tasksOrderInStage[activeStageId].indexOf(payload.taskId);
      const tasksToReorder = tasksOrderInStage[activeStageId].slice(deletedTaskIndex + 1);

      if (tasksToReorder.length) {
        const reOrderMap = tasksToReorder.reduce<Record<string, number>>((acc, taskId) => {
          acc[taskId] = listById[taskId].orderTree - 1;
          return acc;
        }, {});

        const { data: reorderData, errors: reorderErrors } = yield call(
          request,
          'PATCH',
          apiReorderTasks(),
          { data: { tasksOrder: reOrderMap, checklistId: processData?.id } },
        );

        if (reorderData) {
          yield put(
            deleteTaskSuccess({
              taskId: payload.taskId,
              stageId: activeStageId,
              newOrderMap: reOrderMap,
            }),
          );
        } else {
          throw new Error(reorderErrors);
        }
      } else {
        yield put(deleteTaskSuccess({ taskId: payload.taskId, stageId: activeStageId }));
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
    yield* handleCatch('Task', 'deleteTaskSaga', error, true);
  }
}

function* reOrderTaskSaga({ payload }: ReturnType<typeof reOrderTask>) {
  const { to: toIndex, id: fromTaskId, from: fromIndex, activeStageId } = payload;
  const {
    tasks: { tasksOrderInStage },
    data: processData,
  }: RootState['prototypeComposer'] = yield select((state: RootState) => state.prototypeComposer);

  const toTaskId = tasksOrderInStage[activeStageId][toIndex];

  try {
    const { data: reorderData, errors: reorderErrors } = yield call(
      request,
      'PATCH',
      apiReorderTasks(),
      {
        data: {
          tasksOrder: { [toTaskId]: fromIndex + 1, [fromTaskId]: toIndex + 1 },
          checklistId: processData?.id,
        },
      },
    );
    if (reorderData) {
      yield put(reOrderTaskSuccess(payload));
    } else {
      yield put(reOrderTaskError(reorderErrors));
    }
  } catch (error) {
    console.error('error came in reOrderTaskSaga :: ', error);
  }
}

function* addStopSaga({ payload }: ReturnType<typeof addStop>) {
  try {
    const { taskId } = payload;

    const { data, errors } = yield call(request, 'PATCH', apiAddStop(taskId));
    if (data) {
      yield put(updateTask(data));
    } else {
      yield put(setTaskError(errors));
    }
  } catch (error) {
    console.error('error came add stop saga :: ', error);
  }
}

function* removeStopSaga({ payload }: ReturnType<typeof addStop>) {
  try {
    const { taskId } = payload;

    const { data, errors } = yield call(request, 'PATCH', apiRemoveStop(taskId));
    if (data) {
      yield put(updateTask(data));
    } else {
      yield put(setTaskError(errors));
    }
  } catch (error) {
    console.error('error came add stop saga :: ', error);
  }
}

function* updateTaskSaga({ payload }: ReturnType<typeof updateTaskName>) {
  try {
    const { task } = payload;

    const { data, errors } = yield call(request, 'PATCH', apiUpdateTask(task.id), {
      data: { ...task },
    });

    if (data) {
      yield put(updateTask(data));
      // E210 = 'TASK_NAME_CANNOT_BE_EMPTY',
      yield put(resetChecklistValidationErrors(task.id, 'E210'));
    } else {
      yield put(setTaskError(errors));
    }
  } catch (error) {
    console.error('error came in updateTaskSaga :: ', error);
  }
}

function* setTaskTimerSaga({ payload }: ReturnType<typeof setTaskTimer>) {
  try {
    const { maxPeriod, minPeriod, taskId, timerOperator } = payload;

    const { data, errors } = yield call(request, 'PATCH', apiSetTaskTimer(taskId), {
      data: { maxPeriod, minPeriod, timerOperator },
    });

    if (data) {
      yield put(updateTask(data));
      yield put(
        showNotification({
          type: NotificationType.SUCCESS,
          msg: 'Timed Condition is configured successfully!',
        }),
      );
    } else {
      yield put(setValidationError(errors[0]));
    }
  } catch (error) {
    console.error('Error came in setTaskTimerSaga :: ', error);
  }
}

function* removeTaskTimerSaga({ payload }: ReturnType<typeof removeTaskTimer>) {
  try {
    const { taskId } = payload;

    const { data, errors } = yield call(request, 'PATCH', apiRemoveTaskTimer(taskId));

    if (data) {
      yield put(closeOverlayAction(OverlayNames.TIMED_TASK_CONFIG));
      yield put(updateTask(data));
      yield put(
        showNotification({
          type: NotificationType.SUCCESS,
          msg: 'Timed Condition removed successfully!',
        }),
      );
    } else {
      yield put(setTaskError(errors));
    }
  } catch (error) {
    console.error('error came in removeTaskTimerSaga :: ', error);
  }
}

function* setTaskRecurrenceSaga({ payload }: ReturnType<typeof setTaskRecurrence>) {
  try {
    const {
      updating,
      taskId,
      startDateDuration,
      startDateInterval,
      dueDateDuration,
      dueDateInterval,
      positiveStartDateToleranceDuration,
      negativeStartDateToleranceDuration,
      positiveDueDateToleranceDuration,
      negativeDueDateToleranceDuration,
      positiveStartDateToleranceInterval,
      negativeStartDateToleranceInterval,
      positiveDueDateToleranceInterval,
      negativeDueDateToleranceInterval,
    } = payload;

    const { data, errors } = yield call(request, 'PATCH', apiSetTaskRecurrence(taskId), {
      data: {
        startDateDuration,
        startDateInterval,
        dueDateDuration,
        dueDateInterval,
        positiveStartDateToleranceDuration,
        negativeStartDateToleranceDuration,
        positiveDueDateToleranceDuration,
        negativeDueDateToleranceDuration,
        positiveStartDateToleranceInterval,
        negativeStartDateToleranceInterval,
        positiveDueDateToleranceInterval,
        negativeDueDateToleranceInterval,
      },
    });

    if (data) {
      yield put(updateTask(data));
      yield put(closeOverlayAction(OverlayNames.TASK_RECURRENCE_MODAL));
      yield put(
        showNotification({
          type: NotificationType.SUCCESS,
          msg: updating ? 'Task recurrence is updated' : 'Task recurrence is enabled',
        }),
      );
    } else if (errors) {
      throw getErrorMsg(errors);
    }
  } catch (error) {
    yield* handleCatch('Prototype Composer', 'setTaskRecurrenceSaga', error, true);
  }
}

function* removeTaskRecurrenceSaga({ payload }: ReturnType<typeof removeTaskRecurrence>) {
  try {
    const { taskId } = payload;

    const { data, errors } = yield call(request, 'PATCH', apiRemoveTaskRecurrence(taskId));

    if (data) {
      yield put(updateTask(data));
      yield put(closeOverlayAction(OverlayNames.TASK_RECURRENCE_MODAL));
      yield put(
        showNotification({
          type: NotificationType.SUCCESS,
          msg: 'Task recurrence is removed',
        }),
      );
    } else if (errors) {
      throw getErrorMsg(errors);
    }
  } catch (error) {
    yield* handleCatch('Prototype Composer', 'removeTaskRecurrenceSaga', error, true);
  }
}

function* setTaskScheduleSaga({ payload }: ReturnType<typeof setTaskSchedule>) {
  try {
    const {
      taskId,
      type,
      condition,
      scheduledTaskId,
      referencedTaskId,
      startDateDuration,
      startDateInterval,
      dueDateDuration,
      dueDateInterval,
    } = payload;

    const { data, errors } = yield call(request, 'PATCH', apiTaskSchedule(taskId), {
      data: {
        type,
        condition,
        scheduledTaskId,
        referencedTaskId,
        startDateDuration,
        startDateInterval,
        dueDateDuration,
        dueDateInterval,
      },
    });

    if (data) {
      yield put(updateTask(data));
      yield put(closeOverlayAction(OverlayNames.SCHEDULE_TASK_MODAL));
      yield put(
        showNotification({
          type: NotificationType.SUCCESS,
          msg: 'Task is scheduled successfully!',
        }),
      );
    } else if (errors) {
      throw getErrorMsg(errors);
    }
  } catch (error) {
    yield* handleCatch('Prototype Composer', 'setTaskScheduleSaga', error, true);
  }
}

function* removeTaskScheduleSaga({ payload }: ReturnType<typeof removeTaskRecurrence>) {
  try {
    const { taskId } = payload;

    const { data, errors } = yield call(request, 'PATCH', apiRemoveTaskSchedule(taskId));

    if (data) {
      yield put(updateTask(data));
      yield put(closeOverlayAction(OverlayNames.SCHEDULE_TASK_MODAL));
      yield put(
        showNotification({
          type: NotificationType.SUCCESS,
          msg: 'Task schedule is removed',
        }),
      );
    } else if (errors) {
      throw getErrorMsg(errors);
    }
  } catch (error) {
    yield* handleCatch('Prototype Composer', 'removeTaskScheduleSaga', error, true);
  }
}

function* addTaskMediaSaga({ payload }: ReturnType<typeof addTaskMedia>) {
  try {
    const { mediaDetails, taskId } = payload;

    const { data, errors } = yield call(request, 'POST', apiAddMediaToTask(taskId), {
      data: { ...mediaDetails },
    });

    if (data) {
      yield put(updateTask(data));
      yield put(closeOverlayAction(OverlayNames.TASK_MEDIA));
    } else {
      console.error('error from add media to task api :: ', errors);
    }
  } catch (error) {
    console.error('error came in addTaskMediaSaga :: ', error);
  }
}

function* updateTaskMediaSaga({ payload }: ReturnType<typeof updateTaskMedia>) {
  try {
    const { mediaDetails, taskId, parameterId, mediaId } = payload;

    const { data, errors } = yield call(request, 'PATCH', apiUpdateTaskMedia(mediaId), {
      data: { ...mediaDetails },
    });

    //TODO carve out media related logic separately and remove dependency from task media
    if (data && taskId) {
      yield put(closeOverlayAction(OverlayNames.TASK_MEDIA));
      yield put(updateTaskMediaSuccess({ media: data, taskId }));
    } else if (data && parameterId) {
      yield put(closeOverlayAction(OverlayNames.TASK_MEDIA));
    } else {
      console.error('error from update media to task api :: ', errors);
    }
  } catch (error) {
    console.error('error came in updateTaskMediaSaga :: ', error);
  }
}

function* removeTaskMediaSaga({ payload }: ReturnType<typeof removeTaskMedia>) {
  try {
    const { taskId, mediaId } = payload;

    const { data, errors } = yield call(request, 'DELETE', apiRemoveTaskMedia({ taskId, mediaId }));

    if (data) {
      yield put(updateTask(data));
      yield put(closeOverlayAction(OverlayNames.TASK_MEDIA));
    } else {
      console.error('error from remove media to task api :: ', errors);
    }
  } catch (error) {
    console.error('error came in removeTaskMediaSaga :: ', error);
  }
}

function* updateTaskActionSaga({ payload }: ReturnType<typeof updateTaskAction>) {
  try {
    const { taskId, action, actionId } = payload;
    const { data, errors } = yield call(request, 'PATCH', apiUpdateTaskAction(taskId, actionId), {
      data: updatedActions(action),
    });

    if (data) {
      yield put(updateTask(data));
      yield put(
        showNotification({
          type: NotificationType.SUCCESS,
          msg: 'Action updated successfully!',
        }),
      );
    } else if (errors) {
      throw getErrorMsg(errors);
    }
  } catch (error) {
    yield* handleCatch('Configure Actions', 'updateTaskActionSaga', error, true);
  }
}

function* archiveTaskActionSaga({ payload }: ReturnType<typeof archiveTaskAction>) {
  try {
    const { taskId, actionId, setFormErrors } = payload;
    const { data, errors } = yield call(request, 'DELETE', apiUpdateTaskAction(taskId, actionId));

    if (data) {
      yield put(updateTask(data));
      yield put(
        showNotification({
          type: NotificationType.SUCCESS,
          msg: 'Action Deleted',
        }),
      );
      setFormErrors(errors);
    } else if (errors) {
      throw getErrorMsg(errors);
    }
  } catch (error) {
    yield* handleCatch('Configure Actions', 'archiveTaskActionSaga', error, true);
  }
}

function* reOrderParametersSaga({ payload }: ReturnType<typeof reOrderParameters>) {
  try {
    const { checklistId, taskId, stageId, orderedIds } = payload;
    const { errors } = yield call(
      request,
      'PATCH',
      apiReOrderParameters(checklistId, taskId, stageId),
      {
        data: {
          parametersOrder: orderedIds.reduce<Record<string, number>>((acc, id, index) => {
            acc[id] = index + 1;
            return acc;
          }, {}),
        },
      },
    );

    if (errors) {
      console.error('error from reorder parameters from task api :: ', errors);
    }
  } catch (error) {
    console.error('error came in reOrderParametersSaga :: ', error);
  }
}

function* addTaskActionSaga({ payload }: ReturnType<typeof addTaskAction>) {
  try {
    const { action, taskId } = payload;
    const { data, errors } = yield call(request, 'POST', apiAddTaskAction(taskId), {
      data: updatedActions(action, AutomationActionConfigurationDataType.OBJECT),
    });

    if (data) {
      yield put(updateTask(data));
      yield put(
        showNotification({
          type: NotificationType.SUCCESS,
          msg: 'Action added successfully!',
        }),
      );
    } else if (errors) {
      throw getErrorMsg(errors);
    }
  } catch (error) {
    console.error('error came in addTaskActionSaga :: ', error);
    yield* handleCatch('Configure Actions', 'addTaskActionSaga', error, true);
  }
}

function* addSoloTaskLockSaga({ payload }: ReturnType<typeof addSoloTaskLock>) {
  try {
    const { taskId } = payload;

    const { data, errors } = yield call(request, 'PATCH', apiAddSoloTaskLock(taskId));
    if (data) {
      yield put(updateTask(data));
      yield put(
        showNotification({
          type: NotificationType.SUCCESS,
          msg: 'Solo Task Lock added successfully!',
        }),
      );
    } else {
      yield put(setTaskError(errors));
    }
  } catch (error) {
    console.error('error came add solo task lock saga :: ', error);
  }
}

function* removeSoloTaskLockSaga({ payload }: ReturnType<typeof removeSoloTaskLock>) {
  try {
    const { taskId } = payload;

    const { data, errors } = yield call(request, 'PATCH', apiRemoveSoloTaskLock(taskId));
    if (data) {
      yield put(updateTask(data));
      yield put(
        showNotification({
          type: NotificationType.SUCCESS,
          msg: 'Solo Task Lock removed successfully!',
        }),
      );
    } else {
      yield put(setTaskError(errors));
    }
  } catch (error) {
    console.error('error came remove solo task lock saga :: ', error);
  }
}

function* addBulkVerificationSaga({ payload }: ReturnType<typeof addBulkVerification>) {
  try {
    const { taskId } = payload;

    const { data, errors } = yield call(request, 'PATCH', apiUpsertBulkVerification(taskId, 'set'));

    if (data) {
      yield put(updateTask(data));
      yield put(
        showNotification({
          type: NotificationType.SUCCESS,
          msg: 'Bulk Verification added successfully!',
        }),
      );
    } else {
      yield put(setTaskError(errors));
    }
  } catch (error) {
    yield* handleCatch('Task', 'addBulkVerificationSaga', error, false);
  }
}

function* removeBulkVerificationSaga({ payload }: ReturnType<typeof removeBulkVerification>) {
  try {
    const { taskId } = payload;

    const { data, errors } = yield call(
      request,
      'PATCH',
      apiUpsertBulkVerification(taskId, 'unset'),
    );
    if (data) {
      yield put(updateTask(data));
      yield put(
        showNotification({
          type: NotificationType.SUCCESS,
          msg: 'Bulk Verification removed successfully!',
        }),
      );
      // Note - E246 = 'TASK_BULK_VERIFICATION_INVALID_MAPPED_PARAMETERS',
      yield put(resetChecklistValidationErrors(taskId, 'E246'));
    } else {
      yield put(setTaskError(errors));
    }
  } catch (error) {
    yield* handleCatch('Task', 'removeBulkVerificationSaga', error, false);
  }
}

export function* TaskListSaga() {
  yield takeLeading(TaskListActions.ADD_NEW_TASK, addNewTaskSaga);
  yield takeEvery(TaskListActions.DELETE_TASK, deleteTaskSaga);
  yield takeLatest(TaskListActions.ADD_STOP, addStopSaga);
  yield takeLatest(TaskListActions.REMOVE_STOP, removeStopSaga);
  yield takeEvery(TaskListActions.UPDATE_TASK_NAME, updateTaskSaga);
  yield takeLatest(TaskListActions.SET_TASK_TIMER, setTaskTimerSaga);
  yield takeLatest(TaskListActions.REMOVE_TASK_TIMER, removeTaskTimerSaga);
  yield takeLatest(TaskListActions.SET_TASK_RECURRENCE, setTaskRecurrenceSaga);
  yield takeLatest(TaskListActions.REMOVE_TASK_RECURRENCE, removeTaskRecurrenceSaga);
  yield takeLatest(TaskListActions.SET_TASK_SCHEDULE, setTaskScheduleSaga);
  yield takeLatest(TaskListActions.REMOVE_TASK_SCHEDULE, removeTaskScheduleSaga);
  yield takeLatest(TaskListActions.ADD_TASK_MEDIA, addTaskMediaSaga);
  yield takeLeading(TaskListActions.UPDATE_TASK_MEDIA, updateTaskMediaSaga);
  yield takeLatest(TaskListActions.REMOVE_TASK_MEDIA, removeTaskMediaSaga);
  yield takeLeading(TaskListActions.REORDER_TASK, reOrderTaskSaga);
  yield takeLeading(TaskListActions.UPDATE_TASK_ACTION, updateTaskActionSaga);
  yield takeLeading(TaskListActions.ARCHIVE_TASK_ACTION, archiveTaskActionSaga);
  yield takeLatest(TaskListActions.REORDER_PARAMETERS, reOrderParametersSaga);
  yield takeLatest(TaskListActions.ADD_TASK_ACTION, addTaskActionSaga);
  yield takeLatest(TaskListActions.ADD_SOLO_TASK_LOCK, addSoloTaskLockSaga);
  yield takeLatest(TaskListActions.REMOVE_SOLO_TASK_LOCK, removeSoloTaskLockSaga);
  yield takeLatest(TaskListActions.ADD_BULK_VERIFICATION, addBulkVerificationSaga);
  yield takeLatest(TaskListActions.REMOVE_BULK_VERIFICATION, removeBulkVerificationSaga);
}
