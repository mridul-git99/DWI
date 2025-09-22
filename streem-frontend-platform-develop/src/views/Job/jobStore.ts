import { Media } from '#PrototypeComposer/checklist.types';
import { generateActions } from '#store/helpers';
import {
  COMPLETED_TASK_STATES,
  ExceptionValues,
  JobStates,
  JobStore,
  StoreParameter,
  StoreTask,
  SupervisorResponse,
  TaskAction,
  TaskExecution,
} from '#types';
import { DEFAULT_PAGINATION } from '#utils/constants';
import { Pageable } from '#utils/globalTypes';
import { Verification } from '#views/Jobs/ListView/types';
import { produce } from 'immer';
import { keyBy } from 'lodash';

// ACTIONS

const actions = {
  pollJob: { id: '', initialExecutionId: '', parameterExecutionId: '' },
  pollJobSuccess: {
    data: {} as Omit<JobStore, 'loading' | 'isInboxView'>,
  },
  stopPollJob: undefined,
  setUpdating: { updating: false },
  setMovedToNextTask: { movedToNextTask: false, taskCompletionEpoch: 0 },
  startJob: { id: '' },
  completeJob: {} as {
    jobId: string;
    withException?: boolean;
    values?: ExceptionValues;
    details?: {
      code: string;
      name?: string;
    };
  },
  startJobSuccess: undefined,
  updateTask: {} as { id: string; data: StoreTask },
  updateTaskExecution: { id: '', data: {} as TaskExecution },
  updateTaskExecutions: {
    taskId: '',
    data: {} as TaskExecution,
    action: '' as 'add' | 'remove',
    currentTaskExecutionId: '',
  },
  addTaskExecution: { taskId: '', data: {} as TaskExecution },
  performTaskAction: {} as {
    id: string;
    reason?: string;
    action: TaskAction;
    createObjectAutomations?: any[];
    continueRecurrence?: boolean;
    recurringOverdueCompletionReason?: string;
    recurringPrematureStartReason?: string;
    scheduleOverdueCompletionReason?: string;
    schedulePrematureStartReason?: string;
    openAutomationModal?: any;
  },
  repeatTask: {} as { id: string },
  removeRepeatTask: {} as { taskExecutionId: string },
  endTaskRecurrence: {} as { taskExecutionId: string },
  togglePauseResume: {} as {
    id: string;
    reason?: string;
    comment?: string;
    isTaskPaused?: boolean;
  },
  updateErrors: {
    taskErrors: [] as string[],
    parametersErrors: {} as Map<string, string[]>,
  },
  executeParameter: {} as {
    parameter: StoreParameter;
    reason?: string;
  },
  setExecutingParameterIds: {} as { id: string; value: boolean },
  approveRejectParameter: {} as {
    parameterId: string;
    parameterResponseId: string;
    type: SupervisorResponse;
  },
  updateParameter: {} as { data: StoreParameter },
  initiateSelfVerification: {} as { parameterResponseId: string; parameterId: string },
  completeSelfVerification: {} as {
    parameterResponseId: string;
    parameterId: string;
    password?: string;
    code?: string;
    state?: string;
  },
  updateParameterVerifications: {} as {
    parameterResponseId: string;
    parameterId: string;
    data: Verification;
  },
  sendPeerVerification: {} as { parameterResponseId: string; parameterId: string; data: any },
  recallPeerVerification: {} as {
    parameterResponseId: string;
    parameterId: string;
    type: 'self' | 'peer';
  },
  acceptPeerVerification: {} as {
    parameterResponseId: string;
    parameterId: string;
    password?: string;
    code?: string;
    state?: string;
  },
  rejectPeerVerification: {} as {
    parameterResponseId: string;
    parameterId: string;
    comment: string;
  },
  // Same Session Verification Actions
  initiateSameSessionVerification: {} as {
    parameterResponseId: string;
    parameterId: string;
    verifierId: string;
    password: string;
  },
  acceptSameSessionVerification: {} as {
    parameterResponseId: string;
    parameterId: string;
    verifierId: string;
    password: string;
  },
  rejectSameSessionVerification: {} as {
    parameterResponseId: string;
    parameterId: string;
    verifierId: string;
    reason: string;
  },
  reset: undefined,
  startPollActiveStageData: {} as { jobId: string; stageId: string; state: JobStates },
  stopPollActiveStageData: undefined,
  startPollingJobData: {} as { jobId: string },
  stopPollingJobData: undefined,
  getActiveStageDataSuccess: { data: {} as any },
  setTimerState: {} as JobStore['timerState'],
  startTaskTimer: {} as { id: string },
  stopTaskTimer: undefined,
  toggleMobileDrawer: undefined,
  getJobAuditLogs: {} as { jobId: string; params: Record<string, any> },
  getJobAuditLogsSuccess: { data: [] as any, pageable: {} as Pageable },
  enableErrorCorrectionOnTask: {} as { taskExecutionId: string },
  cancelErrorCorrectionOnTask: {} as { taskExecutionId: string },
  initiateErrorCorrectionOnParameter: {} as {
    parameterResponseId: string;
    initiatorReason: string;
    correctors: Record<string, string[]>;
    reviewers: Record<string, string[]>;
    password?: string;
    code?: string;
    state?: string;
  },
  initiateExceptionOnParameter: {} as {
    approver: {
      userId: string[];
      userGroupId: string[];
    };
    initiatorReason: string;
    parameterResponseId: string;
    password?: string;
    value?: string | number;
    choices?: any;
    closeOverlayFn?: () => void;
    code?: string;
    state?: string;
  },
  submitExceptionOnParameter: {} as {
    parameterResponseId: string;
    exceptionId: string;
    reviewerReason: string;
    password?: string;
    reviewStatus: string;
    closeOverlayFn?: () => void;
    code?: string;
    state?: string;
    isCjfException: boolean;
    jobId: string;
    rulesId: string;
  },
  performErrorCorrectionOnParameter: {} as {
    correctionId: string;
    parameterResponseId: string;
    newValue?: string;
    newChoice?: any[];
    correctorReason: string;
    medias?: Media[];
    password?: string;
    code?: string;
    state?: string;
  },
  approveRejectErrorCorrectionOnParameter: {} as {
    correctionId: string;
    parameterResponseId: string;
    reviewerReason: string;
    performCorrectionStatus: string;
    password?: string;
    code?: string;
    state?: string;
  },
  autoAcceptExceptionOnParameter: {} as {
    reason: string;
    parameterResponseId: string;
    value?: string | number;
    choices?: any;
    closeOverlayFn?: () => void;
  },
  sendBulkPeerVerification: {} as {
    values: any[];
  },
  completeBulkSelfVerification: {} as {
    values: any[];
    password?: string;
    code?: string;
    state?: string;
  },
  completeBulkPeerVerification: {} as {
    values: any[];
    password?: string;
    code?: string;
    state?: string;
  },
  updateBulkParameterVerifications: {} as { ids: Record<string, string>; data: Verification[] },
  pollActiveTaskExecution: {} as { taskExecutionId: string },
  pollActiveTaskExecutionSuccess: {} as { data: any },
  stopPollActiveTaskExecution: undefined,
  resetActiveTask: undefined,
  onSuccessErrorsHandler: {} as { parameter: any },
  initiateBulkExceptionsOnParameter: {} as {
    parametersWithException: any;
    password?: string;
    code?: string;
    state?: string;
    closeOverlayFn?: () => void;
  },
  fetchActionsForProcess: {} as { checklistId: string },
  updateTaskActionsAndEffects: {} as { data: any },
};

export const initialState: JobStore = {
  loading: true,
  stages: new Map(),
  tasks: new Map(),
  taskExecutions: new Map(),
  pendingTasks: new Set(),
  cjfValues: [],
  isInboxView: false,
  taskNavState: {
    isMobileDrawerOpen: false,
  },
  showVerificationBanner: false,
  showCorrectionBanner: false,
  showExceptionBanner: false,
  timerState: {
    earlyCompletion: false,
    limitCrossed: false,
    timeElapsed: -1,
  },
  auditLogs: {
    logs: [],
    loading: true,
    pageable: DEFAULT_PAGINATION,
  },
  executingParameterIds: {},
  activeTask: {
    loading: true,
    parameters: new Map(),
    isTaskAssigned: false,
    stageOrderTree: 0,
  },
  errors: {
    parametersErrors: new Map(),
    taskErrors: [],
  },
  showCJFExceptionBanner: false,
  forceCwe: false,
  tasksActionsAndEffects: {},
};

export const { actions: jobActions, actionsEnum: JobActionsEnum } = generateActions(
  actions,
  '@@leucine/jobs/entity/',
);

export type JobActionsType = ReturnType<typeof jobActions[keyof typeof jobActions]>;

// REDUCER FUNCTIONS

function pollJobSuccess(draft: JobStore, payload: typeof actions.pollJobSuccess) {
  const { data } = payload;
  const isInboxView = location.pathname.split('/')[1] === 'inbox';

  draft.loading = false;
  draft.state = data.state;
  draft.cjfValues = data.cjfValues;
  draft.stages = new Map(data.stages);
  draft.tasks = new Map(data.tasks);
  draft.taskExecutions = new Map(data.taskExecutions);
  draft.expectedEndDate = data.expectedEndDate;
  draft.expectedStartDate = data.expectedStartDate;
  draft.code = data.code;
  draft.processId = data.processId;
  draft.processName = data.processName;
  draft.processCode = data.processCode;
  draft.id = data.id;
  draft.pendingTasks = new Set(data.pendingTasks);
  draft.isInboxView = isInboxView;
  draft.showVerificationBanner = data.showVerificationBanner;
  draft.showCorrectionBanner = data.showCorrectionBanner;
  draft.showExceptionBanner = data.showExceptionBanner;
  draft.showCJFExceptionBanner = data.showCJFExceptionBanner;
  draft.forceCwe = data.forceCwe;
}

function pollActiveTaskExecutionSuccess(
  draft: JobStore,
  payload: typeof actions.pollActiveTaskExecutionSuccess,
) {
  const { isTaskAssigned, ...rest } = payload.data;
  const task = draft.tasks.get(payload.data.id);
  let stageOrderTree = 0;
  let stageId = '';
  if (task) {
    stageId = task.stageId!;
    const stage = draft.stages.get(task.stageId!);
    stageOrderTree = stage!.orderTree;
  }
  draft.activeTask = {
    ...rest,
    stageOrderTree,
    stageId,
    isTaskAssigned: isTaskAssigned ? isTaskAssigned : draft.activeTask.isTaskAssigned,
  };
}

function updateTask(draft: JobStore, payload: typeof actions.updateTask) {
  const task = draft.tasks.get(payload.id);
  if (task) {
    draft.tasks.set(payload.id, { ...task, ...payload.data });
  }
}

function updateTaskExecutions(draft: JobStore, payload: typeof actions.updateTaskExecutions) {
  const currentTaskExecution = draft.taskExecutions.get(payload.currentTaskExecutionId)!;

  if (payload.action === 'remove') {
    if (currentTaskExecution.previous) {
      const previousTaskExecution = draft.taskExecutions.get(currentTaskExecution.previous)!;
      draft.taskExecutions.set(currentTaskExecution.previous, {
        ...previousTaskExecution,
        next: currentTaskExecution.next,
      });
    }
    if (currentTaskExecution.next) {
      const nextTaskExecution = draft.taskExecutions.get(currentTaskExecution.next)!;
      draft.taskExecutions.set(currentTaskExecution.next, {
        ...nextTaskExecution,
        previous: currentTaskExecution.previous,
      });
    }
    draft.taskExecutions.delete(currentTaskExecution.id);
  } else {
    draft.taskExecutions.set(payload.data.id, {
      id: payload.data.id,
      orderTree: payload.data.orderTree,
      state: payload.data.state,
      type: payload.data.type,
      hidden: false,
      taskId: payload.taskId,
      next: currentTaskExecution.next,
      previous: currentTaskExecution.id,
    });
    draft.taskExecutions.set(currentTaskExecution.id, {
      ...currentTaskExecution,
      next: payload.data.id,
    });
    if (currentTaskExecution.next) {
      const nextTaskExecution = draft.taskExecutions.get(currentTaskExecution.next)!;
      draft.taskExecutions.set(currentTaskExecution.next, {
        ...nextTaskExecution,
        previous: payload.data.id,
      });
    }
  }
}

function updateTaskExecution(draft: JobStore, payload: typeof actions.updateTaskExecution) {
  if (draft.activeTask.taskExecution?.id === payload.id) {
    draft.activeTask.taskExecution = {
      ...draft.activeTask.taskExecution,
      ...payload.data,
    };
  }

  const taskExecution = draft.taskExecutions.get(payload.id);

  if (taskExecution) {
    draft.taskExecutions.set(payload.id, {
      ...taskExecution,
      state: payload.data.state,
    });

    draft.taskExecutions.set(payload.id, { ...taskExecution, ...payload.data });
  }

  if (payload.data.state in COMPLETED_TASK_STATES) {
    draft.pendingTasks.delete(payload.id);
  }
}

function updateErrors(draft: JobStore, payload: typeof actions.updateErrors) {
  draft.errors.parametersErrors = new Map(payload.parametersErrors);
  draft.errors.taskErrors = payload.taskErrors;
}

function updateHiddenFlag(
  parametersMap: Map<string, StoreParameter>,
  responseId: string,
  hidden: boolean,
) {
  parametersMap.forEach((parameter) => {
    if (parameter.response.id === responseId) {
      parameter.response.hidden = hidden;
    }
  });
}

function updateParameter(draft: JobStore, payload: typeof actions.updateParameter) {
  const { data } = payload;

  if (data?.response[0]?.taskExecutionId === draft.activeTask.taskExecution?.id) {
    draft.activeTask.parameters.set(data.id, {
      ...data,
      response: data.response[0],
    });
  }

  if (data?.show?.length) {
    data.show.forEach((responseId) => {
      updateHiddenFlag(draft.activeTask.parameters, responseId, false);
    });
  }

  if (data?.hide?.length) {
    data.hide.forEach((responseId) => {
      updateHiddenFlag(draft.activeTask.parameters, responseId, true);
    });
  }
}

function updateParameterVerifications(
  draft: JobStore,
  payload: typeof actions.updateParameterVerifications,
) {
  const { data, parameterId } = payload;
  let response = draft.activeTask.parameters.get(parameterId)?.response;
  if (response) {
    if (response.parameterVerifications?.length) {
      response.parameterVerifications = (response.parameterVerifications || []).map(
        (verification: any) => {
          if (verification.verificationType === data.verificationType) {
            return data;
          }
          return verification;
        },
      );
    } else {
      response.parameterVerifications = [data];
    }

    if (data.evaluationState) {
      response.state = data.evaluationState;
    }
  }
}

function getJobAuditLogsSuccess(draft: JobStore, payload: typeof actions.getJobAuditLogsSuccess) {
  const existingLogs = draft.auditLogs.logs;
  draft.auditLogs.loading = false;
  draft.auditLogs.pageable = payload.pageable;
  draft.loading = false;
  draft.auditLogs.logs =
    payload.pageable.page === 0 ? payload.data : [...existingLogs, ...payload.data];
}

// REDUCER

export const jobReducer = (state = initialState, action: JobActionsType) =>
  produce(state, (draft) => {
    switch (action.type) {
      case JobActionsEnum.pollJob:
        draft.loading = true;
        break;
      case JobActionsEnum.pollJobSuccess:
        pollJobSuccess(draft, action.payload);
        break;
      case JobActionsEnum.pollActiveTaskExecution:
        draft.activeTask.loading = true;
        break;
      case JobActionsEnum.pollActiveTaskExecutionSuccess:
        pollActiveTaskExecutionSuccess(draft, action.payload);
        break;
      case JobActionsEnum.resetActiveTask:
        draft.activeTask = initialState.activeTask;
        draft.errors = initialState.errors;
        break;
      case JobActionsEnum.setExecutingParameterIds:
        draft.executingParameterIds = {
          ...draft.executingParameterIds,
          [action.payload.id]: action.payload.value,
        };
        break;
      case JobActionsEnum.executeParameter:
      case JobActionsEnum.startJob:
      case JobActionsEnum.completeJob:
      case JobActionsEnum.togglePauseResume:
      case JobActionsEnum.performTaskAction:
      // case JobActionsEnum.startPollActiveStageData:
      case JobActionsEnum.repeatTask:
      case JobActionsEnum.removeRepeatTask:
      case JobActionsEnum.endTaskRecurrence:
        draft.updating = true;
        break;
      case JobActionsEnum.setUpdating:
        draft.updating = action.payload.updating;
        break;
      case JobActionsEnum.updateTask:
        updateTask(draft, action.payload);
        break;
      case JobActionsEnum.updateTaskExecutions:
        updateTaskExecutions(draft, action.payload);
        break;
      case JobActionsEnum.updateTaskExecution:
        updateTaskExecution(draft, action.payload);
        break;
      case JobActionsEnum.updateErrors:
        updateErrors(draft, action.payload);
        break;
      case JobActionsEnum.startJobSuccess:
        draft.state = 'IN_PROGRESS';
        break;
      case JobActionsEnum.updateParameter:
        updateParameter(draft, action.payload);
        break;
      case JobActionsEnum.updateParameterVerifications:
        updateParameterVerifications(draft, action.payload);
        break;
      case JobActionsEnum.updateBulkParameterVerifications:
        const { ids, data } = action.payload;
        const dataSetByExecutionId: Record<string, any> = keyBy(data, 'parameterExecutionId');
        for (let key in ids) {
          updateParameterVerifications(draft, {
            parameterId: ids?.[key],
            data: dataSetByExecutionId?.[key],
          });
        }
        break;
      case JobActionsEnum.setTimerState:
        draft.timerState = action.payload;
        break;
      case JobActionsEnum.toggleMobileDrawer:
        draft.taskNavState.isMobileDrawerOpen = !draft.taskNavState.isMobileDrawerOpen;
        break;
      case JobActionsEnum.reset:
        return initialState;
      case JobActionsEnum.getJobAuditLogsSuccess:
        getJobAuditLogsSuccess(draft, action.payload);
        break;
      case JobActionsEnum.updateTaskActionsAndEffects:
        draft.tasksActionsAndEffects = action.payload.data;
        break;
      default:
        break;
    }
  });
