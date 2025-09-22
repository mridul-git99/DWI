import { actionSpreader } from '#store';
import { Error } from '#utils/globalTypes';
import { Parameter } from './Activity/types';
import { Checklist } from './checklist.types';
import { ComposerAction } from './reducer.types';
import { CopyEntityType } from './types';

// BLOCK START : Actions related to composer data fetching
type fetchDataType = {
  id: Checklist['id'];
  setLoading?: boolean;
};
export const fetchComposerData = ({ id, setLoading = true }: fetchDataType) =>
  actionSpreader(ComposerAction.FETCH_COMPOSER_DATA, { id, setLoading });

type fetchDataErrorType = {
  error: any;
};
export const fetchComposerDataError = ({ error }: fetchDataErrorType) =>
  actionSpreader(ComposerAction.FETCH_COMPOSER_DATA_ERROR, { error });

export const fetchComposerDataOngoing = () =>
  actionSpreader(ComposerAction.FETCH_COMPOSER_DATA_ONGOING);

type fetchDataSuccessType = {
  data: Checklist;
};

export const fetchComposerDataSuccess = ({ data }: fetchDataSuccessType) =>
  actionSpreader(ComposerAction.FETCH_COMPOSER_DATA_SUCCESS, { data });

export const resetComposer = () => actionSpreader(ComposerAction.RESET_COMPOSER);

export const validatePrototype = (id: Checklist['id'], isCustomPublish = false) =>
  actionSpreader(ComposerAction.VALIDATE_PROTOTYPE, { id, isCustomPublish });

// BLOCK ENDS

export const processParametersMapSuccess = (
  parameters: any[],
  payload: {
    mappedParameters: Record<string, number>;
  },
) => actionSpreader(ComposerAction.PROCESS_PARAMETER_MAP_SUCCESS, { parameters, payload });

export const executeBranchingRulesParameter = (
  parameterValues: Record<string, Parameter>,
  checklistId?: string,
) =>
  actionSpreader(ComposerAction.EXECUTE_LATEST_BRANCHING_RULES, { parameterValues, checklistId });

export const updateHiddenParameterIds = (data: Record<string, Array<string>>[]) =>
  actionSpreader(ComposerAction.UPDATE_HIDDEN_PARAMETER_IDS, { data });

export const setChecklistValidationErrors = (errors: Error[]) =>
  actionSpreader(ComposerAction.SET_CHECKLIST_VALIDATION_ERRORS, { errors });

export const resetChecklistValidationErrors = (id: string, code: string) =>
  actionSpreader(ComposerAction.RESET_CHECKLIST_VALIDATION_ERRORS, { id, code });

export const copyEntities = (params: CopyEntityType) =>
  actionSpreader(ComposerAction.COPY_ENTITY, params);

export const copyEntitiesError = (error: any) =>
  actionSpreader(ComposerAction.COPY_ENTITY_ERROR, { error });

export const copyEntitiesSuccess = (
  params: any,
  entityData: any,
  stageId: string,
  taskId: string,
  type: string,
) =>
  actionSpreader(ComposerAction.COPY_ENTITY_SUCCESS, { params, entityData, stageId, taskId, type });
export const quickPublish = (id: Checklist['id']) =>
  actionSpreader(ComposerAction.QUICK_PUBLISH, { id });

export const quickPublishSuccess = (data: any) => {
  return actionSpreader(ComposerAction.QUICK_PUBLISH_SUCCESS, { data });
};

export const fetchJobLogColumns = (id: string) =>
  actionSpreader(ComposerAction.FETCH_JOB_LOG_COLUMNS, { id });

export const fetchJobLogColumnsSuccess = (data: any) =>
  actionSpreader(ComposerAction.FETCH_JOB_LOG_COLUMNS_SUCCESS, data);
