import { toast } from 'react-toastify';
import { ExtrasState, ExtrasAction, ExtrasActionType } from './types';
import { JobActionsEnum } from '#views/Job/jobStore';

const initialState: ExtrasState = {
  connected: true,
  hasGlobalError: false,
  isDrawerOpen: false,
  retainedToastsIds: [],
};

const reducer = (state = initialState, action: ExtrasActionType): ExtrasState => {
  switch (action.type) {
    case ExtrasAction.SET_INTERNET_CONNECTIVITY:
      return { ...state, connected: action.payload.connected };

    case ExtrasAction.SET_GLOBAL_ERROR:
      return { ...state, hasGlobalError: action.payload.hasError };

    case ExtrasAction.SET_RECENT_SERVER_TIMESTAMP:
      return { ...state, recentServerTimestamp: action.payload.timestamp };

    case ExtrasAction.TOGGLE_IS_DRAWER_OPEN:
      return { ...state, isDrawerOpen: !state.isDrawerOpen };

    case ExtrasAction.ADD_RETAINED_TOAST_ID:
      return { ...state, retainedToastsIds: [...state.retainedToastsIds, action.payload.id] };

    case ExtrasAction.REMOVE_RETAINED_TOAST_ID:
      return {
        ...state,
        retainedToastsIds: state.retainedToastsIds.filter((id) => id !== action.payload.id),
      };

    case JobActionsEnum.reset:
    case JobActionsEnum.completeJob:
    case JobActionsEnum.executeParameter:
    case ExtrasAction.CLEAR_RETAINED_TOAST_IDS:
      state.retainedToastsIds.forEach((id) => {
        toast.dismiss(id);
      });
      return { ...state, retainedToastsIds: [] };

    default:
      return state;
  }
};

export { reducer as ExtrasReducer };
