import { TargetEntityType } from '#types';
import { unionBy } from 'lodash';
import { Reducer } from 'redux';
import { initialState as ParameterListState, parameterReducer } from './Activity/reducer';
import { ParameterListActions } from './Activity/reducer.types';
import { Checklist } from './checklist.types';
import {
  initialState as auditLogsState,
  checklistAuditLogsReducer,
} from './ChecklistAuditLogs/reducers';
import { ComposerAction, ComposerActionType, ComposerState } from './reducer.types';
import { CollaboratorState } from './reviewer.types';
import { initialState as StageListInitialState, stageReducer } from './Stages/reducer';
import { initialState as TaskListInitialState, taskReducer } from './Tasks/reducer';
import { ComposerEntity, EntityType } from './types';

const initialState: ComposerState = {
  parameters: ParameterListState,
  data: undefined,
  entity: undefined,
  error: undefined,
  loading: false,
  stages: StageListInitialState,
  tasks: TaskListInitialState,
  collaborators: [],
  approvers: [],
  auditLogs: auditLogsState,
  errors: [],
  isCopying: false,
  jobLogColumnsLoading: true,
};

/**
 * TODO: optimize the reducer for rendering process and eassy access of the tasks in the stages and parameters in tasks
 * ? mabe look into splitting the reducer to smaller parts
 */
const reducer: Reducer<ComposerState, ComposerActionType> = (state = initialState, action) => {
  switch (action.type) {
    case ComposerAction.FETCH_JOB_LOG_COLUMNS:
      return {
        ...state,
        jobLogColumnsLoading: true,
      };

    case ComposerAction.FETCH_JOB_LOG_COLUMNS_SUCCESS:
      const { id: checklistId, name, jobLogColumns } = action.payload.data;
      return {
        ...state,
        data: {
          checklistId: checklistId,
          name,
          jobLogColumns,
        },
        jobLogColumnsLoading: false,
      };

    case ComposerAction.FETCH_COMPOSER_DATA_ONGOING:
      return {
        ...state,
        entity: ComposerEntity.CHECKLIST,
        loading: true,
      };

    case ComposerAction.FETCH_COMPOSER_DATA_SUCCESS:
      return {
        ...state,
        parameters: parameterReducer(state.parameters, action),
        data: action.payload.data,
        loading: false,
        stages: stageReducer(state.stages, action),
        tasks: taskReducer(state.tasks, action),
      };

    case ParameterListActions.UPDATE_STORE_PARAMETER: {
      const { parameterId, data } = action.payload;

      return {
        ...state,
        data: {
          ...state.data,
          parameters: (state.data?.parameters || []).map((p) => (p.id === parameterId ? data : p)),
        },
        parameters: parameterReducer(state.parameters, action),
      };
    }

    case ParameterListActions.DELETE_PARAMETER_SUCCESS: {
      const { parameterId, targetEntityType } = action.payload;

      return {
        ...state,
        ...(targetEntityType === TargetEntityType.PROCESS
          ? {
              data: {
                ...state.data,
                ...(targetEntityType === TargetEntityType.PROCESS && {
                  parameters: (state.data?.parameters || []).filter((p) => p.id !== parameterId),
                }),
              },
            }
          : {
              parameters: parameterReducer(state.parameters, action),
            }),
      };
    }

    case ComposerAction.FETCH_COMPOSER_DATA_ERROR:
      return {
        ...state,
        error: action.payload.error,
        loading: false,
      };

    case ComposerAction.RESET_COMPOSER:
      return {
        ...initialState,
      };

    case ComposerAction.FETCH_APPROVERS_SUCCESS:
      return { ...state, approvers: action.payload.data };

    case ComposerAction.FETCH_REVIEWERS_FOR_CHECKLIST_SUCCESS:
      return { ...state, collaborators: action.payload.data };

    case ComposerAction.REVERT_REVIEWERS_FOR_CHECKLIST:
      return { ...state, collaborators: action.payload.users };

    case ComposerAction.ASSIGN_REVIEWER_TO_CHECKLIST:
      return {
        ...state,
        collaborators: unionBy(
          [{ ...action.payload.user, state: CollaboratorState.NOT_STARTED }],
          state.collaborators,
          'id',
        ),
      };

    case ComposerAction.UNASSIGN_REVIEWER_FROM_CHECKLIST:
      return {
        ...state,
        collaborators: state.collaborators.filter((item) => item.id !== action.payload.user.id),
      };

    case ComposerAction.UPDATE_FOR_REVIEW_PROCESS:
      return {
        ...state,
        data: {
          ...state.data,
          ...action.payload.checklist,
          collaborators: action.payload.collaborators.length
            ? action.payload.collaborators
            : (state.data as Checklist).collaborators,
          comments: action.payload.comments.length
            ? action.payload.comments
            : (state.data as Checklist).comments,
        } as Checklist,
      };

    case ComposerAction.PROCESS_PARAMETER_MAP_SUCCESS:
      return {
        ...state,
        data: {
          ...state.data,
          parameters: action.payload.parameters,
        },
        parameters: parameterReducer(state.parameters, action),
      };

    case ComposerAction.SET_CHECKLIST_VALIDATION_ERRORS:
      return {
        ...state,
        errors: action.payload.errors,
      };

    case ComposerAction.RESET_CHECKLIST_VALIDATION_ERRORS:
      const { id, code } = action.payload;
      return {
        ...state,
        errors: state.errors.filter((error) => !(error.id === id && error.code === code)),
      };

    case ComposerAction.COPY_ENTITY:
      return {
        ...state,
        isCopying: true,
      };

    case ComposerAction.COPY_ENTITY_SUCCESS:
      const { params, entityData, stageId, taskId, type } = action.payload;
      switch (type) {
        case EntityType.PARAMETER:
          return {
            ...state,
            isCopying: false,
            parameters: {
              ...state.parameters,
              parameterOrderInTaskInStage: {
                ...state.parameters.parameterOrderInTaskInStage,
                [stageId]: {
                  ...state.parameters.parameterOrderInTaskInStage[stageId],
                  [taskId]: [
                    ...state.parameters.parameterOrderInTaskInStage[stageId][taskId],
                    params.id,
                  ],
                },
              },
              listById: {
                ...state.parameters.listById,
                [params.id]: {
                  ...entityData,
                  errors: [],
                },
              },
            },
          };
        case EntityType.TASK:
          return {
            ...state,
            isCopying: false,
            tasks: {
              ...state.tasks,
              listById: {
                ...state.tasks.listById,
                [params.id]: { ...entityData, stageId, errors: [] },
              },
              tasksOrderInStage: {
                ...state.tasks.tasksOrderInStage,
                [stageId]: [...state.tasks.tasksOrderInStage[stageId], params.id],
              },
            },
            parameters: {
              ...state.parameters,
              parameterOrderInTaskInStage: {
                ...state.parameters.parameterOrderInTaskInStage,
                [stageId]: {
                  ...state.parameters.parameterOrderInTaskInStage[stageId],
                  [params.id]: [...entityData.parameters.map((param) => param.id)],
                },
              },
              listById: {
                ...state.parameters.listById,
                ...entityData.parameters.reduce(
                  (acc, param) => ({
                    ...acc,
                    [param.id]: { ...param, errors: [] },
                  }),
                  {},
                ),
              },
            },
          };
        case EntityType.STAGE:
          return {
            ...state,
            isCopying: false,
            stages: {
              ...state.stages,
              listById: {
                ...state.stages.listById,
                [params.id]: { ...entityData, errors: [] },
              },
              listOrder: [...state.stages.listOrder, params.id],
            },
            tasks: {
              ...state.tasks,
              listById: {
                ...state.tasks.listById,
                ...entityData.tasks.reduce(
                  (acc, param) => ({
                    ...acc,
                    [param.id]: { ...param, errors: [] },
                  }),
                  {},
                ),
              },
              tasksOrderInStage: {
                ...state.tasks.tasksOrderInStage,
                [params.id]: entityData.tasks.map((task) => task.id),
              },
            },
            parameters: {
              ...state.parameters,
              parameterOrderInTaskInStage: {
                ...state.parameters.parameterOrderInTaskInStage,
                [params.id]: {
                  ...entityData.tasks.reduce(
                    (acc, param) => ({
                      ...acc,
                      [param.id]: param.parameters.map((p) => p.id),
                    }),
                    {},
                  ),
                },
              },
              listById: {
                ...state.parameters.listById,
                ...entityData.tasks.reduce(
                  (acc, taskParam) => ({
                    ...acc,
                    ...taskParam.parameters.reduce(
                      (paramsAcc, param) => ({
                        ...paramsAcc,
                        [param.id]: { ...param, errors: [] },
                      }),
                      {},
                    ),
                  }),
                  {},
                ),
              },
            },
          };
        default:
          return state;
      }

    case ComposerAction.COPY_ENTITY_ERROR:
      return {
        ...state,
        isCopying: false,
      };

    case ComposerAction.QUICK_PUBLISH_SUCCESS:
      const { state: checklistState } = action.payload.data;
      return {
        ...state,
        data: {
          ...state.data,
          state: checklistState,
        },
      };

    default:
      return {
        ...state,
        parameters: parameterReducer(state.parameters, action),
        stages: stageReducer(state.stages, action),
        tasks: taskReducer(state.tasks, action),
        auditLogs: checklistAuditLogsReducer(state.auditLogs, action),
      };
  }
};

export { reducer as ComposerReducer };
