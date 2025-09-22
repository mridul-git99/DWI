import { omit } from 'lodash';
import { Reducer } from 'redux';
import { ComposerAction } from '../reducer.types';
import { StageListActions, StageListActionType, StageListState, StagesById } from './reducer.types';

export const initialState: StageListState = {
  activeStageId: undefined,
  error: undefined,
  listOrder: [],
  listById: {},
};

const reducer: Reducer<StageListState, StageListActionType> = (state = initialState, action) => {
  switch (action.type) {
    case ComposerAction.FETCH_COMPOSER_DATA_SUCCESS:
      const { data } = action.payload;

      return {
        ...state,
        activeStageId: ((data?.stages ?? [])[0] ?? {}).id,
        listOrder: data?.stages?.map(({ id }) => id) ?? [],
        listById:
          data?.stages?.reduce<StagesById>((acc, stage) => {
            acc[stage.id.toString()] = { ...stage, errors: [] };
            return acc;
          }, {}) ?? {},
      };

    case StageListActions.SET_ACTIVE_STAGE:
      return {
        ...state,
        activeStageId: action.payload.id,
      };

    case StageListActions.ADD_NEW_STAGE_SUCCESS:
      const { stage } = action.payload;

      return {
        ...state,
        activeStageId: stage.id,
        listOrder: [...state.listOrder, action.payload.stage.id],
        listById: {
          ...state.listById,
          [stage.id]: { ...stage, errors: [] },
        },
      };

    case StageListActions.DELETE_STAGE_SUCCESS:
      const deletedStageIndex = state.listOrder.indexOf(action.payload.id);

      return {
        ...state,
        activeStageId: state.listOrder[deletedStageIndex - 1],
        listOrder: state.listOrder.filter((el) => el !== action.payload.id),
        listById: {
          ...omit(state.listById, [action.payload.id]),
          ...Object.entries(action.payload.newOrderMap || {}).reduce<StagesById>(
            (acc, [stageId, orderTree]) => {
              acc[stageId] = { ...state.listById[stageId], orderTree };
              return acc;
            },
            {},
          ),
        },
      };

    case StageListActions.REORDER_STAGE_SUCCESS:
      const { listOrder, listById } = state;
      const toId = listOrder[action.payload.to];
      listOrder[action.payload.from] = listOrder.splice(
        action.payload.to,
        1,
        listOrder[action.payload.from],
      )[0];
      return {
        ...state,
        listOrder,
        listById: {
          ...listById,
          [action.payload.id]: {
            ...listById[action.payload.id],
            orderTree: action.payload.to + 1,
          },
          [toId]: {
            ...listById[toId],
            orderTree: action.payload.from + 1,
          },
        },
      };

    case StageListActions.UPDATE_STAGE_NAME_SUCCESS:
      return {
        ...state,
        listById: {
          ...state.listById,
          [action.payload.updatedStage.id]: {
            ...state.listById[action.payload.updatedStage.id],
            ...action.payload.updatedStage,
            errors: [],
          },
        },
      };

    case StageListActions.SET_VALIDATION_ERROR:
      const { error } = action.payload;
      const stageIdWithError = error.id;
      const stageWithError = state.listById[stageIdWithError];

      return {
        ...state,
        listById: {
          ...state.listById,
          [stageIdWithError]: {
            ...stageWithError,
            errors: [...stageWithError.errors, error],
          },
        },
      };

    case StageListActions.ADD_NEW_STAGE_ERROR:
    case StageListActions.DUPLICATE_STAGE_ERROR:
    case StageListActions.REORDER_STAGE_ERROR:
    case StageListActions.UPDATE_STAGE_NAME_ERROR:
      return {
        ...state,
        error: action.payload.error,
      };

    default:
      return { ...state };
  }
};

export { reducer as stageReducer };
