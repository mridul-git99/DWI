# State Management Layer Analysis - Same Session Verification

**Document Version:** 1.0  
**Date:** January 19, 2025  
**Layer:** Redux Store, Actions, Sagas, Reducers  
**Impact Level:** High  

---

## **Overview**

This document analyzes the impact of Same Session Verification feature on the state management layer, including Redux store structure, actions, sagas, reducers, and side effects management.

---

## **Current State Analysis**

### **Existing Redux Structure**
```
src/views/Job/
├── jobStore.ts ⚠️ MODIFICATION REQUIRED
├── saga.tsx ⚠️ MODIFICATION REQUIRED
└── utils.ts ✅ NO CHANGES

src/store/
├── rootReducer.ts ✅ NO CHANGES
├── rootSaga.ts ✅ NO CHANGES
└── types.ts ✅ NO CHANGES
```

### **Current Job Store Actions**
```typescript
// Existing peer verification actions in jobStore.ts
export const jobActions = {
  sendPeerVerification: createAction<{
    parameterResponseId: string;
    parameterId: string;
    data: any;
  }>('job/sendPeerVerification'),
  
  acceptPeerVerification: createAction<{
    parameterResponseId: string;
    parameterId: string;
    password?: string;
    code?: string;
    state?: string;
  }>('job/acceptPeerVerification'),
  
  rejectPeerVerification: createAction<{
    parameterResponseId: string;
    parameterId: string;
    comment: string;
  }>('job/rejectPeerVerification'),
  
  recallPeerVerification: createAction<{
    parameterResponseId: string;
    parameterId: string;
    type: string;
  }>('job/recallPeerVerification'),
  
  // ... other existing actions
};
```

### **Current Saga Functions**
```typescript
// Existing saga functions in saga.tsx
function* sendPeerVerificationSaga({ payload }) { /* ... */ }
function* acceptPeerVerificationSaga({ payload }) { /* ... */ }
function* rejectPeerVerificationSaga({ payload }) { /* ... */ }
function* recallPeerVerificationSaga({ payload }) { /* ... */ }
```

---

## **Required Changes**

### **1. New Action Types**

#### **JobActionsEnum Updates** ⚠️ MODIFICATION REQUIRED
**File:** `src/views/Job/jobStore.ts`

**Add to existing enum:**
```typescript
export enum JobActionsEnum {
  // ... existing actions
  
  // Same Session Verification Actions
  fetchEligibleVerifiers = 'job/fetchEligibleVerifiers',
  fetchEligibleVerifiersSuccess = 'job/fetchEligibleVerifiersSuccess',
  fetchEligibleVerifiersFailure = 'job/fetchEligibleVerifiersFailure',
  
  initiateSameSessionVerification = 'job/initiateSameSessionVerification',
  completeSameSessionVerification = 'job/completeSameSessionVerification',
  sameSessionVerificationSuccess = 'job/sameSessionVerificationSuccess',
  sameSessionVerificationFailure = 'job/sameSessionVerificationFailure',
  
  // Authentication Actions for Same Session
  authenticateVerifier = 'job/authenticateVerifier',
  authenticateVerifierSuccess = 'job/authenticateVerifierSuccess',
  authenticateVerifierFailure = 'job/authenticateVerifierFailure',
  
  // Cleanup Actions
  clearSameSessionVerificationState = 'job/clearSameSessionVerificationState',
}
```

### **2. New Action Creators**

#### **jobActions Updates** ⚠️ MODIFICATION REQUIRED
**File:** `src/views/Job/jobStore.ts`

**Add to existing jobActions:**
```typescript
export const jobActions = {
  // ... existing actions
  
  // Fetch eligible verifiers for group assignments
  fetchEligibleVerifiers: createAction<{
    parameterResponseId: string;
  }>(JobActionsEnum.fetchEligibleVerifiers),
  
  fetchEligibleVerifiersSuccess: createAction<{
    verifiers: EligibleVerifier[];
  }>(JobActionsEnum.fetchEligibleVerifiersSuccess),
  
  fetchEligibleVerifiersFailure: createAction<{
    error: string;
  }>(JobActionsEnum.fetchEligibleVerifiersFailure),
  
  // Initiate same session verification
  initiateSameSessionVerification: createAction<{
    parameterResponseId: string;
    parameterId: string;
  }>(JobActionsEnum.initiateSameSessionVerification),
  
  // Complete same session verification
  completeSameSessionVerification: createAction<{
    parameterResponseId: string;
    parameterId: string;
    verifierId: string;
    action: 'accept' | 'reject';
    comments?: string;
    password?: string;
    ssoToken?: string;
  }>(JobActionsEnum.completeSameSessionVerification),
  
  sameSessionVerificationSuccess: createAction<{
    parameterResponseId: string;
    parameterId: string;
    verificationData: any;
  }>(JobActionsEnum.sameSessionVerificationSuccess),
  
  sameSessionVerificationFailure: createAction<{
    error: string;
    errorType: 'AUTHENTICATION' | 'VALIDATION' | 'NETWORK' | 'CONCURRENT';
  }>(JobActionsEnum.sameSessionVerificationFailure),
  
  // Authentication actions
  authenticateVerifier: createAction<{
    credentials: {
      username?: string;
      password?: string;
      ssoToken?: string;
    };
  }>(JobActionsEnum.authenticateVerifier),
  
  authenticateVerifierSuccess: createAction<{
    accessToken: string;
    user: User;
  }>(JobActionsEnum.authenticateVerifierSuccess),
  
  authenticateVerifierFailure: createAction<{
    error: string;
  }>(JobActionsEnum.authenticateVerifierFailure),
  
  // Cleanup
  clearSameSessionVerificationState: createAction(
    JobActionsEnum.clearSameSessionVerificationState
  ),
};
```

### **3. Type Definitions**

#### **New Types** 🆕 NEW TYPES
**File:** `src/views/Job/types.ts` (new file)

```typescript
// Eligible Verifier Type
export interface EligibleVerifier {
  id: string;
  firstName: string;
  lastName: string;
  employeeId: string;
  email: string;
  ssoEnabled: boolean;
  roles: string[];
}

// Same Session Verification Request
export interface SameSessionVerificationRequest {
  parameterResponseId: string;
  parameterId: string;
  verifierId: string;
  action: 'accept' | 'reject';
  comments?: string;
  credentials: {
    password?: string;
    ssoToken?: string;
  };
}

// Same Session Verification State
export interface SameSessionVerificationState {
  eligibleVerifiers: EligibleVerifier[];
  loading: boolean;
  authenticating: boolean;
  submitting: boolean;
  error: string | null;
  errorType: 'AUTHENTICATION' | 'VALIDATION' | 'NETWORK' | 'CONCURRENT' | null;
  verifierToken: string | null;
  originalToken: string | null;
}

// Authentication Credentials
export interface VerifierCredentials {
  username?: string;
  password?: string;
  ssoToken?: string;
}

// API Response Types
export interface EligibleVerifiersResponse {
  data: EligibleVerifier[];
  success: boolean;
  message?: string;
}

export interface VerificationApiResponse {
  data: any;
  success: boolean;
  message?: string;
  errors?: any[];
}
```

### **4. Store State Updates**

#### **Job Store State** ⚠️ MODIFICATION REQUIRED
**File:** `src/views/Job/jobStore.ts`

**Add to existing state interface:**
```typescript
export interface JobStore {
  // ... existing state properties
  
  // Same Session Verification State
  sameSessionVerification: SameSessionVerificationState;
}

// Update initial state
export const initialState: JobStore = {
  // ... existing initial state
  
  sameSessionVerification: {
    eligibleVerifiers: [],
    loading: false,
    authenticating: false,
    submitting: false,
    error: null,
    errorType: null,
    verifierToken: null,
    originalToken: null,
  },
};
```

### **5. Reducer Updates**

#### **Job Reducer** ⚠️ MODIFICATION REQUIRED
**File:** `src/views/Job/jobStore.ts`

**Add new reducer cases:**
```typescript
export const jobSlice = createSlice({
  name: 'job',
  initialState,
  reducers: {
    // ... existing reducers
  },
  extraReducers: (builder) => {
    builder
      // ... existing extra reducers
      
      // Fetch Eligible Verifiers
      .addCase(jobActions.fetchEligibleVerifiers, (state) => {
        state.sameSessionVerification.loading = true;
        state.sameSessionVerification.error = null;
      })
      .addCase(jobActions.fetchEligibleVerifiersSuccess, (state, action) => {
        state.sameSessionVerification.loading = false;
        state.sameSessionVerification.eligibleVerifiers = action.payload.verifiers;
        state.sameSessionVerification.error = null;
      })
      .addCase(jobActions.fetchEligibleVerifiersFailure, (state, action) => {
        state.sameSessionVerification.loading = false;
        state.sameSessionVerification.error = action.payload.error;
        state.sameSessionVerification.errorType = 'NETWORK';
      })
      
      // Authentication
      .addCase(jobActions.authenticateVerifier, (state) => {
        state.sameSessionVerification.authenticating = true;
        state.sameSessionVerification.error = null;
      })
      .addCase(jobActions.authenticateVerifierSuccess, (state, action) => {
        state.sameSessionVerification.authenticating = false;
        state.sameSessionVerification.verifierToken = action.payload.accessToken;
        state.sameSessionVerification.error = null;
      })
      .addCase(jobActions.authenticateVerifierFailure, (state, action) => {
        state.sameSessionVerification.authenticating = false;
        state.sameSessionVerification.error = action.payload.error;
        state.sameSessionVerification.errorType = 'AUTHENTICATION';
      })
      
      // Same Session Verification
      .addCase(jobActions.completeSameSessionVerification, (state) => {
        state.sameSessionVerification.submitting = true;
        state.sameSessionVerification.error = null;
      })
      .addCase(jobActions.sameSessionVerificationSuccess, (state, action) => {
        state.sameSessionVerification.submitting = false;
        state.sameSessionVerification.error = null;
        // Update parameter verification state
        // This will be handled by existing updateParameterVerifications logic
      })
      .addCase(jobActions.sameSessionVerificationFailure, (state, action) => {
        state.sameSessionVerification.submitting = false;
        state.sameSessionVerification.error = action.payload.error;
        state.sameSessionVerification.errorType = action.payload.errorType;
      })
      
      // Cleanup
      .addCase(jobActions.clearSameSessionVerificationState, (state) => {
        state.sameSessionVerification = initialState.sameSessionVerification;
      });
  },
});
```

---

## **Saga Implementation**

### **New Saga Functions** 🆕 NEW SAGAS
**File:** `src/views/Job/saga.tsx`

#### **1. Fetch Eligible Verifiers Saga**
```typescript
function* fetchEligibleVerifiersSaga({
  payload,
}: ReturnType<typeof jobActions.fetchEligibleVerifiers>) {
  try {
    const { parameterResponseId } = payload;
    
    const { data, errors }: ResponseObj<EligibleVerifier[]> = yield call(
      request,
      'GET',
      apiGetEligibleVerifiers(parameterResponseId)
    );
    
    if (errors) {
      throw new Error(getErrorMsg(errors));
    }
    
    yield put(
      jobActions.fetchEligibleVerifiersSuccess({
        verifiers: data,
      })
    );
  } catch (error) {
    yield put(
      jobActions.fetchEligibleVerifiersFailure({
        error: error.message || 'Failed to fetch eligible verifiers',
      })
    );
    yield* handleCatch('Job', 'fetchEligibleVerifiersSaga', error, true);
  }
}
```

#### **2. Authenticate Verifier Saga**
```typescript
function* authenticateVerifierSaga({
  payload,
}: ReturnType<typeof jobActions.authenticateVerifier>) {
  try {
    const { credentials } = payload;
    
    // Store current (initiator's) token
    const currentToken = yield select(getAuthToken);
    yield put(jobActions.setOriginalToken(currentToken));
    
    let authResponse;
    
    if (credentials.ssoToken) {
      // Handle SSO authentication
      authResponse = yield call(
        request,
        'POST',
        apiSsoAuthenticate(),
        { data: { token: credentials.ssoToken } }
      );
    } else {
      // Handle password authentication
      authResponse = yield call(
        request,
        'POST',
        apiLogin(),
        { 
          data: { 
            username: credentials.username,
            password: encrypt(credentials.password) 
          } 
        }
      );
    }
    
    const { data: authData, errors: authErrors } = authResponse;
    
    if (authErrors) {
      throw new Error(getErrorMsg(authErrors));
    }
    
    yield put(
      jobActions.authenticateVerifierSuccess({
        accessToken: authData.accessToken,
        user: authData.user,
      })
    );
    
  } catch (error) {
    yield put(
      jobActions.authenticateVerifierFailure({
        error: error.message || 'Authentication failed',
      })
    );
    yield* handleCatch('Job', 'authenticateVerifierSaga', error, true);
  }
}
```

#### **3. Complete Same Session Verification Saga**
```typescript
function* completeSameSessionVerificationSaga({
  payload,
}: ReturnType<typeof jobActions.completeSameSessionVerification>) {
  try {
    const {
      parameterResponseId,
      parameterId,
      verifierId,
      action,
      comments,
      password,
      ssoToken,
    } = payload;
    
    // Step 1: Store current (initiator's) token
    const initiatorToken = yield select(getAuthToken);
    
    // Step 2: Authenticate verifier
    yield put(
      jobActions.authenticateVerifier({
        credentials: {
          password,
          ssoToken,
        },
      })
    );
    
    // Wait for authentication to complete
    const authResult = yield race({
      success: take(JobActionsEnum.authenticateVerifierSuccess),
      failure: take(JobActionsEnum.authenticateVerifierFailure),
    });
    
    if (authResult.failure) {
      throw new Error('Verifier authentication failed');
    }
    
    // Step 3: Set verifier's token as current
    const verifierToken = authResult.success.payload.accessToken;
    yield call(setAuthToken, verifierToken);
    
    // Step 4: Call verification API with initiator token in payload
    const apiPayload = {
      comments,
      sameSession: true,
      initiatorJwtToken: initiatorToken,
    };
    
    const apiUrl = action === 'accept' 
      ? apiAcceptVerification({ parameterResponseId, type: 'peer' })
      : apiRejectPeerVerification({ parameterResponseId });
    
    const { data, errors } = yield call(
      request, 
      'PATCH', 
      apiUrl, 
      { data: apiPayload }
    );
    
    if (errors) {
      // Check for concurrent verification completion
      const concurrentError = errors.find(
        error => error.code === 'VERIFICATION_ALREADY_COMPLETED'
      );
      
      if (concurrentError) {
        yield put(
          jobActions.sameSessionVerificationFailure({
            error: 'This parameter has already been verified by another user',
            errorType: 'CONCURRENT',
          })
        );
        return;
      }
      
      throw new Error(getErrorMsg(errors));
    }
    
    // Step 5: Logout verifier
    yield call(request, 'POST', apiLogOut());
    
    // Step 6: Restore initiator's token
    yield call(setAuthToken, initiatorToken);
    
    // Step 7: Update parameter verification state
    yield put(
      jobActions.updateParameterVerifications({
        parameterResponseId,
        parameterId,
        data,
      })
    );
    
    // Step 8: Show success notification
    yield put(
      showNotification({
        type: NotificationType.SUCCESS,
        msg: action === 'accept' 
          ? 'Parameter has been Peer Verified Successfully'
          : 'Parameter verification has been rejected',
      })
    );
    
    // Step 9: Close modal
    yield put(closeOverlayAction(OverlayNames.SAME_SESSION_VERIFICATION_MODAL));
    
    // Step 10: Clear same session state
    yield put(jobActions.clearSameSessionVerificationState());
    
    yield put(
      jobActions.sameSessionVerificationSuccess({
        parameterResponseId,
        parameterId,
        verificationData: data,
      })
    );
    
  } catch (error) {
    // Always restore initiator token on error
    const initiatorToken = yield select(getOriginalToken);
    if (initiatorToken) {
      yield call(setAuthToken, initiatorToken);
    }
    
    // Determine error type
    let errorType: 'AUTHENTICATION' | 'VALIDATION' | 'NETWORK' | 'CONCURRENT' = 'NETWORK';
    
    if (error.message.includes('authentication') || error.message.includes('credentials')) {
      errorType = 'AUTHENTICATION';
    } else if (error.message.includes('validation') || error.message.includes('required')) {
      errorType = 'VALIDATION';
    } else if (error.message.includes('already verified')) {
      errorType = 'CONCURRENT';
    }
    
    yield put(
      jobActions.sameSessionVerificationFailure({
        error: error.message || 'Same session verification failed',
        errorType,
      })
    );
    
    yield put(
      showNotification({
        type: NotificationType.ERROR,
        msg: error.message || 'Same session verification failed',
      })
    );
    
    yield* handleCatch('Job', 'completeSameSessionVerificationSaga', error, true);
  }
}
```

### **4. Saga Watchers Update**

#### **Add to existing saga watchers** ⚠️ MODIFICATION REQUIRED
**File:** `src/views/Job/saga.tsx`

```typescript
export function* jobSaga() {
  // ... existing watchers
  
  // Same Session Verification Watchers
  yield takeLatest(JobActionsEnum.fetchEligibleVerifiers, fetchEligibleVerifiersSaga);
  yield takeLatest(JobActionsEnum.authenticateVerifier, authenticateVerifierSaga);
  yield takeLatest(JobActionsEnum.completeSameSessionVerification, completeSameSessionVerificationSaga);
  
  // Keep existing watchers at the end
  yield all([fork(takeOneAtMost), fork(TaskPollSaga), fork(TaskTimerSaga), fork(JobPollSaga)]);
}
```

---

## **Selectors**

### **New Selectors** 🆕 NEW SELECTORS
**File:** `src/views/Job/selectors.ts` (new file)

```typescript
import { createSelector } from '@reduxjs/toolkit';
import { RootState } from '#store/types';

// Base selector
const getSameSessionVerificationState = (state: RootState) => 
  state.job.sameSessionVerification;

// Eligible verifiers selector
export const getEligibleVerifiers = createSelector(
  [getSameSessionVerificationState],
  (sameSessionState) => sameSessionState.eligibleVerifiers
);

// Loading states selectors
export const getEligibleVerifiersLoading = createSelector(
  [getSameSessionVerificationState],
  (sameSessionState) => sameSessionState.loading
);

export const getVerifierAuthenticating = createSelector(
  [getSameSessionVerificationState],
  (sameSessionState) => sameSessionState.authenticating
);

export const getVerificationSubmitting = createSelector(
  [getSameSessionVerificationState],
  (sameSessionState) => sameSessionState.submitting
);

// Error selectors
export const getSameSessionVerificationError = createSelector(
  [getSameSessionVerificationState],
  (sameSessionState) => sameSessionState.error
);

export const getSameSessionVerificationErrorType = createSelector(
  [getSameSessionVerificationState],
  (sameSessionState) => sameSessionState.errorType
);

// Token selectors
export const getVerifierToken = createSelector(
  [getSameSessionVerificationState],
  (sameSessionState) => sameSessionState.verifierToken
);

export const getOriginalToken = createSelector(
  [getSameSessionVerificationState],
  (sameSessionState) => sameSessionState.originalToken
);

// Combined selectors
export const getIsAnyVerificationLoading = createSelector(
  [getSameSessionVerificationState],
  (sameSessionState) => 
    sameSessionState.loading || 
    sameSessionState.authenticating || 
    sameSessionState.submitting
);

export const getCanSubmitVerification = createSelector(
  [getSameSessionVerificationState],
  (sameSessionState) => 
    !sameSessionState.loading &&
    !sameSessionState.authenticating &&
    !sameSessionState.submitting &&
    sameSessionState.verifierToken !== null
);
```

---

## **Side Effects Management**

### **Token Management**
```typescript
// Helper functions for token management
const setAuthToken = (token: string) => {
  // Update axios default headers
  axiosClient.defaults.headers.common['Authorization'] = `Bearer ${token}`;
  // Update Redux auth state if needed
};

const getAuthToken = (state: RootState) => {
  return state.auth.accessToken;
};

const clearAuthToken = () => {
  delete axiosClient.defaults.headers.common['Authorization'];
};
```

### **Error Recovery**
```typescript
// Error recovery saga
function* recoverFromSameSessionError() {
  try {
    // Restore original token
    const originalToken = yield select(getOriginalToken);
    if (originalToken) {
      yield call(setAuthToken, originalToken);
    }
    
    // Clear same session state
    yield put(jobActions.clearSameSessionVerificationState());
    
    // Close any open modals
    yield put(closeOverlayAction(OverlayNames.SAME_SESSION_VERIFICATION_MODAL));
    
  } catch (error) {
    yield* handleCatch('Job', 'recoverFromSameSessionError', error, true);
  }
}
```

### **Cleanup on Component Unmount**
```typescript
// Cleanup saga for component unmount
function* cleanupSameSessionVerification() {
  try {
    // Restore original token if verifier token is active
    const verifierToken = yield select(getVerifierToken);
    const originalToken = yield select(getOriginalToken);
    
    if (verifierToken && originalToken) {
      yield call(setAuthToken, originalToken);
    }
    
    // Clear state
    yield put(jobActions.clearSameSessionVerificationState());
    
  } catch (error) {
    yield* handleCatch('Job', 'cleanupSameSessionVerification', error, true);
  }
}
```

---

## **Performance Considerations**

### **State Normalization**
- Eligible verifiers stored as normalized array
- Minimal state updates to prevent unnecessary re-renders
- Memoized selectors for computed values

### **Memory Management**
- Clear verification state after completion
- Cleanup tokens and sensitive data
- Remove event listeners and timers

### **Optimistic Updates**
- Show loading states immediately
- Revert on error with proper error handling
- Maintain UI responsiveness during async operations

---

## **Security Considerations**

### **Token Security**
- Never store tokens in component state
- Clear tokens from memory after use
- Use secure token transmission

### **State Isolation**
- Same session verification state isolated from main job state
- Sensitive data cleared on completion/error
- No token exposure in Redux DevTools

### **Error Handling**
- Sanitize error messages before displaying
- Log security events for audit
- Prevent information leakage in error responses

---

## **Testing Strategy**

### **Unit Tests**
```typescript
// Action creators tests
describe('Same Session Verification Actions', () => {
  it('should create fetchEligibleVerifiers action', () => {});
  it('should create completeSameSessionVerification action', () => {});
  it('should create authentication actions', () => {});
});

// Reducer tests
describe('Same Session Verification Reducer', () => {
  it('should handle fetchEligibleVerifiers', () => {});
  it('should handle authentication flow', () => {});
  it('should handle verification completion', () => {});
  it('should handle errors correctly', () => {});
  it('should clear state on cleanup', () => {});
});

// Selector tests
describe('Same Session Verification Selectors', () => {
  it('should select eligible verifiers', () => {});
  it('should select loading states', () => {});
  it('should select error states', () => {});
});
```

### **Saga Tests**
```typescript
// Saga tests
describe('Same Session Verification Sagas', () => {
  it('should fetch eligible verifiers successfully', () => {});
  it('should handle authentication flow', () => {});
  it('should complete verification with token switching', () => {});
  it('should handle concurrent verification', () => {});
  it('should recover from errors', () => {});
  it('should cleanup state properly', () => {});
});
```

### **Integration Tests**
```typescript
// Integration tests
describe('Same Session Verification Integration', () => {
  it('should complete full verification flow', () => {});
  it('should handle authentication errors', () => {});
  it('should handle network errors', () => {});
  it('should handle concurrent access', () => {});
});
```

---

## **Implementation Dependencies**

### **Prerequisites**
1. **API Endpoints**: Eligible verifiers endpoint must be available
2. **Authentication Service**: Token switching capability
3. **Error Handling**: Enhanced error types and messages
4. **Notification System**: Success/error notifications

### **External Dependencies**
- Redux Toolkit for state management
- Redux Saga for side effects
- Axios for HTTP requests
- Reselect for memoized selectors

---

## **Implementation Order**

### **Phase 1: Basic State Structure**
1. Add new action types and creators
2. Update store state interface
3. Add basic reducer cases
4. Create selectors

### **Phase 2: Saga Implementation**
1. Implement fetch eligible verifiers saga
2. Add authentication saga
3. Create main verification saga
4. Add error recovery and cleanup

### **Phase 3: Integration and Testing**
1. Connect sagas to watchers
2. Test token switching logic
3. Add comprehensive error handling
4. Complete testing suite

---

## **Rollback Plan**

### **Safe Rollback Points**
1. **After State Structure**: Can revert state additions
2. **After Saga Implementation**: Can disable saga watchers
3. **After Full Implementation**: Can remove all new code

### **Rollback Steps**
1. Remove new action types and creators
2. Revert store state interface
3. Remove saga functions and watchers
4. Delete selector file
5. Clear any persisted state

---

## **Risk Assessment**

### **High Risk**
- **Token Management**: Complex token switching logic
- **Concurrent Access**: Race conditions in verification
- **Memory Leaks**: Improper cleanup of state and tokens

### **Medium Risk**
- **State Complexity**: Large state object with multiple loading states
- **Error Handling**: Complex error recovery scenarios

### **Low Risk**
- **Action Structure**: Well-defined action patterns
- **Selector Performance**: Memoized selectors prevent issues

---

## **Success Criteria**

### **Functional**
- ✅ All actions dispatch correctly
- ✅ State updates reflect user interactions
- ✅ Sagas handle async operations properly
- ✅ Token switching works securely
- ✅ Error handling covers all scenarios

### **Non-Functional**
- ✅ No memory leaks or state pollution
- ✅ Performance impact minimal
- ✅ Security standards maintained
- ✅ Code coverage > 90%

---

**Document Status:** Ready for Implementation  
**Next Layer:** Service Layer Analysis  
**Dependencies:** Presentation Layer (for action dispatching)
