import { ComposerEntity } from '#PrototypeComposer/types';

import { fetchError, fetchOngoing, fetchSuccess, resetPropertiesState } from './actions';

export interface Property {
  id: string;
  name: string;
  label: string;
  placeHolder: string;
  mandatory: boolean;
  value: string;
}

export type PropertyById = Record<string, Property>;

export type PropertyByName = Record<string, Property>;

export type EntityProperties = {
  list: Property[];
  listById: PropertyById;
  listByName: PropertyByName;
  loading: boolean;
};

export interface PropertiesState {
  readonly [ComposerEntity.CHECKLIST]: EntityProperties;
  readonly [ComposerEntity.JOB]: EntityProperties;
  readonly error?: unknown;
}

export enum PropertiesAction {
  FETCH_PROPERTIES = '@@properties-service/FETCH_PROPERTIES',
  FETCH_PROPERTIES_ERROR = '@@properties-service/FETCH_PROPERTIES_ERROR',
  FETCH_PROPERTIES_ONGOING = '@@properties-service/FETCH_PROPERTIES_ONGOING',
  FETCH_PROPERTIES_SUCCESS = '@@properties-service/FETCH_PROPERTIES_SUCCESS',
  RESET_PROPERTIES_STATE = '@@properties-service/RESET_PROPERTIES_STATE',
}

export type PropertiesActionType = ReturnType<
  typeof fetchError | typeof fetchOngoing | typeof fetchSuccess | typeof resetPropertiesState
>;

export type fetchSuccessType = {
  data: Property[];
  entity: ComposerEntity;
};
