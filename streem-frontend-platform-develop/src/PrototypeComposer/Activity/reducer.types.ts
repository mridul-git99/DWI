import { ParameterType } from '#types';
import { Pageable } from '#utils/globalTypes';
import { ComposerActionType } from '../reducer.types';
import { addNewTaskSuccess, deleteTaskSuccess, reOrderParameters } from '../Tasks/actions';
import {
  addNewParameterSuccess,
  addStoreParameterItem,
  deleteParameterError,
  deleteParameterSuccess,
  fetchParameters,
  fetchParametersError,
  fetchParametersSuccess,
  removeStoreParameterItem,
  setValidationError,
  toggleAddParameterRender,
  toggleNewParameter,
  updateParameterError,
  updateStoreMediaParameter,
  updateStoreParameter,
} from './actions';
import { Parameter } from './types';

export type ParametersById = Record<string, Parameter>;
export type ParameterOrderInTaskInStage = Record<string, Record<string, Parameter['id'][]>>;

export type ParameterListState = {
  readonly parameterOrderInTaskInStage: ParameterOrderInTaskInStage;
  readonly error?: any;
  readonly listById: ParametersById;
  readonly addParameter?: {
    action: 'task' | 'list';
    title: string;
    parameterId?: string;
    fetchData?: () => void;
    type?: ParameterType;
    taskId?: string;
  };
  readonly hiddenParameterIds: Record<string, boolean>;
  readonly parameters: {
    listLoading: boolean;
    pageable: Pageable;
    list: any[];
    error?: any;
    reRender: boolean;
  };
};

export enum ParameterListActions {
  ADD_NEW_PARAMETER = '@@prototypeComposer/prototype/parameter-list/ADD_NEW_PARAMETER',
  ADD_NEW_PARAMETER_SUCCESS = '@@prototypeComposer/prototype/parameter-list/ADD_NEW_PARAMETER_SUCCESS',
  DELETE_PARAMETER = '@@prototypeComposer/prototype/parameter-list/DELETE_PARAMETER',
  DELETE_PARAMETER_ERROR = '@@prototypeComposer/prototype/parameter-list/DELETE_PARAMETER_ERROR',
  DELETE_PARAMETER_SUCCESS = '@@prototypeComposer/prototype/parameter-list/DELETE_PARAMETER_SUCCESS',
  RESET_VALIDATION_ERROR = '@@prototypeComposer/prototype/parameter-list/RESET_VALIDATION_ERROR',
  SET_VALIDATION_ERROR = '@@prototypeComposer/prototype/parameter-list/SET_VALIDATION_ERROR',
  UPDATE_PARAMETER_API = '@@prototypeComposer/prototype/parameter-list/UPDATE_PARAMETER_API',
  UPDATE_PARAMETER_ERROR = '@@prototypeComposer/prototype/parameter-list/UPDATE_PARAMETER_ERROR',
  UPDATE_STORE_PARAMETER = '@@prototypeComposer/prototype/parameter-list/UPDATE_STORE_PARAMETER',
  UPDATE_STORE_MEDIA_PARAMETER = '@@prototypeComposer/prototype/parameter-list/UPDATE_STORE_MEDIA_PARAMETER',
  ADD_STORE_PARAMETER_ITEM = '@@prototypeComposer/prototype/parameter-list/ADD_STORE_PARAMETER_ITEM',
  REMOVE_STORE_PARAMETER_ITEM = '@@prototypeComposer/prototype/parameter-list/REMOVE_STORE_PARAMETER_ITEM',

  TOGGLE_NEW_PARAMETER = '@@prototypeComposer/prototype/parameter-list/TOGGLE_NEW_PARAMETER',
  FETCH_PARAMETERS = '@@prototypeComposer/prototype/parameter-list/FETCH_PARAMETERS',
  FETCH_PARAMETERS_SUCCESS = '@@prototypeComposer/prototype/parameter-list/FETCH_PARAMETERS_SUCCESS',
  FETCH_PARAMETERS_ERROR = '@@prototypeComposer/prototype/parameter-list/FETCH_PARAMETERS_ERROR',
  TOGGLE_ADD_PARAMETER_RENDER = '@@prototypeComposer/prototype/parameter-list/TOGGLE_ADD_PARAMETER_RENDER',
}

export type ParameterListActionType =
  | ReturnType<
      | typeof toggleNewParameter
      | typeof addNewParameterSuccess
      | typeof deleteParameterError
      | typeof deleteParameterSuccess
      | typeof setValidationError
      | typeof updateParameterError
      | typeof updateStoreParameter
      | typeof updateStoreMediaParameter
      | typeof addStoreParameterItem
      | typeof removeStoreParameterItem
      | typeof fetchParameters
      | typeof fetchParametersError
      | typeof fetchParametersSuccess
      | typeof reOrderParameters
      | typeof toggleAddParameterRender
    >
  | ReturnType<typeof addNewTaskSuccess | typeof deleteTaskSuccess>
  | ComposerActionType;
