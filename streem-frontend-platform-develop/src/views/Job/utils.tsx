import { openOverlayAction } from '#components/OverlayContainer/actions';
import { OverlayNames } from '#components/OverlayContainer/types';
import { useTypedSelector } from '#store';
import { COMPLETED_TASK_STATES, IN_PROGRESS_TASK_STATES, JobStore } from '#types';
import { apiGetLatestParameterInfo } from '#utils/apiUrls';
import { request } from '#utils/request';
import { getEpochTimeDifference } from '#utils/timeUtils';
import StartJobModalBody from '#views/Job/overlays/StartJob';
import { navigate } from '@reach/router';
import { keyBy } from 'lodash';
import React, { useEffect, useState } from 'react';
import { jobActions } from './jobStore';

export function parseNewJobData(data: any) {
  const { checklist, parameterValues } = data;
  const stages: any = new Map();
  const tasks: any = new Map();
  const taskExecutions: any = new Map();
  const pendingTasks: JobStore['pendingTasks'] = new Set();

  let prevVisibleTaskExecutionId: string = '';
  let activeTaskExecutionId: string = '';

  const cjfValues = [...parameterValues].sort((a, b) => a.orderTree - b.orderTree);

  checklist?.stages?.forEach((stage) => {
    const _stage = {
      ...stage,
      tasks: [],
    };

    stage?.tasks?.forEach((task) => {
      _stage.tasks.push(task.id);

      const _task = {
        ...task,
        stageId: stage.id,
        taskExecutions: [],
      };

      task?.taskExecutions?.forEach((taskExecution: any) => {
        _task.taskExecutions.push(taskExecution.id);

        const _taskExecution = {
          ...taskExecution,
          taskId: task.id,
          previous: prevVisibleTaskExecutionId,
        };

        if (!_taskExecution.hidden) {
          if (!activeTaskExecutionId) {
            activeTaskExecutionId = taskExecution.id;
          }

          if (!(_taskExecution.state in COMPLETED_TASK_STATES)) {
            pendingTasks.add(_taskExecution.id);
          }

          if (prevVisibleTaskExecutionId) {
            const prevTaskExecution = taskExecutions.get(prevVisibleTaskExecutionId);
            taskExecutions.set(prevVisibleTaskExecutionId, {
              ...prevTaskExecution,
              next: _taskExecution.id,
            });
          }
          prevVisibleTaskExecutionId = _taskExecution.id;
        }

        taskExecutions.set(taskExecution.id, _taskExecution);
      });

      tasks.set(task.id, _task);
    });

    stages.set(stage.id, _stage);
  });

  return {
    stages,
    taskExecutions,
    tasks,
    activeTaskExecutionId,
    state: data.state,
    expectedEndDate: data.expectedEndDate,
    expectedStartDate: data.expectedStartDate,
    code: data.code,
    processId: checklist.id,
    processName: checklist.name,
    processCode: checklist.code,
    id: data.id,
    cjfValues,
    pendingTasks,
    showVerificationBanner: data.showVerificationBanner,
    showCorrectionBanner: data.showCorrectionBanner,
    showExceptionBanner: data.showExceptionBanner,
    showCJFExceptionBanner: data.showCJFExceptionBanner,
    forceCwe: data.forceCwe,
  };
}

// const isUserInvolvedInException = (exception: any, loggedInUserId: string) => {
//   const {
//     createdBy: { id: createdById },
//     reviewer,
//   } = exception;
//   return (
//     createdById === loggedInUserId ||
//     reviewer.some((currReviewer: any) => currReviewer.user.id === loggedInUserId)
//   );
// };

export const useJobStateToFlags = () => {
  const jobState = useTypedSelector((state) => state.job.state);
  const activeTask = useTypedSelector((state) => state.job.activeTask);

  const taskState = activeTask.taskExecution?.state;
  const reason = activeTask.taskExecution?.reason;

  //TODO: Deprecate isBlocked state for Job
  const [state, setState] = useState<{
    isBlocked?: boolean;
    isInProgress?: boolean;
    isCompleted?: boolean;
    isCompletedWithException?: boolean;
    isAssigned?: boolean;
    isJobStarted?: boolean;
    isJobCompleted?: boolean;

    isTaskStarted?: boolean;
    isTaskDelayed?: boolean;
    isTaskCompleted?: boolean;
    isTaskPaused?: boolean;
    isTaskCompletedWithException?: boolean;
    isTaskBlocked?: boolean;
  }>({});

  useEffect(() => {
    if (jobState && taskState) {
      setState({
        isBlocked: jobState === 'BLOCKED',
        isInProgress: jobState === 'IN_PROGRESS',
        isCompleted: jobState === 'COMPLETED',
        isCompletedWithException: jobState === 'COMPLETED_WITH_EXCEPTION',
        isAssigned: jobState !== 'UNASSIGNED',
        isJobStarted: jobState === 'IN_PROGRESS' || jobState === 'BLOCKED',
        isJobCompleted: jobState === 'COMPLETED' || jobState === 'COMPLETED_WITH_EXCEPTION',

        isTaskStarted: taskState in IN_PROGRESS_TASK_STATES || taskState in COMPLETED_TASK_STATES,
        isTaskCompleted: taskState in COMPLETED_TASK_STATES,
        isTaskDelayed: taskState === 'COMPLETED' && !!reason,
        isTaskPaused: taskState === 'PAUSED',
        isTaskCompletedWithException: taskState === 'COMPLETED_WITH_EXCEPTION',
        isTaskBlocked: taskState === 'BLOCKED',
      });
    }
  }, [jobState, taskState]);

  return state;
};

export const startJob = (id: string, expectedStartDate: number | null) => {
  const isJobStartingEarly = expectedStartDate
    ? getEpochTimeDifference(expectedStartDate) === 'EARLY'
    : false;

  window.store.dispatch(
    openOverlayAction({
      type: OverlayNames.CONFIRMATION_MODAL,
      props: {
        title: isJobStartingEarly ? 'Early Job Start Detected' : 'Start Job',
        primaryText: isJobStartingEarly ? 'Proceed anyway' : 'Start Job',
        secondaryText: isJobStartingEarly ? 'Go Back' : 'Cancel',
        body: (
          <StartJobModalBody
            isJobStartingEarly={isJobStartingEarly}
            expectedStartDate={expectedStartDate}
          />
        ),
        onPrimary: () =>
          window.store.dispatch(
            jobActions.startJob({
              id: id!,
            }),
          ),
      },
    }),
  );
};

export const navigateToTaskExecution = (
  jobId?: string,
  taskExecutionId?: string,
  parameterExecutionId?: string,
  redirectToInboxTab?: boolean,
) => {
  if (jobId && taskExecutionId) {
    let url = `/${
      window.store.getState().job.isInboxView || redirectToInboxTab ? 'inbox' : 'jobs'
    }/${jobId}?taskExecutionId=${taskExecutionId}`;

    if (parameterExecutionId) {
      url += `&parameterExecutionId=${parameterExecutionId}`;
    }

    navigate(url, { replace: true });
  }
};

export const getParametersInfo = async (jobId: string, parameterIds: string[]) => {
  const { data, errors } = await request('PATCH', apiGetLatestParameterInfo(jobId), {
    data: {
      parameterIds,
    },
  });

  if (errors) {
    return {};
  }

  if (data) {
    const _data = keyBy(data, 'id');
    return _data;
  }
};
