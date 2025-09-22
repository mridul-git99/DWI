// Define User interface locally to avoid import issues
interface User {
  id: string;
  firstName: string;
  lastName: string;
  employeeId: string;
  username?: string;
}

/**
 * Interface for parameter with verification data
 */
export interface ParameterWithVerification {
  id: string;
  label?: string;
  taskName?: string;
  response?: {
    id: string;
  };
  verifications?: {
    requestedTo?: User[];
  };
}

/**
 * Interface for common verifier result
 */
export interface CommonVerifierResult {
  commonVerifiers: User[];
  hasCommonVerifiers: boolean;
  totalParameters: number;
}

/**
 * Extract common verifiers across multiple parameters
 * @param parameters Array of parameters with verification data
 * @returns Object containing common verifiers and metadata
 */
export const extractCommonVerifiers = (
  parameters: ParameterWithVerification[],
): CommonVerifierResult => {
  if (!parameters || parameters.length === 0) {
    return {
      commonVerifiers: [],
      hasCommonVerifiers: false,
      totalParameters: 0,
    };
  }

  if (parameters.length === 1) {
    const singleParam = parameters[0];
    const verifiers = singleParam.verifications?.requestedTo || [];
    return {
      commonVerifiers: verifiers,
      hasCommonVerifiers: verifiers.length > 0,
      totalParameters: 1,
    };
  }

  // Get verifiers from first parameter as starting point
  const firstParameterVerifiers = parameters[0].verifications?.requestedTo || [];

  if (firstParameterVerifiers.length === 0) {
    return {
      commonVerifiers: [],
      hasCommonVerifiers: false,
      totalParameters: parameters.length,
    };
  }

  // Find intersection of verifiers across all parameters
  const commonVerifiers = firstParameterVerifiers.filter((verifier) => {
    // Check if this verifier exists in all other parameters
    return parameters.slice(1).every((param) => {
      const paramVerifiers = param.verifications?.requestedTo || [];
      return paramVerifiers.some((paramVerifier) => paramVerifier.id === verifier.id);
    });
  });

  return {
    commonVerifiers,
    hasCommonVerifiers: commonVerifiers.length > 0,
    totalParameters: parameters.length,
  };
};

/**
 * Validate if bulk same-session verification is possible
 * @param parameters Array of parameters
 * @returns Validation result with error message if invalid
 */
export const validateBulkSameSessionVerification = (
  parameters: ParameterWithVerification[],
): { isValid: boolean; errorMessage?: string } => {
  if (!parameters || parameters.length < 2) {
    return {
      isValid: false,
      errorMessage: 'At least 2 parameters are required for bulk verification',
    };
  }

  // Check if all parameters have verification data
  const parametersWithoutVerification = parameters.filter(
    (param) => !param.verifications?.requestedTo || param.verifications.requestedTo.length === 0,
  );

  if (parametersWithoutVerification.length > 0) {
    return {
      isValid: false,
      errorMessage: 'All parameters must have assigned verifiers',
    };
  }

  const { hasCommonVerifiers } = extractCommonVerifiers(parameters);

  if (!hasCommonVerifiers) {
    return {
      isValid: false,
      errorMessage: 'No common verifiers found across selected parameters',
    };
  }

  return { isValid: true };
};

/**
 * Format parameter list for display in modal
 * @param parameters Array of parameters
 * @returns Formatted parameter list
 */
export const formatParametersForDisplay = (
  parameters: ParameterWithVerification[],
): Array<{ id: string; name: string; taskName?: string }> => {
  return parameters.map((param) => ({
    id: param.id,
    name: param.label || `Parameter ${param.id}`,
    taskName: param.taskName || 'Unknown Task',
  }));
};

/**
 * Get verifier assignment summary for parameters
 * @param parameters Array of parameters
 * @returns Summary of verifier assignments
 */
export const getVerifierAssignmentSummary = (
  parameters: ParameterWithVerification[],
): Record<string, string[]> => {
  const summary: Record<string, string[]> = {};

  parameters.forEach((param) => {
    const verifiers = param.verifications?.requestedTo || [];
    const verifierNames = verifiers.map((v) => `${v.firstName} ${v.lastName}`);
    summary[param.id] = verifierNames;
  });

  return summary;
};

/**
 * Constants for bulk same-session verification
 */
export const BULK_SAME_SESSION_CONSTANTS = {
  MIN_PARAMETERS: 2,
  MAX_PARAMETERS: 50, // Performance limit
  MODAL_STEPS: {
    VERIFIER_SELECTION: 'verifier-selection',
    ACTION_SELECTION: 'action-selection',
    APPROVE_CONFIRMATION: 'approve-confirmation',
    REJECT_WITH_REASON: 'reject-with-reason',
    PROCESSING: 'processing',
    SUCCESS: 'success',
    ERROR: 'error',
  },
} as const;

/**
 * Type definitions for bulk same-session verification
 */
export type BulkVerificationAction = 'approve' | 'reject';

export interface BulkSameSessionVerificationData {
  parameters: ParameterWithVerification[];
  selectedVerifier: User;
  action: BulkVerificationAction;
  reason?: string;
  password?: string;
}

export interface BulkSameSessionModalStep {
  id: string;
  title: string;
  description: string;
  canGoBack: boolean;
  canGoNext: boolean;
}

/**
 * Get modal steps configuration
 */
export const getBulkSameSessionModalSteps = (): BulkSameSessionModalStep[] => [
  {
    id: BULK_SAME_SESSION_CONSTANTS.MODAL_STEPS.VERIFIER_SELECTION,
    title: 'Select Verifier',
    description: 'Choose a verifier from common verifiers across all parameters',
    canGoBack: false,
    canGoNext: true,
  },
  {
    id: BULK_SAME_SESSION_CONSTANTS.MODAL_STEPS.ACTION_SELECTION,
    title: 'Choose Action',
    description: 'Select whether to approve or reject all parameters',
    canGoBack: true,
    canGoNext: true,
  },
  {
    id: BULK_SAME_SESSION_CONSTANTS.MODAL_STEPS.APPROVE_CONFIRMATION,
    title: 'Confirm Approval',
    description: 'Enter password to approve all selected parameters',
    canGoBack: true,
    canGoNext: false,
  },
  {
    id: BULK_SAME_SESSION_CONSTANTS.MODAL_STEPS.REJECT_WITH_REASON,
    title: 'Reject with Reason',
    description: 'Provide reason and password to reject all parameters',
    canGoBack: true,
    canGoNext: false,
  },
  {
    id: BULK_SAME_SESSION_CONSTANTS.MODAL_STEPS.PROCESSING,
    title: 'Processing',
    description: 'Processing bulk verification...',
    canGoBack: false,
    canGoNext: false,
  },
];
