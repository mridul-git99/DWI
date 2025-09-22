import { ComposerEntity } from '#PrototypeComposer/types';
import { actionSpreader } from '#store';
import { fetchSuccessType, PropertiesAction } from './types';

export const fetch = (entityArr: ComposerEntity[], useCaseId: string) =>
  actionSpreader(PropertiesAction.FETCH_PROPERTIES, {
    entityArr,
    useCaseId,
  });

export const fetchOngoing = (entity: ComposerEntity) =>
  actionSpreader(PropertiesAction.FETCH_PROPERTIES_ONGOING, { entity });

export const fetchSuccess = (args: fetchSuccessType) =>
  actionSpreader(PropertiesAction.FETCH_PROPERTIES_SUCCESS, { ...args });

export const fetchError = (error: string) =>
  actionSpreader(PropertiesAction.FETCH_PROPERTIES_ERROR, { error });

export const resetPropertiesState = () => actionSpreader(PropertiesAction.RESET_PROPERTIES_STATE);
