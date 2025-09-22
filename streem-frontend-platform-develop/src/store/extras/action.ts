import { actionSpreader } from '#store/helpers';
import { ExtrasAction } from './types';

export const setInternetConnectivity = ({ connected }: { connected: boolean }) =>
  actionSpreader(ExtrasAction.SET_INTERNET_CONNECTIVITY, { connected });

export const setGlobalError = (hasError: boolean) =>
  actionSpreader(ExtrasAction.SET_GLOBAL_ERROR, { hasError });

export const setRecentServerTimestamp = (timestamp: number) =>
  actionSpreader(ExtrasAction.SET_RECENT_SERVER_TIMESTAMP, { timestamp });

export const toggleIsDrawerOpen = () => actionSpreader(ExtrasAction.TOGGLE_IS_DRAWER_OPEN);

export const addRetainedToastId = (id: string) =>
  actionSpreader(ExtrasAction.ADD_RETAINED_TOAST_ID, { id });

export const removeRetainedToastId = (id: string) =>
  actionSpreader(ExtrasAction.REMOVE_RETAINED_TOAST_ID, { id });

export const clearRetainedToastIds = () => actionSpreader(ExtrasAction.CLEAR_RETAINED_TOAST_IDS);
