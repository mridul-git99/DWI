import { FacilitiesAction, FacilitiesActionType, FacilitiesState } from './types';

const initialState: FacilitiesState = {
  list: undefined,
  loading: false,
};

const reducer = (state = initialState, action: FacilitiesActionType): FacilitiesState => {
  switch (action.type) {
    case FacilitiesAction.FETCH_FACILITIES_ONGOING:
      return { ...state, loading: true };

    case FacilitiesAction.FETCH_FACILITIES_SUCCESS:
      return {
        ...state,
        loading: false,
        list: action.payload?.facilities,
      };

    case FacilitiesAction.FETCH_FACILITIES_ERROR:
      return { ...state, error: action.payload?.error };

    default:
      return { ...state };
  }
};

export { reducer as FacilitiesReducer };
