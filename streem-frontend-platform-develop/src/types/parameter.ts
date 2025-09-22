import { Media, ParameterExceptionTypeEnum } from '#PrototypeComposer/checklist.types';
import { Audit, Constraint } from './common';

export enum MandatoryParameter {
  CHECKLIST = 'CHECKLIST',
  MEDIA = 'MEDIA',
  MULTISELECT = 'MULTISELECT',
  SHOULD_BE = 'SHOULD_BE',
  SIGNATURE = 'SIGNATURE',
  SINGLE_SELECT = 'SINGLE_SELECT',
  MULTI_LINE = 'MULTI_LINE',
  YES_NO = 'YES_NO',
  NUMBER = 'NUMBER',
  CALCULATION = 'CALCULATION',
  RESOURCE = 'RESOURCE',
  DATE = 'DATE',
  DATE_TIME = 'DATE_TIME',
  SINGLE_LINE = 'SINGLE_LINE',
  MULTI_RESOURCE = 'MULTI_RESOURCE',
  FILE_UPLOAD = 'FILE_UPLOAD',
}

export enum NonMandatoryParameter {
  INSTRUCTION = 'INSTRUCTION',
  MATERIAL = 'MATERIAL',
}

export type ParameterType = MandatoryParameter | NonMandatoryParameter;

export enum ParameterState {
  NOT_STARTED = 'NOT_STARTED',
  EXECUTED = 'EXECUTED',
  BEGIN_EXECUTED = 'BEGIN_EXECUTED',
  APPROVAL_PENDING = 'APPROVAL_PENDING',
  VERIFICATION_PENDING = 'VERIFICATION_PENDING',
  BEING_EXECUTED = 'BEING_EXECUTED',
  PENDING_FOR_APPROVAL = 'PENDING_FOR_APPROVAL',
  APPROVED = 'APPROVED',
}

export interface ParameterResponseChoice {
  objectId: string;
  objectExternalId: string;
  objectDisplayName: string;
  collection: string;
}

export interface ParameterResponse {
  audit: Audit;
  state: ParameterState;
  hidden: boolean;
  parameterVerifications?: any[];
  reason?: string;
  value?: string | number;
  parameterValueApprovalDto?: Audit | null;
  medias?: Media[];
  choices?: ParameterResponseChoice[];
  stageOrderTree: number;
  taskOrderTree: number;
  variations: any[] | null;
  taskExecutionId: string;
  correction: any;
  id: string;
  hasActiveException: boolean;
}

export type PartialParameter = Pick<ParameterResponse, 'choices' | 'value' | 'hidden'> &
  Pick<Parameter, 'id' | 'label' | 'type' | 'data'> & {
    taskId?: string;
    taskExecutionId?: string;
    parameterValueId?: string;
  };

export enum TargetEntityType {
  TASK = 'TASK',
  PROCESS = 'PROCESS',
  UNMAPPED = 'UNMAPPED',
}

export interface BranchingRule {
  id: string;
  key: string;
  constraint: Constraint;
  input: string[];
  thenValue?: any;
  hide?: {
    parameters: string[];
  };
  show?: {
    parameters: string[];
  };
}

// TODO: look into this any type for parameter data
export interface Parameter {
  code: string;
  data: any;
  id: string;
  label: string;
  mandatory: boolean;
  orderTree: number;
  response: ParameterResponse;
  description?: string;
  type: ParameterType;
  validations: any;
  targetEntityType: TargetEntityType;
  reason?: string;
  autoInitialize?: Record<string, any>;
  autoInitialized?: boolean;
  hidden: boolean;
  processId?: string;
  processName?: string;
  verificationType: string;
  rules?: BranchingRule[];
  hide?: string[];
  show?: string[];
}

export interface StoreParameter extends Parameter {
  exceptionApprovalType: ParameterExceptionTypeEnum;
  taskId: string;
  stageId: string;
}

export enum ParameterExecutionState {
  NOT_STARTED = 'NOT_STARTED',
  EXECUTED = 'EXECUTED',
  BEGIN_EXECUTED = 'BEGIN_EXECUTED',
  APPROVAL_PENDING = 'APPROVAL_PENDING',
  VERIFICATION_PENDING = 'VERIFICATION_PENDING',
  BEING_EXECUTED = 'BEING_EXECUTED',
  PENDING_FOR_APPROVAL = 'PENDING_FOR_APPROVAL',
}

export const RETAIN_NOTIFICATION_ERRORS = {
  E442: 'E442', // NUMBER_PARAMETER_RELATION_PROPERTY_VALIDATION_ERROR
  E443: 'E443', // PARAMETER_RELATION_PROPERTY_VALIDATION_COULD_NOT_RUN_MISSING_DATA
  E9004: 'E9004', // NUMBER_PARAMETER_RESOURCE_VALIDATION_ERROR
  490: '490', // NUMBER_PARAMETER_CRITERIA_VALIDATION_ERROR
  E474: 'E474', // NUMBER_LEAST_COUNT_VALIDATION_ERROR, CALCULATION_PRECISION_ERROR, INVALID_RESOURCE_SELECTION_ERROR
  E1801: 'E1801', // RESOURCE_PARAMETER_VALIDATION_ERROR
  E1802: 'E1802', // RESOURCE_PARAMETER_VALIDATION_ERROR
  E1833: 'E1833', // DATE_PARAMETER_VALIDATION_ERROR
} as const;

export const PARAMETER_ERRORS = {
  ...RETAIN_NOTIFICATION_ERRORS,
  E401: 'E401', // 'PARAMETER_INCOMPLETE',
  E9002: 'E9002', // 'PARAMETER_VERIFICATION_INCOMPLETE',
  E482: 'E482', // 'SHOULD_BE_PARAMETER_PENDING_FOR_APPROVAL',
  E504: 'E504', // LEAST_COUNT_LINKED_PARAMETER_INCOMPLETE_ERROR
  E485: 'E485', // 'LEAST_COUNT_MISSING_PARAMETER_INCOMPLETE_ERROR',
} as const;

export enum ParameterVariationType {
  SHOULD_BE = 'SHOULD_BE',
  FILTER = 'FILTER',
  VALIDATION = 'VALIDATION',
}

export enum ParameterCorrectionStatus {
  INITIATED = 'INITIATED',
  CORRECTED = 'CORRECTED',
  ACCEPTED = 'ACCEPTED',
  REJECTED = 'REJECTED',
}
