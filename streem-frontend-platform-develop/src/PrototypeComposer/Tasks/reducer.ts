import { omit } from 'lodash';
import { Reducer } from 'redux';
import { ComposerAction } from '../reducer.types';
import { StageListActions } from '../Stages/reducer.types';
import { TaskListActions, TaskListActionType, TaskListState, TasksById } from './reducer.types';
import { getTasks } from './utils';

export const initialState: TaskListState = {
  activeTaskId: undefined,
  error: undefined,
  listById: {},
  tasksOrderInStage: {},
};

const reducer: Reducer<TaskListState, TaskListActionType> = (state = initialState, action) => {
  switch (action.type) {
    case ComposerAction.FETCH_COMPOSER_DATA_SUCCESS:
      const { data } = action.payload;

      return {
        ...state,
        ...getTasks(data),
        activeTaskId: (data?.stages ?? [])[0]?.tasks[0]?.id,
      };

    case TaskListActions.SET_ACTIVE_TASK:
      return {
        ...state,
        activeTaskId: action.payload.taskId,
      };

    case TaskListActions.ADD_NEW_TASK_SUCCESS:
      const { newTask, stageId } = action.payload;

      return {
        ...state,
        listById: {
          ...state.listById,
          [newTask.id]: { ...newTask, stageId, errors: [] },
        },
        tasksOrderInStage: {
          ...state.tasksOrderInStage,
          [stageId]: [...state.tasksOrderInStage[stageId], newTask.id],
        },
      };

    case TaskListActions.DELETE_TASK_SUCCESS:
      return {
        ...state,
        listById: {
          ...omit(state.listById, [action.payload.taskId]),
          ...Object.entries(action.payload.newOrderMap || {}).reduce<TasksById>(
            (acc, [taskId, orderTree]) => {
              acc[taskId] = { ...state.listById[taskId], orderTree };
              return acc;
            },
            {},
          ),
        },
        tasksOrderInStage: {
          ...state.tasksOrderInStage,
          [action.payload.stageId]: state.tasksOrderInStage[action.payload.stageId].filter(
            (el) => el !== action.payload.taskId,
          ),
        },
      };

    case TaskListActions.UPDATE_TASK:
      const updatedTask = action.payload.task;
      const updatedTaskId = updatedTask.id;

      return {
        ...state,
        listById: {
          ...state.listById,
          [updatedTaskId]: {
            ...action.payload.task,
            stageId: state.listById[updatedTaskId].stageId,
            errors: [],
          },
        },
      };

    case TaskListActions.UPDATE_TASK_MEDIA_SUCCESS:
      return {
        ...state,
        listById: {
          ...state.listById,
          [action.payload.taskId]: {
            ...state.listById[action.payload.taskId],
            medias: [
              ...state.listById[action.payload.taskId].medias.filter(
                (media) => media.id !== action.payload.media.id,
              ),
              action.payload.media,
            ],
            errors: [],
          },
        },
      };

    case TaskListActions.SET_VALIDATION_ERROR:
      const { error } = action.payload;
      const taskIdWithError = error.id;
      const taskWithError = state.listById[taskIdWithError];

      return {
        ...state,
        listById: {
          ...state.listById,
          [taskIdWithError]: {
            ...taskWithError,
            errors: [...taskWithError.errors, error],
          },
        },
      };

    case TaskListActions.RESET_TASK_PARAMETER_ERROR:
      return {
        ...state,
        listById: {
          ...state.listById,
          [action.payload.taskId]: {
            ...state.listById[action.payload.taskId],
            errors: state.listById[action.payload.taskId]?.errors.filter(
              (error) => error.code !== 'E211',
            ),
          },
        },
      };

    case StageListActions.ADD_NEW_STAGE_SUCCESS:
      const { stage } = action.payload;

      return {
        ...state,
        tasksOrderInStage: {
          ...state.tasksOrderInStage,
          [stage.id]: stage.tasks.map((task) => task.id),
        },
      };

    case StageListActions.DELETE_STAGE_SUCCESS:
      return {
        ...state,
        tasksOrderInStage: {
          ...omit(state.tasksOrderInStage, [action.payload.id]),
        },
      };

    case TaskListActions.RESET_TASK_ERROR:
      return {
        ...state,
        listById: {
          ...state.listById,
          [action.payload.taskId]: {
            ...state.listById[action.payload.taskId],
            errors: [],
          },
        },
      };

    case TaskListActions.SET_TASK_ERROR:
      return {
        ...state,
        error: action.payload.error,
      };

    case TaskListActions.REORDER_TASK_SUCCESS:
      const { tasksOrderInStage, listById } = state;
      const { activeStageId, from: fromIndex, to: toIndex, id: fromId } = action.payload;
      const stageWiseTasks = [...tasksOrderInStage[activeStageId]];
      const toId = tasksOrderInStage[activeStageId][toIndex];
      stageWiseTasks[fromIndex] = stageWiseTasks.splice(toIndex, 1, stageWiseTasks[fromIndex])[0];
      return {
        ...state,
        tasksOrderInStage: {
          ...tasksOrderInStage,
          [activeStageId]: [...stageWiseTasks],
        },
        listById: {
          ...listById,
          [fromId]: { ...listById[fromId], orderTree: toIndex + 1 },
          [toId]: { ...listById[toId], orderTree: fromIndex + 1 },
        },
      };

    case TaskListActions.REORDER_TASK_ERROR:
      return { ...state, error: action.payload.error };

    case TaskListActions.UPDATE_TASK_ACTION_SUCCESS:
      return {
        ...state,
        listById: {
          ...state.listById,
          [action.payload.taskId]: {
            ...state.listById[action.payload.taskId],
            automations: [action.payload.action],
            errors: [],
          },
        },
      };

    default:
      return { ...state };
  }
};

export { reducer as taskReducer };
