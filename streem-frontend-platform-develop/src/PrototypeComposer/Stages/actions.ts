import { ReOrderType } from '#PrototypeComposer/types';
import { Stage } from '#types';
import { Error } from '#utils/globalTypes';
import { actionSpreader } from '../../store/helpers';
import { StageListActions } from './reducer.types';

// add new stage actions
export const addNewStage = () => actionSpreader(StageListActions.ADD_NEW_STAGE);

export const addNewStageError = (error: any) =>
  actionSpreader(StageListActions.ADD_NEW_STAGE_ERROR, { error });

export const addNewStageSuccess = (stage: Stage) =>
  actionSpreader(StageListActions.ADD_NEW_STAGE_SUCCESS, { stage });

// delete stage actions
export const deleteStage = ({ id }: Pick<Stage, 'id'>) =>
  actionSpreader(StageListActions.DELETE_STAGE, { id });

type deleteStageSuccessType = {
  id: Stage['id'];
  newOrderMap?: Record<string, number>;
};

export const deleteStageSuccess = ({ id, newOrderMap }: deleteStageSuccessType) =>
  actionSpreader(StageListActions.DELETE_STAGE_SUCCESS, { id, newOrderMap });

// duplicate stage actions
export const duplicateStage = ({ id }: Pick<Stage, 'id'>) =>
  actionSpreader(StageListActions.DUPLICATE_STAGE, { id });

export const duplicateStageError = (error: any) =>
  actionSpreader(StageListActions.DUPLICATE_STAGE_ERROR, { error });

export const duplicateStageSuccess = ({ id }: Pick<Stage, 'id'>) =>
  actionSpreader(StageListActions.DUPLICATE_STAGE_SUCCESS, { id });

// reorder stage actions
export const reOrderStage = ({ from, id, to }: ReOrderType) =>
  actionSpreader(StageListActions.REORDER_STAGE, { from, id, to });

export const reOrderStageError = (error: any) =>
  actionSpreader(StageListActions.REORDER_STAGE_ERROR, { error });

export const reOrderStageSuccess = ({ from, id, to }: ReOrderType) =>
  actionSpreader(StageListActions.REORDER_STAGE_SUCCESS, { from, id, to });

// set active stage action
export const setActiveStage = ({ id }: Pick<Stage, 'id'>) =>
  actionSpreader(StageListActions.SET_ACTIVE_STAGE, { id });

// upodate stage/stage name actions
export const updateStageName = (stage: Pick<Stage, 'name' | 'id' | 'orderTree'>) =>
  actionSpreader(StageListActions.UPDATE_STAGE_NAME, { stage });

export const updateStageNameError = (error: any) =>
  actionSpreader(StageListActions.UPDATE_STAGE_NAME_ERROR, { error });

export const updateStageNameSuccess = (updatedStage: Stage) =>
  actionSpreader(StageListActions.UPDATE_STAGE_NAME_SUCCESS, { updatedStage });

export const setValidationError = (error: Error) =>
  actionSpreader(StageListActions.SET_VALIDATION_ERROR, { error });
