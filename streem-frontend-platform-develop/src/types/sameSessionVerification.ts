import {
  VerificationAction,
  AssignmentType,
  AuthMethod,
  VerificationState,
} from '../utils/sameSessionVerificationConstants';

// Core interfaces for Same Session Verification

export interface EligibleVerifier {
  id: string;
  name: string;
  employeeId: string;
  email?: string;
  department?: string;
  role?: string;
}

export interface SameSessionVerificationFormData {
  selectedVerifier: EligibleVerifier | null;
  action: VerificationAction | null;
  comments: string;
  password: string;
  authMethod: AuthMethod;
}

export interface SameSessionVerificationRequest {
  parameterResponseId: string;
  parameterId: string;
  verifierId: string;
  action: VerificationAction;
  comments?: string;
  authenticationData: {
    method: AuthMethod;
    password?: string;
    ssoToken?: string;
  };
}

export interface SameSessionVerificationResponse {
  success: boolean;
  verificationId?: string;
  message?: string;
  error?: {
    code: string;
    message: string;
    details?: any;
  };
}

export interface VerificationAssignment {
  type: AssignmentType;
  eligibleVerifiers: EligibleVerifier[];
  selectedVerifier?: EligibleVerifier;
}

export interface SameSessionVerificationState {
  // UI State
  isModalOpen: boolean;
  currentState: VerificationState;

  // Form Data
  formData: SameSessionVerificationFormData;

  // Assignment Data
  assignment: VerificationAssignment | null;

  // Error and Loading States
  loading: boolean;
  error: string | null;

  // Current verification context
  currentVerification: {
    parameterResponseId: string | null;
    parameterId: string | null;
    verifications: Record<string, any> | null;
  };
}

// Redux Action Types
export interface OpenSameSessionVerificationModalAction {
  type: 'OPEN_SAME_SESSION_VERIFICATION_MODAL';
  payload: {
    parameterResponseId: string;
    parameterId: string;
    verifications: Record<string, any>;
  };
}

export interface CloseSameSessionVerificationModalAction {
  type: 'CLOSE_SAME_SESSION_VERIFICATION_MODAL';
}

export interface UpdateFormDataAction {
  type: 'UPDATE_SAME_SESSION_VERIFICATION_FORM_DATA';
  payload: Partial<SameSessionVerificationFormData>;
}

export interface SetVerificationStateAction {
  type: 'SET_SAME_SESSION_VERIFICATION_STATE';
  payload: VerificationState;
}

export interface SetVerificationErrorAction {
  type: 'SET_SAME_SESSION_VERIFICATION_ERROR';
  payload: string | null;
}

export interface SetEligibleVerifiersAction {
  type: 'SET_ELIGIBLE_VERIFIERS';
  payload: EligibleVerifier[];
}

export interface SubmitVerificationAction {
  type: 'SUBMIT_SAME_SESSION_VERIFICATION';
  payload: SameSessionVerificationRequest;
}

export interface VerificationSuccessAction {
  type: 'SAME_SESSION_VERIFICATION_SUCCESS';
  payload: SameSessionVerificationResponse;
}

export interface VerificationFailureAction {
  type: 'SAME_SESSION_VERIFICATION_FAILURE';
  payload: string;
}

// Union type for all Same Session Verification actions
export type SameSessionVerificationAction =
  | OpenSameSessionVerificationModalAction
  | CloseSameSessionVerificationModalAction
  | UpdateFormDataAction
  | SetVerificationStateAction
  | SetVerificationErrorAction
  | SetEligibleVerifiersAction
  | SubmitVerificationAction
  | VerificationSuccessAction
  | VerificationFailureAction;

// API related types
export interface FetchEligibleVerifiersRequest {
  parameterResponseId: string;
  parameterId: string;
}

export interface FetchEligibleVerifiersResponse {
  verifiers: EligibleVerifier[];
  assignmentType: AssignmentType;
}

// Validation types
export interface ValidationResult {
  isValid: boolean;
  errors: {
    verifier?: string;
    action?: string;
    comments?: string;
    password?: string;
  };
}

// Hook types for custom hooks (to be implemented in later phases)
export interface UseSameSessionVerificationReturn {
  // State
  state: SameSessionVerificationState;

  // Actions
  openModal: (params: {
    parameterResponseId: string;
    parameterId: string;
    verifications: Record<string, any>;
  }) => void;
  closeModal: () => void;
  updateFormData: (data: Partial<SameSessionVerificationFormData>) => void;
  submitVerification: () => void;

  // Computed values
  isFormValid: boolean;
  canSubmit: boolean;
  isGroupAssignment: boolean;
}

// Component prop types
export interface SameSessionVerificationModalProps {
  parameterResponseId: string;
  parameterId: string;
  verifications: Record<string, any>;
}

export interface SameSessionVerificationButtonProps {
  parameterResponseId: string;
  parameterId: string;
  verifications: Record<string, any>;
  disabled?: boolean;
  className?: string;
}
