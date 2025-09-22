import { TaskActionType } from '#types';
import { Validate } from 'react-hook-form';

export interface Pageable {
  page: number;
  pageSize: number;
  numberOfElements: number;
  totalPages: number;
  totalElements: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

export type ResponseError = {
  id: string;
  code: string;
  field: string;
  ignore: string;
  message: string;
  timestamp: number;
};

export interface ResponseObj<T> {
  object: string;
  state: string;
  message: string;
  data: T;
  pageable: Pageable | null;
  errors?: ResponseError[];
  timestamp: number;
  status: string;
}

export type ValidatorProps = {
  functions: Record<string, Validate>;
  messages: Record<string, string>;
};

export type Error = {
  id: string;
  code: string;
  message: string;
  type: string;
  entity?: string;
};

export type FileUploadData = {
  mediaId: string;
  filename: string;
  originalFilename: string;
  link: string;
  type: string;
};

export enum FilterOperators {
  EQ = 'EQ',
  ANY = 'ANY',
  LIKE = 'LIKE',
  GT = 'GT',
  LT = 'LT',
  NE = 'NE',
  GOE = 'GOE',
  LOE = 'LOE',
  AND = 'AND',
  IS_NOT_SET = 'IS_NOT_SET',
  OR = 'OR',
  NOT_IN = 'NOT_IN',
  NIN = 'NIN',
}

export enum SelectorOptionsEnum {
  CONSTANT = 'CONSTANT',
  PARAMETER = 'PARAMETER',
}

export enum OffsetSelectorOptionsEnum {
  CONSTANT = 'CONSTANT',
  PARAMETER = 'PARAMETER',
}

export enum CapturePropertySelectorOptionsEnum {
  END_TIME = 'END_TIME',
  START_TIME = 'START_TIME',
  CONSTANT = 'CONSTANT',
  PARAMETER = 'PARAMETER',
}

export type FilterField = {
  id?: string;
  field: string;
  op: FilterOperators;
  values: [string | boolean] | string[];
};

export enum InputTypes {
  DATE = 'DATE',
  DATE_TIME = 'DATE_TIME',
  TIME = 'TIME',
  NUMBER = 'NUMBER',
  MULTI_LINE = 'MULTI_LINE',
  MULTI_SELECT = 'MULTI_SELECT',
  SINGLE_LINE = 'SINGLE_LINE',
  SINGLE_SELECT = 'SINGLE_SELECT',
  PASSWORD = 'PASSWORD',
  ROLE = 'ROLE',
  ERROR_CONTAINER = 'ERROR_CONTAINER',
  RADIO = 'RADIO',
  ONE_TO_ONE = 'ONE_TO_ONE',
  ONE_TO_MANY = 'ONE_TO_MANY',
}

export type SelectOptions =
  | {
      label: string;
      value: string;
      externalId?: string;
    }[]
  | undefined;

export enum CustomViewsTargetType {
  PROCESS = 'PROCESS',
  JOB = 'JOB',
}

export type fetchDataParams = {
  page?: number | string | null;
  size?: number | string | null;
  query?: string;
  filters?: FilterField[];
  objectId?: string;
  propertyId?: string;
};

export const SsoStates = {
  LOGIN: 'LOGIN',
  RE_LOGIN: 'RE_LOGIN',
  SIGN_OFF: 'SIGN_OFF',
  RELEASE: 'RELEASE',
  SELF_VERIFICATION: 'SELF_VERIFICATION',
  PEER_VERIFICATION: 'PEER_VERIFICATION',
  INITIATE_CORRECTION: 'INITIATE_CORRECTION',
  PERFORM_CORRECTION: 'PERFORM_CORRECTION',
  REVIEW_CORRECTION: 'REVIEW_CORRECTION',
  INITIATE_EXCEPTION: 'INITIATE_EXCEPTION',
  REVIEW_EXCEPTION: 'REVIEW_EXCEPTION',
  INITIATE_BULK_EXCEPTION: 'INITIATE_BULK_EXCEPTION',
  ...TaskActionType,
} as const;

export enum ComparisonOperator {
  EQUAL_TO = 'EQUAL_TO',
  NOT_EQUAL_TO = 'NOT_EQUAL_TO',
  LESS_THAN = 'LESS_THAN',
  LESS_THAN_OR_EQUAL_TO = 'LESS_THAN_OR_EQUAL_TO',
  GREATER_THAN = 'GREATER_THAN',
  GREATER_THAN_OR_EQUAL_TO = 'GREATER_THAN_OR_EQUAL_TO',
}

export const configuredAutomationsOptions = [
  {
    label: 'Constant',
    value: 'CONSTANT',
  },
  {
    label: 'Parameter',
    value: 'PARAMETER',
  },
];

export enum ConfigureAutomationSelect {
  CONSTANT = 'CONSTANT',
  PARAMETER = 'PARAMETER',
}
