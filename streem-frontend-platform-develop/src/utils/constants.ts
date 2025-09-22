export const fetchBaseUrl = () => {
  const { hostname, protocol } = window.location;

  let apiUrl = '';

  if (hostname === 'localhost' || hostname === '0.0.0.0') {
    apiUrl = process.env.API_URL ?? 'http://localhost:8080/v1';
  } else {
    apiUrl = `${protocol}//api.${hostname}/v1`;
  }

  return apiUrl;
};

// DO Not Delete Any Error Code Even If it looks unused or redundent.
export enum LoginErrorCodes {
  JWT_ACCESS_TOKEN_EXPIRED = '101104',
  JTI_TOKEN_REVOKED = '101102',
  USER_ARCHIVED = '104007',
  USER_LOCKED = '104012',
  INVALID_CREDENTIALS = '101003',
  JWT_TOKEN_EXPIRED = '101105',
  JWT_TOKEN_INVALID = '101101',
  PASSWORD_EXPIRED = '101004',
  USER_SELF_LOCKED = '104006',
  FORGOT_PASSWORD_TOKEN_EXPIRED = '101006',
  REGISTRATION_TOKEN_EXPIRED = '101005',
  USER_ACCOUNT_LOCKED = '104014',
  USER_INVITE_EXPIRED = '104010',
  SSO_INVALID_CREDENTIALS = '105001',
}

export enum ErrorCodesToLogout {
  JTI_TOKEN_REVOKED = '101102',
  USER_ARCHIVED = '104007',
  USER_LOCKED = '104012',
  USER_SELF_LOCKED = '104006',
  JWT_REFRESH_TOKEN_EXPIRED = '101103',
}

export const EXCULDE_BY_REGEX_FOR_NO_INTERNET_TOAST = [
  // This helps in Not calling the toaster for Stage Polling in Job Execution ie. `apiGetStageData` in api urls.
  '([/]jobs[/][0-9]+[/]stages[/]state[?]stageId=[0-9]+)',
];

export const isInDevelopment = process.env.NODE_ENV === 'development';

export const DEFAULT_SESSION_TIMEOUT_IN_MIN = 10;

export const ALL_FACILITY_ID = '-1';

export const validReasonRegex = /^\s*\S+(?:\s+\S+)*\s*$/;

export const DEFAULT_PAGE_NUMBER = 0;

export const DEFAULT_PAGE_SIZE = 25;

export const MAX_PAGE_SIZE = 250;

export const MIN_PAGE_SIZE = 10;

export const UNPAGED_PAGE_SIZE = 0;

export const EXPORT_LIMIT_LEVELS = {
  MEDIUM: 50000,
  HIGH: 100000,
};

export const DEFAULT_PAGINATION = {
  page: 0,
  pageSize: 10,
  numberOfElements: 0,
  totalPages: 0,
  totalElements: 0,
  first: true,
  last: true,
  empty: true,
};

export const SELECTOR_OPTIONS = [
  { label: 'Constant', value: 'CONSTANT' },
  { label: 'Parameter', value: 'PARAMETER' },
];

export enum EmojisUniCodes {
  CHECK = '2705',
  EYE = 'DC41',
  STAR = '2B50',
  TORCH = 'DD26',
  TOOLBOX = 'DDF0',
  HELMET = '26D1',
  GLASSES = 'DD7D',
  GLOVES = 'DDE4',
  VEST = 'DDBA',
  CANCEL = 'DEAB',
  STOP = '26D4',
  CLEAN = 'DEAF',
  RECYCLE = '267B',
  SOS = 'DD98',
  FLAG = 'DEA9',
  ELECTRIC = '26A1',
  FIRE = 'DD25',
  CAUTION = '26A0',
  HAND = '270B',
  BIN = 'DDD1',
  CROSS = '274E',
  LOCK = 'DD12',
}

export const emojis = [
  {
    value: '✅',
    name: 'Tick Mark',
  },
  {
    value: '👁',
    name: 'Inspect',
  },
  {
    value: '⭐',
    name: 'Star',
  },
  {
    value: '🔦',
    name: 'Torch',
  },
  {
    value: '🧰',
    name: 'Toolkit',
  },
  {
    value: '⛑',
    name: 'Safety Helmet',
  },
  {
    value: '🥽',
    name: 'Eye Glasses',
  },
  {
    value: '🧤',
    name: 'Hand Gloves',
  },
  {
    value: '🦺',
    name: 'Safety Vest',
  },
  {
    value: '🚫',
    name: 'Stop',
  },
  {
    value: '⛔',
    name: 'Not Allowed',
  },
  {
    value: '🚯',
    name: 'Keep Clean',
  },
  {
    value: '♻',
    name: 'Recycle',
  },
  {
    value: '🆘',
    name: 'SOS',
  },
  {
    value: '🚩',
    name: 'Important',
  },
  {
    value: '⚡',
    name: 'Electricity',
  },
  {
    value: '🔥',
    name: 'Fire',
  },
  {
    value: '⚠',
    name: 'Alert',
  },
  {
    value: '✋',
    name: 'Stop',
  },
  {
    value: '🗑',
    name: 'Waste Bin',
  },
  {
    value: '❎',
    name: 'Cross',
  },
  {
    value: '🔒',
    name: 'Protected',
  },
];

export const JOB_STAGE_POLLING_TIMEOUT = 3000;

export const nonEmptyStringRegex = /^\s*\S+(?:\s+\S+)*\s*$/;

export const NOTIFICATION_TIMEOUT = 5000;

export const SYSTEM_USER = {
  id: 'system',
  employeeId: 'SYSTEM',
  firstName: 'System',
  lastName: 'L',
  label: 'System L, ID : SYSTEM',
  value: '1',
};
