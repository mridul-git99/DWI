import { ComposerActionType } from '../reducer.types';
import {
  addNewStageError,
  addNewStageSuccess,
  deleteStageSuccess,
  duplicateStageError,
  duplicateStageSuccess,
  reOrderStageError,
  reOrderStageSuccess,
  setActiveStage,
  setValidationError,
  updateStageNameError,
  updateStageNameSuccess,
} from './actions';
import { Stage } from './types';

export type StagesById = Record<string, Stage>;

export type StageListState = {
  readonly activeStageId?: Stage['id'];
  readonly error?: any;
  readonly listOrder: Stage['id'][];
  readonly listById: StagesById;
};

export enum StageListActions {
  ADD_NEW_STAGE = '@@prototypeComposer/prototype/stage-list/ADD_NEW_STAGE',
  ADD_NEW_STAGE_ERROR = '@@prototypeComposer/prototype/stage-list/ADD_NEW_STAGE_ERROR',
  ADD_NEW_STAGE_SUCCESS = '@@prototypeComposer/prototype/stage-list/ADD_NEW_STAGE_SUCCESS',

  DELETE_STAGE = '@@prototypeComposer/prototype/stage-list/DELETE_STAGE',
  DELETE_STAGE_SUCCESS = '@@prototypeComposer/prototype/stage-list/DELETE_STAGE_SUCCESS',

  DUPLICATE_STAGE = '@@prototypeComposer/prototype/stage-list/DUPLICATE_STAGE',
  DUPLICATE_STAGE_ERROR = '@@prototypeComposer/prototype/stage-list/DUPLICATE_STAGE_ERROR',
  DUPLICATE_STAGE_SUCCESS = '@@prototypeComposer/prototype/stage-list/DUPLICATE_STAGE_SUCCESS',

  REORDER_STAGE = '@@prototypeComposer/prototype/stage-list/REORDER_STAGE',
  REORDER_STAGE_ERROR = '@@prototypeComposer/prototype/stage-list/REORDER_STAGE_ERROR',
  REORDER_STAGE_SUCCESS = '@@prototypeComposer/prototype/stage-list/REORDER_STAGE_SUCCESS',

  SET_ACTIVE_STAGE = '@@prototypeComposer/prototype/stage-list/SET_ACTIVE_STAGE',
  SET_VALIDATION_ERROR = '@@prototypeComposer/protottype/stage-list/SET_VALIDATION_ERROR',

  UPDATE_STAGE_NAME = '@@prototypeComposer/prototype/stage-list/UPDATE_STAGE_NAME',
  UPDATE_STAGE_NAME_ERROR = '@@prototypeComposer/prototype/stage-list/UPDATE_STAGE_NAME_ERROR',
  UPDATE_STAGE_NAME_SUCCESS = '@@prototypeComposer/prototype/stage-list/UPDATE_STAGE_NAME_SUCCESS',
}

export type StageListActionType =
  | ReturnType<
      | typeof addNewStageError
      | typeof addNewStageSuccess
      | typeof deleteStageSuccess
      | typeof duplicateStageError
      | typeof duplicateStageSuccess
      | typeof reOrderStageError
      | typeof reOrderStageSuccess
      | typeof setActiveStage
      | typeof setValidationError
      | typeof updateStageNameError
      | typeof updateStageNameSuccess
    >
  | ComposerActionType;
