import { Property } from '#store/properties/types';
import { User } from '#store/users/types';
import { Audit, Parameter, PartialUser, Stage } from '#types';
import { InputTypes } from '#utils/globalTypes';
import { Collaborator, CollaboratorState } from './reviewer.types';

// TODO : merge (job composer & prototype composer) types.

export type ChecklistProperty = Pick<Property, 'id' | 'name' | 'value' | 'label'>;

export type Media = {
  id: string;
  name: string;
  link: string;
  type: string;
  filename: string;
  description: string;
  archived: boolean;
  reason: string;
  mediaId: string;
  originalFilename: string;
};

export enum TaskExecutionState {
  COMPLETED = 'COMPLETED',
  COMPLETED_WITH_EXCEPTION = 'COMPLETED_WITH_EXCEPTION',
  IN_PROGRESS = 'IN_PROGRESS',
  NOT_STARTED = 'NOT_STARTED',
  SKIPPED = 'SKIPPED',
  PAUSED = 'PAUSED',
}

export type TaskExecution = {
  audit: Audit;
  id: string;
  period: number;
  reason: string;
  startedBy: PartialUser;
  startedAt: string;
  state: TaskExecutionState;
  assignees: User[];
};

export enum TimerOperator {
  LESS_THAN = 'LESS_THAN',
  NOT_LESS_THAN = 'NOT_LESS_THAN',
}

export enum EnabledStates {
  BEING_BUILT = 'BEING_BUILT',
  BEING_REVISED = 'BEING_REVISED',
  REQUESTED_CHANGES = 'REQUESTED_CHANGES',
}

export enum DisabledStates {
  SUBMITTED_FOR_REVIEW = 'SUBMITTED_FOR_REVIEW',
  BEING_REVIEWED = 'BEING_REVIEWED',
  READY_FOR_SIGNING = 'READY_FOR_SIGNING',
  SIGN_OFF_INITIATED = 'SIGN_OFF_INITIATED',
  SIGNING_IN_PROGRESS = 'SIGNING_IN_PROGRESS',
  READY_FOR_RELEASE = 'READY_FOR_RELEASE',
  PUBLISHED = 'PUBLISHED',
  DEPRECATED = 'DEPRECATED',
}

export type AllChecklistStates = EnabledStates | DisabledStates;

export const ChecklistStates = {
  ...EnabledStates,
  ...DisabledStates,
};

export enum ChecklistStatesContent {
  BEING_BUILT = 'Being Built',
  SUBMITTED_FOR_REVIEW = 'Submitted For Review',
  BEING_REVIEWED = 'Being Reviewed',
  BEING_REVISED = 'Being Revised',
  REQUESTED_CHANGES = 'Requested Changes',
  READY_FOR_SIGNING = 'Ready for Signing',
  SIGN_OFF_INITIATED = 'Under Signing',
  SIGNING_IN_PROGRESS = 'Signing in progress',
  READY_FOR_RELEASE = 'Ready For Release',
  PUBLISHED = 'Published',
  DEPRECATED = 'Deprecated',
}

export enum ChecklistStatesColors {
  BEING_BUILT = '#1d84ff',
  SUBMITTED_FOR_REVIEW = '#f7b500',
  BEING_REVIEWED = '#f7b500',
  BEING_REVISED = '#f7b500',
  REQUESTED_CHANGES = '#f7b500',
  READY_FOR_SIGNING = '#5aa700',
  SIGN_OFF_INITIATED = '#5aa700',
  SIGNING_IN_PROGRESS = '#5aa700',
  READY_FOR_RELEASE = '#5aa700',
  PUBLISHED = '#5aa700',
  DEPRECATED = '#FF6B6B',
}

export type Comment = {
  id: string;
  comments: string;
  commentedAt: number;
  modifiedAt: number;
  commentedBy: Pick<User, 'id' | 'firstName' | 'lastName' | 'employeeId'>;
  phase: number;
  state: CollaboratorState;
};

export type JobLogColumnType = {
  id: string;
  type: LogType;
  displayName: string;
  triggerType: TriggerTypeEnum;
  orderTree: number;
  pinned?: boolean;
};

export type Checklist = {
  description: string;
  id: string;
  name: string;
  code: string;
  state: EnabledStates | DisabledStates;
  versionNumber: number;
  archived?: boolean;
  stages: Stage[];
  properties?: ChecklistProperty[];
  audit: Audit;
  phase?: number;
  comments: Comment[];
  collaborators: Collaborator[];
  jobLogColumns: JobLogColumnType[];
  noOfJobs?: number;
  noOfTasks?: number;
  version: number | null;
  global: boolean;
  parameters: Parameter[];
  colorCode: string | null;
};

export enum LogType {
  DATE = 'DATE',
  FILE = 'FILE',
  TEXT = 'TEXT',
  DATE_TIME = 'DATE_TIME',
  TIME = 'TIME',
}

export enum TriggerTypeEnum {
  JOB_ID = 'JOB_ID',
  CHK_ID = 'CHK_ID',
  CHK_NAME = 'CHK_NAME',
  JOB_STATE = 'JOB_STATE',
  JOB_CREATED_AT = 'JOB_CREATED_AT',
  JOB_CREATED_BY = 'JOB_CREATED_BY',
  JOB_MODIFIED_BY = 'JOB_MODIFIED_BY',
  JOB_STARTED_BY = 'JOB_STARTED_BY',
  JOB_START_TIME = 'JOB_START_TIME',
  JOB_END_TIME = 'JOB_END_TIME',
  JOB_ENDED_BY = 'JOB_ENDED_BY',
  TSK_STARTED_BY = 'TSK_STARTED_BY',
  TSK_START_TIME = 'TSK_START_TIME',
  PARAMETER_VALUE = 'PARAMETER_VALUE',
  RESOURCE = 'RESOURCE',
  RESOURCE_PARAMETER = 'RESOURCE_PARAMETER',
  PARAMETER_SELF_VERIFIED_AT = 'PARAMETER_SELF_VERIFIED_AT',
  PARAMETER_SELF_VERIFIED_BY = 'PARAMETER_SELF_VERIFIED_BY',
  PARAMETER_PEER_VERIFIED_AT = 'PARAMETER_PEER_VERIFIED_AT',
  PARAMETER_PEER_VERIFIED_BY = 'PARAMETER_PEER_VERIFIED_BY',
  PARAMETER_PEER_STATUS = 'PARAMETER_PEER_STATUS',
  ANNOTATION_MEDIA = 'ANNOTATION_MEDIA',
  ANNOTATION_REMARK = 'ANNOTATION_REMARK',
}

export enum ParameterVerificationTypeEnum {
  SELF = 'SELF',
  PEER = 'PEER',
  BOTH = 'BOTH',
  NONE = 'NONE',
}

export enum AutomationActionActionType {
  INCREASE_PROPERTY = 'INCREASE_PROPERTY',
  DECREASE_PROPERTY = 'DECREASE_PROPERTY',
  CREATE_OBJECT = 'CREATE_OBJECT',
  BULK_CREATE_OBJECT = 'BULK_CREATE_OBJECT',
  SET_PROPERTY = 'SET_PROPERTY',
  ARCHIVE_OBJECT = 'ARCHIVE_OBJECT',
  SET_RELATION = 'SET_RELATION',
}

export type AutomationActionDetails = {
  value?: number;
  sortOrder: number;
  parameterId?: string;
  propertyId: string;
  propertyInputType: InputTypes;
  propertyExternalId: string;
  propertyDisplayName: string;
  relationId: number;
  urlPath: string;
  collection: string;
  objectTypeId: string;
  objectTypeExternalId: string;
  objectTypeDisplayName: string;
  referencedParameterId: string;
  choices?: any[];
  entityId?: string;
  entityType?: string;
  dateUnit?: string;
  captureProperty?: string;
  selector?: string;
  configuration?: any[];
};

export enum AutomationActionTriggerType {
  TASK_STARTED = 'TASK_STARTED',
  TASK_COMPLETED = 'TASK_COMPLETED',
}

export enum AutomationActionType {
  PROCESS_BASED = 'PROCESS_BASED',
  OBJECT_BASED = 'OBJECT_BASED',
}

export enum AutomationTargetEntityType {
  OBJECT = 'OBJECT',
  RESOURCE_PARAMETER = 'RESOURCE_PARAMETER',
}

export type AutomationAction = {
  id: string;
  type: AutomationActionType;
  actionType: AutomationActionActionType;
  actionDetails: AutomationActionDetails;
  triggerType: AutomationActionTriggerType;
  displayName: string;
  orderTree: number;
};

export enum AutomationActionActionTypeVisual {
  INCREASE_PROPERTY = 'Increase',
  DECREASE_PROPERTY = 'Decrease',
  CREATE_OBJECT = 'Create Object',
  BULK_CREATE_OBJECT = 'Bulk Create Objects',
  SET_PROPERTY = 'Set Property',
  ARCHIVE_OBJECT = 'Archive Object',
  SET_RELATION = 'Set Relation',
}

export enum AutomationActionTriggerTypeVisual {
  TASK_COMPLETED = 'task is completed',
  TASK_STARTED = 'task is started',
}

export enum ParameterExceptionTypeEnum {
  NONE = 'NONE',
  DEFAULT_FLOW = 'DEFAULT_FLOW',
  ACCEPT_WITH_REASON_FLOW = 'ACCEPT_WITH_REASON_FLOW',
  APPROVER_REVIEWER_FLOW = 'APPROVER_REVIEWER_FLOW',
}

export enum AutomationActionConfigurationDataType {
  OBJECT = 'OBJECT',
  ARRAY = 'ARRAY',
}
