import { SAME_SESSION_VERIFICATION_CONSTANTS } from './sameSessionVerificationConstants';
import type {
  SameSessionVerificationFormData,
  ValidationResult,
  EligibleVerifier,
} from '../types/sameSessionVerification';

/**
 * Validates the entire Same Session Verification form
 */
export const validateSameSessionVerificationForm = (
  formData: SameSessionVerificationFormData,
  isGroupAssignment: boolean,
): ValidationResult => {
  const errors: ValidationResult['errors'] = {};

  // Validate verifier selection (only for group assignments)
  if (isGroupAssignment && !formData.selectedVerifier) {
    errors.verifier = 'Please select a verifier';
  }

  // Validate action selection
  if (!formData.action) {
    errors.action = 'Please select an action (Accept or Reject)';
  }

  // Validate comments (required for rejection)
  if (formData.action === SAME_SESSION_VERIFICATION_CONSTANTS.VERIFICATION_ACTIONS.REJECT) {
    if (!formData.comments || formData.comments.trim().length === 0) {
      errors.comments = SAME_SESSION_VERIFICATION_CONSTANTS.ERROR_MESSAGES.MISSING_REJECTION_REASON;
    }
  }

  // Validate comment length
  if (
    formData.comments &&
    formData.comments.length > SAME_SESSION_VERIFICATION_CONSTANTS.MAX_COMMENT_LENGTH
  ) {
    errors.comments = `Comments must be less than ${SAME_SESSION_VERIFICATION_CONSTANTS.MAX_COMMENT_LENGTH} characters`;
  }

  // Validate password
  if (
    !formData.password ||
    formData.password.length < SAME_SESSION_VERIFICATION_CONSTANTS.MIN_PASSWORD_LENGTH
  ) {
    errors.password = 'Password is required';
  }

  return {
    isValid: Object.keys(errors).length === 0,
    errors,
  };
};

/**
 * Validates verifier selection
 */
export const validateVerifierSelection = (
  selectedVerifier: EligibleVerifier | null,
  eligibleVerifiers: EligibleVerifier[],
): boolean => {
  if (!selectedVerifier) return false;

  return eligibleVerifiers.some((verifier) => verifier.id === selectedVerifier.id);
};

/**
 * Validates comment content and length
 */
export const validateComments = (
  comments: string,
  isRequired: boolean = false,
): { isValid: boolean; error?: string } => {
  // Check if required
  if (isRequired && (!comments || comments.trim().length === 0)) {
    return {
      isValid: false,
      error: SAME_SESSION_VERIFICATION_CONSTANTS.ERROR_MESSAGES.MISSING_REJECTION_REASON,
    };
  }

  // Check length
  if (comments && comments.length > SAME_SESSION_VERIFICATION_CONSTANTS.MAX_COMMENT_LENGTH) {
    return {
      isValid: false,
      error: `Comments must be less than ${SAME_SESSION_VERIFICATION_CONSTANTS.MAX_COMMENT_LENGTH} characters`,
    };
  }

  return { isValid: true };
};

/**
 * Validates password strength (basic validation for Phase 1)
 */
export const validatePassword = (password: string): { isValid: boolean; error?: string } => {
  if (!password || password.length < SAME_SESSION_VERIFICATION_CONSTANTS.MIN_PASSWORD_LENGTH) {
    return {
      isValid: false,
      error: 'Password is required',
    };
  }

  // Additional password strength validation can be added here in future phases
  return { isValid: true };
};

/**
 * Validates parameter IDs
 */
export const validateParameterIds = (
  parameterResponseId: string,
  parameterId: string,
): { isValid: boolean; error?: string } => {
  if (!parameterResponseId || !parameterId) {
    return {
      isValid: false,
      error: 'Invalid parameter information',
    };
  }

  // Additional parameter validation can be added here
  return { isValid: true };
};

/**
 * Sanitizes comment input to prevent XSS and other security issues
 */
export const sanitizeComments = (comments: string): string => {
  if (!comments) return '';

  // Basic sanitization - remove potentially harmful characters
  return comments
    .replace(/[<>]/g, '') // Remove angle brackets
    .replace(/javascript:/gi, '') // Remove javascript: protocol
    .replace(/on\w+=/gi, '') // Remove event handlers
    .trim();
};

/**
 * Checks if form data has changed from initial state
 */
export const hasFormDataChanged = (
  currentData: SameSessionVerificationFormData,
  initialData: SameSessionVerificationFormData,
): boolean => {
  return (
    currentData.selectedVerifier?.id !== initialData.selectedVerifier?.id ||
    currentData.action !== initialData.action ||
    currentData.comments !== initialData.comments ||
    currentData.password !== initialData.password ||
    currentData.authMethod !== initialData.authMethod
  );
};

/**
 * Gets validation error message for a specific field
 */
export const getFieldErrorMessage = (
  field: keyof ValidationResult['errors'],
  errors: ValidationResult['errors'],
): string | undefined => {
  return errors[field];
};

/**
 * Checks if the form can be submitted
 */
export const canSubmitForm = (
  formData: SameSessionVerificationFormData,
  isGroupAssignment: boolean,
  isLoading: boolean = false,
): boolean => {
  if (isLoading) return false;

  const validation = validateSameSessionVerificationForm(formData, isGroupAssignment);
  return validation.isValid;
};
