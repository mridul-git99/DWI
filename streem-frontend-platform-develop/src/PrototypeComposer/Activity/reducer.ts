import { TargetEntityType } from '#types';
import { DEFAULT_PAGINATION } from '#utils/constants';
import { cloneDeep, omit, set } from 'lodash';
import { Reducer } from 'redux';
import { ComposerAction } from '../reducer.types';
import { TaskListActions } from '../Tasks/reducer.types';
import { ParameterListActions, ParameterListActionType, ParameterListState } from './reducer.types';
import { getParameters, updateHiddenParameterIds } from './utils';

export const initialState: ParameterListState = {
  parameterOrderInTaskInStage: {},
  listById: {},
  hiddenParameterIds: {},
  parameters: {
    list: [],
    listLoading: true,
    pageable: DEFAULT_PAGINATION,
    reRender: false,
  },
};

const reducer: Reducer<ParameterListState, ParameterListActionType> = (
  state = initialState,
  action,
) => {
  switch (action.type) {
    case ParameterListActions.TOGGLE_NEW_PARAMETER:
      return { ...state, addParameter: action.payload };

    case ComposerAction.FETCH_COMPOSER_DATA_SUCCESS:
      const { data } = action.payload;

      return {
        ...state,
        ...getParameters(data),
      };

    case ParameterListActions.ADD_NEW_PARAMETER_SUCCESS:
      return {
        ...state,
        parameterOrderInTaskInStage: {
          ...state.parameterOrderInTaskInStage,
          [action.payload.stageId]: {
            ...state.parameterOrderInTaskInStage[action.payload.stageId],
            [action.payload.taskId]: [
              ...state.parameterOrderInTaskInStage[action.payload.stageId][action.payload.taskId],
              action.payload.parameter.id,
            ],
          },
        },
        listById: {
          ...state.listById,
          [action.payload.parameter.id]: {
            ...action.payload.parameter,
            errors: [],
          },
        },
      };

    case ParameterListActions.TOGGLE_ADD_PARAMETER_RENDER:
      return {
        ...state,
        parameters: {
          ...state.parameters,
          reRender: action.payload.shouldReRender
            ? !state.parameters.reRender
            : state.parameters.reRender,
        },
      };
    case ParameterListActions.DELETE_PARAMETER_SUCCESS:
      return {
        ...state,
        ...(action.payload.taskId &&
          action.payload.stageId && {
            parameterOrderInTaskInStage: {
              ...state.parameterOrderInTaskInStage,
              [action.payload.stageId]: {
                ...state.parameterOrderInTaskInStage[action.payload.stageId],
                [action.payload.taskId]: [
                  ...state.parameterOrderInTaskInStage[action.payload.stageId][
                    action.payload.taskId
                  ].filter((el) => el !== action.payload.parameterId),
                ],
              },
            },
            listById: {
              ...omit(state.listById, [action.payload.parameterId]),
            },
          }),
      };

    case ParameterListActions.UPDATE_STORE_PARAMETER: {
      const { parameterId, data } = action.payload;

      return {
        ...state,
        ...(data.targetEntityType === TargetEntityType.TASK
          ? {
              listById: {
                ...state.listById,
                [parameterId]: { ...data, errors: [] },
              },
              parameters: {
                ...state.parameters,
                list: state.parameters.list.map((p) =>
                  p.id === parameterId
                    ? {
                        ...p,
                        ...data,
                      }
                    : p,
                ),
              },
            }
          : {
              parameters: {
                ...state.parameters,
                list: state.parameters.list.map((p) =>
                  p.id === parameterId
                    ? {
                        ...p,
                        ...data,
                      }
                    : p,
                ),
              },
            }),
      };
    }

    case ParameterListActions.UPDATE_STORE_MEDIA_PARAMETER: {
      const { parameterId, dataIndex, data } = action.payload;

      return {
        ...state,
        listById: {
          ...state.listById,
          [parameterId]: cloneDeep({
            ...set(state.listById[parameterId], ['data', dataIndex], {
              ...state.listById[parameterId].data[dataIndex],
              ...data,
            }),
            errors: [],
          }),
        },
      };
    }

    case ParameterListActions.REMOVE_STORE_PARAMETER_ITEM: {
      const { parameterId, parameterItemId } = action.payload;
      const parameterToUpdate = state.listById[parameterId];

      return {
        ...state,
        listById: {
          ...state.listById,
          [parameterId]: {
            ...parameterToUpdate,
            data: parameterToUpdate.data.filter(({ id }: { id: string }) => id !== parameterItemId),
          },
        },
      };
    }

    case ParameterListActions.ADD_STORE_PARAMETER_ITEM: {
      const { parameterId, parameterItemData } = action.payload;
      const parameterToUpdate = state.listById[parameterId];
      parameterToUpdate.data.push(parameterItemData);

      return {
        ...state,
        listById: {
          ...state.listById,
          [parameterId]: { ...parameterToUpdate },
        },
      };
    }

    case TaskListActions.REORDER_PARAMETERS:
      return {
        ...state,
        parameterOrderInTaskInStage: {
          ...state.parameterOrderInTaskInStage,
          [action.payload.stageId]: {
            ...state.parameterOrderInTaskInStage[action.payload.stageId],
            [action.payload.taskId]: action.payload.orderedIds,
          },
        },
      };

    case TaskListActions.ADD_NEW_TASK_SUCCESS:
      const { newTask, stageId } = action.payload;

      return {
        ...state,
        parameterOrderInTaskInStage: {
          ...state.parameterOrderInTaskInStage,
          [stageId]: {
            ...state.parameterOrderInTaskInStage[stageId],
            [newTask.id]: newTask.parameters.map((parameter) => parameter.id),
          },
        },
      };

    case TaskListActions.DELETE_TASK_SUCCESS:
      return {
        ...state,
        parameterOrderInTaskInStage: {
          ...state.parameterOrderInTaskInStage,
          [action.payload.stageId]: {
            ...omit(state.parameterOrderInTaskInStage[action.payload.stageId], [
              action.payload.taskId,
            ]),
          },
        },
      };

    case ParameterListActions.DELETE_PARAMETER_ERROR:
    case ParameterListActions.UPDATE_PARAMETER_ERROR:
      return {
        ...state,
        error: action.payload.error,
      };

    case ParameterListActions.SET_VALIDATION_ERROR:
      const { id, errors } = action.payload;
      const parameterWithError = state.listById[id];
      return {
        ...state,
        listById: {
          ...state.listById,
          [id]: {
            ...parameterWithError,
            errors,
          },
        },
      };

    case ParameterListActions.FETCH_PARAMETERS:
      return {
        ...state,
        parameters: {
          ...state.parameters,
          listLoading: true,
        },
      };

    case ParameterListActions.FETCH_PARAMETERS_SUCCESS:
      return {
        ...state,
        parameters: {
          list: action.payload.data,
          pageable: action.payload.pageable!,
          listLoading: false,
          reRender: state.parameters.reRender,
        },
      };

    case ParameterListActions.FETCH_PARAMETERS_ERROR:
      return {
        ...state,
        parameters: {
          ...state.parameters,
          listLoading: false,
          error: action.payload.error,
        },
      };

    case ComposerAction.PROCESS_PARAMETER_MAP_SUCCESS:
      return {
        ...state,
        parameters: {
          ...state.parameters,
          list: state.parameters.list.map((p) =>
            action.payload.payload.mappedParameters?.[p.id]
              ? {
                  ...p,
                  targetEntityType: TargetEntityType.PROCESS,
                  orderTree: action.payload.payload.mappedParameters[p.id],
                }
              : {
                  ...p,
                  ...(p.targetEntityType === TargetEntityType.PROCESS && {
                    targetEntityType: TargetEntityType.UNMAPPED,
                    orderTree: 1,
                  }),
                },
          ),
        },
      };

    case ComposerAction.UPDATE_HIDDEN_PARAMETER_IDS:
      return {
        ...state,
        hiddenParameterIds: updateHiddenParameterIds(action.payload.data),
      };
    default:
      return { ...state };
  }
};

export { reducer as parameterReducer };
