# Utility Layer Analysis - Same Session Verification

**Document Version:** 1.0  
**Date:** January 19, 2025  
**Layer:** Utilities, Constants, Helpers, Types  
**Impact Level:** Low-Medium  

---

## **Overview**

This document analyzes the impact of Same Session Verification feature on the utility layer, including constants, helper functions, type definitions, validation utilities, and common patterns.

---

## **Current State Analysis**

### **Existing Utility Structure**
```
src/utils/
├── constants.ts ⚠️ MODIFICATION REQUIRED
├── globalTypes.ts ⚠️ MODIFICATION REQUIRED
├── stringUtils.ts ✅ NO CHANGES
├── timeUtils.ts ✅ NO CHANGES
├── parameterUtils.ts ✅ NO CHANGES
├── request.ts ✅ NO CHANGES
└── index.ts ⚠️ MODIFICATION REQUIRED

src/types/
├── index.ts ⚠️ MODIFICATION REQUIRED
├── verifications.ts ✅ NO CHANGES (already has required types)
├── common.ts ✅ NO CHANGES
└── parameter.ts ✅ NO CHANGES
```

### **Current Constants**
```typescript
// Existing constants in constants.ts
export const DEFAULT_PAGE_SIZE = 25;
export const DEFAULT_PAGE_NUMBER = 0;
export const JOB_STAGE_POLLING_TIMEOUT = 5000;
export const MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
```

### **Current Global Types**
```typescript
// Existing types in globalTypes.ts
export enum InputTypes {
  SINGLE_LINE = 'SINGLE_LINE',
  MULTI_LINE = 'MULTI_LINE',
  NUMBER = 'NUMBER',
  DATE = 'DATE',
  DATE_TIME = 'DATE_TIME',
  PASSWORD = 'PASSWORD',
}

export enum SsoStates {
  SELF_VERIFICATION = 'SELF_VERIFICATION',
  PEER_VERIFICATION = 'PEER_VERIFICATION',
  BULK_SELF_VERIFICATION = 'BULK_SELF_VERIFICATION',
  BULK_PEER_VERIFICATION = 'BULK_PEER_VERIFICATION',
}
```

---

## **Required Changes**

### **1. Constants Updates**

#### **constants.ts Updates** ⚠️ MODIFICATION REQUIRED
**File:** `src/utils/constants.ts`

**Add new constants:**
```typescript
// Same Session Verification Constants

/**
 * Maximum time allowed for same session verification (in milliseconds)
 * After this time, verification falls back to traditional method
 */
export const SAME_SESSION_VERIFICATION_TIMEOUT = 15 * 60 * 1000; // 15 minutes

/**
 * Polling interval for verification status checks (in milliseconds)
 */
export const VERIFICATION_STATUS_POLLING_INTERVAL = 5000; // 5 seconds

/**
 * Maximum number of authentication attempts before lockout
 */
export const MAX_AUTHENTICATION_ATTEMPTS = 3;

/**
 * Token refresh threshold (in milliseconds before expiry)
 */
export const TOKEN_REFRESH_THRESHOLD = 5 * 60 * 1000; // 5 minutes

/**
 * Maximum comment length for verification rejection
 */
export const MAX_VERIFICATION_COMMENT_LENGTH = 500;

/**
 * Minimum comment length for verification rejection
 */
export const MIN_VERIFICATION_COMMENT_LENGTH = 10;

/**
 * Cache TTL for eligible verifiers (in milliseconds)
 */
export const ELIGIBLE_VERIFIERS_CACHE_TTL = 10 * 60 * 1000; // 10 minutes

/**
 * Cache TTL for verification status (in milliseconds)
 */
export const VERIFICATION_STATUS_CACHE_TTL = 30 * 1000; // 30 seconds

/**
 * Debounce delay for API calls (in milliseconds)
 */
export const API_DEBOUNCE_DELAY = 300;

/**
 * Request timeout for same session verification APIs (in milliseconds)
 */
export const SAME_SESSION_API_TIMEOUT = 30 * 1000; // 30 seconds

/**
 * Maximum concurrent verification requests
 */
export const MAX_CONCURRENT_VERIFICATIONS = 5;

/**
 * Session cleanup delay after verification completion (in milliseconds)
 */
export const SESSION_CLEANUP_DELAY = 2000; // 2 seconds

/**
 * Error retry attempts for recoverable errors
 */
export const ERROR_RETRY_ATTEMPTS = 3;

/**
 * Error retry delay (in milliseconds)
 */
export const ERROR_RETRY_DELAY = 1000; // 1 second

/**
 * Modal auto-close delay for success messages (in milliseconds)
 */
export const SUCCESS_MODAL_AUTO_CLOSE_DELAY = 3000; // 3 seconds
```

### **2. Global Types Updates**

#### **globalTypes.ts Updates** ⚠️ MODIFICATION REQUIRED
**File:** `src/utils/globalTypes.ts`

**Add new SSO states:**
```typescript
export enum SsoStates {
  // ... existing states
  
  // Same Session Verification States
  SAME_SESSION_VERIFICATION = 'SAME_SESSION_VERIFICATION',
  SAME_SESSION_PEER_VERIFICATION = 'SAME_SESSION_PEER_VERIFICATION',
  SAME_SESSION_SELF_VERIFICATION = 'SAME_SESSION_SELF_VERIFICATION',
}

/**
 * Same session verification specific types
 */
export enum VerificationActionType {
  ACCEPT = 'ACCEPT',
  REJECT = 'REJECT',
}

export enum VerificationAssignmentType {
  SINGLE_USER = 'SINGLE_USER',
  USER_GROUP = 'USER_GROUP',
}

export enum AuthenticationMethod {
  PASSWORD = 'PASSWORD',
  SSO = 'SSO',
}

export enum VerificationErrorType {
  AUTHENTICATION = 'AUTHENTICATION',
  VALIDATION = 'VALIDATION',
  NETWORK = 'NETWORK',
  CONCURRENT = 'CONCURRENT',
  TIMEOUT = 'TIMEOUT',
}

export enum VerificationModalState {
  INITIAL = 'INITIAL',
  LOADING_VERIFIERS = 'LOADING_VERIFIERS',
  FORM_READY = 'FORM_READY',
  AUTHENTICATING = 'AUTHENTICATING',
  SUBMITTING = 'SUBMITTING',
  SUCCESS = 'SUCCESS',
  ERROR = 'ERROR',
  COMPLETED_BY_OTHER = 'COMPLETED_BY_OTHER',
}
```

### **3. New Type Definitions**

#### **Same Session Verification Types** 🆕 NEW TYPES
**File:** `src/types/sameSessionVerification.ts` (new file)

```typescript
import { User } from '#store/users/types';
import { ParameterVerificationStatus, ParameterVerificationTypeEnum } from './verifications';

/**
 * Core same session verification types
 */
export interface SameSessionVerificationConfig {
  enabled: boolean;
  timeout: number;
  maxAttempts: number;
  allowedRoles: string[];
  excludedRoles: string[];
}

export interface VerificationContext {
  parameterResponseId: string;
  parameterId: string;
  parameterName: string;
  parameterValue: string;
  parameterSpecification?: string;
  initiatorId: string;
  initiatorName: string;
  jobId: string;
  taskId: string;
  taskName: string;
}

export interface VerificationAssignment {
  type: 'SINGLE_USER' | 'USER_GROUP';
  assignedTo: {
    id: string;
    name: string;
    type: 'USER' | 'GROUP';
  };
  eligibleVerifiers: EligibleVerifier[];
  createdAt: string;
  expiresAt: string;
}

export interface EligibleVerifier {
  id: string;
  firstName: string;
  lastName: string;
  employeeId: string;
  email: string;
  department?: string;
  roles: string[];
  ssoEnabled: boolean;
  isActive: boolean;
  lastLoginAt?: string;
}

export interface VerificationCredentials {
  method: 'PASSWORD' | 'SSO';
  username?: string;
  password?: string;
  ssoToken?: string;
  rememberMe?: boolean;
}

export interface VerificationAction {
  type: 'ACCEPT' | 'REJECT';
  comments?: string;
  timestamp: number;
  verifierId: string;
  verifierName: string;
}

export interface VerificationResult {
  success: boolean;
  verificationId: string;
  status: ParameterVerificationStatus;
  verifiedBy: {
    id: string;
    name: string;
    employeeId: string;
  };
  verifiedAt: string;
  comments?: string;
  auditTrail: VerificationAuditTrail;
}

export interface VerificationAuditTrail {
  initiatorId: string;
  initiatorName: string;
  verifierId: string;
  verifierName: string;
  sessionContext: 'same_session' | 'traditional';
  ipAddress?: string;
  userAgent?: string;
  timestamp: string;
  action: 'ACCEPT' | 'REJECT';
  comments?: string;
}

export interface VerificationError {
  code: string;
  message: string;
  type: 'AUTHENTICATION' | 'VALIDATION' | 'NETWORK' | 'CONCURRENT' | 'TIMEOUT';
  details?: any;
  timestamp: string;
  recoverable: boolean;
  suggestedAction?: string;
}

export interface VerificationFormData {
  selectedVerifier: EligibleVerifier | null;
  action: 'ACCEPT' | 'REJECT' | null;
  comments: string;
  credentials: VerificationCredentials;
}

export interface VerificationModalProps {
  parameterResponseId: string;
  parameterId: string;
  verifications: any;
  closeOverlay: () => void;
  closeAllOverlays: () => void;
}

export interface VerificationStatusUpdate {
  parameterResponseId: string;
  status: ParameterVerificationStatus;
  verifiedBy?: {
    id: string;
    name: string;
    employeeId: string;
  };
  verifiedAt?: string;
  comments?: string;
}
```

### **4. Validation Utilities**

#### **Verification Validation Utilities** 🆕 NEW UTILITIES
**File:** `src/utils/verificationValidation.ts` (new file)

```typescript
import { 
  MAX_VERIFICATION_COMMENT_LENGTH, 
  MIN_VERIFICATION_COMMENT_LENGTH 
} from './constants';
import { EligibleVerifier, VerificationCredentials, VerificationFormData } from '#types/sameSessionVerification';

/**
 * Validation utilities for same session verification
 */
export class VerificationValidator {
  /**
   * Validate verifier selection
   */
  static validateVerifierSelection(
    verifier: EligibleVerifier | null,
    isGroupAssignment: boolean
  ): { isValid: boolean; error?: string } {
    if (isGroupAssignment && !verifier) {
      return {
        isValid: false,
        error: 'Please select a verifier from the list',
      };
    }

    if (verifier && !verifier.isActive) {
      return {
        isValid: false,
        error: 'Selected verifier is not active',
      };
    }

    if (verifier && verifier.roles.includes('OPERATOR')) {
      return {
        isValid: false,
        error: 'Operators cannot perform peer verification',
      };
    }

    return { isValid: true };
  }

  /**
   * Validate verification action
   */
  static validateAction(action: 'ACCEPT' | 'REJECT' | null): { isValid: boolean; error?: string } {
    if (!action) {
      return {
        isValid: false,
        error: 'Please select Accept or Reject',
      };
    }

    return { isValid: true };
  }

  /**
   * Validate comments for rejection
   */
  static validateComments(
    comments: string,
    action: 'ACCEPT' | 'REJECT' | null
  ): { isValid: boolean; error?: string } {
    if (action === 'REJECT') {
      if (!comments || comments.trim().length === 0) {
        return {
          isValid: false,
          error: 'Comments are required when rejecting verification',
        };
      }

      if (comments.trim().length < MIN_VERIFICATION_COMMENT_LENGTH) {
        return {
          isValid: false,
          error: `Comments must be at least ${MIN_VERIFICATION_COMMENT_LENGTH} characters`,
        };
      }

      if (comments.length > MAX_VERIFICATION_COMMENT_LENGTH) {
        return {
          isValid: false,
          error: `Comments cannot exceed ${MAX_VERIFICATION_COMMENT_LENGTH} characters`,
        };
      }
    }

    return { isValid: true };
  }

  /**
   * Validate credentials
   */
  static validateCredentials(
    credentials: VerificationCredentials,
    ssoEnabled: boolean
  ): { isValid: boolean; error?: string } {
    if (ssoEnabled && credentials.method === 'SSO') {
      if (!credentials.ssoToken) {
        return {
          isValid: false,
          error: 'SSO authentication is required',
        };
      }
    } else if (credentials.method === 'PASSWORD') {
      if (!credentials.password || credentials.password.length === 0) {
        return {
          isValid: false,
          error: 'Password is required',
        };
      }

      if (credentials.password.length < 6) {
        return {
          isValid: false,
          error: 'Password must be at least 6 characters',
        };
      }
    } else {
      return {
        isValid: false,
        error: 'Please provide authentication credentials',
      };
    }

    return { isValid: true };
  }

  /**
   * Validate complete form
   */
  static validateForm(
    formData: VerificationFormData,
    isGroupAssignment: boolean,
    ssoEnabled: boolean
  ): { isValid: boolean; errors: Record<string, string> } {
    const errors: Record<string, string> = {};

    // Validate verifier selection
    const verifierValidation = this.validateVerifierSelection(
      formData.selectedVerifier,
      isGroupAssignment
    );
    if (!verifierValidation.isValid) {
      errors.verifier = verifierValidation.error!;
    }

    // Validate action
    const actionValidation = this.validateAction(formData.action);
    if (!actionValidation.isValid) {
      errors.action = actionValidation.error!;
    }

    // Validate comments
    const commentsValidation = this.validateComments(formData.comments, formData.action);
    if (!commentsValidation.isValid) {
      errors.comments = commentsValidation.error!;
    }

    // Validate credentials
    const credentialsValidation = this.validateCredentials(formData.credentials, ssoEnabled);
    if (!credentialsValidation.isValid) {
      errors.credentials = credentialsValidation.error!;
    }

    return {
      isValid: Object.keys(errors).length === 0,
      errors,
    };
  }

  /**
   * Validate parameter context
   */
  static validateParameterContext(context: any): { isValid: boolean; error?: string } {
    if (!context.parameterResponseId) {
      return {
        isValid: false,
        error: 'Parameter response ID is required',
      };
    }

    if (!context.parameterId) {
      return {
        isValid: false,
        error: 'Parameter ID is required',
      };
    }

    return { isValid: true };
  }

  /**
   * Sanitize comments input
   */
  static sanitizeComments(comments: string): string {
    return comments
      .trim()
      .replace(/\s+/g, ' ') // Replace multiple spaces with single space
      .replace(/[<>]/g, '') // Remove potential HTML tags
      .substring(0, MAX_VERIFICATION_COMMENT_LENGTH);
  }

  /**
   * Check if verification is expired
   */
  static isVerificationExpired(createdAt: string, timeoutMs: number): boolean {
    const createdTime = new Date(createdAt).getTime();
    const currentTime = Date.now();
    return (currentTime - createdTime) > timeoutMs;
  }
}
```

### **5. Helper Functions**

#### **Same Session Verification Helpers** 🆕 NEW HELPERS
**File:** `src/utils/sameSessionHelpers.ts` (new file)

```typescript
import { EligibleVerifier, VerificationAssignment } from '#types/sameSessionVerification';
import { getFullName } from './stringUtils';
import { formatDateTime } from './timeUtils';

/**
 * Helper functions for same session verification
 */
export class SameSessionHelpers {
  /**
   * Format verifier display name
   */
  static formatVerifierDisplayName(verifier: EligibleVerifier): string {
    return `${getFullName(verifier)} (ID: ${verifier.employeeId})`;
  }

  /**
   * Format verifier option for dropdown
   */
  static formatVerifierOption(verifier: EligibleVerifier) {
    return {
      label: this.formatVerifierDisplayName(verifier),
      value: verifier.id,
      verifier: verifier,
      isDisabled: !verifier.isActive,
    };
  }

  /**
   * Filter eligible verifiers
   */
  static filterEligibleVerifiers(
    verifiers: EligibleVerifier[],
    initiatorId: string
  ): EligibleVerifier[] {
    return verifiers.filter(verifier => 
      verifier.id !== initiatorId && // Exclude initiator
      verifier.isActive && // Only active users
      !verifier.roles.includes('OPERATOR') // Exclude operators
    );
  }

  /**
   * Group verifiers by department
   */
  static groupVerifiersByDepartment(verifiers: EligibleVerifier[]) {
    return verifiers.reduce((groups, verifier) => {
      const department = verifier.department || 'Other';
      if (!groups[department]) {
        groups[department] = [];
      }
      groups[department].push(verifier);
      return groups;
    }, {} as Record<string, EligibleVerifier[]>);
  }

  /**
   * Sort verifiers by name
   */
  static sortVerifiersByName(verifiers: EligibleVerifier[]): EligibleVerifier[] {
    return [...verifiers].sort((a, b) => {
      const nameA = getFullName(a).toLowerCase();
      const nameB = getFullName(b).toLowerCase();
      return nameA.localeCompare(nameB);
    });
  }

  /**
   * Check if assignment is for single user
   */
  static isSingleUserAssignment(assignment: VerificationAssignment): boolean {
    return assignment.type === 'SINGLE_USER';
  }

  /**
   * Check if assignment is for user group
   */
  static isGroupAssignment(assignment: VerificationAssignment): boolean {
    return assignment.type === 'USER_GROUP';
  }

  /**
   * Get assignment display text
   */
  static getAssignmentDisplayText(assignment: VerificationAssignment): string {
    if (this.isSingleUserAssignment(assignment)) {
      return `Assigned to: ${assignment.assignedTo.name}`;
    } else {
      return `Assigned to group: ${assignment.assignedTo.name}`;
    }
  }

  /**
   * Format verification expiry time
   */
  static formatExpiryTime(expiresAt: string): string {
    return formatDateTime({ value: expiresAt, type: 'DATE_TIME' });
  }

  /**
   * Calculate time remaining for verification
   */
  static getTimeRemaining(expiresAt: string): {
    isExpired: boolean;
    timeRemaining: string;
    urgency: 'low' | 'medium' | 'high';
  } {
    const expiryTime = new Date(expiresAt).getTime();
    const currentTime = Date.now();
    const timeDiff = expiryTime - currentTime;

    if (timeDiff <= 0) {
      return {
        isExpired: true,
        timeRemaining: 'Expired',
        urgency: 'high',
      };
    }

    const minutes = Math.floor(timeDiff / (1000 * 60));
    const hours = Math.floor(minutes / 60);

    let timeRemaining: string;
    let urgency: 'low' | 'medium' | 'high';

    if (hours > 0) {
      timeRemaining = `${hours}h ${minutes % 60}m remaining`;
      urgency = hours > 2 ? 'low' : 'medium';
    } else {
      timeRemaining = `${minutes}m remaining`;
      urgency = minutes > 15 ? 'medium' : 'high';
    }

    return {
      isExpired: false,
      timeRemaining,
      urgency,
    };
  }

  /**
   * Generate verification summary
   */
  static generateVerificationSummary(
    parameterName: string,
    verifierName: string,
    action: 'ACCEPT' | 'REJECT',
    comments?: string
  ): string {
    const actionText = action === 'ACCEPT' ? 'accepted' : 'rejected';
    let summary = `Parameter "${parameterName}" has been ${actionText} by ${verifierName}`;
    
    if (comments && action === 'REJECT') {
      summary += ` with reason: "${comments}"`;
    }
    
    return summary;
  }

  /**
   * Create verification audit message
   */
  static createAuditMessage(
    initiatorName: string,
    verifierName: string,
    parameterName: string,
    action: 'ACCEPT' | 'REJECT'
  ): string {
    const actionText = action === 'ACCEPT' ? 'verified' : 'rejected';
    return `Parameter "${parameterName}" ${actionText} by ${verifierName} in the session of initiator ${initiatorName}`;
  }

  /**
   * Get verification icon based on status
   */
  static getVerificationIcon(status: string): string {
    const iconMap: Record<string, string> = {
      'PENDING': '⏳',
      'ACCEPTED': '✅',
      'REJECTED': '❌',
      'RECALLED': '↩️',
      'EXPIRED': '⏰',
    };
    
    return iconMap[status] || '❓';
  }

  /**
   * Get verification color based on status
   */
  static getVerificationColor(status: string): string {
    const colorMap: Record<string, string> = {
      'PENDING': '#F1C21B',
      'ACCEPTED': '#24A148',
      'REJECTED': '#DA1E28',
      'RECALLED': '#6C6C6C',
      'EXPIRED': '#FF8C00',
    };
    
    return colorMap[status] || '#6C6C6C';
  }

  /**
   * Check if user can perform same session verification
   */
  static canPerformSameSessionVerification(
    user: any,
    assignment: VerificationAssignment
  ): boolean {
    if (!user || !user.id) {
      return false;
    }

    // Check if user is in eligible verifiers list
    return assignment.eligibleVerifiers.some(verifier => 
      verifier.id === user.id && verifier.isActive
    );
  }

  /**
   * Generate request ID for tracking
   */
  static generateRequestId(): string {
    return `ssv_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
  }

  /**
   * Mask sensitive data for logging
   */
  static maskSensitiveData(data: any): any {
    const masked = { ...data };
    
    if (masked.password) {
      masked.password = '***';
    }
    
    if (masked.ssoToken) {
      masked.ssoToken = masked.ssoToken.substring(0, 10) + '...';
    }
    
    if (masked.accessToken) {
      masked.accessToken = masked.accessToken.substring(0, 10) + '...';
    }
    
    return masked;
  }
}
```

### **6. Custom Hooks**

#### **Same Session Verification Hooks** 🆕 NEW HOOKS
**File:** `src/hooks/useSameSessionVerification.ts` (new file)

```typescript
import { useState, useEffect, useCallback, useRef } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { 
  EligibleVerifier, 
  VerificationFormData, 
  VerificationError 
} from '#types/sameSessionVerification';
import { jobActions } from '#views/Job/jobStore';
import { VerificationValidator } from '#utils/verificationValidation';
import { SameSessionHelpers } from '#utils/sameSessionHelpers';
import { 
  VERIFICATION_STATUS_POLLING_INTERVAL,
  SAME_SESSION_VERIFICATION_TIMEOUT 
} from '#utils/constants';

/**
 * Custom hook for same session verification logic
 */
export const useSameSessionVerification = (
  parameterResponseId: string,
  parameterId: string
) => {
  const dispatch = useDispatch();
  const intervalRef = useRef<NodeJS.Timeout | null>(null);
  
  // Redux state
  const {
    eligibleVerifiers,
    loading,
    authenticating,
    submitting,
    error,
    errorType,
  } = useSelector((state: any) => state.job.sameSessionVerification);
  
  // Local state
  const [formData, setFormData] = useState<VerificationFormData>({
    selectedVerifier: null,
    action: null,
    comments: '',
    credentials: {
      method: 'PASSWORD',
      username: '',
      password: '',
    },
  });
  
  const [validationErrors, setValidationErrors] = useState<Record<string, string>>({});
  const [isGroupAssignment, setIsGroupAssignment] = useState(false);
  const [verificationCompleted, setVerificationCompleted] = useState(false);

  /**
   * Fetch eligible verifiers on mount
   */
  useEffect(() => {
    dispatch(jobActions.fetchEligibleVerifiers({ parameterResponseId }));
  }, [dispatch, parameterResponseId]);

  /**
   * Determine assignment type and set default verifier
   */
  useEffect(() => {
    if (eligibleVerifiers.length > 0) {
      const isGroup = eligibleVerifiers.length > 1;
      setIsGroupAssignment(isGroup);
      
      // For single user assignment, auto-select the verifier
      if (!isGroup) {
        setFormData(prev => ({
          ...prev,
          selectedVerifier: eligibleVerifiers[0],
        }));
      }
    }
  }, [eligibleVerifiers]);

  /**
   * Start polling for verification status
   */
  const startStatusPolling = useCallback(() => {
    if (intervalRef.current) {
      clearInterval(intervalRef.current);
    }
    
    intervalRef.current = setInterval(() => {
      // Check if verification was completed by another user
      // This would be implemented based on your polling logic
    }, VERIFICATION_STATUS_POLLING_INTERVAL);
  }, [parameterResponseId]);

  /**
   * Stop polling
   */
  const stopStatusPolling = useCallback(() => {
    if (intervalRef.current) {
      clearInterval(intervalRef.current);
      intervalRef.current = null;
    }
  }, []);

  /**
   * Cleanup on unmount
   */
  useEffect(() => {
    return () => {
      stopStatusPolling();
      dispatch(jobActions.clearSameSessionVerificationState());
    };
  }, [dispatch, stopStatusPolling]);

  /**
   * Update form field
   */
  const updateFormField = useCallback((field: keyof VerificationFormData, value: any) => {
    setFormData(prev => ({
      ...prev,
      [field]: value,
    }));
    
    // Clear validation error for this field
    if (validationErrors[field]) {
      setValidationErrors(prev => {
        const newErrors = { ...prev };
        delete newErrors[field];
        return newErrors;
      });
    }
  }, [validationErrors]);

  /**
   * Update credentials
   */
  const updateCredentials = useCallback((credentials: Partial<VerificationFormData['credentials']>) => {
    setFormData(prev => ({
      ...prev,
      credentials: {
        ...prev.credentials,
        ...credentials,
      },
    }));
  }, []);

  /**
   * Validate form
   */
  const validateForm = useCallback(() => {
    const ssoEnabled = formData.selectedVerifier?.ssoEnabled || false;
    const validation = VerificationValidator.validateForm(
      formData,
      isGroupAssignment,
      ssoEnabled
    );
    
    setValidationErrors(validation.errors);
    return validation.isValid;
  }, [formData, isGroupAssignment]);

  /**
   * Submit verification
   */
  const submitVerification = useCallback(() => {
    if (!validateForm()) {
      return;
    }

    const payload = {
      parameterResponseId,
      parameterId,
      verifierId: formData.selectedVerifier!.id,
      action: formData.action!,
      comments: formData.action === 'REJECT' ? formData.comments : undefined,
      password: formData.credentials.method === 'PASSWORD' ? formData.credentials.password : undefined,
      ssoToken: formData.credentials.method === 'SSO' ? formData.credentials.ssoToken : undefined,
    };

    dispatch(jobActions.completeSameSessionVerification(payload));
    startStatusPolling();
  }, [
    validateForm,
    parameterResponseId,
    parameterId,
    formData,
    dispatch,
    startStatusPolling,
  ]);

  /**
   * Reset form
   */
  const resetForm = useCallback(() => {
    setFormData({
      selectedVerifier: isGroupAssignment ? null : eligibleVerifiers[0] || null,
      action: null,
      comments: '',
      credentials: {
        method: 'PASSWORD',
        username: '',
        password: '',
      },
    });
    setValidationErrors({});
  }, [isGroupAssignment, eligibleVerifiers]);

  /**
   * Check if form is valid
   */
  const isFormValid = useCallback(() => {
    const hasVerifier = isGroupAssignment ? formData.selectedVerifier : true;
    const hasAction = formData.action !== null;
    const hasComments = formData.action === 'REJECT' ? formData.comments.trim().length > 0 : true;
    const hasAuth = formData.credentials.password || formData.credentials.ssoToken;
    
    return hasVerifier && hasAction && hasComments && hasAuth && !loading && !authenticating && !submitting;
  }, [formData, isGroupAssignment, loading, authenticating, submitting]);

  return {
    // State
    formData,
    validationErrors,
    isGroupAssignment,
    verificationCompleted,
    
    // Redux state
    eligibleVerifiers,
    loading,
    authenticating,
    submitting,
    error,
    errorType,
    
    // Actions
    updateFormField,
    updateCredentials,
    submitVerification,
    resetForm,
    validateForm,
    startStatusPolling,
    stopStatusPolling,
    
    // Computed
    isFormValid: isFormValid(),
    
    // Helpers
    formatVerifierDisplayName: SameSessionHelpers.formatVerifierDisplayName,
    formatVerifierOption: SameSessionHelpers.formatVerifierOption,
    sortVerifiersByName: SameSessionHelpers.sortVerifiersByName,
  };
};

/**
 * Hook for verification status polling
 */
export const useVerificationStatusPolling = (
  parameterResponseId: string,
  onStatusChange?: (status: any) => void
) => {
  const [status, setStatus] = useState<any>(null);
  const [isPolling, setIsPolling] = useState(false);
  const intervalRef = useRef<NodeJS.Timeout | null>(null);

  const startPolling = useCallback(() => {
    if (intervalRef.current) {
      clearInterval(intervalRef.current);
    }
    
    setIsPolling(true);
    
    intervalRef.current = setInterval(async () => {
      try {
        // This would call your status check API
        // const newStatus = await getVerificationStatus(parameterResponseId);
        // setStatus(newStatus);
        // onStatusChange?.(newStatus);
      } catch (error) {
        console.error('Status polling error:', error);
      }
    }, VERIFICATION_STATUS_POLLING_INTERVAL);
  }, [parameterResponseId, onStatusChange]);

  const stopPolling = useCallback(() => {
    if (intervalRef.current) {
      clearInterval(intervalRef.current);
      intervalRef.current = null;
    }
    setIsPolling(false);
  }, []);

  useEffect(() => {
    return () => {
      stopPolling();
    };
  }, [stopPolling]);

  return {
    status,
    isPolling,
    startPolling,
    stopPolling,
  };
};
```

---

## **Index File Updates**

### **utils/index.ts Updates** ⚠️ MODIFICATION REQUIRED
**File:** `src/utils/index.ts`

**Add new exports:**
```typescript
// ... existing exports

// Same Session Verification Utilities
export * from './verificationValidation';
export * from './sameSessionHelpers';
export { 
  SAME_SESSION_VERIFICATION_TIMEOUT,
  VERIFICATION_STATUS_POLLING_INTERVAL,
  MAX_AUTHENTICATION_ATTEMPTS,
  MAX_VERIFICATION_COMMENT_LENGTH,
  MIN_VERIFICATION_COMMENT_LENGTH,
  ELIGIBLE_VERIFIERS_CACHE_TTL,
  VERIFICATION_STATUS_CACHE_TTL,
  API_DEBOUNCE_DELAY,
  SAME_SESSION_API_TIMEOUT,
  MAX_CONCURRENT_VERIFICATIONS,
  SESSION_CLEANUP_DELAY,
  ERROR_RETRY_ATTEMPTS,
  ERROR_RETRY_DELAY,
  SUCCESS_MODAL_AUTO_CLOSE_DELAY,
} from './constants';
```

### **types/index.ts Updates** ⚠️ MODIFICATION REQUIRED
**File:** `src/types/index.ts`

**Add new type exports:**
```typescript
// ... existing exports

// Same Session Verification Types
export * from './sameSessionVerification';
export {
  VerificationActionType,
  VerificationAssignmentType,
  AuthenticationMethod,
  VerificationErrorType,
  VerificationModalState,
} from '../utils/globalTypes';
```

---

## **Performance Optimizations**

### **Memoization Utilities** 🆕 NEW UTILITIES
**File:** `src/utils/memoization.ts` (new file)

```typescript
import { useMemo } from 'react';
import { EligibleVerifier } from '#types/sameSessionVerification';

/**
 * Memoization utilities for same session verification
 */
export const useMemoizedVerifiers = (verifiers: EligibleVerifier[], initiatorId: string) => {
  return useMemo(() => {
    return verifiers
      .filter(v => v.id !== initiatorId && v.isActive && !v.roles.includes('OPERATOR'))
      .sort((a, b) => `${a.firstName} ${a.lastName}`.localeCompare(`${b.firstName} ${b.lastName}`));
  }, [verifiers, initiatorId]);
};

export const useMemoizedVerifierOptions = (verifiers: EligibleVerifier[]) => {
  return useMemo(() => {
    return verifiers.map(verifier => ({
      label: `${verifier.firstName} ${verifier.lastName} (ID: ${verifier.employeeId})`,
      value: verifier.id,
      verifier,
      isDisabled: !verifier.isActive,
    }));
  }, [verifiers]);
};

export const useMemoizedFormValidation = (
  formData: any,
  isGroupAssignment: boolean,
  ssoEnabled: boolean
) => {
  return useMemo(() => {
    const hasVerifier = isGroupAssignment ? formData.selectedVerifier : true;
    const hasAction = formData.action !== null;
    const hasComments = formData.action === 'REJECT' ? formData.comments.trim().length > 0 : true;
    const hasAuth = formData.credentials.password || formData.credentials.ssoToken;
    
    return hasVerifier && hasAction && hasComments && hasAuth;
  }, [formData, isGroupAssignment, ssoEnabled]);
};
```

### **Debouncing Utilities** 🆕 NEW UTILITIES
**File:** `src/utils/debounce.ts` (new file)

```typescript
import { useCallback, useRef } from 'react';

/**
 * Debounce hook for API calls
 */
export const useDebounce = <T extends (...args: any[]) => any>(
  callback: T,
  delay: number
): T => {
  const timeoutRef = useRef<NodeJS.Timeout | null>(null);

  return useCallback(
    ((...args: Parameters<T>) => {
      if (timeoutRef.current) {
        clearTimeout(timeoutRef.current);
      }

      timeoutRef.current = setTimeout(() => {
        callback(...args);
      }, delay);
    }) as T,
    [callback, delay]
  );
};

/**
 * Throttle hook for frequent operations
 */
export const useThrottle = <T extends (...args: any[]) => any>(
  callback: T,
  delay: number
): T => {
  const lastCallRef = useRef<number>(0);

  return useCallback(
    ((...args: Parameters<T>) => {
      const now = Date.now();
      if (now - lastCallRef.current >= delay) {
        lastCallRef.current = now;
        callback(...args);
      }
    }) as T,
    [callback, delay]
  );
};
```

---

## **Testing Utilities**

### **Test Helpers** 🆕 NEW TEST UTILITIES
**File:** `src/utils/testHelpers.ts` (new file)

```typescript
import { EligibleVerifier, VerificationFormData } from '#types/sameSessionVerification';

/**
 * Test utilities for same session verification
 */
export const createMockEligibleVerifier = (overrides: Partial<EligibleVerifier> = {}): EligibleVerifier => {
  return {
    id: 'verifier-1',
    firstName: 'John',
    lastName: 'Smith',
    employeeId: 'EMP001',
    email: 'john.smith@company.com',
    department: 'Quality',
    roles: ['QUALITY_INSPECTOR'],
    ssoEnabled: true,
    isActive: true,
    lastLoginAt: '2025-01-19T10:00:00Z',
    ...overrides,
  };
};

export const createMockVerificationFormData = (
  overrides: Partial<VerificationFormData> = {}
): VerificationFormData => {
  return {
    selectedVerifier: createMockEligibleVerifier(),
    action: 'ACCEPT',
    comments: '',
    credentials: {
      method: 'PASSWORD',
      username: 'john.smith',
      password: 'password123',
    },
    ...overrides,
  };
};

export const createMockVerificationContext = (overrides: any = {}) => {
  return {
    parameterResponseId: 'param-response-1',
    parameterId: 'param-1',
    parameterName: 'Temperature Check',
    parameterValue: '75.2°C',
    parameterSpecification: '75°C ±1°C',
    initiatorId: 'initiator-1',
    initiatorName: 'Jane Doe',
    jobId: 'job-1',
    taskId: 'task-1',
    taskName: 'Quality Check',
    ...overrides,
  };
};

export const mockApiResponses = {
  eligibleVerifiers: {
    data: [
      createMockEligibleVerifier(),
      createMockEligibleVerifier({
        id: 'verifier-2',
        firstName: 'Alice',
        lastName: 'Johnson',
        employeeId: 'EMP002',
      }),
    ],
    success: true,
  },
  
  verificationSuccess: {
    data: {
      verificationId: 'verification-1',
      status: 'ACCEPTED',
      verifiedBy: {
        id: 'verifier-1',
        name: 'John Smith',
        employeeId: 'EMP001',
      },
      verifiedAt: '2025-01-19T10:30:00Z',
      auditTrail: {
        initiatorId: 'initiator-1',
        initiatorName: 'Jane Doe',
        verifierId: 'verifier-1',
        verifierName: 'John Smith',
        sessionContext: 'same_session',
        timestamp: '2025-01-19T10:30:00Z',
        action: 'ACCEPT',
      },
    },
    success: true,
  },
};

export const waitFor = (ms: number) => new Promise(resolve => setTimeout(resolve, ms));

export const createMockDispatch = () => jest.fn();

export const createMockSelector = (state: any) => (selector: any) => selector(state);
```

---

## **Implementation Dependencies**

### **Prerequisites**
1. **React Hooks**: useState, useEffect, useCallback, useMemo, useRef
2. **Redux**: useDispatch, useSelector
3. **Existing Utilities**: stringUtils, timeUtils
4. **Type Definitions**: All new types must be properly exported

### **External Dependencies**
- React (hooks)
- Redux/React-Redux
- Lodash (for utility functions)
- Date-fns (for date operations)

---

## **Implementation Order**

### **Phase 1: Constants and Types**
1. Add new constants to constants.ts
2. Update globalTypes.ts with new enums
3. Create sameSessionVerification.ts types file
4. Update index files for exports

### **Phase 2: Validation and Helpers**
1. Create verificationValidation.ts
2. Create sameSessionHelpers.ts
3. Add memoization utilities
4. Add debouncing utilities

### **Phase 3: Custom Hooks**
1. Create useSameSessionVerification hook
2. Create useVerificationStatusPolling hook
3. Add test helpers
4. Complete testing utilities

---

## **Testing Strategy**

### **Unit Tests**
```typescript
// verificationValidation.test.ts
describe('VerificationValidator', () => {
  it('should validate verifier selection correctly', () => {});
  it('should validate comments for rejection', () => {});
  it('should validate credentials', () => {});
  it('should validate complete form', () => {});
});

// sameSessionHelpers.test.ts
describe('SameSessionHelpers', () => {
  it('should format verifier display name', () => {});
  it('should filter eligible verifiers', () => {});
  it('should calculate time remaining', () => {});
  it('should generate verification summary', () => {});
});

// useSameSessionVerification.test.ts
describe('useSameSessionVerification', () => {
  it('should initialize with correct default state', () => {});
  it('should fetch eligible verifiers on mount', () => {});
  it('should validate form correctly', () => {});
  it('should submit verification', () => {});
});
```

---

## **Performance Considerations**

### **Optimization Strategies**
- Memoize expensive calculations
- Debounce API calls
- Use callback memoization
- Optimize re-renders with proper dependencies

### **Memory Management**
- Clear intervals on unmount
- Remove event listeners
- Clean up validation state

---

## **Security Considerations**

### **Data Sanitization**
- Sanitize comment inputs
- Mask sensitive data in logs
- Validate all user inputs

### **Type Safety**
- Strong typing for all utilities
- Runtime validation where needed
- Proper error handling

---

## **Rollback Plan**

### **Safe Rollback Points**
1. **After Constants**: Can revert constant additions
2. **After Types**: Can remove type definitions
3. **After Utilities**: Can remove utility files
4. **After Hooks**: Can remove custom hooks

### **Rollback Steps**
1. Remove new constants from constants.ts
2. Revert globalTypes.ts changes
3. Delete new utility files
4. Remove custom hooks
5. Update index file exports

---

## **Risk Assessment**

### **Low Risk**
- **Constants Addition**: Simple value definitions
- **Type Definitions**: No runtime impact
- **Helper Functions**: Pure functions with no side effects

### **Medium Risk**
- **Custom Hooks**: Complex state management logic
- **Validation Logic**: Critical for form functionality

---

## **Success Criteria**

### **Functional**
- ✅ All constants are properly defined
- ✅ Type definitions are complete and accurate
- ✅ Validation utilities work correctly
- ✅ Helper functions provide expected output
- ✅ Custom hooks manage state properly

### **Non-Functional**
- ✅ Performance optimizations work
- ✅ Memory usage is optimized
- ✅ Code coverage > 90%
- ✅ Type safety maintained

---

**Document Status:** Ready for Implementation  
**Next Layer:** Configuration Layer Analysis  
**Dependencies:** None (can start immediately)
