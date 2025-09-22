import {
  AutomationAction,
  AutomationActionActionType,
  Media,
  TimerOperator,
} from '#PrototypeComposer/checklist.types';
import { User } from '#store/users/types';
import { Audit, ObjectKeys, PartialUser } from './common';
import { Parameter } from './parameter';

export const NOT_STARTED_TASK_STATES = {
  NOT_STARTED: {},
} as const;

export const IN_PROGRESS_TASK_STATES = {
  IN_PROGRESS: {},
  BLOCKED: {},
  PAUSED: {},
} as const;

export const COMPLETED_TASK_STATES = {
  COMPLETED: {},
  COMPLETED_WITH_EXCEPTION: {},
  SKIPPED: {},
} as const;

export const TASK_EXECUTION_STATES = {
  ...NOT_STARTED_TASK_STATES,
  ...IN_PROGRESS_TASK_STATES,
  ...COMPLETED_TASK_STATES,
} as const;

export type NotStartedTaskStates = ObjectKeys<typeof NOT_STARTED_TASK_STATES>;
export type InProgressTaskStates = ObjectKeys<typeof IN_PROGRESS_TASK_STATES>;
export type CompletedTaskStates = ObjectKeys<typeof COMPLETED_TASK_STATES>;
export type TaskExecutionStates = ObjectKeys<typeof TASK_EXECUTION_STATES>;

export const TASK_TYPE = {
  DYNAMIC: {},
  STATIC: {},
} as const;

export type TaskType = ObjectKeys<typeof TASK_TYPE>;

export enum TaskExecutionType {
  MASTER = 'MASTER',
  REPEAT = 'REPEAT',
  RECURRING = 'RECURRING',
}

export enum TaskPauseReasons {
  BIO_BREAK = 'Bio break',
  SHIFT_CHANGE = 'Shift change',
  LUNCH_DINNER_TEA_BREAK = 'Lunch/ Dinner/ Tea break',
  NO_MANPOWER = 'No manpower',
  DAY_END_WEEK_OFF = 'Day end/ Week off',
  BREAKDOWN_AREA_EQUIPMENT_OTHER = 'Breakdown (Area/ Equipment/Other)',
  EMERGENCY = 'Emergency',
  FIRE_ALARM = 'Fire alarm',
  OTHER = 'Others',
}

export type ObjectAutomationResponseDto = {
  createdObjectExternalId: string;
  createdObjectId: string;
};

export type TExecutedAutomation = {
  id: string;
  actionType: AutomationActionActionType;
  createObjectAutomationResponseDto?: ObjectAutomationResponseDto[];
  propertyDisplayName: string | null;
};

export interface TaskExecution {
  id: string;
  period: any;
  correctionEnabled: boolean;
  reason: string;
  assignees: User[];
  startedBy: Record<string, string>;
  startedAt: number;
  endedAt: number;
  endedBy: Record<string, string>;
  state: TaskExecutionStates;
  audit: Audit;
  hide: string[];
  show: string[];
  correctedBy: PartialUser;
  correctedAt: number;
  duration: number;
  pauseReasons: Record<string, string>[];
  type: TaskExecutionType;
  errors: string[];
  executedAutomations: TExecutedAutomation[];
  orderTree: number;
}

export interface Task {
  parameters: Parameter[];
  code: string;
  hasStop: boolean;
  id: string;
  mandatory: boolean;
  maxPeriod?: number;
  medias: Media[];
  minPeriod?: number;
  name: string;
  orderTree: number;
  taskExecutions: TaskExecution[];
  timed: boolean;
  timerOperator: TimerOperator;
  automations: AutomationAction[];
  hidden: boolean;
  parentTaskId?: string;
  showInJob?: boolean;
  type?: TaskType;
  data?: {
    parameterId: string;
  };
  enableRecurrence?: boolean;
  enableScheduling?: boolean;
  soloTask: boolean;
}

export interface StoreTask extends Omit<Task, 'parameters' | 'taskExecutions'> {
  stageId?: string;
  visibleTaskExecutionsCount: number;
  canSkipTask: boolean;
  parameters: string[];
  taskExecutions: string[];
  taskExecution?: StoreTaskExecution;
  taskRecurrence?: Record<string, any>;
}

export enum TaskAction {
  START = 'start',
  COMPLETE = 'complete',
  SKIP = 'skip',
  COMPLETE_WITH_EXCEPTION = 'complete-with-exception',
  ENABLE_ERROR_CORRECTION = 'correction/start',
  COMPLETE_ERROR_CORRECTION = 'correction/complete',
  CANCEL_ERROR_CORRECTION = 'correction/cancel',
}

// From Job Composer
export enum TaskErrors {
  E201 = 'TASK_INCOMPLETE',
  E202 = 'TASK_NOT_FOUND',
  E242 = 'SOLO_TASK_LOCKED',
  E2301 = 'INTERLOCK_NOT_FOUND_FOR_TASK',
  E2302 = 'INTERLOCK_INPUT_CANNOT_BE_EMPTY',
  E2304 = 'INTERLOCK_CONDITION_CANNOT_BE_VALIDATED_DUE_TO_MISSING_RESOURCES',
  E2307 = 'NUMBER_INTERLOCK_VALIDATION_ERROR',
  E2308 = 'SINGLE_SELECT_INTERLOCK_VALIDATION_ERROR',
  E2309 = 'DATE_INTERLOCK_VALIDATION_ERROR',
  E2310 = 'DATE_TIME_INTERLOCK_VALIDATION_ERROR',
  E496 = 'CORRECTION_CANNOT_DISABLE',
}
export interface StoreTaskExecution extends TaskExecution {
  previous?: string;
  next?: string;
  taskId: string;
  stageId: string;
  isUserAssignedToTask: boolean;
  parameterResponses?: string[];
  visibleParametersCount: number;
  scheduleTaskSummary?: string;
  errors: string[];
  pendingSelfVerificationParameters: any[];
  pendingPeerVerificationParameters: any[];
  executedParametersWithPeerVerification: any[];
}

export enum ScheduledTaskType {
  TASK = 'TASK',
  JOB = 'JOB',
}

export enum ScheduledTaskCondition {
  START = 'START',
  COMPLETE = 'COMPLETE',
}

export const TaskActionType = {
  BULK_SELF_VERIFICATION: 'BULK_SELF_VERIFICATION',
  BULK_PEER_VERIFICATION: 'BULK_PEER_VERIFICATION',
} as const;
