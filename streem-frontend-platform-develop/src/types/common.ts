import { User } from '#store/users/types';

export type ObjectKeys<T> = keyof T;
export type ObjectValues<T> = T[keyof T];
export type PartialUser = Pick<User, 'employeeId' | 'firstName' | 'id' | 'lastName' | 'archived'>;

export type Audit = {
  createdAt: number;
  modifiedAt: number;
  modifiedBy: PartialUser;
  createdBy: PartialUser;
};

export enum Constraint {
  LT = 'LT', // Date and Number
  GT = 'GT', // Date and Number
  LTE = 'LTE', // Date and Number
  GTE = 'GTE', // Date and Number
  NE = 'NE', // Date and Number
  MIN = 'MIN', // String Length or Choice Count
  MAX = 'MAX', // String Length or Choice Count
  PATTERN = 'PATTERN',
  EQ = 'EQ',
  ANY = 'ANY',
  LIKE = 'LIKE',
  ALL = 'ALL',
  NOT_ALL = 'NOT_ALL',
  NIN = 'NIN',
}

export interface Property {
  id: string;
  name: string;
  label: string;
  placeHolder: string;
  mandatory: boolean;
  value: string;
}

export enum Selections {
  SELECTED = 'SELECTED',
  NOT_SELECTED = 'NOT_SELECTED',
}

export enum SupervisorResponse {
  APPROVE = 'APPROVE',
  REJECT = 'REJECT',
}

export type Emoji = {
  name: string;
  value: string;
};

export enum filterPageTypeEnum {
  CONFIGURE_TASK_CONDITIONS = 'CONFIGURE_TASK_CONDITIONS',
  OBJECTS_FILTERS_DRAWER = 'OBJECTS_FILTERS_DRAWER',
  JOB_LOGS_FILTERS_DRAWER = 'JOB_LOGS_FILTERS_DRAWER',
  RESOURCE_FILTERS = 'RESOURCE_FILTERS',
  VALIDATIONS = 'VALIDATIONS',
}

export enum SelectAllOptionAction {
  SELECT_ACTION = 'select-option',
  DESELECT_ACTION = 'deselect-option',
}
