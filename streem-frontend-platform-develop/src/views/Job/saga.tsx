import {
  AutomationAction,
  AutomationActionActionType,
  AutomationActionTriggerType,
  TimerOperator,
} from '#PrototypeComposer/checklist.types';
import { showNotification } from '#components/Notification/actions';
import { NotificationType } from '#components/Notification/types';
import { closeOverlayAction, openOverlayAction } from '#components/OverlayContainer/actions';
import { OverlayNames } from '#components/OverlayContainer/types';
import { RootState } from '#store';
import { setRecentServerTimestamp } from '#store/extras/action';
import {
  COMPLETED_TASK_STATES,
  JobAuditLogType,
  JobStore,
  JobWithExceptionInCompleteTaskErrors,
  MandatoryParameter,
  NOT_STARTED_TASK_STATES,
  PARAMETER_ERRORS,
  Parameter,
  ParameterCorrectionStatus,
  ParameterExecutionState,
  ParameterState,
  ParameterVerificationStatus,
  ParameterVerificationTypeEnum,
  REFETCH_JOB_ERROR_CODES,
  RETAIN_NOTIFICATION_ERRORS,
  StoreTask,
  SupervisorResponse,
  TaskAction,
  TaskErrors,
  TaskExecution,
} from '#types';
import {
  apiAcceptVerification,
  apiApproveErrorCorrectionOnParameter,
  apiApproveParameter,
  apiAutoAcceptExceptionOnParameter,
  apiBulkAcceptVerification,
  apiBulkParameterException,
  apiCancelErrorCorrectionOnTask,
  apiCompleteJob,
  apiEnableErrorCorrectionOnTask,
  apiEndTaskRecurrence,
  apiExecuteParameter,
  apiGetChecklistActions,
  apiGetIsTaskAssigned,
  apiGetJobAuditLogs,
  apiGetSelectedJobLite,
  apiInitiateErrorCorrectionOnParameter,
  apiInitiateExceptionOnParameter,
  apiInitiatePeerVerification,
  apiInitiateSelfVerification,
  apiLogin,
  apiLogOut,
  apiPauseJob,
  apiPerformActionOnTask,
  apiPerformErrorCorrectionOnParameter,
  apiRecallVerification,
  apiRejectErrorCorrectionOnParameter,
  apiRejectParameter,
  apiRejectPeerVerification,
  apiRemoveRepeatTask,
  apiRepeatTask,
  apiResumeJob,
  apiSendBulkPeerVerification,
  apiStartJob,
  apiSubmitExceptionOnParameter,
  apiValidatePassword,
} from '#utils/apiUrls';
import { JOB_STAGE_POLLING_TIMEOUT } from '#utils/constants';
import { ResponseError, ResponseObj } from '#utils/globalTypes';
import { getAutomationActionTexts } from '#utils/parameterUtils';
import { getErrorCode, getErrorMsg, handleCatch, request } from '#utils/request';
import { encrypt } from '#utils/stringUtils';
import { getStartOfDayEpochInTimezone } from '#utils/timeUtils';
import { CompletedJobStates, Job, Verification } from '#views/Jobs/ListView/types';
import { navigate } from '@reach/router';
import { getTime } from 'date-fns';
import {
  actionChannel,
  all,
  call,
  delay,
  fork,
  put,
  race,
  select,
  take,
  takeEvery,
  takeLatest,
  takeLeading,
} from 'redux-saga/effects';
import { JobActionsEnum, initialState, jobActions } from './jobStore';
import { RefetchJobErrorType } from './overlays/RefetchJob';
import { getParametersInfo, navigateToTaskExecution, parseNewJobData } from './utils';
import { groupBy } from 'lodash';
import { getVerificationStatus } from './components/Task/Parameters/Verification/PeerVerification';
import { updateApprovalsList } from '#views/Inbox/ListView/actions';
import { TriggerType } from '#types/actionsAndEffects';
import { executeEffects } from '#utils/effects';

const getJobStore = (state: RootState) => state.job;
const getUserId = (state: RootState) => state.auth.userId;

// TODO: remove this and make respective changes in the Parameters
function getParametersDataByTaskId(task: StoreTask) {
  const parameters: any[] = [];
  task.parameters.forEach((parameter: any) => {
    const response = parameter.response;
    if (parameter)
      switch (parameter.type) {
        case MandatoryParameter.SIGNATURE:
        case MandatoryParameter.FILE_UPLOAD:
        case MandatoryParameter.MEDIA:
          parameters.push({
            ...parameter,
            reason: response?.reason || null,
            data: { medias: response?.medias },
          });
          break;
        case MandatoryParameter.SHOULD_BE:
        case MandatoryParameter.MULTI_LINE:
        case MandatoryParameter.DATE:
        case MandatoryParameter.DATE_TIME:
        case MandatoryParameter.SINGLE_LINE:
        case MandatoryParameter.NUMBER:
          parameters.push({
            ...parameter,
            reason: response?.reason || null,
            data: { ...parameter.data, input: response?.value },
          });
          break;
        case MandatoryParameter.MULTISELECT:
        case MandatoryParameter.SINGLE_SELECT:
        case MandatoryParameter.CHECKLIST:
        case MandatoryParameter.YES_NO:
          parameters.push({
            ...parameter,
            reason: response?.reason || null,
            data: parameter.data.map((d: any) => ({
              ...d,
              ...(response?.choices?.[d.id] && {
                state: response.choices[d.id],
              }),
            })),
          });
          break;
        case MandatoryParameter.RESOURCE:
        case MandatoryParameter.MULTI_RESOURCE:
        case MandatoryParameter.CALCULATION:
          parameters.push({
            ...parameter,
            reason: response?.reason || null,
            data: response?.choices,
          });
          break;
        default:
          parameters.push(parameter);
      }
  });
  return parameters;
}

const getScheduleTasksMessage = (
  scheduledTaskExecutionIds: string[],
  taskExecutions: RootState['job']['taskExecutions'],
  tasks: RootState['job']['tasks'],
  stages: RootState['job']['stages'],
) => {
  const message = scheduledTaskExecutionIds
    .map((id) => {
      const taskExecution = taskExecutions.get(id);
      const task = tasks.get(taskExecution!.taskId!);
      const stage = stages.get(task!.stageId!);
      return `Task ${stage?.orderTree}.${task?.orderTree}`;
    })
    .join('\n');

  return message;
};

const groupTaskErrors = (errors: ResponseError[]) => {
  const parametersErrors = new Map();
  const taskErrors: string[] = [];
  const generalErrors: string[] = [];
  errors.forEach((error) => {
    if (error.code in PARAMETER_ERRORS) {
      parametersErrors.set(error.id, error.message);
    } else if (error.code in { ...TaskErrors, ...JobWithExceptionInCompleteTaskErrors }) {
      taskErrors.push(error.message);
    } else {
      generalErrors.push(error.message);
    }
  });

  return { parametersErrors, taskErrors, generalErrors };
};

function* onSuccessErrorsHandlerSaga({
  payload,
}: ReturnType<typeof jobActions.onSuccessErrorsHandler>) {
  try {
    const { parameter } = payload;
    const { errors } = (yield select(getJobStore)) as JobStore;
    const updatedParameterErrors = new Map<string, string[]>(errors.parametersErrors);
    updatedParameterErrors.delete(parameter.response.id);

    yield put(
      jobActions.updateErrors({
        taskErrors: updatedParameterErrors.size ? errors.taskErrors : [],
        parametersErrors: updatedParameterErrors,
      }),
    );
  } catch (error) {
    yield* handleCatch('Job Saga', 'onSuccessErrorsHandlerSaga', error);
  }
}

function* performTaskActionSaga({ payload }: ReturnType<typeof jobActions.performTaskAction>) {
  try {
    const {
      id,
      reason,
      action,
      createObjectAutomations,
      continueRecurrence,
      recurringOverdueCompletionReason,
      recurringPrematureStartReason,
      scheduleOverdueCompletionReason,
      schedulePrematureStartReason,
    } = payload;

    const {
      id: jobId,
      stages,
      tasks,
      taskExecutions,
      activeTask: task,
      tasksActionsAndEffects,
    }: RootState['job'] = yield select((state: RootState) => state.job);

    const taskExecution = task.taskExecution;

    const taskStartActionEffects = tasksActionsAndEffects[task.id]?.start?.[0]?.effects;
    const taskCompleteActionEffects = tasksActionsAndEffects[task.id]?.complete?.[0]?.effects;

    if (!task) {
      return false;
    }

    yield put(
      jobActions.updateErrors({
        taskErrors: [],
        parametersErrors: new Map(),
      }),
    );

    const isCompleteAction = [
      TaskAction.COMPLETE_WITH_EXCEPTION,
      TaskAction.COMPLETE,
      TaskAction.SKIP,
      TaskAction.COMPLETE_ERROR_CORRECTION,
    ].includes(action);

    const { next } = taskExecutions.get(id)!;

    const { data, errors, timestamp }: ResponseObj<TaskExecution> = yield call(
      request,
      'PATCH',
      apiPerformActionOnTask(id, action),
      {
        data: {
          jobId,
          reason,
          continueRecurrence,
          recurringOverdueCompletionReason,
          recurringPrematureStartReason,
          scheduleOverdueCompletionReason,
          schedulePrematureStartReason,
          ...(isCompleteAction && {
            parameters: getParametersDataByTaskId(task),
          }),
          ...(createObjectAutomations &&
            createObjectAutomations?.length > 0 && {
              createObjectAutomations,
            }),
        },
      },
    );

    if (errors) {
      const shouldRefetch = errors.find((error) => error.code in REFETCH_JOB_ERROR_CODES);
      if (shouldRefetch && shouldRefetch?.code === 'E450') {
        yield put(
          openOverlayAction({
            type: OverlayNames.REFETCH_JOB_COMPOSER_DATA,
            props: {
              modalTitle: shouldRefetch.message,
              jobId,
              taskExecutionId: id,
              errorType: RefetchJobErrorType.PARAMETER,
            },
          }),
        );
      }

      const { taskDependencyError, taskExecutorLockError, _parameterErrors } = errors.reduce<{
        _parameterErrors: any[];
        taskDependencyError?: Record<string, any>;
        taskExecutorLockError?: Record<string, any>;
      }>(
        (acc, error) => {
          if (error.code in PARAMETER_ERRORS) {
            acc._parameterErrors.push(error);
          } else if (error.code === 'E2402') {
            acc.taskDependencyError = error;
          } else if (error.code === 'E2403') {
            acc.taskExecutorLockError = error;
          }
          return acc;
        },
        {
          _parameterErrors: [],
        },
      );

      if (taskDependencyError) {
        yield put(
          openOverlayAction({
            type: OverlayNames.TASK_DEPENDENCY_ERROR_MODAL,
            props: {
              taskDependencyError: taskDependencyError.errorInfo['TASK_DEPENDENCY_ERROR'],
            },
          }),
        );
      }

      if (taskExecutorLockError) {
        yield put(
          openOverlayAction({
            type: OverlayNames.TASK_EXECUTOR_LOCK_ERROR,
            props: {
              taskExecutorLockError:
                taskExecutorLockError.errorInfo[
                  'TASK_INITIATION_BLOCKED_DUE_TO_TASK_EXECUTOR_LOCK'
                ],
            },
          }),
        );
      }

      const { taskErrors, parametersErrors, generalErrors } = groupTaskErrors(errors);
      yield put(
        jobActions.updateErrors({
          taskErrors,
          parametersErrors,
        }),
      );

      yield all(
        _parameterErrors.map((error) => {
          const parameter = Array.from(task.parameters.values())?.find(
            (value) => value?.response?.id === error?.id || value?.id === error?.id,
          );

          const isValidationError = error.code in RETAIN_NOTIFICATION_ERRORS;
          return put(
            showNotification({
              type: NotificationType.ERROR,
              msg: `Parameter "${parameter?.label}" is ${
                isValidationError ? 'incorrect' : 'incomplete'
              }`,
              detail: error.message,
              buttonText: 'Go to the Parameter',
              onClick: () => {
                navigateToTaskExecution(jobId, id, error?.id);
              },
              autoClose: isValidationError ? false : undefined,
            }),
          );
        }),
      );

      if (taskErrors?.length || generalErrors?.length) {
        const error = generalErrors?.length ? generalErrors?.[0] : taskErrors?.[0];
        throw getErrorMsg(error);
      }
    } else {
      yield put(setRecentServerTimestamp(timestamp));
      yield put(
        jobActions.updateTaskExecution({
          id,
          data,
        }),
      );

      if (action === TaskAction.START) {
        const updatedTasksActionsAndEffects = {
          ...tasksActionsAndEffects,
          [task.id]: {
            ...(tasksActionsAndEffects[task.id] || {}),
            taskStartData: {
              data,
            },
          },
        };

        yield put(
          jobActions.updateTaskActionsAndEffects({
            data: updatedTasksActionsAndEffects,
          }),
        );
      } else if (isCompleteAction) {
        const updatedTasksActionsAndEffects = {
          ...tasksActionsAndEffects,
          [task.id]: {
            ...(tasksActionsAndEffects[task.id] || {}),
            taskCompleteData: {
              data,
            },
          },
        };

        yield put(
          jobActions.updateTaskActionsAndEffects({
            data: updatedTasksActionsAndEffects,
          }),
        );
      }

      const { automations: allAutomations = [] } = task;
      let filteredAutomations: AutomationAction[] = [];

      if (action === TaskAction.COMPLETE) {
        filteredAutomations = allAutomations.filter(
          (automation: AutomationAction) =>
            automation.triggerType === AutomationActionTriggerType.TASK_COMPLETED,
        );
      } else if (action === TaskAction.START) {
        filteredAutomations = allAutomations.filter(
          (automation: AutomationAction) =>
            automation.triggerType === AutomationActionTriggerType.TASK_STARTED,
        );
      }

      if (filteredAutomations.length) {
        const referencedParameterIds = filteredAutomations
          .map((automation) => automation.actionDetails.referencedParameterId)
          .filter(Boolean);

        const parametersData: any = yield call(getParametersInfo, jobId, referencedParameterIds);

        for (const automation of filteredAutomations) {
          const { executedAutomations = [] } = data;
          const triggeredAutomation = executedAutomations.find(
            (automationObject) => automationObject.id === automation.id,
          );

          const notificationType = errors
            ? NotificationType.ERROR
            : triggeredAutomation
            ? NotificationType.SUCCESS
            : NotificationType.WARNING;
          const parameterRefData = parametersData[automation.actionDetails.referencedParameterId];
          yield put(
            showNotification({
              type: notificationType,
              msg: getAutomationActionTexts({
                automation,
                forNotify: notificationType,
                parameterRefData,
                executedAutomationObject: triggeredAutomation,
              }),
              ...(triggeredAutomation?.actionType === AutomationActionActionType.CREATE_OBJECT && {
                autoClose: false,
              }),
            }),
          );
        }
      }

      if (data?.scheduledTaskExecutionIds?.length) {
        const message = getScheduleTasksMessage(
          data.scheduledTaskExecutionIds,
          taskExecutions,
          tasks,
          stages,
        );

        yield put(
          showNotification({
            type: NotificationType.SUCCESS,
            msg: `The following tasks are scheduled:\n${message}`,
          }),
        );
      }

      if (action === TaskAction.START && taskStartActionEffects?.length) {
        yield call(executeEffects, {
          effects: taskStartActionEffects,
        });
      } else if (action === TaskAction.COMPLETE && taskCompleteActionEffects?.length) {
        yield call(executeEffects, {
          effects: taskCompleteActionEffects,
        });
      }

      const triggerRecurrenceNotification = [
        TaskAction.COMPLETE,
        TaskAction.COMPLETE_WITH_EXCEPTION,
        TaskAction.SKIP,
      ].includes(action);

      const completeJobModalParameters = (code: string | undefined) => ({
        type: OverlayNames.CONFIRMATION_MODAL,
        props: {
          title: 'Complete Job',
          body: 'All the tasks are completed. Do you want to complete this job now?',
          primaryText: 'Complete Job',
          secondaryText: 'Cancel',
          onPrimary: () => {
            if (jobId && code) {
              window.store.dispatch(jobActions.completeJob({ jobId, details: { code } }));
            }
            window.store.dispatch(closeOverlayAction(OverlayNames.CONFIRMATION_MODAL));
          },
          onSecondary: () => {
            window.store.dispatch(closeOverlayAction(OverlayNames.CONFIRMATION_MODAL));
          },
        },
      });

      if (
        task?.enableRecurrence &&
        taskExecution?.continueRecurrence &&
        triggerRecurrenceNotification
      ) {
        if (data?.continueRecurrence) {
          yield put(
            showNotification({
              type: NotificationType.SUCCESS,
              msg: 'Recurring Task created successfully',
            }),
          );
          yield put(
            jobActions.setMovedToNextTask({
              movedToNextTask: false,
              taskCompletionEpoch: timestamp,
            }),
          );
        } else if (!data?.continueRecurrence) {
          navigateToTaskExecution(jobId, next);
          yield put(
            showNotification({
              type: NotificationType.SUCCESS,
              msg: 'Recurrence Ended. No new recurring task can be created.',
            }),
          );
          const { pendingTasks, code }: RootState['job'] = yield select(
            (state: RootState) => state.job,
          );
          if (pendingTasks.size === 0) {
            yield put(openOverlayAction(completeJobModalParameters(code)));
          }
        }
      }

      if (!task.enableRecurrence && triggerRecurrenceNotification) {
        const { pendingTasks, code }: RootState['job'] = yield select(
          (state: RootState) => state.job,
        );

        if (pendingTasks.size === 0) {
          yield put(openOverlayAction(completeJobModalParameters(code)));
        } else {
          navigateToTaskExecution(jobId, next);
        }
      }
    }
  } catch (error) {
    yield* handleCatch('Job', 'performTaskActionSaga', error, true);
  } finally {
    yield put(
      jobActions.setUpdating({
        updating: false,
      }),
    );
  }
}

function* repeatTaskSaga({ payload }: ReturnType<typeof jobActions.repeatTask>) {
  try {
    const { id: taskId } = payload;

    const {
      id: jobId,
      tasks,
      activeTask,
    }: RootState['job'] = yield select((state: RootState) => state.job);

    const { data, errors } = yield call(request, 'POST', apiRepeatTask(), {
      data: {
        taskId,
        jobId,
      },
    });

    if (data) {
      yield put(
        jobActions.updateTaskExecutions({
          taskId,
          data,
          action: 'add',
          currentTaskExecutionId: activeTask.taskExecution?.id,
        }),
      );
      const task = tasks.get(taskId);

      if (task) {
        const updatedTaskExecutions = [...task.taskExecutions, data.id];
        yield put(
          jobActions.updateTask({
            id: taskId,
            data: {
              taskExecutions: updatedTaskExecutions,
            },
          }),
        );
      }

      yield put(
        showNotification({
          type: NotificationType.SUCCESS,
          msg: 'Repeated Task created successfully',
        }),
      );

      navigateToTaskExecution(jobId, data.id);
    }

    if (errors) {
      throw getErrorMsg(errors);
    }
  } catch (error) {
    yield put(
      openOverlayAction({
        type: OverlayNames.REPEAT_TASK_ERROR_MODAL,
      }),
    );
    yield handleCatch('Task', 'repeatTaskSaga', error, true);
  } finally {
    yield put(
      jobActions.setUpdating({
        updating: false,
      }),
    );
  }
}

function* removeRepeatTaskSaga({ payload }: ReturnType<typeof jobActions.removeRepeatTask>) {
  try {
    const { taskExecutionId } = payload;

    const { taskExecutions, tasks, id }: RootState['job'] = yield select(
      (state: RootState) => state.job,
    );

    const taskExecution = taskExecutions.get(taskExecutionId)!;

    const { data, errors } = yield call(request, 'DELETE', apiRemoveRepeatTask(taskExecutionId));

    if (data) {
      yield put(
        jobActions.updateTaskExecutions({
          taskId: taskExecution.taskId,
          data,
          action: 'remove',
          currentTaskExecutionId: taskExecutionId,
        }),
      );
      const task = tasks.get(taskExecution.taskId);
      if (task) {
        const updatedTaskExecutions = task.taskExecutions.filter((id) => id !== taskExecutionId);
        yield put(
          jobActions.updateTask({
            id: task.id,
            data: {
              taskExecutions: updatedTaskExecutions,
            },
          }),
        );
      }

      yield put(
        showNotification({
          type: NotificationType.SUCCESS,
          msg: 'Repeated Task removed successfully',
        }),
      );

      if (taskExecution?.previous) {
        navigateToTaskExecution(id, taskExecution?.previous);
      }
    }

    if (errors) {
      throw getErrorMsg(errors);
    }
  } catch (error) {
    yield handleCatch('Task', 'removeRepeatTaskSaga', error, true);
  } finally {
    yield put(
      jobActions.setUpdating({
        updating: false,
      }),
    );
  }
}

function* endTaskRecurrenceSaga({ payload }: ReturnType<typeof jobActions.endTaskRecurrence>) {
  try {
    const { taskExecutionId } = payload;

    const { data, errors } = yield call(request, 'PATCH', apiEndTaskRecurrence(taskExecutionId));

    if (data) {
      yield put(
        showNotification({
          type: NotificationType.SUCCESS,
          msg: 'Recurrence Ended. No new recurring task can be created.',
        }),
      );
    }

    if (errors) {
      throw getErrorMsg(errors);
    }
  } catch (error) {
    yield handleCatch('Task', 'endTaskRecurrenceSaga', error, true);
  } finally {
    yield put(
      jobActions.setUpdating({
        updating: false,
      }),
    );
  }
}

function* togglePauseResumeSaga({ payload }: ReturnType<typeof jobActions.togglePauseResume>) {
  try {
    const { id, reason, comment, isTaskPaused } = payload;
    const { id: jobId }: RootState['job'] = yield select((state: RootState) => state.job);

    const { data, errors }: ResponseObj<TaskExecution> = yield call(
      request,
      isTaskPaused ? 'PATCH' : 'POST',
      isTaskPaused ? apiResumeJob(id) : apiPauseJob(id),
      {
        data: { jobId, ...(!isTaskPaused && { reason, ...(comment && { comment }) }) },
      },
    );

    if (errors) {
      const { taskErrors, parametersErrors } = groupTaskErrors(errors);
      yield put(
        jobActions.updateErrors({
          taskErrors,
          parametersErrors,
        }),
      );
      throw getErrorMsg(errors);
    }

    yield put(
      jobActions.updateTaskExecution({
        id,
        data,
      }),
    );
    yield put(
      showNotification({
        type: NotificationType.SUCCESS,
        msg: data.state === 'PAUSED' ? 'Task Paused successfully' : 'Task Resumed successfully',
      }),
    );
  } catch (error) {
    yield* handleCatch('Job', 'togglePauseResumeSaga', error, true);
  } finally {
    yield put(
      jobActions.setUpdating({
        updating: false,
      }),
    );
  }
}

function* startJobSaga({ payload }: ReturnType<typeof jobActions.startJob>) {
  try {
    const { id } = payload;
    const { taskExecutions, tasks, stages }: RootState['job'] = yield select(
      (state: RootState) => state.job,
    );

    const { data, errors }: ResponseObj<Job> = yield call(request, 'PATCH', apiStartJob(id));

    if (errors) {
      yield put(
        openOverlayAction({
          type: OverlayNames.BULK_EXCEPTION_MODAL,
          props: {
            jobId: id,
            errors,
            isCjfException: true,
          },
        }),
      );
      return;
    }

    if (data?.scheduledTaskExecutionIds?.length) {
      const message = getScheduleTasksMessage(
        data.scheduledTaskExecutionIds,
        taskExecutions,
        tasks,
        stages,
      );

      yield put(
        showNotification({
          type: NotificationType.SUCCESS,
          msg: `The following tasks are scheduled:\n${message}`,
        }),
      );
    }

    yield put(jobActions.startJobSuccess());
    yield put(
      showNotification({
        type: NotificationType.SUCCESS,
        msg: 'Job Started',
        detail:
          'You have started the Job. To start the Task you have to press the ‘Start Task’ button',
      }),
    );
    yield put(closeOverlayAction(OverlayNames.CONFIRMATION_MODAL));
  } catch (error) {
    yield* handleCatch('Job', 'startJobSaga', error, true);
  } finally {
    yield put(
      jobActions.setUpdating({
        updating: false,
      }),
    );
  }
}

function* completeJobSaga({ payload }: ReturnType<typeof jobActions.completeJob>) {
  try {
    const { stages, tasks }: RootState['job'] = yield select((state: RootState) => state.job);
    const { jobId, withException = false, values, details } = payload;
    const { errors, data }: ResponseObj<Job> = yield call(
      request,
      'PATCH',
      apiCompleteJob(withException, jobId),
      {
        ...(withException ? { data: { ...values } } : {}),
      },
    );

    if (withException) {
      yield put(closeOverlayAction(OverlayNames.COMPLETE_JOB_WITH_EXCEPTION));
    }

    if (errors) {
      if (withException) {
        const showInCompleteTasksError = errors.some(
          (err) => err.code in JobWithExceptionInCompleteTaskErrors,
        );
        if (showInCompleteTasksError) {
          yield put(closeOverlayAction(OverlayNames.COMPLETE_JOB_WITH_EXCEPTION));
          yield put(
            openOverlayAction({
              type: OverlayNames.JOB_COMPLETE_ALL_TASKS_ERROR,
            }),
          );

          const filteredErrors = errors.filter((err) => err.code === 'E223');

          if (filteredErrors.length) {
            let errorText = 'Error Correction has been initiated but not completed for Tasks:';
            const errorMessages = filteredErrors.map((error) => {
              const task = tasks.get(error.id);
              let stage;
              if (task?.stageId) {
                stage = stages.get(task.stageId);
              }
              return `${stage?.orderTree}.${task?.orderTree}`;
            });
            if (errorMessages.length > 0) {
              errorText += ` ${errorMessages.join(', ')}${errorMessages.length > 1 ? '.' : ''}`;
            }
            throw errorText;
          }
        }
      }
      throw getErrorMsg(errors);
    }

    if (data) {
      yield put(
        showNotification({
          type: NotificationType.SUCCESS,
          msg: `JobId ${details?.code} was successfully completed ${
            withException ? 'with exception' : ''
          }`,
        }),
      );
      navigate('/inbox');
    }
  } catch (error) {
    yield* handleCatch('Job', 'completeJobSaga', error, true);
  } finally {
    yield put(
      jobActions.setUpdating({
        updating: false,
      }),
    );
  }
}

function* executeParameterSaga({ payload }: ReturnType<typeof jobActions.executeParameter>) {
  const { parameter, reason } = payload;
  try {
    const { id: jobId }: RootState['job'] = yield select((state: RootState) => state.job);
    const clientEpoch = getTime(new Date());

    yield put(
      jobActions.setUpdating({
        updating: true,
      }),
    );
    yield put(
      jobActions.setExecutingParameterIds({
        id: parameter.id,
        value: true,
      }),
    );

    const { data, errors }: ResponseObj<Parameter> = yield call(
      request,
      'PATCH',
      apiExecuteParameter(parameter?.response?.id),
      {
        data: {
          jobId,
          parameter,
          ...(!!reason ? { reason } : {}),
          clientEpoch,
        },
      },
    );

    if (errors) {
      throw errors;
    }

    if (data) {
      const parameterResponse = data.response?.find(
        (currResponse) => currResponse.id === parameter.response.id,
      );
      if (
        data?.type === MandatoryParameter.SHOULD_BE &&
        parameterResponse.state === ParameterExecutionState.PENDING_FOR_APPROVAL
      ) {
        yield put(
          openOverlayAction({
            type: OverlayNames.PARAMETER_APPROVAL,
            props: {
              observationSent: true,
              observationApproved: false,
              observationRejected: false,
            },
          }),
        );
      }
    }

    yield put(
      jobActions.updateParameter({
        data,
      }),
    );

    if (data?.softErrors?.length) {
      yield put(
        openOverlayAction({
          type: OverlayNames.BULK_EXCEPTION_MODAL,
          props: {
            jobId,
            errors: data.softErrors,
          },
        }),
      );
    }
  } catch (errors: any) {
    const errorMessage = getErrorMsg(errors) || errors;
    const isValidationError = getErrorCode(errors) in RETAIN_NOTIFICATION_ERRORS;
    yield* handleCatch('Job', 'executeParameterSaga', errorMessage, true, {
      autoClose: isValidationError ? false : undefined,
    });
  } finally {
    yield put(
      jobActions.setUpdating({
        updating: false,
      }),
    );
    yield put(
      jobActions.setExecutingParameterIds({
        id: parameter.id,
        value: false,
      }),
    );
  }
}

function* approveRejectParameterSaga({
  payload,
}: ReturnType<typeof jobActions.approveRejectParameter>) {
  try {
    const { parameterId, parameterResponseId, type } = payload;
    const { id: jobId }: RootState['job'] = yield select((state: RootState) => state.job);

    const isApproving = type === SupervisorResponse.APPROVE;

    let url: string;

    if (isApproving) {
      url = apiApproveParameter(parameterResponseId);
    } else {
      url = apiRejectParameter(parameterResponseId);
    }

    const { data, errors }: ResponseObj<Parameter> = yield call(request, 'PATCH', url, {
      data: { jobId, parameterId },
    });

    if (errors) {
      throw getErrorMsg(errors);
    }

    yield put(
      jobActions.updateParameter({
        data,
      }),
    );
    yield put(
      openOverlayAction({
        type: OverlayNames.PARAMETER_APPROVAL,
        props: {
          observationSent: false,
          observationApproved: isApproving,
          observationRejected: !isApproving,
        },
      }),
    );
  } catch (error) {
    yield* handleCatch('Job', 'approveRejectParameterSaga', error, true);
  } finally {
    yield put(
      jobActions.setUpdating({
        updating: false,
      }),
    );
  }
}

function* initiateSelfVerificationSaga({
  payload,
}: ReturnType<typeof jobActions.initiateSelfVerification>) {
  try {
    const { parameterResponseId, parameterId } = payload;

    const { data, errors }: ResponseObj<Verification> = yield call(
      request,
      'POST',
      apiInitiateSelfVerification({ parameterResponseId }),
    );

    if (errors) {
      throw getErrorMsg(errors);
    }

    yield put(
      jobActions.updateParameterVerifications({
        parameterResponseId,
        data,
        parameterId,
      }),
    );
  } catch (error) {
    yield* handleCatch('Job', 'initiateSelfVerificationSaga', error, true);
  }
}

function* completeSelfVerificationSaga({
  payload,
}: ReturnType<typeof jobActions.completeSelfVerification>) {
  try {
    const { parameterResponseId, parameterId, password, code, state } = payload;

    const { errors: validateErrors } = yield call(request, 'PATCH', apiValidatePassword(), {
      data: { password: password ? encrypt(password) : null, code, state },
    });

    if (validateErrors) {
      throw getErrorMsg(validateErrors);
    }

    const { data, errors }: ResponseObj<Verification> = yield call(
      request,
      'PATCH',
      apiAcceptVerification({ parameterResponseId, type: 'self' }),
    );

    if (errors) {
      throw getErrorMsg(errors);
    }

    yield put(
      jobActions.updateParameterVerifications({
        parameterResponseId,
        parameterId,
        data,
      }),
    );
    yield put(
      showNotification({
        type: NotificationType.SUCCESS,
        msg: 'Parameter has been Self Verified Successfully',
      }),
    );
  } catch (error) {
    yield* handleCatch('Job', 'completeSelfVerificationSaga', error, true);
  }
}

function* completeBulkSelfVerificationSaga({
  payload,
}: ReturnType<typeof jobActions.completeBulkSelfVerification>) {
  try {
    const { values, password, code, state } = payload;

    const { errors: validateErrors } = yield call(request, 'PATCH', apiValidatePassword(), {
      data: { password: password ? encrypt(password) : null, code, state },
    });

    if (validateErrors) {
      throw getErrorMsg(validateErrors);
    }

    const { data, errors }: ResponseObj<Verification[]> = yield call(
      request,
      'PATCH',
      apiBulkAcceptVerification({ type: 'self' }),
      {
        data: { selfVerify: values },
      },
    );

    if (errors) {
      throw getErrorMsg(errors);
    }

    yield put(
      jobActions.updateBulkParameterVerifications({
        ids: values.reduce((acc, item) => {
          acc[item.parameterExecutionId] = item.parameterId;
          return acc;
        }, {}),
        data,
      }),
    );
    yield put(
      showNotification({
        type: NotificationType.SUCCESS,
        msg: 'Parameters have been Self Verified Successfully',
      }),
    );
  } catch (error) {
    yield* handleCatch('Job', 'completeBulkSelfVerificationSaga', error, true);
  }
}

function* sendBulkPeerVerificationSaga({
  payload,
}: ReturnType<typeof jobActions.sendBulkPeerVerification>) {
  try {
    const { values } = payload;

    const { data, errors }: ResponseObj<Verification[]> = yield call(
      request,
      'PATCH',
      apiSendBulkPeerVerification(),
      {
        data: values,
      },
    );

    if (errors) {
      throw getErrorMsg(errors);
    }

    yield put(
      jobActions.updateBulkParameterVerifications({
        ids: values.reduce((acc, item) => {
          acc[item.parameterExecutionId] = item.parameterId;
          return acc;
        }, {}),
        data,
      }),
    );
    yield put(
      showNotification({
        type: NotificationType.SUCCESS,
        msg: 'Request for Parameter Verification Sent Successfully',
      }),
    );
  } catch (error) {
    yield* handleCatch('Job', 'sendBulkPeerVerificationSaga', error, true);
  }
}

function* completeBulkPeerVerificationSaga({
  payload,
}: ReturnType<typeof jobActions.completeBulkPeerVerification>) {
  try {
    const { values, password, code, state } = payload;

    const { errors: validateErrors } = yield call(request, 'PATCH', apiValidatePassword(), {
      data: { password: password ? encrypt(password) : null, code, state },
    });

    if (validateErrors) {
      throw getErrorMsg(validateErrors);
    }

    const { data, errors }: ResponseObj<Verification[]> = yield call(
      request,
      'PATCH',
      apiBulkAcceptVerification({ type: 'peer' }),
      {
        data: { peerVerify: values },
      },
    );

    if (errors) {
      throw getErrorMsg(errors);
    }

    yield put(
      jobActions.updateBulkParameterVerifications({
        ids: values.reduce((acc, item) => {
          acc[item.parameterExecutionId] = item.parameterId;
          return acc;
        }, {}),
        data,
      }),
    );
    yield put(
      showNotification({
        type: NotificationType.SUCCESS,
        msg: 'Parameters have been Peer Verified Successfully',
      }),
    );
  } catch (error) {
    yield* handleCatch('Job', 'completeBulkPeerVerificationSaga', error, true);
  }
}

function* sendPeerVerificationSaga({
  payload,
}: ReturnType<typeof jobActions.sendPeerVerification>) {
  try {
    const { parameterResponseId, parameterId, data: payloadData } = payload;

    const { data, errors }: ResponseObj<Verification> = yield call(
      request,
      'POST',
      apiInitiatePeerVerification({ parameterResponseId }),
      {
        data: payloadData,
      },
    );

    if (errors) {
      throw getErrorMsg(errors);
    }

    yield put(
      jobActions.updateParameterVerifications({
        parameterResponseId,
        parameterId,
        data,
      }),
    );
    yield put(
      showNotification({
        type: NotificationType.SUCCESS,
        msg: 'Request for Parameter Verification Sent Successfully',
      }),
    );
  } catch (error) {
    yield* handleCatch('Job', 'sendPeerVerificationSaga', error, true);
  }
}

function* recallPeerVerificationSaga({
  payload,
}: ReturnType<typeof jobActions.recallPeerVerification>) {
  try {
    const { parameterResponseId, parameterId, type } = payload;

    const { data, errors }: ResponseObj<Verification> = yield call(
      request,
      'PATCH',
      apiRecallVerification({ parameterResponseId, type }),
    );

    if (errors) {
      throw getErrorMsg(errors);
    }

    yield put(
      jobActions.updateParameterVerifications({
        parameterResponseId,
        parameterId,
        data,
      }),
    );
  } catch (error) {
    yield* handleCatch('Job', 'recallPeerVerificationSaga', error, true);
  }
}

function* acceptPeerVerificationSaga({
  payload,
}: ReturnType<typeof jobActions.acceptPeerVerification>) {
  try {
    const { parameterResponseId, parameterId, password, code, state } = payload;

    const { errors: validateErrors } = yield call(request, 'PATCH', apiValidatePassword(), {
      data: { password: password ? encrypt(password) : null, code, state },
    });

    if (validateErrors) {
      throw getErrorMsg(validateErrors);
    }

    const { data, errors }: ResponseObj<Verification> = yield call(
      request,
      'PATCH',
      apiAcceptVerification({ parameterResponseId, type: 'peer' }),
    );

    if (errors) {
      throw getErrorMsg(errors);
    }

    yield put(
      jobActions.updateParameterVerifications({
        parameterResponseId,
        parameterId,
        data,
      }),
    );
  } catch (error) {
    yield* handleCatch('Job', 'acceptPeerVerificationSaga', error, true);
  }
}

function* rejectPeerVerificationSaga({
  payload,
}: ReturnType<typeof jobActions.rejectPeerVerification>) {
  try {
    const { parameterResponseId, parameterId, comment } = payload;

    const { data, errors }: ResponseObj<Verification> = yield call(
      request,
      'PATCH',
      apiRejectPeerVerification({ parameterResponseId }),
      {
        data: {
          comments: comment,
        },
      },
    );

    if (errors) {
      throw getErrorMsg(errors);
    }

    yield put(
      jobActions.updateParameterVerifications({
        parameterResponseId,
        parameterId,
        data,
      }),
    );
    yield put(closeOverlayAction(OverlayNames.REASON_MODAL));
  } catch (error) {
    yield* handleCatch('Job', 'rejectPeerVerificationSaga', error, true);
  }
}

function* activeJobPollingSaga(payload: ReturnType<typeof jobActions.pollJob>['payload']) {
  const { id, initialExecutionId, parameterExecutionId } = payload;
  let initial = true;
  while (true) {
    try {
      const { data, errors } = yield call(request, 'GET', apiGetSelectedJobLite(id));

      if (errors) {
        throw 'Could Not Fetch Active Stage Data';
      }

      const parsedJobData = parseNewJobData(data);

      yield put(
        jobActions.pollJobSuccess({
          data: parsedJobData,
        }),
      );

      if (initial) {
        initial = false;
        navigateToTaskExecution(
          parsedJobData.id!,
          initialExecutionId || parsedJobData.activeTaskExecutionId,
          parameterExecutionId,
        );
      }

      if (data.state in CompletedJobStates) {
        yield put(jobActions.stopPollJob());
      }

      //yield put(jobActions.stopPollJob());

      yield delay(JOB_STAGE_POLLING_TIMEOUT);
    } catch (err) {
      console.error('error from startPollActiveStageData in Job Saga :: ', err);
      yield delay(JOB_STAGE_POLLING_TIMEOUT);
    }
  }
}

/**
 * Saga to update the timer state of the task
 * This saga keeps track of task's duration provided by the BE & keeps incrementing by 1/sec till its changed.
 */
function* taskTimerSaga() {
  // default time elapsed value
  let previousTaskDuration: number = 0;
  // reset the timer state on task change
  yield put(jobActions.setTimerState(initialState.timerState));
  while (true) {
    try {
      // get the current task and timer state
      const { activeTask: task, timerState } = (yield select(getJobStore)) as JobStore;
      const _timerState = { ...timerState };
      const taskExecution = task.taskExecution;

      if (taskExecution) {
        const { state, duration } = taskExecution;
        // duration is updated only through task polling ie (BE is the source of truth)
        // it could be null if the task is not started, so only update the time elapsed if the duration is not null or 0
        if (duration && previousTaskDuration !== duration) {
          previousTaskDuration = duration;
          _timerState.timeElapsed = duration;
        } else {
          _timerState.timeElapsed++;
        }

        // task.timerOperator is null for tasks that are not time bound
        if (task.timerOperator === TimerOperator.NOT_LESS_THAN) {
          if (task.minPeriod && _timerState.timeElapsed < task.minPeriod) {
            _timerState.earlyCompletion = true;
          } else {
            _timerState.earlyCompletion = false;
          }
        }

        if (task.maxPeriod && _timerState.timeElapsed > task.maxPeriod) {
          _timerState.limitCrossed = true;
        }

        yield put(jobActions.setTimerState(_timerState));

        // if task is not in progress, stop the timer
        if (state !== 'IN_PROGRESS') {
          yield put(jobActions.stopTaskTimer());
        }
      }

      // yield put(jobActions.stopTaskTimer());

      // wait for 1 second before updating the timer
      yield delay(1000);
    } catch (err) {
      console.error('error from taskTimerSaga in Job Saga :: ', err);
      yield put(jobActions.stopTaskTimer());
    }
  }
}

function* JobPollSaga() {
  while (true) {
    const { payload } = yield take(JobActionsEnum.pollJob);
    yield race([call(activeJobPollingSaga, payload), take(JobActionsEnum.stopPollJob)]);
  }
}

function* TaskTimerSaga() {
  while (true) {
    yield take(JobActionsEnum.startTaskTimer);
    yield race([call(taskTimerSaga), take(JobActionsEnum.stopTaskTimer)]);
  }
}

function* pollActiveTaskExecutionSaga(
  payload: ReturnType<typeof jobActions.pollActiveTaskExecution>['payload'],
): any {
  let initial = true;
  while (true) {
    try {
      const { taskExecutionId } = payload;
      const userId = getUserId(yield select());
      const [taskData, isTaskAssignedFetchedData] = yield all([
        call(request, 'GET', apiPerformActionOnTask(taskExecutionId, 'state')),
        ...(initial ? [call(request, 'GET', apiGetIsTaskAssigned(taskExecutionId))] : []),
      ]);

      if (taskData?.data) {
        const {
          task: { taskExecutions, ...task },
          hidden,
          jobState,
        } = taskData.data;

        const taskExecution = taskExecutions?.[0];

        const pendingSelfVerificationParameters: any[] = [];
        const pendingPeerVerificationParameters: any[] = [];
        const executedParametersWithPeerVerification: any[] = [];

        const parameters = new Map();
        let canSkipTask = true;

        task.parameters.forEach((_parameter: any) => {
          let showSelfVerification = false;
          let showPeerVerification = false;
          let requestPeerVerification = false;
          const response = _parameter.response?.[0];
          const parameter = {
            ..._parameter,
            response,
          };

          if (response && !response.hidden) {
            const { verificationType } = parameter;
            const { parameterVerifications, state } = response;
            const verificationGroupedData = groupBy(parameterVerifications, 'verificationType');
            const selfVerificationsArray =
              verificationGroupedData?.[ParameterVerificationTypeEnum.SELF] || [];
            const peerVerificationsArray =
              verificationGroupedData?.[ParameterVerificationTypeEnum.PEER] || [];
            const { status: selfVerificationStatus } = getVerificationStatus(
              selfVerificationsArray,
              userId!,
            );
            const { isUserReviewer, status: peerVerificationStatus } = getVerificationStatus(
              peerVerificationsArray,
              userId!,
            );
            const findSelfVerification = verificationGroupedData['SELF']
              ? verificationGroupedData['SELF'][0]
              : null;

            requestPeerVerification =
              !(taskExecution.state in { ...NOT_STARTED_TASK_STATES, ...COMPLETED_TASK_STATES }) &&
              userId === response?.audit?.modifiedBy?.id &&
              !taskExecution.correctionEnabled &&
              peerVerificationStatus !== ParameterVerificationStatus.PENDING &&
              [
                ParameterState.BEING_EXECUTED,
                ParameterState.APPROVAL_PENDING,
                ParameterState.VERIFICATION_PENDING,
              ].includes(state) &&
              ((verificationType === ParameterVerificationTypeEnum.PEER &&
                state !== ParameterState.NOT_STARTED) ||
                (verificationType === ParameterVerificationTypeEnum.BOTH &&
                  findSelfVerification?.verificationStatus ===
                    ParameterVerificationStatus.ACCEPTED &&
                  findSelfVerification?.evaluationState !== ParameterState.BEING_EXECUTED));

            showSelfVerification =
              [ParameterState.BEING_EXECUTED, ParameterState.APPROVAL_PENDING].includes(state) &&
              !(taskExecution.state in { ...NOT_STARTED_TASK_STATES, ...COMPLETED_TASK_STATES }) &&
              selfVerificationStatus !== ParameterVerificationStatus.ACCEPTED &&
              selfVerificationStatus !== ParameterVerificationStatus.PENDING &&
              userId === response?.audit?.modifiedBy?.id &&
              [ParameterVerificationTypeEnum.SELF, ParameterVerificationTypeEnum.BOTH].includes(
                verificationType,
              );

            showPeerVerification =
              isUserReviewer &&
              peerVerificationStatus === ParameterVerificationStatus.PENDING &&
              [ParameterVerificationTypeEnum.PEER, ParameterVerificationTypeEnum.BOTH].includes(
                verificationType,
              ) &&
              !(taskExecution.state in { ...NOT_STARTED_TASK_STATES, ...COMPLETED_TASK_STATES });

            if (showSelfVerification) {
              pendingSelfVerificationParameters.push(parameter);
            }

            if (showPeerVerification) {
              pendingPeerVerificationParameters.push(parameter);
            }

            if (requestPeerVerification) {
              executedParametersWithPeerVerification.push(parameter);
            }
          }

          parameters.set(parameter.id, {
            ...parameter,
            response,
          });

          if (canSkipTask) {
            canSkipTask = !parameter.mandatory;
          }
        });

        yield put(
          jobActions.pollActiveTaskExecutionSuccess({
            data: {
              ...task,
              pendingSelfVerificationParameters,
              pendingPeerVerificationParameters,
              executedParametersWithPeerVerification,
              canSkipTask,
              taskExecution,
              hidden,
              parameters,
              loading: false,
              ...(initial
                ? {
                    isTaskAssigned: !!isTaskAssignedFetchedData?.data?.assigned,
                  }
                : {}),
            },
          }),
        );

        if (initial) {
          initial = false;
        }

        if (jobState in CompletedJobStates) {
          yield put(jobActions.stopPollActiveTaskExecution());
        }

        //  yield put(jobActions.stopPollActiveTaskExecution());
      }
      yield delay(JOB_STAGE_POLLING_TIMEOUT);
    } catch (error) {
      yield* handleCatch('Job', 'pollActiveTaskExecution', error, true);
      yield delay(JOB_STAGE_POLLING_TIMEOUT);
    }
  }
}

function* TaskPollSaga() {
  while (true) {
    const { payload } = yield take(JobActionsEnum.pollActiveTaskExecution);
    yield race([
      call(pollActiveTaskExecutionSaga, payload),
      take(JobActionsEnum.stopPollActiveTaskExecution),
    ]);
  }
}

function* jobAuditLogsSaga({ payload }: ReturnType<typeof jobActions.getJobAuditLogs>) {
  try {
    const { jobId, params } = payload;

    const { data, pageable, errors }: ResponseObj<JobAuditLogType[]> = yield call(
      request,
      'GET',
      apiGetJobAuditLogs(jobId),
      { params },
    );

    if (errors) {
      throw getErrorMsg(errors);
    }

    const newData = data.map((el) => ({
      ...el,
      triggeredOn: getStartOfDayEpochInTimezone({ value: el.triggeredAt }),
    }));

    yield put(
      jobActions.getJobAuditLogsSuccess({
        data: newData,
        pageable,
      }),
    );
  } catch (e) {
    yield* handleCatch('JobParameter', 'fetchJobAuditLogsSaga', e);
  }
}

function* enableErrorCorrectionOnTaskSaga({
  payload,
}: ReturnType<typeof jobActions.enableErrorCorrectionOnTask>) {
  try {
    const { taskExecutionId } = payload;

    const { data, errors, timestamp } = yield call(
      request,
      'POST',
      apiEnableErrorCorrectionOnTask(taskExecutionId),
    );

    if (data) {
      yield put(setRecentServerTimestamp(timestamp));
      yield put(
        jobActions.updateTaskExecution({
          id: taskExecutionId,
          data,
        }),
      );
    } else {
      throw getErrorMsg(errors);
    }
  } catch (error) {
    yield* handleCatch('Job', 'enableErrorCorrectionOnTaskSaga', error, true);
  } finally {
    yield put(
      jobActions.setUpdating({
        updating: false,
      }),
    );
  }
}

function* cancelErrorCorrectionOnTaskSaga({
  payload,
}: ReturnType<typeof jobActions.cancelErrorCorrectionOnTask>) {
  try {
    const { taskExecutionId } = payload;

    const { data, errors, timestamp } = yield call(
      request,
      'PATCH',
      apiCancelErrorCorrectionOnTask(taskExecutionId),
    );

    if (data) {
      yield put(setRecentServerTimestamp(timestamp));
      yield put(
        jobActions.updateTaskExecution({
          id: taskExecutionId,
          data,
        }),
      );
    } else {
      throw getErrorMsg(errors);
    }
  } catch (error) {
    yield* handleCatch('Job', 'cancelErrorCorrectionOnTaskSaga', error, true);
  } finally {
    yield put(
      jobActions.setUpdating({
        updating: false,
      }),
    );
  }
}

function* initiateErrorCorrectionOnParameterSaga({
  payload,
}: ReturnType<typeof jobActions.initiateErrorCorrectionOnParameter>) {
  try {
    const { parameterResponseId, initiatorReason, correctors, reviewers, password, code, state } =
      payload;

    const { errors: validateErrors } = yield call(request, 'PATCH', apiValidatePassword(), {
      data: { password: password ? encrypt(password) : null, code, state },
    });

    if (validateErrors) {
      throw getErrorMsg(validateErrors);
    }

    const { errors, data } = yield call(
      request,
      'POST',
      apiInitiateErrorCorrectionOnParameter(parameterResponseId),
      { data: { initiatorReason, correctors, reviewers } },
    );

    if (errors) {
      throw getErrorMsg(errors);
    } else if (data) {
      yield put(
        showNotification({
          type: NotificationType.SUCCESS,
          msg: `Correction (ID: ${data.code}) request initiated successfully`,
        }),
      );
    }
  } catch (error) {
    yield* handleCatch('Job', 'initiateErrorCorrectionOnParameterSaga', error, true);
  }
}

function* initiateExceptionOnParameterSaga({
  payload,
}: ReturnType<typeof jobActions.initiateExceptionOnParameter>) {
  try {
    const {
      parameterResponseId,
      initiatorReason,
      approver,
      value,
      choices,
      password,
      code,
      state,
      closeOverlayFn,
    } = payload;

    const { errors: validateErrors } = yield call(request, 'PATCH', apiValidatePassword(), {
      data: { password: password ? encrypt(password) : null, code, state },
    });

    if (validateErrors) {
      throw getErrorMsg(validateErrors);
    }

    const { errors, data } = yield call(
      request,
      'POST',
      apiInitiateExceptionOnParameter(parameterResponseId),
      { data: { initiatorReason, reviewers: approver, value, choices } },
    );

    if (errors) {
      throw getErrorMsg(errors);
    } else if (data) {
      yield put(
        showNotification({
          type: NotificationType.SUCCESS,
          msg: `${data.code} : Exception request initiated successfully`,
        }),
      );
      closeOverlayFn?.();
    }
  } catch (error) {
    yield* handleCatch('Job', 'initiateExceptionOnParameterSaga', error, true);
  }
}

function* submitExceptionOnParameterSaga({
  payload,
}: ReturnType<typeof jobActions.submitExceptionOnParameter>) {
  try {
    const {
      parameterResponseId,
      exceptionId,
      reviewerReason,
      password,
      reviewStatus,
      code,
      state,
      closeOverlayFn,
      isCjfException,
      jobId,
      rulesId,
    } = payload;

    const { errors: validateErrors } = yield call(request, 'PATCH', apiValidatePassword(), {
      data: { password: password ? encrypt(password) : null, code, state },
    });

    if (validateErrors) {
      throw getErrorMsg(validateErrors);
    }

    const { errors, data } = yield call(
      request,
      'PATCH',
      apiSubmitExceptionOnParameter(parameterResponseId, reviewStatus),
      { data: { reviewerReason, exceptionId } },
    );

    if (errors) {
      throw getErrorMsg(errors);
    } else if (data) {
      yield put(
        showNotification({
          type: reviewStatus === 'approve' ? NotificationType.SUCCESS : NotificationType.ERROR,
          msg:
            reviewStatus === 'approve'
              ? `${data.code} : Exception request approved successfully`
              : `${data.code} : Exception request rejected`,
          ...(isCjfException &&
            reviewStatus !== 'approve' && {
              buttonText: 'Complete with Exception',
              detail:
                'Since the CJF parameter is not executed, this job cannot be executed. Please complete this job with exception',
              onClick: () => {
                navigateToTaskExecution(jobId);
              },
            }),
          autoClose: false,
        }),
      );
      closeOverlayFn?.();
      yield put(
        updateApprovalsList({
          rulesId,
        }),
      );
    }
  } catch (error) {
    yield* handleCatch('Job', 'initiateExceptionOnParameterSaga', error, true);
  }
}

function* performErrorCorrectionOnParameterSaga({
  payload,
}: ReturnType<typeof jobActions.performErrorCorrectionOnParameter>) {
  try {
    const {
      parameterResponseId,
      password,
      newChoice,
      newValue,
      correctorReason,
      correctionId,
      medias,
      code,
      state,
    } = payload;

    const { errors: validateErrors } = yield call(request, 'PATCH', apiValidatePassword(), {
      data: { password: password ? encrypt(password) : null, code, state },
    });

    if (validateErrors) {
      throw getErrorMsg(validateErrors);
    }

    const { errors, data } = yield call(
      request,
      'PATCH',
      apiPerformErrorCorrectionOnParameter(parameterResponseId),
      { data: { newChoice, newValue, correctorReason, correctionId, medias } },
    );

    if (errors) {
      throw getErrorMsg(errors);
    } else if (data) {
      yield put(
        showNotification({
          type: NotificationType.SUCCESS,
          msg: `Correction (ID: ${data.code}) submit for review successfully`,
        }),
      );
    }
  } catch (error) {
    yield* handleCatch('Job', 'performErrorCorrectionOnParameterSaga', error, true);
  }
}

function* approveRejectErrorCorrectionOnParameterSaga({
  payload,
}: ReturnType<typeof jobActions.approveRejectErrorCorrectionOnParameter>) {
  try {
    const {
      parameterResponseId,
      correctionId,
      reviewerReason,
      password,
      performCorrectionStatus,
      code,
      state,
    } = payload;
    const { errors: validateErrors } = yield call(request, 'PATCH', apiValidatePassword(), {
      data: { password: password ? encrypt(password) : null, code, state },
    });

    if (validateErrors) {
      throw getErrorMsg(validateErrors);
    }

    const isApproving = performCorrectionStatus === ParameterCorrectionStatus.ACCEPTED;

    const { errors, data } = yield call(
      request,
      'PATCH',
      isApproving
        ? apiApproveErrorCorrectionOnParameter(parameterResponseId)
        : apiRejectErrorCorrectionOnParameter(parameterResponseId),
      { data: { correctionId, reviewerReason } },
    );

    if (errors) {
      throw getErrorMsg(errors);
    } else if (data) {
      yield put(
        showNotification({
          type: isApproving ? NotificationType.SUCCESS : NotificationType.ERROR,
          msg: `Correction (ID: ${data.code}) request ${
            isApproving ? 'approved' : 'rejected'
          } successfully`,
        }),
      );
    }
  } catch (error) {
    yield* handleCatch('Job', 'approveRejectErrorCorrectionOnParameterSaga', error, true);
  }
}

function* autoAcceptExceptionOnParameterSaga({
  payload,
}: ReturnType<typeof jobActions.autoAcceptExceptionOnParameter>) {
  try {
    const { parameterResponseId, reason, value, choices, closeOverlayFn } = payload;

    const { errors, data } = yield call(
      request,
      'POST',
      apiAutoAcceptExceptionOnParameter(parameterResponseId),
      { data: { reason, value, choices } },
    );

    if (errors) {
      throw getErrorMsg(errors);
    } else if (data) {
      yield put(
        showNotification({
          type: NotificationType.SUCCESS,
          msg: `${data.code} :  Exception auto approved successfully`,
        }),
      );
      closeOverlayFn?.();
    }
  } catch (error) {
    yield* handleCatch('Job', 'autoAcceptExceptionOnParameter', error, true);
  }
}

function* initiateBulkExceptionsOnParametersSaga({
  payload,
}: ReturnType<typeof jobActions.initiateBulkExceptionsOnParameter>) {
  try {
    const { parametersWithException, password, code, state, closeOverlayFn } = payload;

    const { errors: validateErrors } = yield call(request, 'PATCH', apiValidatePassword(), {
      data: { password: password ? encrypt(password) : null, code, state },
    });

    if (validateErrors) {
      throw getErrorMsg(validateErrors);
    }

    const { errors, data } = yield call(request, 'POST', apiBulkParameterException(), {
      data: parametersWithException,
    });

    if (errors) {
      throw getErrorMsg(errors);
    } else if (data) {
      yield put(
        showNotification({
          type: NotificationType.SUCCESS,
          msg: 'Exceptions submitted successfully!',
        }),
      );
      closeOverlayFn?.();
    }
  } catch (error) {
    yield* handleCatch('Job', 'initiateBulkExceptionsOnParametersSaga', error, true);
  }
}

function* fetchActionsForProcessSaga({
  payload,
}: ReturnType<typeof jobActions.fetchActionsForProcess>) {
  try {
    const { checklistId } = payload;

    const { errors, data } = yield call(request, 'GET', apiGetChecklistActions(checklistId));

    if (errors) {
      throw getErrorMsg(errors);
    }

    if (data?.length) {
      const taskActions = data.reduce((acc, action) => {
        const taskId = action.triggerEntityId;
        const triggerType = action.triggerType === TriggerType.START_TASK ? 'start' : 'complete';

        if (!acc[taskId]) {
          acc[taskId] = { start: [], complete: [] };
        }

        acc[taskId][triggerType].push(action);

        return acc;
      }, {});

      yield put(
        jobActions.updateTaskActionsAndEffects({
          data: taskActions,
        }),
      );
    }
  } catch (error) {
    yield* handleCatch('Job', 'fetchActionsForProcessSaga', error, true);
  }
}

function* takeOneAtMost() {
  // 1- Create a channel for request actions
  const requestChan: any = yield actionChannel(JobActionsEnum.executeParameter);

  while (true) {
    // 2- take from the channel
    const action = yield take(requestChan);
    // 3- Note that we're using a blocking call
    yield call(executeParameterSaga as any, action);
  }
}

export function* jobSaga() {
  yield takeLeading(JobActionsEnum.performTaskAction, performTaskActionSaga);
  yield takeLeading(JobActionsEnum.repeatTask, repeatTaskSaga);
  yield takeLeading(JobActionsEnum.removeRepeatTask, removeRepeatTaskSaga);
  yield takeLeading(JobActionsEnum.endTaskRecurrence, endTaskRecurrenceSaga);
  yield takeLeading(JobActionsEnum.togglePauseResume, togglePauseResumeSaga);
  yield takeLeading(JobActionsEnum.startJob, startJobSaga);
  yield takeLeading(JobActionsEnum.completeJob, completeJobSaga);
  yield takeLeading(JobActionsEnum.approveRejectParameter, approveRejectParameterSaga);
  yield takeLeading(JobActionsEnum.initiateSelfVerification, initiateSelfVerificationSaga);
  yield takeLeading(JobActionsEnum.completeSelfVerification, completeSelfVerificationSaga);
  yield takeLeading(JobActionsEnum.sendPeerVerification, sendPeerVerificationSaga);
  yield takeLeading(JobActionsEnum.recallPeerVerification, recallPeerVerificationSaga);
  yield takeLeading(JobActionsEnum.acceptPeerVerification, acceptPeerVerificationSaga);
  yield takeLeading(JobActionsEnum.rejectPeerVerification, rejectPeerVerificationSaga);
  yield takeLeading(JobActionsEnum.completeBulkSelfVerification, completeBulkSelfVerificationSaga);
  yield takeLeading(JobActionsEnum.completeBulkPeerVerification, completeBulkPeerVerificationSaga);
  yield takeLeading(JobActionsEnum.sendBulkPeerVerification, sendBulkPeerVerificationSaga);
  yield takeLatest(JobActionsEnum.getJobAuditLogs, jobAuditLogsSaga);
  yield takeLatest(JobActionsEnum.enableErrorCorrectionOnTask, enableErrorCorrectionOnTaskSaga);
  yield takeLatest(JobActionsEnum.cancelErrorCorrectionOnTask, cancelErrorCorrectionOnTaskSaga);
  yield takeLatest(
    JobActionsEnum.initiateErrorCorrectionOnParameter,
    initiateErrorCorrectionOnParameterSaga,
  );
  yield takeLatest(JobActionsEnum.initiateExceptionOnParameter, initiateExceptionOnParameterSaga);
  yield takeLatest(JobActionsEnum.submitExceptionOnParameter, submitExceptionOnParameterSaga);
  yield takeLatest(
    JobActionsEnum.performErrorCorrectionOnParameter,
    performErrorCorrectionOnParameterSaga,
  );
  yield takeLatest(
    JobActionsEnum.approveRejectErrorCorrectionOnParameter,
    approveRejectErrorCorrectionOnParameterSaga,
  );
  yield takeLatest(
    JobActionsEnum.autoAcceptExceptionOnParameter,
    autoAcceptExceptionOnParameterSaga,
  );
  yield takeLatest(
    JobActionsEnum.initiateBulkExceptionsOnParameter,
    initiateBulkExceptionsOnParametersSaga,
  );
  yield takeLatest(JobActionsEnum.fetchActionsForProcess, fetchActionsForProcessSaga);
  yield takeEvery(JobActionsEnum.onSuccessErrorsHandler, onSuccessErrorsHandlerSaga);

  // Keep this at the very last
  yield all([fork(takeOneAtMost), fork(TaskPollSaga), fork(TaskTimerSaga), fork(JobPollSaga)]);
}
