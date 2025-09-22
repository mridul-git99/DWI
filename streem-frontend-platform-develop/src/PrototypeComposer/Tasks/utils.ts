import { cloneDeep, toArray } from 'lodash';
import {
  AutomationActionActionType,
  AutomationActionConfigurationDataType,
  Checklist,
} from '../checklist.types';
import { TaskOrderInStage, TasksById } from './reducer.types';

export const getTasks = (checklist: Checklist | Partial<Checklist>) => {
  const listById: TasksById = {},
    tasksOrderInStage: TaskOrderInStage = {};

  checklist?.stages?.map((stage) => {
    tasksOrderInStage[stage.id.toString()] = [];

    stage?.tasks?.map((task) => {
      tasksOrderInStage[stage.id.toString()].push(task.id);

      listById[task.id.toString()] = { ...task, stageId: stage.id, errors: [] };
    });
  });

  return { listById, tasksOrderInStage };
};

//For AutomationActions
export const updatedActions = (
  action: any,
  currentDataType: AutomationActionConfigurationDataType,
) => {
  const _action = cloneDeep(action);

  if (
    _action.actionType === AutomationActionActionType.CREATE_OBJECT ||
    _action.actionType === AutomationActionActionType.BULK_CREATE_OBJECT
  ) {
    let updatedConfiguration;

    if (currentDataType === AutomationActionConfigurationDataType.ARRAY) {
      updatedConfiguration = (_action.actionDetails.configuration || []).reduce((acc, item) => {
        const key = item.propertyId || item.relationId;
        if (key) {
          acc[key] = item;
        }
        return acc;
      }, {});
    } else {
      updatedConfiguration = toArray(_action.actionDetails.configuration || {});
    }
    _action.actionDetails.configuration = updatedConfiguration;
  }
  return _action;
};
