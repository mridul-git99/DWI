import { Checklist } from '#PrototypeComposer/checklist.types';
import { User } from '#services/users';
import { Property } from '#store/properties/types';
import { Error } from '#utils/globalTypes';
import { addNewPrototype, updatePrototype } from './actions';

export enum NewPrototypeActions {
  ADD_NEW_PROTOTYPE = '@@prototype/ADD_NEW_PROTOTYPE',
  ADD_REVISION_PROTOTYPE = '@@prototype/ADD_REVISION_PROTOTYPE',
  UPDATE_PROTOTYPE = '@@prototype/UPDATE_PROTOTYPE',
}

export enum AuthorState {
  IN_PROGRESS = 'IN_PROGRESS',
}

export type PrototypeProperties = Pick<
  Property,
  'id' | 'name' | 'placeHolder' | 'mandatory' | 'label'
> & {
  value: string;
};

export type Author = Pick<User, 'id' | 'employeeId' | 'firstName' | 'lastName' | 'email'> & {
  state: AuthorState;
};

export enum FormMode {
  ADD = 'ADD',
  EDIT = 'EDIT',
  VIEW = 'VIEW',
}

export type KeyValue = {
  label: string;
  value: string;
};

export type PropertyTag = {
  [key: string]: KeyValue;
};

export type FormValues = {
  authors: Author[];
  description: string;
  name: string;
  createdBy: Pick<User, 'id' | 'employeeId' | 'firstName' | 'lastName' | 'email'>;
  properties: PrototypeProperties[];
  property: PropertyTag;
  colorCode: string;
};

export type FormValuesOnlyWithAuthorIds = Pick<
  FormValues,
  'description' | 'name' | 'createdBy' | 'properties' | 'colorCode'
> & { authors: Author['id'][]; useCaseId: string };

export type FormErrors = {
  name: string;
  properties: Record<string, string>;
  authors?: Error;
};

export type FormData = {
  authors: Author[];
  description: string;
  name: string;
  properties: PrototypeProperties[];
  prototypeId: Checklist['id'];
  revisedCode?: Checklist['code'];
  revisedName?: Checklist['name'];
  createdBy: Pick<User, 'id' | 'employeeId' | 'firstName' | 'lastName' | 'email'>;
  colorCode: string;
};

export type Props = {
  formData: FormData;
  formMode: FormMode;
};

export const colorCodes = {
  RICE_FLOWER: '#EAFFE5',
  PATTENS_BLUE: '#DBE4FF',
  EGG_SOUR: '#FFF5D9',
  FROSTED_MINT: '#D4FFFA',
  WHITE_POINTER: '#FCEAFF',
  BLUE_CHALK: '#E8E0FF',
  CORN_FIELD: '#F9F3C4',
  LINEN: '#F9EEE6',
  ONAHAU: '#C7E8FF',
  ORINOCO: '#F1FBDB',
};
