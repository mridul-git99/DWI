export const SAME_SESSION_VERIFICATION_CONSTANTS = {
  // Timeouts and polling intervals (in milliseconds)
  VERIFICATION_TIMEOUT: 300000, // 5 minutes
  STATUS_POLLING_INTERVAL: 2000, // 2 seconds
  AUTHENTICATION_TIMEOUT: 30000, // 30 seconds

  // Form validation
  MAX_COMMENT_LENGTH: 500,
  MIN_PASSWORD_LENGTH: 1,

  // UI constants
  MODAL_MAX_WIDTH: 'md',
  MODAL_MIN_HEIGHT: '400px',

  // Error messages
  ERROR_MESSAGES: {
    AUTHENTICATION_FAILED: 'Authentication failed. Please check your credentials.',
    VERIFICATION_TIMEOUT: 'Verification request timed out. Please try again.',
    INVALID_VERIFIER: 'Selected verifier is not valid.',
    MISSING_REJECTION_REASON: 'Please provide a reason for rejection.',
    NETWORK_ERROR: 'Network error occurred. Please try again.',
    CONCURRENT_VERIFICATION: 'This parameter has already been verified by another user.',
    INSUFFICIENT_PERMISSIONS: 'You do not have permission to perform this action.',
  },

  // Success messages
  SUCCESS_MESSAGES: {
    VERIFICATION_COMPLETED: 'Verification completed successfully.',
    AUTHENTICATION_SUCCESS: 'Authentication successful.',
  },

  // Action types
  VERIFICATION_ACTIONS: {
    ACCEPT: 'ACCEPT',
    REJECT: 'REJECT',
  } as const,

  // Assignment types
  ASSIGNMENT_TYPES: {
    SINGLE_USER: 'SINGLE_USER',
    GROUP: 'GROUP',
  } as const,

  // Authentication methods
  AUTH_METHODS: {
    PASSWORD: 'PASSWORD',
    SSO: 'SSO',
  } as const,

  // Verification states
  VERIFICATION_STATES: {
    IDLE: 'IDLE',
    AUTHENTICATING: 'AUTHENTICATING',
    SUBMITTING: 'SUBMITTING',
    SUCCESS: 'SUCCESS',
    ERROR: 'ERROR',
  } as const,
} as const;

// Type exports for better type safety
export type VerificationAction =
  typeof SAME_SESSION_VERIFICATION_CONSTANTS.VERIFICATION_ACTIONS[keyof typeof SAME_SESSION_VERIFICATION_CONSTANTS.VERIFICATION_ACTIONS];
export type AssignmentType =
  typeof SAME_SESSION_VERIFICATION_CONSTANTS.ASSIGNMENT_TYPES[keyof typeof SAME_SESSION_VERIFICATION_CONSTANTS.ASSIGNMENT_TYPES];
export type AuthMethod =
  typeof SAME_SESSION_VERIFICATION_CONSTANTS.AUTH_METHODS[keyof typeof SAME_SESSION_VERIFICATION_CONSTANTS.AUTH_METHODS];
export type VerificationState =
  typeof SAME_SESSION_VERIFICATION_CONSTANTS.VERIFICATION_STATES[keyof typeof SAME_SESSION_VERIFICATION_CONSTANTS.VERIFICATION_STATES];
