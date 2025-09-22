import { actionSpreader } from '#store/helpers';
import { Stage, Task, Parameter } from '#types';
import { Error } from '#utils/globalTypes';
import { fetchListSuccessType } from '#views/Ontology/types';
import { ParameterListActions, ParameterListState } from './reducer.types';
import { AddNewParameterType, DeleteParameterType } from './types';

export const addNewParameter = (params: AddNewParameterType) =>
  actionSpreader(ParameterListActions.ADD_NEW_PARAMETER, params);

export const addNewParameterSuccess = (params: {
  stageId: Stage['id'];
  taskId: Task['id'];
  parameter: Parameter;
}) => actionSpreader(ParameterListActions.ADD_NEW_PARAMETER_SUCCESS, params);

export const deleteParameter = (params: DeleteParameterType) =>
  actionSpreader(ParameterListActions.DELETE_PARAMETER, params);

export const deleteParameterError = (error: any) =>
  actionSpreader(ParameterListActions.DELETE_PARAMETER_ERROR, { error });

export const deleteParameterSuccess = (params: DeleteParameterType) =>
  actionSpreader(ParameterListActions.DELETE_PARAMETER_SUCCESS, { ...params });

export const updateParameterApi = (params: {
  parameter: Parameter;
  setParameterSubmitting?: React.Dispatch<React.SetStateAction<boolean>>;
}) => actionSpreader(ParameterListActions.UPDATE_PARAMETER_API, params);

export const updateStoreParameter = (data: Parameter['data'], parameterId: Parameter['id']) =>
  actionSpreader(ParameterListActions.UPDATE_STORE_PARAMETER, {
    parameterId,
    data,
  });

export const updateStoreMediaParameter = (
  parameterId: Parameter['id'],
  dataIndex: number,
  data: Parameter['data'],
) =>
  actionSpreader(ParameterListActions.UPDATE_STORE_MEDIA_PARAMETER, {
    parameterId,
    dataIndex,
    data,
  });

export const removeStoreParameterItem = (parameterId: Parameter['id'], parameterItemId: string) =>
  actionSpreader(ParameterListActions.REMOVE_STORE_PARAMETER_ITEM, {
    parameterId,
    parameterItemId,
  });

export const addStoreParameterItem = (
  parameterId: Parameter['id'],
  parameterItemData: Parameter['data'],
) =>
  actionSpreader(ParameterListActions.ADD_STORE_PARAMETER_ITEM, {
    parameterId,
    parameterItemData,
  });

export const updateParameterError = (error: any) =>
  actionSpreader(ParameterListActions.UPDATE_PARAMETER_ERROR, { error });

export const setValidationError = (id: string, errors: Error[]) =>
  actionSpreader(ParameterListActions.SET_VALIDATION_ERROR, { id, errors });

export const toggleNewParameter = (payload?: ParameterListState['addParameter']) =>
  actionSpreader(ParameterListActions.TOGGLE_NEW_PARAMETER, payload);

export const fetchParameters = (checklistId: string, params?: Record<string, any>) =>
  actionSpreader(ParameterListActions.FETCH_PARAMETERS, { checklistId, params });

export const fetchParametersSuccess = ({ data, pageable }: fetchListSuccessType<any>) =>
  actionSpreader(ParameterListActions.FETCH_PARAMETERS_SUCCESS, { data, pageable });

export const fetchParametersError = (error: any) =>
  actionSpreader(ParameterListActions.FETCH_PARAMETERS_ERROR, { error });

export const toggleAddParameterRender = (payload: Record<string, boolean>) =>
  actionSpreader(ParameterListActions.TOGGLE_ADD_PARAMETER_RENDER, payload);
