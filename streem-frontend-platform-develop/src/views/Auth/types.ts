import { FormGroupProps } from '#components';
import { switchFacilityError, switchFacilitySuccess } from '#store/facilities/actions';
import { User } from '#store/users/types';
import { RouteComponentProps } from '@reach/router';
import {
  authError,
  cleanUp,
  fetchProfileSuccess,
  fetchUseCaseList,
  fetchUseCaseListError,
  fetchUseCaseListOngoing,
  fetchUseCaseListSuccess,
  login,
  loginSuccess,
  logout,
  logoutSuccess,
  refreshTokenSuccess,
  resetError,
  resetPassword,
  setChallengeQuestionSuccess,
  setIdentityToken,
  setIdle,
  setSelectedUseCase,
  accountLookUp,
  accountLookUpSuccess,
  triggerPersist,
} from './actions';

interface Settings {
  logoUrl: string;
  sessionIdleTimeoutInMinutes: number;
  ssoType?: string;
  passwordPolicy: {
    minimumPasswordLength: number;
    minimumLowercaseCharacters: number;
    minimumUppercaseCharacters: number;
    minimumSpecialCharacters: number;
    minimumNumericCharacters: number;
  };
}

export interface LoginResponse {
  id: User['id'];
  firstName: string;
  message: string;
  accessToken: string;
  refreshToken: string;
  roles: string[];
  facilities: Facility[];
  settings: Settings;
  licenses: LicenseType[];
}

export interface RefreshTokenResponse {
  accessToken: string;
}

type Facility = {
  id: string;
  name: string;
  dateFormat: string;
  timeZone: string;
};

// TODO Try to figure out a way to handle api data as not conditional once fetched
export type UseCaseType = {
  id: string;
  name: string;
  label: string;
  description: string;
  enabled: boolean;
  archived: boolean;
  metadata: {
    ['card-color']: string;
  };
  orderTree: string;
};

export interface AuthState {
  readonly accessToken: string;
  readonly email?: string;
  readonly identity: string;
  readonly employeeId?: string;
  readonly error?: string;
  readonly facilities: Facility[];
  readonly firstName?: string;
  readonly hasSetChallengeQuestion?: boolean;
  readonly isIdle: boolean;
  readonly isLoggedIn: boolean;
  readonly lastName?: string;
  readonly loading: boolean;
  readonly profile: User | null;
  readonly refreshToken: string;
  readonly roles?: string[];
  readonly selectedFacility?: Facility;
  readonly settings: Settings;
  readonly token?: string;
  readonly userId: User['id'] | null;
  readonly NonGenuineLicenseMap: { [facilityId: string]: LicenseType };
  readonly useCases: UseCaseType[];
  readonly selectedUseCase?: UseCaseType;
  readonly fetchingUseCaseList: boolean;
  readonly userType?: string;
  readonly features?: FeatureFlags;
  readonly ssoIdToken?: string;
  readonly triggerPersist: boolean;
}

export type FeatureFlags = {
  metabaseReports: boolean;
  quicksightReports: boolean;
  plannedVariation: boolean;
  repeatTask: boolean;
  recurringTask: boolean;
  createObjectFromQR: boolean;
  jobAnnotation: boolean;
  qrPdfProperties: boolean;
  quickPublish: boolean;
  bulkCreateObjectAction: boolean;
  actionEffectsConfiguration: boolean;
  bulkImportExport: boolean;
  downloadProcess: boolean;
  sameSessionVerification: boolean;
  jobsDownload: boolean;
  recallErrorCorrection: boolean;
  copyElement: boolean;
  redirectToJobAfterCreation: boolean;
  numberBranching: boolean;
};

export enum TokenTypes {
  PASSWORD_RESET = 'PASSWORD_RESET',
  REGISTRATION = 'REGISTRATION',
}

export enum AdditionalVerificationTypes {
  EMAIL = 'EMAIL',
  EMPLOYEE_ID = 'EMPLOYEE_ID',
}

export enum LicenseState {
  INTIMATE = 'INTIMATE',
  GRACE = 'GRACE',
  GRACE_EXCEEDED = 'GRACE_EXCEEDED',
  GENUINE = 'GENUINE',
}

export enum LicenseWorkflowType {
  NONE = 'NONE',
  NOTIFICATION_UNBLOCKED = 'NOTIFICATION_UNBLOCKED',
  NOTIFICATION_BLOCKED = 'NOTIFICATION_BLOCKED',
}

export enum PAGE_NAMES {
  ACCOUNT_LOCKED = 'ACCOUNT_LOCKED',
  ADMIN_NOTIFIED = 'ADMIN_NOTIFIED',
  CHANGE_PASSWORD = 'CHANGE_PASSWORD',
  FACILITY_SELECTION = 'FACILITY_SELECTION',
  FORGOT_EMAIL_SENT = 'FORGOT_EMAIL_SENT',
  FORGOT_IDENTITY = 'FORGOT_IDENTITY',
  FORGOT_NEW_PASSWORD = 'FORGOT_NEW_PASSWORD',
  FORGOT_QUESTIONS = 'FORGOT_QUESTIONS',
  FORGOT_RECOVERY = 'FORGOT_RECOVERY',
  FORGOT_SECRET_KEY = 'FORGOT_SECRET_KEY',
  INVITATION_EXPIRED = 'INVITATION_EXPIRED',
  KEY_EXPIRED = 'KEY_EXPIRED',
  LOCKED = 'LOCKED',
  LOGIN = 'LOGIN',
  PASSWORD_EXPIRED = 'PASSWORD_EXPIRED',
  PASSWORD_UPDATED = 'PASSWORD_UPDATED',
  REGISTER_CREDENTIALS = 'REGISTER_CREDENTIALS',
  REGISTER_EMPLOYEE_ID = 'REGISTER_EMPLOYEE_ID',
  REGISTER_RECOVERY = 'REGISTER_RECOVERY',
  REGISTER_SECRET_KEY = 'REGISTER_SECRET_KEY',
  ACCOUNT_LOOKUP = 'ACCOUNT_LOOKUP',
}

export enum RecoveryOptions {
  CHALLENGE_QUESTION = 'CHALLENGE_QUESTION',
  EMAIL = 'EMAIL',
  CONTACT_ADMIN = 'CONTACT_ADMIN',
}

export enum ChallengeQuestionPurpose {
  PASSWORD_RECOVERY_CHALLENGE_QUESTION_NOT_SET = 'PASSWORD_RECOVERY_CHALLENGE_QUESTION_NOT_SET',
  INVITE_EXPIRED = 'INVITE_EXPIRED',
  PASSWORD_RECOVERY_ACCOUNT_LOCKED = 'PASSWORD_RECOVERY_ACCOUNT_LOCKED',
  PASSWORD_RECOVERY_KEY_EXPIRED = 'PASSWORD_RECOVERY_KEY_EXPIRED',
}

export enum CARD_POSITIONS {
  LEFT = 'flex-start',
  CENTER = 'center',
}

export type LicenseType = {
  days: number;
  expired: boolean;
  facilityId: string;
  graceEndsOn: string;
  renewalDate: string;
  state: LicenseState;
  workflow: LicenseWorkflowType;
};

export type BaseViewConfigType = {
  wrapperStyle: React.CSSProperties;
  cardPosition: CARD_POSITIONS;
  cardStyle: React.CSSProperties;
  heading?: string;
  headingIcon?: JSX.Element;
  subHeading?: string;
  footerAction?: JSX.Element;
  formData?: {
    formInputs: FormGroupProps['inputs'];
    onSubmit: (data: any) => void;
    buttons: JSX.Element[];
  };
};

export type BaseViewProps = RouteComponentProps & {
  pageName: PAGE_NAMES;
};

export type LoginInputs = {
  username: string;
  password: string;
};

export type SecretKeyInputs = {
  token: string;
};

export type EmployeeIdInputs = {
  identifier: string;
};

export type CredentialsInputs = {
  username: string;
  password: string;
  confirmPassword: string;
  token: string;
};

export type RecoveryInputs = {
  id: string;
  answer: string;
  token: string;
};

export type ForgotPasswordInputs = {
  identity: string;
};

export type ForgotPasswordRecoveryInputs = {
  recoveryOption: RecoveryOptions;
};

export type NewPasswordInputs = {
  password: string;
  confirmPassword: string;
  token: string;
};

export enum AuthAction {
  ADDITIONAL_VERIFICATION = '@@auth/Register/ADDITIONAL_VERIFICATION',
  AUTH_ERROR = '@@auth/AUTH_ERROR',
  CHECK_TOKEN_EXPIRY = '@@auth/CHECK_TOKEN_EXPIRY',
  CLEANUP = '@@auth/Logout/CLEANUP',
  FETCH_PROFILE = '@@auth/Login/FETCH_PROFILE',
  FETCH_PROFILE_SUCCESS = '@@auth/Login/FETCH_PROFILE_SUCCESS',
  RE_LOGIN = '@@auth/Login/RE_LOGIN',
  LOGIN = '@@auth/Login/LOGIN',
  LOGIN_SUCCESS = '@@auth/Login/LOGIN_SUCCESS',
  LOGOUT = '@@auth/Logout/LOGOUT',
  LOGOUT_SUCCESS = '@@auth/Logout/LOGOUT_SUCCESS',
  NOTIFY_ADMIN = '@@auth/Forgot/NOTIFY_ADMIN',
  REFRESH_TOKEN_SUCCESS = '@@auth/Login/REFRESH_TOKEN_SUCCESS',
  REGISTER = '@@auth/Register/REGISTER',
  RESET_ERROR = '@@auth/Login/RESET_ERROR',
  RESET_PASSWORD = '@@auth/Register/RESET_PASSWORD',
  RESET_BY_MAIL = '@@auth/Forgot/RESET_BY_MAIL',
  SET_CHALLENGE_QUESTION = '@@auth/Register/SET_CHALLENGE_QUESTION',
  SET_CHALLENGE_QUESTION_SUCCESS = '@@auth/Register/SET_CHALLENGE_QUESTION_SUCCESS',
  SET_IDENTITY_TOKEN = '@@auth/Register/SET_IDENTITY_TOKEN',
  SET_IDLE = '@@auth/MyProfile/SET_IDLE',
  UPDATE_USER_PROFILE = '@@auth/MyProfile/UPDATE_USER_PROFILE',
  VALIDATE_IDENTITY = '@@auth/Forgot/VALIDATE_IDENTITY',
  VALIDATE_QUESTION = '@@auth/Forgot/VALIDATE_QUESTION',
  FETCH_USE_CASE_LIST = '@@auth/FETCH_USE_CASE_LIST',
  FETCH_USE_CASE_LIST_ERROR = '@@auth/FETCH_USE_CASE_LIST_ERROR',
  FETCH_USE_CASE_LIST_ONGOING = '@@auth/FETCH_USE_CASE_LIST_ONGOING',
  FETCH_USE_CASE_LIST_SUCCESS = '@@auth/FETCH_USE_CASE_LIST_SUCCESS',
  SET_SELECTED_USE_CASE = '@@auth/SET_SELECTED_USE_CASE',
  ACCOUNT_LOOKUP = '@@auth/ACCOUNT_LOOKUP',
  ACCOUNT_LOOKUP_SUCCESS = '@@auth/ACCOUNT_LOOKUP_SUCCESS',
  TRIGGER_PERSIST = '@@auth/TRIGGER_PERSIST',
}

export type AuthActionType = ReturnType<
  | typeof authError
  | typeof cleanUp
  | typeof fetchProfileSuccess
  | typeof logoutSuccess
  | typeof login
  | typeof loginSuccess
  | typeof logout
  | typeof refreshTokenSuccess
  | typeof resetError
  | typeof resetPassword
  | typeof setIdentityToken
  | typeof setIdle
  | typeof switchFacilityError
  | typeof switchFacilitySuccess
  | typeof setChallengeQuestionSuccess
  | typeof fetchUseCaseList
  | typeof fetchUseCaseListOngoing
  | typeof fetchUseCaseListSuccess
  | typeof fetchUseCaseListError
  | typeof setSelectedUseCase
  | typeof accountLookUp
  | typeof accountLookUpSuccess
  | typeof triggerPersist
>;
