# Service Layer Analysis - Same Session Verification

**Document Version:** 1.0  
**Date:** January 19, 2025  
**Layer:** API Integration, HTTP Services, Authentication  
**Impact Level:** Medium  

---

## **Overview**

This document analyzes the impact of Same Session Verification feature on the service layer, including API endpoints, HTTP client modifications, authentication services, and data transformation.

---

## **Current State Analysis**

### **Existing API Structure**
```
src/utils/
├── apiUrls.ts ⚠️ MODIFICATION REQUIRED
├── request.ts ✅ NO CHANGES
├── axiosClient.ts ✅ NO CHANGES
└── globalTypes.ts ✅ NO CHANGES

src/services/
├── users/ ✅ NO CHANGES
└── commonTypes.ts ✅ NO CHANGES
```

### **Current Verification APIs**
```typescript
// Existing peer verification APIs in apiUrls.ts
export const apiInitiatePeerVerification = ({
  parameterResponseId,
}: {
  parameterResponseId: string;
}) => `${baseUrl}/parameter-verifications/parameter-executions/${parameterResponseId}/peer/assign`;

export const apiAcceptVerification = ({
  parameterResponseId,
  type,
}: {
  parameterResponseId: string;
  type: 'self' | 'peer';
}) =>
  `${baseUrl}/parameter-verifications/parameter-executions/${parameterResponseId}/${type}/accept`;

export const apiRejectPeerVerification = ({
  parameterResponseId,
}: {
  parameterResponseId: string;
}) => `${baseUrl}/parameter-verifications/parameter-executions/${parameterResponseId}/peer/reject`;

export const apiRecallVerification = ({
  parameterResponseId,
  type,
}: {
  parameterResponseId: string;
  type: 'self' | 'peer';
}) =>
  `${baseUrl}/parameter-verifications/parameter-executions/${parameterResponseId}/${type}/recall`;
```

### **Current Authentication APIs**
```typescript
// Existing authentication APIs
export const apiLogin = () => `${baseUrl}/auth/login`;
export const apiLogOut = () => `${baseUrl}/auth/logout`;
export const apiValidatePassword = () => `${baseUrl}/auth/credentials/validate`;
export const apiSsoRedirect = () => `${baseUrl}/auth/sso/redirect-url`;
```

---

## **Required Changes**

### **1. New API Endpoints**

#### **apiUrls.ts Updates** ⚠️ MODIFICATION REQUIRED
**File:** `src/utils/apiUrls.ts`

**Add new API endpoints:**
```typescript
// Same Session Verification APIs

/**
 * Get eligible verifiers for a parameter verification request
 * Excludes initiator and users with operator role
 */
export const apiGetEligibleVerifiers = (parameterResponseId: string) =>
  `${baseUrl}/parameter-verifications/parameter-executions/${parameterResponseId}/eligible-verifiers`;

/**
 * Get parameter verification status for real-time updates
 */
export const apiGetParameterVerificationStatus = (parameterResponseId: string) =>
  `${baseUrl}/parameter-verifications/parameter-executions/${parameterResponseId}/status`;

/**
 * Validate verifier credentials for same session verification
 */
export const apiValidateVerifierCredentials = () =>
  `${baseUrl}/auth/verifier/validate`;

/**
 * SSO authentication for verifiers in same session
 */
export const apiSsoVerifierAuth = () =>
  `${baseUrl}/auth/sso/verifier`;

/**
 * Get verification assignment details (single user vs group)
 */
export const apiGetVerificationAssignment = (parameterResponseId: string) =>
  `${baseUrl}/parameter-verifications/parameter-executions/${parameterResponseId}/assignment`;
```

### **2. Enhanced API Payloads**

#### **Request/Response Type Definitions** 🆕 NEW TYPES
**File:** `src/utils/apiTypes.ts` (new file)

```typescript
// API Request Types for Same Session Verification

export interface EligibleVerifiersRequest {
  parameterResponseId: string;
}

export interface EligibleVerifiersResponse {
  data: EligibleVerifier[];
  success: boolean;
  message?: string;
  errors?: ApiError[];
}

export interface EligibleVerifier {
  id: string;
  firstName: string;
  lastName: string;
  employeeId: string;
  email: string;
  ssoEnabled: boolean;
  roles: string[];
  department?: string;
  isActive: boolean;
}

export interface VerificationStatusRequest {
  parameterResponseId: string;
}

export interface VerificationStatusResponse {
  data: {
    status: ParameterVerificationStatus;
    verifiedBy?: {
      id: string;
      name: string;
      employeeId: string;
    };
    verifiedAt?: string;
    comments?: string;
  };
  success: boolean;
  message?: string;
}

export interface VerifierCredentialsRequest {
  username?: string;
  password?: string;
  ssoToken?: string;
  parameterResponseId: string;
}

export interface VerifierCredentialsResponse {
  data: {
    accessToken: string;
    user: {
      id: string;
      firstName: string;
      lastName: string;
      employeeId: string;
      email: string;
      roles: string[];
    };
    expiresIn: number;
  };
  success: boolean;
  message?: string;
  errors?: ApiError[];
}

export interface SameSessionVerificationRequest {
  comments?: string;
  sameSession: boolean;
  initiatorJwtToken: string;
}

export interface SameSessionVerificationResponse {
  data: {
    verificationId: string;
    status: ParameterVerificationStatus;
    verifiedBy: {
      id: string;
      name: string;
      employeeId: string;
    };
    verifiedAt: string;
    comments?: string;
    auditTrail: {
      initiatorId: string;
      verifierId: string;
      sessionContext: string;
    };
  };
  success: boolean;
  message?: string;
  errors?: ApiError[];
}

export interface VerificationAssignmentResponse {
  data: {
    assignmentType: 'SINGLE_USER' | 'USER_GROUP';
    assignedTo: {
      id: string;
      name: string;
      type: 'USER' | 'GROUP';
    };
    eligibleVerifiers?: EligibleVerifier[];
  };
  success: boolean;
  message?: string;
}

export interface ApiError {
  code: string;
  message: string;
  field?: string;
  details?: any;
}
```

### **3. HTTP Service Functions**

#### **New Service Functions** 🆕 NEW SERVICES
**File:** `src/services/sameSessionVerification.ts` (new file)

```typescript
import { request } from '#utils/request';
import {
  apiGetEligibleVerifiers,
  apiGetParameterVerificationStatus,
  apiValidateVerifierCredentials,
  apiSsoVerifierAuth,
  apiGetVerificationAssignment,
  apiAcceptVerification,
  apiRejectPeerVerification,
} from '#utils/apiUrls';
import {
  EligibleVerifiersResponse,
  VerificationStatusResponse,
  VerifierCredentialsRequest,
  VerifierCredentialsResponse,
  SameSessionVerificationRequest,
  SameSessionVerificationResponse,
  VerificationAssignmentResponse,
} from '#utils/apiTypes';

/**
 * Fetch eligible verifiers for a parameter verification
 */
export const fetchEligibleVerifiers = async (
  parameterResponseId: string
): Promise<EligibleVerifiersResponse> => {
  try {
    const response = await request('GET', apiGetEligibleVerifiers(parameterResponseId));
    return response;
  } catch (error) {
    throw new Error(`Failed to fetch eligible verifiers: ${error.message}`);
  }
};

/**
 * Get current verification status for real-time updates
 */
export const getVerificationStatus = async (
  parameterResponseId: string
): Promise<VerificationStatusResponse> => {
  try {
    const response = await request('GET', apiGetParameterVerificationStatus(parameterResponseId));
    return response;
  } catch (error) {
    throw new Error(`Failed to get verification status: ${error.message}`);
  }
};

/**
 * Validate verifier credentials
 */
export const validateVerifierCredentials = async (
  credentials: VerifierCredentialsRequest
): Promise<VerifierCredentialsResponse> => {
  try {
    const response = await request('POST', apiValidateVerifierCredentials(), {
      data: credentials,
    });
    return response;
  } catch (error) {
    throw new Error(`Verifier authentication failed: ${error.message}`);
  }
};

/**
 * SSO authentication for verifiers
 */
export const authenticateVerifierSSO = async (
  ssoToken: string,
  parameterResponseId: string
): Promise<VerifierCredentialsResponse> => {
  try {
    const response = await request('POST', apiSsoVerifierAuth(), {
      data: { ssoToken, parameterResponseId },
    });
    return response;
  } catch (error) {
    throw new Error(`SSO authentication failed: ${error.message}`);
  }
};

/**
 * Get verification assignment details
 */
export const getVerificationAssignment = async (
  parameterResponseId: string
): Promise<VerificationAssignmentResponse> => {
  try {
    const response = await request('GET', apiGetVerificationAssignment(parameterResponseId));
    return response;
  } catch (error) {
    throw new Error(`Failed to get verification assignment: ${error.message}`);
  }
};

/**
 * Accept verification in same session
 */
export const acceptSameSessionVerification = async (
  parameterResponseId: string,
  payload: SameSessionVerificationRequest
): Promise<SameSessionVerificationResponse> => {
  try {
    const response = await request(
      'PATCH',
      apiAcceptVerification({ parameterResponseId, type: 'peer' }),
      { data: payload }
    );
    return response;
  } catch (error) {
    throw new Error(`Failed to accept verification: ${error.message}`);
  }
};

/**
 * Reject verification in same session
 */
export const rejectSameSessionVerification = async (
  parameterResponseId: string,
  payload: SameSessionVerificationRequest
): Promise<SameSessionVerificationResponse> => {
  try {
    const response = await request(
      'PATCH',
      apiRejectPeerVerification({ parameterResponseId }),
      { data: payload }
    );
    return response;
  } catch (error) {
    throw new Error(`Failed to reject verification: ${error.message}`);
  }
};
```

### **4. Authentication Service Enhancements**

#### **Enhanced Authentication Utilities** 🆕 NEW UTILITIES
**File:** `src/services/authService.ts` (new file)

```typescript
import { axiosClient } from '#utils/axiosClient';
import { encrypt } from '#utils/stringUtils';

/**
 * Token management for same session verification
 */
export class TokenManager {
  private static originalToken: string | null = null;
  private static verifierToken: string | null = null;

  /**
   * Store the current (initiator's) token
   */
  static storeOriginalToken(token: string): void {
    this.originalToken = token;
  }

  /**
   * Set verifier's token as current
   */
  static setVerifierToken(token: string): void {
    this.verifierToken = token;
    axiosClient.defaults.headers.common['Authorization'] = `Bearer ${token}`;
  }

  /**
   * Restore original (initiator's) token
   */
  static restoreOriginalToken(): void {
    if (this.originalToken) {
      axiosClient.defaults.headers.common['Authorization'] = `Bearer ${this.originalToken}`;
    }
  }

  /**
   * Clear all stored tokens
   */
  static clearTokens(): void {
    this.originalToken = null;
    this.verifierToken = null;
    delete axiosClient.defaults.headers.common['Authorization'];
  }

  /**
   * Get current token state
   */
  static getTokenState(): {
    hasOriginalToken: boolean;
    hasVerifierToken: boolean;
    currentToken: string | null;
  } {
    return {
      hasOriginalToken: !!this.originalToken,
      hasVerifierToken: !!this.verifierToken,
      currentToken: axiosClient.defaults.headers.common['Authorization']?.replace('Bearer ', '') || null,
    };
  }
}

/**
 * Authentication methods for same session verification
 */
export class SameSessionAuth {
  /**
   * Authenticate verifier with password
   */
  static async authenticateWithPassword(
    username: string,
    password: string
  ): Promise<{ accessToken: string; user: any }> {
    try {
      const response = await axiosClient.post('/auth/login', {
        username,
        password: encrypt(password),
      });

      if (response.data.errors) {
        throw new Error(response.data.errors[0]?.message || 'Authentication failed');
      }

      return response.data.data;
    } catch (error) {
      throw new Error(`Password authentication failed: ${error.message}`);
    }
  }

  /**
   * Authenticate verifier with SSO
   */
  static async authenticateWithSSO(ssoToken: string): Promise<{ accessToken: string; user: any }> {
    try {
      const response = await axiosClient.post('/auth/sso/verifier', {
        token: ssoToken,
      });

      if (response.data.errors) {
        throw new Error(response.data.errors[0]?.message || 'SSO authentication failed');
      }

      return response.data.data;
    } catch (error) {
      throw new Error(`SSO authentication failed: ${error.message}`);
    }
  }

  /**
   * Logout verifier (cleanup session)
   */
  static async logoutVerifier(): Promise<void> {
    try {
      await axiosClient.post('/auth/logout');
    } catch (error) {
      // Log error but don't throw - logout should always succeed for cleanup
      console.warn('Verifier logout failed:', error.message);
    }
  }

  /**
   * Validate verifier has required permissions
   */
  static validateVerifierPermissions(user: any, requiredRoles: string[] = []): boolean {
    if (!user || !user.roles) {
      return false;
    }

    // Check if user has operator role (should be excluded)
    if (user.roles.includes('OPERATOR')) {
      return false;
    }

    // Check if user has required roles for verification
    const hasRequiredRole = requiredRoles.length === 0 || 
      requiredRoles.some(role => user.roles.includes(role));

    return hasRequiredRole;
  }
}
```

---

## **Error Handling Enhancements**

### **API Error Types** 🆕 NEW ERROR TYPES
**File:** `src/utils/errorTypes.ts` (new file)

```typescript
export enum SameSessionVerificationErrorCodes {
  // Authentication Errors
  INVALID_CREDENTIALS = 'SSV_001',
  SSO_TOKEN_EXPIRED = 'SSV_002',
  VERIFIER_NOT_AUTHORIZED = 'SSV_003',
  VERIFIER_ACCOUNT_LOCKED = 'SSV_004',

  // Verification Errors
  VERIFICATION_ALREADY_COMPLETED = 'SSV_101',
  VERIFICATION_EXPIRED = 'SSV_102',
  VERIFIER_NOT_ASSIGNED = 'SSV_103',
  PARAMETER_NOT_FOUND = 'SSV_104',

  // Session Errors
  INITIATOR_TOKEN_INVALID = 'SSV_201',
  SESSION_EXPIRED = 'SSV_202',
  CONCURRENT_VERIFICATION = 'SSV_203',

  // Validation Errors
  COMMENTS_REQUIRED = 'SSV_301',
  COMMENTS_TOO_LONG = 'SSV_302',
  INVALID_ACTION = 'SSV_303',

  // System Errors
  NETWORK_ERROR = 'SSV_401',
  SERVER_ERROR = 'SSV_402',
  TIMEOUT_ERROR = 'SSV_403',
}

export interface SameSessionVerificationError {
  code: SameSessionVerificationErrorCodes;
  message: string;
  details?: any;
  timestamp: string;
  requestId?: string;
}

export const ERROR_MESSAGES: Record<SameSessionVerificationErrorCodes, string> = {
  // Authentication Errors
  [SameSessionVerificationErrorCodes.INVALID_CREDENTIALS]: 
    'Invalid credentials. Please check your username and password.',
  [SameSessionVerificationErrorCodes.SSO_TOKEN_EXPIRED]: 
    'SSO session has expired. Please login again.',
  [SameSessionVerificationErrorCodes.VERIFIER_NOT_AUTHORIZED]: 
    'You are not authorized to verify this parameter.',
  [SameSessionVerificationErrorCodes.VERIFIER_ACCOUNT_LOCKED]: 
    'Your account is locked. Please contact administrator.',

  // Verification Errors
  [SameSessionVerificationErrorCodes.VERIFICATION_ALREADY_COMPLETED]: 
    'This parameter has already been verified by another user.',
  [SameSessionVerificationErrorCodes.VERIFICATION_EXPIRED]: 
    'Verification request has expired. Please use traditional verification method.',
  [SameSessionVerificationErrorCodes.VERIFIER_NOT_ASSIGNED]: 
    'You are not assigned to verify this parameter.',
  [SameSessionVerificationErrorCodes.PARAMETER_NOT_FOUND]: 
    'Parameter not found or has been removed.',

  // Session Errors
  [SameSessionVerificationErrorCodes.INITIATOR_TOKEN_INVALID]: 
    'Initiator session is invalid. Please refresh and try again.',
  [SameSessionVerificationErrorCodes.SESSION_EXPIRED]: 
    'Session has expired. Please refresh and try again.',
  [SameSessionVerificationErrorCodes.CONCURRENT_VERIFICATION]: 
    'Another verification is in progress. Please wait and try again.',

  // Validation Errors
  [SameSessionVerificationErrorCodes.COMMENTS_REQUIRED]: 
    'Comments are required when rejecting verification.',
  [SameSessionVerificationErrorCodes.COMMENTS_TOO_LONG]: 
    'Comments exceed maximum length limit.',
  [SameSessionVerificationErrorCodes.INVALID_ACTION]: 
    'Invalid verification action. Please select Accept or Reject.',

  // System Errors
  [SameSessionVerificationErrorCodes.NETWORK_ERROR]: 
    'Network error. Please check your connection and try again.',
  [SameSessionVerificationErrorCodes.SERVER_ERROR]: 
    'Server error. Please try again later.',
  [SameSessionVerificationErrorCodes.TIMEOUT_ERROR]: 
    'Request timed out. Please try again.',
};
```

### **Error Handler Service** 🆕 NEW SERVICE
**File:** `src/services/errorHandler.ts` (new file)

```typescript
import { SameSessionVerificationError, SameSessionVerificationErrorCodes, ERROR_MESSAGES } from '#utils/errorTypes';

export class SameSessionErrorHandler {
  /**
   * Parse API error response and return structured error
   */
  static parseApiError(error: any): SameSessionVerificationError {
    const timestamp = new Date().toISOString();
    
    // Handle network errors
    if (!error.response) {
      return {
        code: SameSessionVerificationErrorCodes.NETWORK_ERROR,
        message: ERROR_MESSAGES[SameSessionVerificationErrorCodes.NETWORK_ERROR],
        timestamp,
      };
    }

    // Handle HTTP errors
    const { status, data } = error.response;
    
    if (status >= 500) {
      return {
        code: SameSessionVerificationErrorCodes.SERVER_ERROR,
        message: ERROR_MESSAGES[SameSessionVerificationErrorCodes.SERVER_ERROR],
        details: data,
        timestamp,
      };
    }

    // Handle specific error codes from backend
    if (data?.errors?.length > 0) {
      const backendError = data.errors[0];
      const errorCode = this.mapBackendErrorCode(backendError.code);
      
      return {
        code: errorCode,
        message: ERROR_MESSAGES[errorCode] || backendError.message,
        details: backendError,
        timestamp,
        requestId: data.requestId,
      };
    }

    // Default error
    return {
      code: SameSessionVerificationErrorCodes.SERVER_ERROR,
      message: data?.message || 'An unexpected error occurred',
      timestamp,
    };
  }

  /**
   * Map backend error codes to frontend error codes
   */
  private static mapBackendErrorCode(backendCode: string): SameSessionVerificationErrorCodes {
    const errorCodeMap: Record<string, SameSessionVerificationErrorCodes> = {
      'INVALID_CREDENTIALS': SameSessionVerificationErrorCodes.INVALID_CREDENTIALS,
      'SSO_TOKEN_EXPIRED': SameSessionVerificationErrorCodes.SSO_TOKEN_EXPIRED,
      'VERIFICATION_COMPLETED': SameSessionVerificationErrorCodes.VERIFICATION_ALREADY_COMPLETED,
      'VERIFICATION_EXPIRED': SameSessionVerificationErrorCodes.VERIFICATION_EXPIRED,
      'NOT_AUTHORIZED': SameSessionVerificationErrorCodes.VERIFIER_NOT_AUTHORIZED,
      'ACCOUNT_LOCKED': SameSessionVerificationErrorCodes.VERIFIER_ACCOUNT_LOCKED,
      'COMMENTS_REQUIRED': SameSessionVerificationErrorCodes.COMMENTS_REQUIRED,
      'COMMENTS_TOO_LONG': SameSessionVerificationErrorCodes.COMMENTS_TOO_LONG,
      'CONCURRENT_ACCESS': SameSessionVerificationErrorCodes.CONCURRENT_VERIFICATION,
    };

    return errorCodeMap[backendCode] || SameSessionVerificationErrorCodes.SERVER_ERROR;
  }

  /**
   * Get user-friendly error message
   */
  static getUserFriendlyMessage(error: SameSessionVerificationError): string {
    return ERROR_MESSAGES[error.code] || error.message;
  }

  /**
   * Determine if error is recoverable
   */
  static isRecoverableError(error: SameSessionVerificationError): boolean {
    const recoverableErrors = [
      SameSessionVerificationErrorCodes.NETWORK_ERROR,
      SameSessionVerificationErrorCodes.TIMEOUT_ERROR,
      SameSessionVerificationErrorCodes.INVALID_CREDENTIALS,
      SameSessionVerificationErrorCodes.COMMENTS_REQUIRED,
      SameSessionVerificationErrorCodes.COMMENTS_TOO_LONG,
      SameSessionVerificationErrorCodes.INVALID_ACTION,
    ];

    return recoverableErrors.includes(error.code);
  }

  /**
   * Get suggested action for error
   */
  static getSuggestedAction(error: SameSessionVerificationError): string {
    const actionMap: Record<SameSessionVerificationErrorCodes, string> = {
      [SameSessionVerificationErrorCodes.INVALID_CREDENTIALS]: 'Please check your credentials and try again.',
      [SameSessionVerificationErrorCodes.SSO_TOKEN_EXPIRED]: 'Please login again using SSO.',
      [SameSessionVerificationErrorCodes.VERIFICATION_ALREADY_COMPLETED]: 'Refresh the page to see updated status.',
      [SameSessionVerificationErrorCodes.VERIFICATION_EXPIRED]: 'Use the traditional verification method from your inbox.',
      [SameSessionVerificationErrorCodes.VERIFIER_NOT_AUTHORIZED]: 'Contact administrator for access.',
      [SameSessionVerificationErrorCodes.NETWORK_ERROR]: 'Check your internet connection and try again.',
      [SameSessionVerificationErrorCodes.SERVER_ERROR]: 'Please try again later or contact support.',
      [SameSessionVerificationErrorCodes.COMMENTS_REQUIRED]: 'Please provide comments for rejection.',
      [SameSessionVerificationErrorCodes.COMMENTS_TOO_LONG]: 'Please shorten your comments.',
      [SameSessionVerificationErrorCodes.CONCURRENT_VERIFICATION]: 'Please wait and try again.',
    };

    return actionMap[error.code] || 'Please try again or contact support.';
  }
}
```

---

## **Data Transformation**

### **Response Transformers** 🆕 NEW TRANSFORMERS
**File:** `src/services/dataTransformers.ts` (new file)

```typescript
import { EligibleVerifier } from '#utils/apiTypes';
import { getFullName } from '#utils/stringUtils';

/**
 * Transform eligible verifiers for UI consumption
 */
export const transformEligibleVerifiers = (verifiers: EligibleVerifier[]) => {
  return verifiers.map(verifier => ({
    ...verifier,
    fullName: getFullName(verifier),
    displayName: `${getFullName(verifier)} (ID: ${verifier.employeeId})`,
    isEligible: verifier.isActive && !verifier.roles.includes('OPERATOR'),
  }));
};

/**
 * Transform verification status for UI
 */
export const transformVerificationStatus = (status: any) => {
  return {
    ...status,
    isCompleted: ['ACCEPTED', 'REJECTED'].includes(status.status),
    isPending: status.status === 'PENDING',
    isRecalled: status.status === 'RECALLED',
    displayStatus: status.status.toLowerCase().replace('_', ' '),
  };
};

/**
 * Transform verification assignment for UI
 */
export const transformVerificationAssignment = (assignment: any) => {
  return {
    ...assignment,
    isSingleUser: assignment.assignmentType === 'SINGLE_USER',
    isGroup: assignment.assignmentType === 'USER_GROUP',
    assignedToName: assignment.assignedTo.name,
    assignedToType: assignment.assignedTo.type,
  };
};

/**
 * Prepare verification request payload
 */
export const prepareVerificationPayload = (
  action: 'accept' | 'reject',
  comments: string,
  initiatorToken: string
) => {
  return {
    comments: action === 'reject' ? comments : undefined,
    sameSession: true,
    initiatorJwtToken: initiatorToken,
  };
};
```

---

## **Caching Strategy**

### **API Response Caching** 🆕 NEW CACHING
**File:** `src/services/cacheService.ts` (new file)

```typescript
interface CacheEntry<T> {
  data: T;
  timestamp: number;
  expiresIn: number;
}

export class ApiCache {
  private static cache = new Map<string, CacheEntry<any>>();
  private static readonly DEFAULT_TTL = 5 * 60 * 1000; // 5 minutes

  /**
   * Set cache entry
   */
  static set<T>(key: string, data: T, ttl: number = this.DEFAULT_TTL): void {
    this.cache.set(key, {
      data,
      timestamp: Date.now(),
      expiresIn: ttl,
    });
  }

  /**
   * Get cache entry
   */
  static get<T>(key: string): T | null {
    const entry = this.cache.get(key);
    
    if (!entry) {
      return null;
    }

    // Check if expired
    if (Date.now() - entry.timestamp > entry.expiresIn) {
      this.cache.delete(key);
      return null;
    }

    return entry.data;
  }

  /**
   * Clear cache entry
   */
  static clear(key: string): void {
    this.cache.delete(key);
  }

  /**
   * Clear all cache
   */
  static clearAll(): void {
    this.cache.clear();
  }

  /**
   * Generate cache key for eligible verifiers
   */
  static getEligibleVerifiersKey(parameterResponseId: string): string {
    return `eligible_verifiers_${parameterResponseId}`;
  }

  /**
   * Generate cache key for verification status
   */
  static getVerificationStatusKey(parameterResponseId: string): string {
    return `verification_status_${parameterResponseId}`;
  }
}
```

---

## **Performance Optimizations**

### **Request Optimization**
```typescript
// Debounced status checking
export const debouncedStatusCheck = debounce(async (parameterResponseId: string) => {
  return await getVerificationStatus(parameterResponseId);
}, 1000);

// Batch eligible verifiers requests
export const batchEligibleVerifiersRequests = async (parameterResponseIds: string[]) => {
  const requests = parameterResponseIds.map(id => 
    fetchEligibleVerifiers(id).catch(error => ({ error, id }))
  );
  
  return await Promise.allSettled(requests);
};
```

### **Connection Pooling**
```typescript
// Configure axios for optimal performance
axiosClient.defaults.timeout = 30000; // 30 seconds
axiosClient.defaults.maxRedirects = 3;

// Add request/response interceptors for performance monitoring
axiosClient.interceptors.request.use(
  (config) => {
    config.metadata = { startTime: Date.now() };
    return config;
  }
);

axiosClient.interceptors.response.use(
  (response) => {
    const duration = Date.now() - response.config.metadata.startTime;
    console.log(`API ${response.config.url} took ${duration}ms`);
    return response;
  }
);
```

---

## **Security Considerations**

### **Request Security**
```typescript
// Add security headers for same session verification
export const addSecurityHeaders = (config: any) => {
  return {
    ...config,
    headers: {
      ...config.headers,
      'X-Same-Session-Verification': 'true',
      'X-Request-ID': generateRequestId(),
      'X-Timestamp': Date.now().toString(),
    },
  };
};

// Validate response integrity
export const validateResponse = (response: any) => {
  // Check for required fields
  if (!response.data || typeof response.success !== 'boolean') {
    throw new Error('Invalid response format');
  }

  // Validate timestamp
  const serverTime = response.timestamp;
  const clientTime = Date.now();
  const timeDiff = Math.abs(clientTime - serverTime);
  
  if (timeDiff > 5 * 60 * 1000) { // 5 minutes
    throw new Error('Response timestamp is too old');
  }

  return response;
};
```

---

## **Testing Strategy**

### **API Testing**
```typescript
// Mock API responses for testing
export const mockApiResponses = {
  eligibleVerifiers: {
    data: [
      {
        id: '1',
        firstName: 'John',
        lastName: 'Smith',
        employeeId: 'EMP001',
        email: 'john.smith@company.com',
        ssoEnabled: true,
        roles: ['QUALITY_INSPECTOR'],
        isActive: true,
      },
    ],
    success: true,
  },
  
  verificationStatus: {
    data: {
      status: 'PENDING',
    },
    success: true,
  },
  
  verificationSuccess: {
    data: {
      verificationId: 'VER123',
      status: 'ACCEPTED',
      verifiedBy: {
        id: '1',
        name: 'John Smith',
        employeeId: 'EMP001',
      },
      verifiedAt: '2025-01-19T10:30:00Z',
      auditTrail: {
        initiatorId: 'INIT001',
        verifierId: '1',
        sessionContext: 'same_session',
      },
    },
    success: true,
  },
};

// Test utilities
export const createMockApiClient = () => {
  return {
    get: jest.fn(),
    post: jest.fn(),
    patch: jest.fn(),
    delete: jest.fn(),
  };
};
```

### **Service Integration Tests**
```typescript
// Integration tests for service layer
describe('Same Session Verification Services', () => {
  let mockApiClient: any;

  beforeEach(() => {
    mockApiClient = createMockApiClient();
  });

  describe('fetchEligibleVerifiers', () => {
    it('should fetch eligible verifiers successfully', async () => {
      mockApiClient.get.mockResolvedValue(mockApiResponses.eligibleVerifiers);
      
      const result = await fetchEligibleVerifiers('param123');
      
      expect(result.success).toBe(true);
      expect(result.data).toHaveLength(1);
      expect(result.data[0].employeeId).toBe('EMP001');
    });

    it('should handle API errors', async () => {
      mockApiClient.get.mockRejectedValue(new Error('Network error'));
      
      await expect(fetchEligibleVerifiers('param123')).rejects.toThrow('Failed to fetch eligible verifiers');
    });
  });

  describe('validateVerifierCredentials', () => {
    it('should validate credentials successfully', async () => {
      const mockResponse = {
        data: {
          accessToken: 'token123',
          user: { id: '1', name: 'John Smith' },
        },
        success: true,
      };
      
      mockApiClient.post.mockResolvedValue(mockResponse);
      
      const result = await validateVerifierCredentials({
        username: 'john.smith',
        password: 'password123',
        parameterResponseId: 'param123',
      });
      
      expect(result.success).toBe(true);
      expect(result.data.accessToken).toBe('token123');
    });
  });
});
```

---

## **Implementation Dependencies**

### **Prerequisites**
1. **Backend APIs**: All new endpoints must be implemented
2. **Authentication Service**: Enhanced token management
3. **Database Schema**: Support for same session verification audit
4. **Security Configuration**: CORS and authentication updates

### **External Dependencies**
- Axios for HTTP requests
- Lodash for utility functions
- Date-fns for date manipulation
- Crypto-js for encryption (if needed)

---

## **Implementation Order**

### **Phase 1: API Endpoints**
1. Add new API URL definitions
2. Create API type definitions
3. Implement basic service functions
4. Add error handling types

### **Phase 2: Service Implementation**
1. Implement authentication services
2. Add data transformation utilities
3. Create caching service
4. Add error handler service

### **Phase 3: Integration and Testing**
1. Integrate with existing request utilities
2. Add comprehensive error handling
3. Implement caching strategy
4. Complete testing suite

---

## **Rollback Plan**

### **Safe Rollback Points**
1. **After API URLs**: Can revert URL additions
2. **After Service Implementation**: Can disable service calls
3. **After Full Implementation**: Can remove all new services

### **Rollback Steps**
1. Remove new API URL definitions
2. Delete service files
3. Revert any request utility changes
4. Clear cached data
5. Remove error type definitions

---

## **Risk Assessment**

### **High Risk**
- **API Integration**: New endpoints may have different response formats
- **Authentication Flow**: Token switching could cause security issues
- **Error Handling**: Complex error scenarios need proper handling

### **Medium Risk**
- **Performance**: Additional API calls may impact performance
- **Caching**: Cache invalidation strategy needs careful implementation

### **Low Risk**
- **Data Transformation**: Well-defined transformation logic
- **Type Safety**: Strong typing prevents runtime errors

---

## **Success Criteria**

### **Functional**
- ✅ All API endpoints respond correctly
- ✅ Authentication flow works securely
- ✅ Error handling covers all scenarios
- ✅ Data transformation maintains integrity
- ✅ Caching improves performance

### **Non-Functional**
- ✅ API response times < 2 seconds
- ✅ Error recovery works properly
- ✅ Security standards maintained
- ✅ Memory usage optimized
- ✅ Code coverage > 85%

---

**Document Status:** Ready for Implementation  
**Next Layer:** Utility Layer Analysis  
**Dependencies:** Backend API implementation
