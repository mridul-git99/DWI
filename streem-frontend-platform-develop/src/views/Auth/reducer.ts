import { roles } from '#services/uiPermissions';
import { FacilitiesAction } from '#store/facilities/types';
import { DEFAULT_SESSION_TIMEOUT_IN_MIN } from '#utils/constants';

import {
  AuthAction,
  AuthActionType,
  AuthState,
  LicenseState,
  LicenseType,
  LicenseWorkflowType,
} from './types';

export const authInitialState: AuthState = {
  userId: null,
  isLoggedIn: false,
  isIdle: false,
  profile: null,
  accessToken: '',
  refreshToken: '',
  loading: false,
  facilities: [],
  NonGenuineLicenseMap: {},
  useCases: [],
  fetchingUseCaseList: true,
  ssoIdToken: '',
  identity: '',
  triggerPersist: false,
  settings: {
    logoUrl: '',
    sessionIdleTimeoutInMinutes: DEFAULT_SESSION_TIMEOUT_IN_MIN,
    passwordPolicy: {
      minimumPasswordLength: 8,
      minimumLowercaseCharacters: 1,
      minimumNumericCharacters: 1,
      minimumSpecialCharacters: 1,
      minimumUppercaseCharacters: 1,
    },
  },
};

const reducer = (state = authInitialState, action: AuthActionType): AuthState => {
  switch (action.type) {
    case AuthAction.ACCOUNT_LOOKUP:
    case AuthAction.RESET_PASSWORD:
    case AuthAction.LOGIN:
      return { ...state, loading: true };

    case AuthAction.LOGIN_SUCCESS:
      const getNonGenuineLicenseHashMap = (
        licesneArr: LicenseType[],
      ): { [facilityId: string]: LicenseType } => {
        return licesneArr.reduce((acc, { facilityId, ...rest }) => {
          if (rest.state === LicenseState.GENUINE || rest.workflow === LicenseWorkflowType.NONE) {
            return acc;
          }
          return { ...acc, [facilityId]: { ...rest } };
        }, {});
      };

      return {
        ...state,
        isIdle: false,
        loading: false,
        isLoggedIn: true,
        userId: action.payload.id,
        ssoIdToken: action.payload.idToken,
        ...action.payload,
        selectedFacility:
          action.payload?.facilities?.length < 2 ||
          action.payload.roles.some((r) => r === roles.SYSTEM_ADMIN)
            ? action.payload?.facilities[0]
            : state.selectedFacility,
        facilities: action.payload.facilities,
        ...(!!action.payload.licenses && {
          NonGenuineLicenseMap: getNonGenuineLicenseHashMap(action.payload.licenses),
        }),
        settings: {
          ...state.settings,
          ...action.payload.settings,
        },
      };

    case AuthAction.SET_IDLE:
      return {
        ...state,
        isIdle: action.payload,
      };

    case AuthAction.CLEANUP:
      return {
        ...authInitialState,
      };

    case AuthAction.AUTH_ERROR:
      return { ...state, loading: false, error: action.payload };

    case AuthAction.FETCH_PROFILE_SUCCESS:
      return { ...state, profile: action.payload, userType: action.payload?.userType };

    case AuthAction.REFRESH_TOKEN_SUCCESS:
      return {
        ...state,
        isLoggedIn: true,
        accessToken: action.payload.accessToken,
      };

    case AuthAction.SET_IDENTITY_TOKEN:
      return {
        ...state,
        ...action.payload,
        error: undefined,
        settings: {
          ...state.settings,
          ...action.payload.settings,
        },
      };

    case AuthAction.RESET_ERROR:
      return { ...state, error: undefined };

    case AuthAction.SET_CHALLENGE_QUESTION_SUCCESS:
      return { ...state, hasSetChallengeQuestion: true, token: undefined };

    case FacilitiesAction.SWITCH_FACILITY_SUCCESS:
      const facilityDetails = state.facilities.find(
        (facility) => facility.id === action.payload.facilityId,
      );
      return {
        ...state,
        accessToken: action.payload.accessToken,
        selectedFacility: {
          ...facilityDetails,
        },
        selectedUseCase: undefined,
      };

    case AuthAction.FETCH_USE_CASE_LIST_ONGOING:
      return { ...state, fetchingUseCaseList: true };

    case AuthAction.FETCH_USE_CASE_LIST_SUCCESS:
      return {
        ...state,
        fetchingUseCaseList: false,
        useCases: action.payload.useCases,
      };

    case AuthAction.SET_SELECTED_USE_CASE:
      return {
        ...state,
        selectedUseCase: action.payload.selectedUseCase,
      };

    case AuthAction.FETCH_USE_CASE_LIST_ERROR:
      return { ...state, error: action.payload?.error };

    case AuthAction.ACCOUNT_LOOKUP_SUCCESS:
      return {
        ...state,
        loading: false,
        userType: action.payload.type,
        email: action.payload.username,
        identity: action.payload.username,
      };

    case AuthAction.TRIGGER_PERSIST:
      return {
        ...state,
        triggerPersist: !state.triggerPersist,
      };

    default:
      return state;
  }
};

export { reducer as AuthReducer };
