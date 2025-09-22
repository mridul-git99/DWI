# Same Session Verification - Frontend User Stories & Implementation Guide

**Document Version:** 1.0  
**Date:** January 19, 2025  
**Author:** Frontend Development Team  
**Target Audience:** Frontend Developers, UI/UX Team, QA Team  

---

## **Epic: Same Session Verification Frontend Implementation**

**As a** frontend developer  
**I want** to implement same session verification UI components and flows  
**So that** verifiers can complete verification directly from the initiator's session with a seamless user experience

### **Epic Goals**
- Create intuitive UI components for same session verification
- Implement secure authentication flow within the existing session
- Provide clear visual feedback and error handling
- Maintain consistency with existing peer verification UI patterns
- Support both single user and group assignment scenarios

### **Current Frontend State**
- Peer verification UI is implemented in `PeerVerification.tsx` and `PeerVerificationAction.tsx`
- Initiators see "Recall Verification" button when verification is pending
- Verifiers currently use traditional flow through Inbox → Verifications
- Authentication is handled via password/SSO in existing components

### **Enhancement Scope**
- Add "Same Session Verification" button next to "Recall Verification"
- Create new modal component for same session verification workflow
- Implement user selection for group assignments
- Handle authentication flow with token switching
- Provide real-time feedback and error handling

---

## **Frontend User Stories**

### **Story 1: Same Session Verification Button**

**As a** frontend developer  
**I want** to add a "Same Session Verification" button to the peer verification UI  
**So that** initiators can see the option to enable same session verification

**Priority:** High  
**Story Points:** 3  

**Acceptance Criteria:**
- ✅ Button appears next to "Recall Verification" button in the same div
- ✅ Button is only visible to the initiator (isUserSubmitter = true)
- ✅ Button is only shown when verification status is PENDING
- ✅ Button has same styling as "Recall Verification" button
- ✅ Button text is "Same Session Verification"
- ✅ Button opens same session verification modal when clicked

**Technical Implementation:**
```javascript
// Location: src/views/Job/components/Task/Parameters/Verification/PeerVerification.tsx
// In renderByVerificationState() function, isUserSubmitter section

<Button
  onClick={() => {
    dispatch(
      openOverlayAction({
        type: OverlayNames.SAME_SESSION_VERIFICATION_MODAL,
        props: {
          parameterResponseId,
          parameterId,
          verifications,
        },
      }),
    );
  }}
>
  Same Session Verification
</Button>
```

**Files Modified:**
- `src/views/Job/components/Task/Parameters/Verification/PeerVerification.tsx`
- `src/components/OverlayContainer/types.ts` (add new overlay type)

---

### **Story 2: Same Session Verification Modal Component**

**As a** frontend developer  
**I want** to create a modal component for same session verification  
**So that** verifiers can complete verification with proper UI and validation

**Priority:** High  
**Story Points:** 8  

**Acceptance Criteria:**
- ✅ Modal title is "Same Session Verification"
- ✅ Modal displays parameter details (name, value, specification)
- ✅ Modal shows verifier selection based on assignment type:
  - Single user: Read-only field with assigned user name
  - Group: Dropdown with eligible members (excluding initiator and operator role users)
- ✅ Modal has Accept/Reject radio buttons
- ✅ Comments field is optional for Accept, mandatory for Reject
- ✅ Comments validation matches existing peer verification (character limits, required validation)
- ✅ Authentication section supports both password and SSO
- ✅ Submit button is disabled until all required fields are filled
- ✅ Cancel button closes modal without action

**UI Layout:**
```
┌─────────────────────────────────────────────────────────┐
│ Same Session Verification                    [X]        │
├─────────────────────────────────────────────────────────┤
│ Parameter Details:                                      │
│ Name: Temperature Check                                 │
│ Value: 75.2°C                                          │
│ Specification: 75°C ±1°C                              │
├─────────────────────────────────────────────────────────┤
│ Verifier: [John Smith ▼] or [John Smith (read-only)]   │
│                                                         │
│ Action: ○ Accept  ○ Reject                             │
│                                                         │
│ Comments: [Text Area - shows "Required" if Reject]     │
│ [Character count: 0/500]                               │
│                                                         │
│ Authentication:                                         │
│ Password: [••••••••] or [SSO Login Button]            │
│                                                         │
│ [Complete Verification] [Cancel]                        │
└─────────────────────────────────────────────────────────┘
```

**Component Structure:**
```javascript
// File: src/views/Job/components/Task/Parameters/Verification/SameSessionVerificationModal.tsx

interface SameSessionVerificationModalProps {
  parameterResponseId: string;
  parameterId: string;
  verifications: Dictionary<Verification[]>;
  closeOverlay: () => void;
}

const SameSessionVerificationModal: FC<SameSessionVerificationModalProps> = ({
  parameterResponseId,
  parameterId,
  verifications,
  closeOverlay,
}) => {
  // Component implementation
};
```

**State Management:**
```javascript
const [selectedVerifier, setSelectedVerifier] = useState<User | null>(null);
const [action, setAction] = useState<'accept' | 'reject' | null>(null);
const [comments, setComments] = useState('');
const [password, setPassword] = useState('');
const [eligibleVerifiers, setEligibleVerifiers] = useState<User[]>([]);
const [loading, setLoading] = useState(false);
```

---

### **Story 3: User Selection for Group Assignments**

**As a** frontend developer  
**I want** to implement user selection dropdown for group assignments  
**So that** any eligible group member can select themselves for verification

**Priority:** High  
**Story Points:** 5  

**Acceptance Criteria:**
- ✅ Dropdown is shown only for group assignments
- ✅ Dropdown fetches eligible users via API call when modal opens
- ✅ Dropdown excludes initiator and users with operator role
- ✅ Dropdown shows user's full name and employee ID
- ✅ Dropdown has loading state while fetching users
- ✅ Dropdown shows "No eligible verifiers" if list is empty
- ✅ Selected user is stored in component state
- ✅ Dropdown becomes read-only after selection (until modal reset)

**API Integration:**
```javascript
// New API endpoint to add to apiUrls.ts
export const apiGetEligibleVerifiers = (parameterResponseId: string) =>
  `${baseUrl}/parameter-verifications/parameter-executions/${parameterResponseId}/eligible-verifiers`;

// Usage in component
useEffect(() => {
  if (isGroupAssignment) {
    fetchEligibleVerifiers();
  }
}, []);

const fetchEligibleVerifiers = async () => {
  setLoading(true);
  try {
    const response = await request('GET', apiGetEligibleVerifiers(parameterResponseId));
    setEligibleVerifiers(response.data);
  } catch (error) {
    // Handle error
  } finally {
    setLoading(false);
  }
};
```

**Dropdown Component:**
```javascript
<Select
  placeholder="Select verifier"
  options={eligibleVerifiers.map(user => ({
    label: `${getFullName(user)} (ID: ${user.employeeId})`,
    value: user.id,
    user: user
  }))}
  onChange={(option) => setSelectedVerifier(option.user)}
  isLoading={loading}
  isDisabled={loading}
/>
```

---

### **Story 4: Authentication Integration**

**As a** frontend developer  
**I want** to integrate authentication flow for same session verification  
**So that** verifiers can securely authenticate without compromising session security

**Priority:** High  
**Story Points:** 8  

**Acceptance Criteria:**
- ✅ Authentication section supports both password and SSO methods
- ✅ Password field has show/hide toggle (reuse existing component)
- ✅ SSO button redirects to SSO provider when clicked
- ✅ Authentication state is managed in Redux saga
- ✅ Current (initiator's) token is preserved during verification
- ✅ Verifier's token is used for API call with initiator token in payload
- ✅ Verifier is logged out after verification completion
- ✅ Authentication errors are displayed clearly
- ✅ Loading states are shown during authentication

**Redux Saga Implementation:**
```javascript
// File: src/views/Job/saga.tsx
// New saga function

function* sameSessionVerificationSaga({ payload }) {
  try {
    const { 
      verifierCredentials, 
      action, 
      comments, 
      parameterResponseId, 
      parameterId,
      selectedVerifierId 
    } = payload;
    
    // 1. Store current (initiator's) token
    const initiatorToken = yield select(getAuthToken);
    
    // 2. Authenticate verifier
    const { data: authData, errors: authErrors } = yield call(
      request, 
      'POST', 
      apiLogin(), 
      { data: verifierCredentials }
    );
    
    if (authErrors) {
      throw new Error('Authentication failed');
    }
    
    // 3. Temporarily set verifier's token
    yield put(setAuthToken(authData.accessToken));
    
    // 4. Call verification API
    const apiPayload = {
      comments,
      sameSession: true,
      initiatorJwtToken: initiatorToken
    };
    
    const apiUrl = action === 'accept' 
      ? apiAcceptVerification({ parameterResponseId, type: 'peer' })
      : apiRejectPeerVerification({ parameterResponseId });
    
    const { data, errors } = yield call(request, 'PATCH', apiUrl, { data: apiPayload });
    
    if (errors) {
      throw new Error(getErrorMsg(errors));
    }
    
    // 5. Logout verifier
    yield call(request, 'POST', apiLogOut());
    
    // 6. Restore initiator's token
    yield put(setAuthToken(initiatorToken));
    
    // 7. Update parameter verification state
    yield put(
      jobActions.updateParameterVerifications({
        parameterResponseId,
        parameterId,
        data,
      }),
    );
    
    // 8. Show success message
    yield put(
      showNotification({
        type: NotificationType.SUCCESS,
        msg: action === 'accept' 
          ? 'Parameter has been Peer Verified Successfully'
          : 'Parameter verification has been rejected',
      }),
    );
    
    // 9. Close modal
    yield put(closeOverlayAction(OverlayNames.SAME_SESSION_VERIFICATION_MODAL));
    
  } catch (error) {
    // Restore initiator token on error
    const initiatorToken = yield select(getInitiatorToken);
    if (initiatorToken) {
      yield put(setAuthToken(initiatorToken));
    }
    
    yield put(
      showNotification({
        type: NotificationType.ERROR,
        msg: error.message || 'Same session verification failed',
      }),
    );
  }
}
```

**SSO Integration:**
```javascript
// For SSO users
const handleSSOAuthentication = () => {
  if (selectedVerifier?.ssoEnabled) {
    dispatch(
      ssoSigningRedirect({
        state: SsoStates.SAME_SESSION_VERIFICATION,
        parameterResponseId,
        action,
        comments,
        location: pathname,
      }),
    );
  }
};
```

---

### **Story 5: Form Validation and Error Handling**

**As a** frontend developer  
**I want** to implement comprehensive form validation and error handling  
**So that** users receive clear feedback and cannot submit invalid data

**Priority:** Medium  
**Story Points:** 5  

**Acceptance Criteria:**
- ✅ Verifier selection is required (for group assignments)
- ✅ Action selection (Accept/Reject) is required
- ✅ Comments are required when Reject is selected
- ✅ Comments character limit matches existing peer verification
- ✅ Password is required for local authentication
- ✅ Submit button is disabled when form is invalid
- ✅ Real-time validation feedback is provided
- ✅ Error messages are clear and actionable
- ✅ Network errors are handled gracefully
- ✅ Concurrent verification completion is detected and handled

**Validation Logic:**
```javascript
const isFormValid = useMemo(() => {
  const hasVerifier = isGroupAssignment ? selectedVerifier : true;
  const hasAction = action !== null;
  const hasComments = action === 'reject' ? comments.trim().length > 0 : true;
  const hasAuth = ssoIdToken || password.length > 0;
  
  return hasVerifier && hasAction && hasComments && hasAuth;
}, [selectedVerifier, action, comments, password, ssoIdToken, isGroupAssignment]);

const validateComments = (value: string) => {
  if (action === 'reject' && value.trim().length === 0) {
    return 'Comments are required for rejection';
  }
  if (value.length > MAX_COMMENT_LENGTH) {
    return `Comments cannot exceed ${MAX_COMMENT_LENGTH} characters`;
  }
  return null;
};
```

**Error Handling:**
```javascript
const errorMessages = {
  AUTHENTICATION_FAILED: 'Invalid credentials. Please check your password and try again.',
  VERIFICATION_COMPLETED: 'This parameter has already been verified by another user.',
  NETWORK_ERROR: 'Network error. Please check your connection and try again.',
  UNAUTHORIZED: 'You are not authorized to verify this parameter.',
  TIMEOUT: 'Verification request has timed out. Please try again.',
};
```

---

### **Story 6: Real-time Updates and Concurrent Access**

**As a** frontend developer  
**I want** to handle real-time updates and concurrent access scenarios  
**So that** users are informed when verification state changes during their interaction

**Priority:** Medium  
**Story Points:** 5  

**Acceptance Criteria:**
- ✅ Modal detects if verification is completed while open
- ✅ Clear message is shown: "Peer verification already completed"
- ✅ Modal is automatically closed when verification is completed by another user
- ✅ Parameter state updates are reflected in real-time
- ✅ Optimistic UI updates are handled correctly
- ✅ Network interruptions are handled gracefully
- ✅ User is notified of any state changes

**Implementation:**
```javascript
// WebSocket or polling to detect state changes
useEffect(() => {
  const checkVerificationStatus = async () => {
    try {
      const response = await request('GET', apiGetParameterStatus(parameterResponseId));
      if (response.data.verificationStatus !== ParameterVerificationStatus.PENDING) {
        setVerificationCompleted(true);
        setTimeout(() => {
          closeOverlay();
        }, 3000);
      }
    } catch (error) {
      // Handle error
    }
  };

  const interval = setInterval(checkVerificationStatus, 5000);
  return () => clearInterval(interval);
}, [parameterResponseId]);

// Show completion message
{verificationCompleted && (
  <div className="verification-completed-message">
    <Icon name="check-circle" color="green" />
    <span>Peer verification already completed</span>
  </div>
)}
```

---

### **Story 7: Responsive Design and Accessibility**

**As a** frontend developer  
**I want** to ensure the same session verification modal is responsive and accessible  
**So that** it works well on different devices and for users with disabilities

**Priority:** Low  
**Story Points:** 3  

**Acceptance Criteria:**
- ✅ Modal is responsive on tablet and desktop screens
- ✅ Modal has proper ARIA labels and roles
- ✅ Keyboard navigation works correctly
- ✅ Focus management is handled properly
- ✅ Color contrast meets WCAG guidelines
- ✅ Screen reader compatibility is ensured
- ✅ Touch targets are appropriately sized

**Accessibility Implementation:**
```javascript
<BaseModal
  role="dialog"
  aria-labelledby="same-session-verification-title"
  aria-describedby="same-session-verification-description"
>
  <h2 id="same-session-verification-title">Same Session Verification</h2>
  <div id="same-session-verification-description">
    Complete parameter verification without switching sessions
  </div>
  
  <Select
    aria-label="Select verifier"
    aria-required="true"
    // ... other props
  />
  
  <fieldset>
    <legend>Verification Action</legend>
    <input
      type="radio"
      id="accept"
      name="action"
      value="accept"
      aria-describedby="accept-description"
    />
    <label htmlFor="accept">Accept</label>
    
    <input
      type="radio"
      id="reject"
      name="action"
      value="reject"
      aria-describedby="reject-description"
    />
    <label htmlFor="reject">Reject</label>
  </fieldset>
</BaseModal>
```

---

## **Component Architecture**

### **File Structure**
```
src/views/Job/components/Task/Parameters/Verification/
├── PeerVerification.tsx (modified - add button)
├── PeerVerificationAction.tsx (existing)
├── SameSessionVerificationModal.tsx (new)
├── SameSessionVerificationModal.styles.ts (new)
└── types.ts (new - shared types)

src/components/OverlayContainer/
├── types.ts (modified - add new overlay type)

src/views/Job/
├── saga.tsx (modified - add same session saga)
├── jobStore.ts (modified - add new actions)

src/utils/
├── apiUrls.ts (modified - add eligible verifiers API)
```

### **Component Props Interface**
```typescript
// SameSessionVerificationModal Props
interface SameSessionVerificationModalProps {
  parameterResponseId: string;
  parameterId: string;
  verifications: Dictionary<Verification[]>;
  closeOverlay: () => void;
  closeAllOverlays: () => void;
}

// Eligible Verifier Type
interface EligibleVerifier {
  id: string;
  firstName: string;
  lastName: string;
  employeeId: string;
  email: string;
  ssoEnabled: boolean;
}

// Same Session Verification Request
interface SameSessionVerificationRequest {
  verifierId: string;
  action: 'accept' | 'reject';
  comments?: string;
  password?: string;
  ssoToken?: string;
}
```

### **Redux Actions**
```typescript
// New actions to add to jobStore.ts
export const jobActions = {
  // ... existing actions
  
  initiateSameSessionVerification: createAction<{
    parameterResponseId: string;
    parameterId: string;
  }>('job/initiateSameSessionVerification'),
  
  completeSameSessionVerification: createAction<{
    parameterResponseId: string;
    parameterId: string;
    verifierId: string;
    action: 'accept' | 'reject';
    comments?: string;
    password?: string;
  }>('job/completeSameSessionVerification'),
  
  fetchEligibleVerifiers: createAction<{
    parameterResponseId: string;
  }>('job/fetchEligibleVerifiers'),
  
  fetchEligibleVerifiersSuccess: createAction<{
    verifiers: EligibleVerifier[];
  }>('job/fetchEligibleVerifiersSuccess'),
};
```

---

## **API Integration Requirements**

### **New API Endpoints**
```typescript
// Add to apiUrls.ts

export const apiGetEligibleVerifiers = (parameterResponseId: string) =>
  `${baseUrl}/parameter-verifications/parameter-executions/${parameterResponseId}/eligible-verifiers`;

export const apiGetParameterStatus = (parameterResponseId: string) =>
  `${baseUrl}/parameter-executions/${parameterResponseId}/status`;
```

### **Enhanced API Payloads**
```typescript
// Accept/Reject API payload enhancement
interface VerificationApiPayload {
  comments?: string;
  sameSession?: boolean;
  initiatorJwtToken?: string;
}

// Usage in saga
const apiPayload: VerificationApiPayload = {
  comments: rejectionComments,
  sameSession: true,
  initiatorJwtToken: initiatorToken
};
```

---

## **Testing Strategy**

### **Unit Tests**
```typescript
// SameSessionVerificationModal.test.tsx
describe('SameSessionVerificationModal', () => {
  it('should render modal with correct title', () => {});
  it('should show user dropdown for group assignments', () => {});
  it('should show read-only user for single assignments', () => {});
  it('should require comments for rejection', () => {});
  it('should disable submit when form is invalid', () => {});
  it('should handle authentication errors', () => {});
  it('should close modal on successful verification', () => {});
});

// PeerVerification.test.tsx (modified)
describe('PeerVerification - Same Session Button', () => {
  it('should show same session verification button for initiator', () => {});
  it('should not show button for non-initiators', () => {});
  it('should open modal when button is clicked', () => {});
});
```

### **Integration Tests**
```typescript
// Same session verification flow
describe('Same Session Verification Flow', () => {
  it('should complete verification for single user assignment', () => {});
  it('should complete verification for group assignment', () => {});
  it('should handle authentication with password', () => {});
  it('should handle authentication with SSO', () => {});
  it('should handle rejection with comments', () => {});
  it('should handle concurrent verification completion', () => {});
});
```

### **E2E Tests**
```typescript
// Cypress tests
describe('Same Session Verification E2E', () => {
  it('should allow verifier to complete verification from initiator screen', () => {
    // Test complete user journey
  });
  
  it('should handle group assignment with user selection', () => {
    // Test group assignment flow
  });
  
  it('should show error for invalid credentials', () => {
    // Test error handling
  });
});
```

---

## **Performance Considerations**

### **Optimization Strategies**
1. **Lazy Loading**: Load eligible verifiers only when needed
2. **Memoization**: Use React.memo for expensive components
3. **Debouncing**: Debounce API calls for real-time status checks
4. **Caching**: Cache eligible verifiers for repeated access
5. **Bundle Size**: Ensure new components don't significantly increase bundle size

### **Performance Metrics**
- Modal load time: < 500ms
- API response time: < 2s for eligible verifiers
- Authentication flow: < 5s total
- Real-time updates: < 1s latency

---

## **Security Considerations**

### **Frontend Security Measures**
1. **Token Handling**: Never expose tokens in component props or state
2. **Input Validation**: Sanitize all user inputs
3. **XSS Prevention**: Use proper escaping for dynamic content
4. **CSRF Protection**: Include CSRF tokens in API requests
5. **Session Management**: Proper cleanup of temporary authentication state

### **Implementation Guidelines**
```typescript
// Secure token handling in saga
function* sameSessionVerificationSaga({ payload }) {
  // Store token in saga scope, not in Redux state
  const initiatorToken = yield select(getAuthToken);
  
  try {
    // Use token for API calls
    yield call(apiWithToken, verifierToken);
  } finally {
    // Always restore original token
    yield put(setAuthToken(initiatorToken));
  }
}
```

---

## **Error Scenarios & Handling**

### **Error Types and Messages**
```typescript
const ERROR_MESSAGES = {
  AUTHENTICATION_FAILED: {
    title: 'Authentication Failed',
    message: 'Invalid credentials. Please check your password and try again.',
    action: 'retry'
  },
  VERIFICATION_COMPLETED: {
    title: 'Already Completed',
    message: 'This parameter has already been verified by another user.',
    action: 'close'
  },
  NETWORK_ERROR: {
    title: 'Connection Error',
    message: 'Unable to connect to server. Please check your internet connection.',
    action: 'retry'
  },
  UNAUTHORIZED: {
    title: 'Access Denied',
    message: 'You are not authorized to verify this parameter.',
    action: 'close'
  },
  VALIDATION_ERROR: {
    title: 'Invalid Input',
    message: 'Please check your input and try again.',
    action: 'fix'
  }
};
```

### **Error Handling Component**
```typescript
const ErrorDisplay: FC<{ error: ErrorType; onRetry?: () => void; onClose: () => void }> = ({
  error,
  onRetry,
  onClose
}) => (
  <div className="error-display">
    <Icon name="error" color="red" />
    <h3>{ERROR_MESSAGES[error].title}</h3>
    <p>{ERROR_MESSAGES[error].message}</p>
    <div className="error-actions">
      {onRetry && ERROR_MESSAGES[error].action === 'retry' && (
        <Button onClick={onRetry}>Try Again</Button>
      )}
      <Button variant="secondary" onClick={onClose}>
        {ERROR_MESSAGES[error].action === 'close' ? 'Close' : 'Cancel'}
      </Button>
    </div>
  </div>
);
```

---

## **Implementation Phases**

### **Phase 1: Core UI Components (Sprint 1)**
- [ ] Add "Same Session Verification" button to PeerVerification.tsx
- [ ] Create basic SameSessionVerificationModal component
- [ ] Add new overlay type to OverlayContainer
- [ ] Implement basic form structure and validation

### **Phase 2: User Selection & API Integration (Sprint 2)**
- [ ] Implement eligible verifiers API integration
- [ ] Add user selection dropdown for group assignments
- [ ] Handle single user vs group assignment logic
- [ ] Add loading states and error handling

### **Phase 3: Authentication Flow (Sprint 3)**
- [ ] Implement Redux saga for same session verification
- [ ] Add token switching logic
- [ ] Integrate password and SSO authentication
- [ ] Add comprehensive error handling

### **Phase 4: Polish & Testing (Sprint 4)**
- [ ] Add real-time updates and concurrent access handling
- [ ] Implement accessibility features
- [ ] Add comprehensive testing
- [ ] Performance optimization and code review

---

## **Success Criteria**

### **Functional Requirements**
- ✅ Same session verification button appears for initiators
- ✅ Modal opens with correct parameter details
- ✅ User selection works for both single and group assignments
- ✅ Authentication flow completes successfully
- ✅ Verification updates parameter state correctly
- ✅ Error handling provides clear feedback

### **Non-Functional Requirements**
- ✅ Modal loads within 500ms
- ✅ Authentication completes within 5 seconds
- ✅ UI is responsive on tablet and desktop
- ✅ Accessibility standards are met
- ✅ No security vulnerabilities introduced

### **User Experience Goals**
- ✅ Intuitive and easy to use interface
- ✅ Clear visual feedback for all actions
- ✅ Consistent with existing UI patterns
- ✅ Minimal learning curve for users
- ✅ Graceful error handling and recovery

---

## **Appendix**

### **Related Components**
- `PeerVerification.tsx` - Main peer verification component
- `PeerVerificationAction.tsx` - Verification action buttons
- `BulkVerificationModal.tsx` - Bulk verification modal
- `BaseModal.tsx` - Base modal component
- `PasswordInputSection.tsx` - Password input component

### **Styling Guidelines**
- Use existing design system components
- Maintain consistent spacing and typography
- Follow accessibility color contrast guidelines
- Use semantic HTML elements
- Implement responsive design patterns

### **Browser Support**
- Chrome 90+
- Firefox 88+
- Safari 14+
- Edge 90+

---

**Document Status:** Ready for Implementation  
**Next Steps:** Begin Phase 1 implementation  
**Review Required:** UI/UX Team, Security Team, Backend Team
