import { NotificationType } from '#components/Notification/types';
import { actionSpreader } from '#store';
import { User } from '#store/users/types';
import {
  AuthState,
  ChallengeQuestionPurpose,
  CredentialsInputs,
  UseCaseType,
} from '#views/Auth/types';
import { EditUserRequestInputs } from '#views/UserAccess/ManageUser/types';
import {
  AdditionalVerificationTypes,
  AuthAction,
  LoginResponse,
  RefreshTokenResponse,
  TokenTypes,
} from './types';

export const login = (payload: {
  username: string;
  password?: string;
  code?: string;
  state?: string;
  pathname?: string;
}) => actionSpreader(AuthAction.LOGIN, payload);

export const loginSuccess = (data: LoginResponse) => actionSpreader(AuthAction.LOGIN_SUCCESS, data);

export const reLogin = (payload: {
  username: string;
  password?: string;
  code?: string;
  state?: string;
  pathname?: string;
}) => actionSpreader(AuthAction.RE_LOGIN, payload);

export const authError = (error: string) => actionSpreader(AuthAction.AUTH_ERROR, error);

export const setIdle = (data: boolean) => actionSpreader(AuthAction.SET_IDLE, data);

export const logout = (payload: { ssoIdToken?: string }) =>
  actionSpreader(AuthAction.LOGOUT, payload);

export const logoutSuccess = (payload?: {
  type?: NotificationType;
  msg?: string;
  delayTime?: number;
}) => actionSpreader(AuthAction.LOGOUT_SUCCESS, payload);

export const cleanUp = () => actionSpreader(AuthAction.CLEANUP);

export const register = (payload: CredentialsInputs) =>
  actionSpreader(AuthAction.REGISTER, payload);

export const fetchProfile = (payload: { id: User['id'] }) =>
  actionSpreader(AuthAction.FETCH_PROFILE, payload);

export const fetchProfileSuccess = (data: User) =>
  actionSpreader(AuthAction.FETCH_PROFILE_SUCCESS, data);

export const updateUserProfile = (payload: {
  body: Omit<EditUserRequestInputs, 'roles'> & {
    roles: { id: string }[];
  };
  id: User['id'];
}) => actionSpreader(AuthAction.UPDATE_USER_PROFILE, payload);

export const refreshTokenSuccess = (data: RefreshTokenResponse) =>
  actionSpreader(AuthAction.REFRESH_TOKEN_SUCCESS, data);

export const checkTokenExpiry = (payload: { type: TokenTypes; token: string }) =>
  actionSpreader(AuthAction.CHECK_TOKEN_EXPIRY, payload);

export const resetPassword = (payload: {
  password: string;
  confirmPassword: string;
  token: string;
}) => actionSpreader(AuthAction.RESET_PASSWORD, payload);

export const resetError = () => actionSpreader(AuthAction.RESET_ERROR);

export const setIdentityToken = (payload: {
  token?: string;
  fullName?: string;
  employeeId?: string;
  settings?: AuthState['settings'];
}) => actionSpreader(AuthAction.SET_IDENTITY_TOKEN, payload);

export const additionalVerification = (payload: {
  identifier: string;
  token: string;
  type: AdditionalVerificationTypes;
}) => actionSpreader(AuthAction.ADDITIONAL_VERIFICATION, payload);

export const setChallengeQuestion = (payload: { id: string; answer: string; token: string }) =>
  actionSpreader(AuthAction.SET_CHALLENGE_QUESTION, payload);

export const setChallengeQuestionSuccess = () =>
  actionSpreader(AuthAction.SET_CHALLENGE_QUESTION_SUCCESS);

export const validateIdentity = (payload: { identity: string }) =>
  actionSpreader(AuthAction.VALIDATE_IDENTITY, payload);

export const resetByMail = (payload: { identity: string }) =>
  actionSpreader(AuthAction.RESET_BY_MAIL, payload);

export const notifyAdmin = (payload: { identity: string; purpose: ChallengeQuestionPurpose }) =>
  actionSpreader(AuthAction.NOTIFY_ADMIN, payload);

export const validateQuestion = (payload: { identity: string; id: string; answer: string }) =>
  actionSpreader(AuthAction.VALIDATE_QUESTION, payload);

export const fetchUseCaseList = () => actionSpreader(AuthAction.FETCH_USE_CASE_LIST);

export const fetchUseCaseListOngoing = () => actionSpreader(AuthAction.FETCH_USE_CASE_LIST_ONGOING);

export const fetchUseCaseListSuccess = (useCases: UseCaseType[]) =>
  actionSpreader(AuthAction.FETCH_USE_CASE_LIST_SUCCESS, {
    useCases,
  });

export const setSelectedUseCase = (selectedUseCase: UseCaseType) =>
  actionSpreader(AuthAction.SET_SELECTED_USE_CASE, { selectedUseCase });

export const fetchUseCaseListError = (error: string) =>
  actionSpreader(AuthAction.FETCH_USE_CASE_LIST_ERROR, { error });

export const accountLookUp = (username: string, query?: string) =>
  actionSpreader(AuthAction.ACCOUNT_LOOKUP, { username, query });

export const accountLookUpSuccess = (payload: { type: string; username: string }) =>
  actionSpreader(AuthAction.ACCOUNT_LOOKUP_SUCCESS, payload);

export const triggerPersist = () => actionSpreader(AuthAction.TRIGGER_PERSIST);
