import { jobActions } from '#views/Job/jobStore';
import {
  addRetainedToastId,
  clearRetainedToastIds,
  removeRetainedToastId,
  setGlobalError,
  setInternetConnectivity,
  setRecentServerTimestamp,
  toggleIsDrawerOpen,
} from './action';

export interface ExtrasState {
  readonly connected: boolean;
  readonly hasGlobalError: boolean;
  readonly recentServerTimestamp?: number;
  readonly isDrawerOpen: boolean;
  readonly retainedToastsIds: string[];
}

export enum ExtrasAction {
  SET_INTERNET_CONNECTIVITY = '@@extra/SET_INTERNET_CONNECTIVITY',
  SET_GLOBAL_ERROR = '@@extra/SET_GLOBAL_ERROR',
  SET_RECENT_SERVER_TIMESTAMP = '@@extra/SET_RECENT_SERVER_TIMESTAMP',
  TOGGLE_IS_DRAWER_OPEN = '@@extra/TOGGLE_IS_DRAWER_OPEN',
  ADD_RETAINED_TOAST_ID = '@@extra/ADD_RETAINED_TOAST_ID',
  REMOVE_RETAINED_TOAST_ID = '@@extra/REMOVE_RETAINED_TOAST_ID',
  CLEAR_RETAINED_TOAST_IDS = '@@extra/CLEAR_RETAINED_TOAST_IDS',
}

export type ExtrasActionType = ReturnType<
  | typeof setInternetConnectivity
  | typeof setGlobalError
  | typeof setRecentServerTimestamp
  | typeof toggleIsDrawerOpen
  | typeof addRetainedToastId
  | typeof removeRetainedToastId
  | typeof clearRetainedToastIds
  | typeof jobActions.reset
  | typeof jobActions.completeJob
  | typeof jobActions.executeParameter
>;
