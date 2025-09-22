import {
  Parameter,
  ScheduledTaskCondition,
  ScheduledTaskType,
  Stage,
  Task as TaskType,
} from '#types';
import { Error, FileUploadData } from '#utils/globalTypes';
import { Checklist } from '../checklist.types';

export type Task = TaskType & {
  stageId: Stage['id'];
  errors: Error[];
  hasExecutorLock: boolean;
  hasPrerequisites: boolean;
  interlocks: boolean;
  hasBulkVerification: boolean;
};

export type TaskCardProps = {
  index: number;
  task: Task;
  isActive: boolean;
};

export type TaskCardWrapperProps = {
  isActive: boolean;
  isReadOnly: boolean;
};

export type TaskMediasProps = {
  medias: Task['medias'];
  taskId?: Task['id'];
  parameterId?: Parameter['id'];
  isParameter?: boolean;
  isTaskCompleted?: boolean;
  isCorrectingError?: boolean;
  execute?: (data: MediaDetails, isDeleting?: boolean) => void;
  uploadedMedia?: any;
  setCorrectedParameterValues?: React.Dispatch<React.SetStateAction<any>>;
};

export type AddNewTaskType = {
  checklistId: Checklist['id'];
  stageId: Stage['id'];
  orderTree?: number;
  type?: string;
  data?: Record<string, string>;
  name?: string;
};

export type SetTaskTimerType = {
  maxPeriod: number;
  minPeriod?: number;
  taskId: Task['id'];
  timerOperator: string;
};

export type SetTaskRecurrenceType = {
  taskId: Task['id'];
  startDateDuration: Record<string, number>;
  startDateInterval: number;
  dueDateDuration: Record<string, number>;
  dueDateInterval: number;
  positiveStartDateToleranceDuration: Record<string, number>;
  negativeStartDateToleranceDuration: Record<string, number>;
  positiveDueDateToleranceDuration: Record<string, number>;
  negativeDueDateToleranceDuration: Record<string, number>;
  positiveStartDateToleranceInterval: number;
  negativeStartDateToleranceInterval: number;
  positiveDueDateToleranceInterval: number;
  negativeDueDateToleranceInterval: number;
  updating: boolean;
};

export type SetTaskScheduleType = {
  taskId: Task['id'];
  type: ScheduledTaskType;
  condition: ScheduledTaskCondition;
  referencedTaskId: string | null;
  scheduledTaskId: string;
  startDateDuration: Record<string, number>;
  startDateInterval: number;
  dueDateDuration: Record<string, number>;
  dueDateInterval: number;
};

export type MediaDetails = FileUploadData & {
  name: string;
  description?: string;
  id?: string;
};

export type AddMediaType = {
  mediaDetails: MediaDetails;
  taskId: Task['id'];
};

export type UpdateMediaType = {
  taskId: Task['id'];
  parameterId: Parameter['id'];
  mediaId: MediaDetails['mediaId'];
  mediaDetails: Pick<MediaDetails, 'name' | 'description'>;
};

export type AddActionType = {
  action: any;
  taskId: Task['id'];
};

export type UpdateActionType = {
  taskId: Task['id'];
  action: any;
  actionId: string;
};

export type ArchiveActionType = {
  taskId: Task['id'];
  actionId: string;
  setFormErrors: (errors?: Error[]) => void;
};

export enum TaskErrors {
  E210 = 'TASK_NAME_CANNOT_BE_EMPTY',
  E211 = 'TASK_SHOULD_HAVE_ATLEAST_ONE_EXECUTABLE_PARAMETER',
  E225 = 'TASK_AUTOMATION_INVALID_MAPPED_PARAMETERS',
  E246 = 'TASK_BULK_VERIFICATION_INVALID_MAPPED_PARAMETERS',
  E2303 = 'START_TASK_INTERLOCK_NOT_ALLOWED_FOR_SAME_TASK_PARAMETER',
}

export enum TaskTimerErrorCodes {
  E217 = 'TIMED_TASK_LT_TIMER_CANNOT_BE_ZERO',
  E218 = 'TIMED_TASK_NLT_MIN_PERIOD_CANNOT_BE_ZERO',
  E219 = 'TIMED_TASK_NLT_MAX_PERIOD_SHOULD_BE_GT_MIN_PERIOD',
}

export enum TaskTypeEnum {
  DYNAMIC = 'DYNAMIC',
  SUBPROCESS = 'SUBPROCESS',
  STATIC = 'STATIC',
}

export enum ProcessLevelOptions {
  DEPENDENCIES = 'Dependencies',
  AUTOMATION = 'Task Automations',
  TIMED = 'Timed Condition',
  MEDIA = 'Media',
  INTERLOCKS = 'Parameter Conditions',
  RECURRENCE = 'Task Recurrence',
  SCHEDULE = 'Schedule Task',
  SOLO_TASK = 'Solo Task Lock',
  EXECUTOR_LOCK = 'Task Executor Lock',
  BULK_VERIFICATION = 'Bulk Verification',
}

export enum ProcessConfigurations {
  BULK_VERIFICATION = 'BULK_VERIFICATION',
}
