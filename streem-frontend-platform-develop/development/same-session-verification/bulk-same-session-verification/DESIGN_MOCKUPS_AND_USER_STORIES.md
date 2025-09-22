# Bulk Same Session Verification - Design Mockups & User Stories

## 📋 Feature Overview

### Purpose
Enable users to perform same-session verification for multiple parameters simultaneously within the existing bulk peer verification workflow, streamlining the verification process and improving efficiency.

### Integration Point
- **Entry Point**: Enhanced AssignBulkPeerVerification component
- **Trigger**: Additional "Same Session Verification" button alongside existing "Submit" button
- **Flow**: Extends existing bulk peer verification workflow with immediate verification capability

---

## 👥 User Stories

### **Epic: Bulk Same Session Verification**

#### **User Story 1: Initiator Bulk Same Session Verification**
**As a** job initiator  
**I want to** perform same-session verification for multiple parameters at once  
**So that** I can efficiently verify multiple parameters without waiting for individual verifier responses

**Acceptance Criteria:**
- [ ] I can access bulk same-session verification from the bulk peer verification interface
- [ ] I can select a common verifier from all assigned verifiers across selected parameters
- [ ] I can enter the verifier's password once to verify all selected parameters
- [ ] I can choose to either approve or reject all parameters in bulk
- [ ] I receive confirmation of successful bulk verification
- [ ] All parameter states are updated simultaneously
- [ ] Audit logs are created for each parameter verification

#### **User Story 2: Verifier Selection for Multiple Parameters**
**As a** job initiator  
**I want to** see only verifiers who are assigned to all selected parameters  
**So that** I can ensure the chosen verifier has authority to verify all parameters

**Acceptance Criteria:**
- [ ] Only common verifiers across all selected parameters are shown
- [ ] If no common verifiers exist, I see an appropriate error message
- [ ] Verifier information includes name and employee ID
- [ ] I can see how many parameters will be verified

#### **User Story 3: Bulk Rejection with Reason**
**As a** job initiator  
**I want to** provide a single rejection reason for all parameters  
**So that** I can efficiently reject multiple parameters with consistent reasoning

**Acceptance Criteria:**
- [ ] I can enter a rejection reason that applies to all selected parameters
- [ ] The reason field is mandatory for bulk rejection
- [ ] The same reason is recorded for all parameters in audit logs
- [ ] I can cancel the rejection and return to the previous step

#### **User Story 4: Error Handling and Validation**
**As a** job initiator  
**I want to** receive clear error messages if bulk verification fails  
**So that** I can understand what went wrong and take corrective action

**Acceptance Criteria:**
- [ ] I see specific error messages for authentication failures
- [ ] I see clear messages if some parameters fail verification
- [ ] I can retry verification after fixing issues
- [ ] Partial failures are handled gracefully with rollback capability

---

## 🎨 UI/UX Design Mockups

### **1. Enhanced AssignBulkPeerVerification Footer**

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        Request Peer Verification                           │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  Step 2: Assign Users                                                      │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │  👥 Users                                                           │   │
│  │  ┌─────────────────────────────────────────────────────────────┐   │   │
│  │  │ ☑ Vivek L (ID: L-account.owner.01)                         │   │   │
│  │  │ ☑ Aarush L (ID: L-process.publisher.01)                    │   │   │
│  │  │ ☐ Manoj L (ID: L-supervisor.09)                            │   │   │
│  │  └─────────────────────────────────────────────────────────────┘   │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
├─────────────────────────────────────────────────────────────────────────────┤
│  Footer Actions:                                                           │
│                                                                             │
│  [Cancel]           [Submit]           [Same Session Verification]         │
│   ↑                  ↑                  ↑                                  │
│  Secondary         Primary            Secondary Blue                       │
│  Gray              Blue               (New Feature)                        │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### **2. Bulk Same Session Verification Modal - Initial State**

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  🔄 Bulk Same Session Verification                                   [×]   │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  📊 Verification Summary                                                   │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │  Selected Parameters: 5                                            │   │
│  │  • Parameter A - Temperature Check (Task 1.2)                     │   │
│  │  • Parameter B - Pressure Reading (Task 1.3)                      │   │
│  │  • Parameter C - Flow Rate (Task 2.1)                             │   │
│  │  • Parameter D - Quality Check (Task 2.2)                         │   │
│  │  • Parameter E - Final Inspection (Task 3.1)                      │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  👤 Available Verifiers (Common across all parameters):                   │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │  ▼ Select a verifier                                               │   │
│  │     • Vivek L (ID: L-account.owner.01)                             │   │
│  │     • Aarush L (ID: L-process.publisher.01)                        │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  ⚠️  Note: Only verifiers assigned to ALL selected parameters are shown    │
│                                                                             │
│                                                                             │
│  [Cancel]                                                    [Continue]    │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### **3. Bulk Same Session Verification Modal - Action Selection**

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  🔄 Bulk Same Session Verification                                   [×]   │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  👤 Verifying as: Vivek L (ID: L-account.owner.01)                        │
│  📊 Parameters: 5 selected                                                │
│                                                                             │
│  🎯 Choose Verification Action:                                            │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │  ✅ Bulk Approve All Parameters                                    │   │
│  │     Approve all 5 selected parameters                              │   │
│  │                                                                     │   │
│  │  [Select Approve]                                                  │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │  ❌ Bulk Reject All Parameters                                     │   │
│  │     Reject all 5 selected parameters with reason                   │   │
│  │                                                                     │   │
│  │  [Select Reject]                                                   │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│                                                                             │
│  [Back]                                                      [Continue]    │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### **4. Bulk Same Session Verification Modal - Approve Flow**

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  ✅ Bulk Approve Verification                                        [×]   │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  👤 Verifying as: Vivek L (ID: L-account.owner.01)                        │
│  📊 Action: Approve 5 parameters                                          │
│                                                                             │
│  🔐 Authentication Required:                                               │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │  Password:                                                          │   │
│  │  ┌─────────────────────────────────────────────────────────────┐   │   │
│  │  │ Enter verifier's account password            [👁]          │   │   │
│  │  └─────────────────────────────────────────────────────────────┘   │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  📋 Parameters to be approved:                                            │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │  ✓ Parameter A - Temperature Check                                 │   │
│  │  ✓ Parameter B - Pressure Reading                                  │   │
│  │  ✓ Parameter C - Flow Rate                                         │   │
│  │  ✓ Parameter D - Quality Check                                     │   │
│  │  ✓ Parameter E - Final Inspection                                  │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  [Back]                                              [Approve All]         │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### **5. Bulk Same Session Verification Modal - Reject Flow**

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  ❌ Bulk Reject Verification                                         [×]   │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  👤 Verifying as: Vivek L (ID: L-account.owner.01)                        │
│  📊 Action: Reject 5 parameters                                           │
│                                                                             │
│  📝 Rejection Reason (Required):                                          │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │  ┌─────────────────────────────────────────────────────────────┐   │   │
│  │  │ Provide reason for bulk rejection                           │   │   │
│  │  │                                                             │   │   │
│  │  │ This reason will be applied to all selected parameters     │   │   │
│  │  │                                                             │   │   │
│  │  └─────────────────────────────────────────────────────────────┘   │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  🔐 Authentication Required:                                               │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │  Password:                                                          │   │
│  │  ┌─────────────────────────────────────────────────────────────┐   │   │
│  │  │ Enter verifier's account password            [👁]          │   │   │
│  │  └─────────────────────────────────────────────────────────────┘   │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  [Back]                                               [Reject All]         │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### **6. Bulk Same Session Verification Modal - Processing State**

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  ⏳ Processing Bulk Verification...                               [×]   │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  👤 Verifying as: Vivek L (ID: L-account.owner.01)                        │
│                                                                             │
│  📊 Progress:                                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │  ████████████████████████████████████████████████████████████████   │   │
│  │  Processing verification for 5 parameters...                       │   │
│  │                                                                     │   │
│  │  ✅ Authenticating verifier credentials                            │   │
│  │  ⏳ Executing bulk verification...                                 │   │
│  │  ⏸️ Updating parameter states...                                   │   │
│  │  ⏸️ Creating audit logs...                                         │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  ⚠️  Please do not close this window during processing                     │
│                                                                             │
│                                                                             │
│                              [Cancel]                                      │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### **7. Error State - No Common Verifiers**

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  ⚠️ Bulk Same Session Verification                                   [×]   │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  📊 Selected Parameters: 5                                                │
│                                                                             │
│  ❌ No Common Verifiers Found                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │  The selected parameters do not have any common verifiers.         │   │
│  │                                                                     │   │
│  │  Parameter A: Assigned to Vivek L, Aarush L                       │   │
│  │  Parameter B: Assigned to Aarush L, Manoj L                       │   │
│  │  Parameter C: Assigned to Vivek L, Manoj L                        │   │
│  │  Parameter D: Assigned to Vivek L                                 │   │
│  │  Parameter E: Assigned to Manoj L                                 │   │
│  │                                                                     │   │
│  │  💡 Suggestion: Use individual same-session verification or       │   │
│  │     select parameters with common verifiers.                       │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│                                                                             │
│  [Back to Selection]                                        [Close]        │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 🔄 User Flow Diagrams

### **Primary Flow: Bulk Same Session Verification**

```
Start: Bulk Peer Verification Interface
    ↓
[Select Parameters] (Existing Step 1)
    ↓
[Assign Verifiers] (Existing Step 2)
    ↓
[Click "Same Session Verification"] (New)
    ↓
[Extract Common Verifiers]
    ↓
Decision: Common Verifiers Found?
    ├─ No → [Show Error Message] → [Back to Selection]
    └─ Yes → [Show Verifier Selection Modal]
              ↓
          [Select Verifier]
              ↓
          [Choose Action: Approve/Reject]
              ↓
          Decision: Approve or Reject?
              ├─ Approve → [Enter Password] → [Execute Bulk Approve]
              └─ Reject → [Enter Reason] → [Enter Password] → [Execute Bulk Reject]
                  ↓
              [Show Processing State]
                  ↓
              Decision: Success?
                  ├─ Yes → [Show Success Message] → [Close Modal] → [Update UI]
                  └─ No → [Show Error Message] → [Retry Option]
```

### **Error Handling Flow**

```
Authentication Error
    ↓
[Show Error Message]
    ↓
[Allow Retry with Different Password]
    ↓
[Return to Password Entry]

Partial Verification Failure
    ↓
[Show Detailed Error for Failed Parameters]
    ↓
[Offer Options: Retry All, Retry Failed Only, Cancel]
    ↓
[Execute Selected Option]

Network/API Error
    ↓
[Show Generic Error Message]
    ↓
[Offer Retry Option]
    ↓
[Return to Previous Step]
```

---

## 🛠️ Technical Requirements

### **Component Architecture**

#### **1. Enhanced AssignBulkPeerVerification Component**
```typescript
// New props and state
interface BulkSameSessionState {
  showSameSessionModal: boolean;
  selectedParameters: Parameter[];
  commonVerifiers: Verifier[];
  bulkVerificationInProgress: boolean;
}

// New methods
const handleSameSessionVerification = () => {
  const commonVerifiers = extractCommonVerifiers(selectedParameters);
  if (commonVerifiers.length === 0) {
    showNoCommonVerifiersError();
  } else {
    openBulkSameSessionModal();
  }
};
```

#### **2. BulkSameSessionVerificationModal Component**
```typescript
interface BulkSameSessionVerificationModalProps {
  parameters: Parameter[];
  commonVerifiers: Verifier[];
  onApprove: (verifier: Verifier, password: string) => void;
  onReject: (verifier: Verifier, password: string, reason: string) => void;
  onCancel: () => void;
}

// Modal states
type ModalState = 
  | 'verifier-selection'
  | 'action-selection' 
  | 'approve-confirmation'
  | 'reject-with-reason'
  | 'processing'
  | 'success'
  | 'error';
```

### **Redux Actions**

```typescript
// New action types
enum JobActionsEnum {
  // ... existing actions
  COMPLETE_BULK_SAME_SESSION_VERIFICATION = 'COMPLETE_BULK_SAME_SESSION_VERIFICATION',
  COMPLETE_BULK_SAME_SESSION_VERIFICATION_SUCCESS = 'COMPLETE_BULK_SAME_SESSION_VERIFICATION_SUCCESS',
  COMPLETE_BULK_SAME_SESSION_VERIFICATION_FAILURE = 'COMPLETE_BULK_SAME_SESSION_VERIFICATION_FAILURE',
}

// Action creators
const completeBulkSameSessionVerification = (payload: {
  parameters: Parameter[];
  verifier: Verifier;
  password: string;
  action: 'approve' | 'reject';
  reason?: string;
}) => ({
  type: JobActionsEnum.COMPLETE_BULK_SAME_SESSION_VERIFICATION,
  payload,
});
```

### **API Integration**

```typescript
// Extend existing bulk verification API
const apiBulkSameSessionVerification = () => 
  `${baseUrl}/parameter-verifications/parameter-executions/bulk/same-session`;

// Request payload
interface BulkSameSessionVerificationRequest {
  parameters: {
    parameterExecutionId: string;
    parameterId: string;
  }[];
  verifier: {
    id: string;
    credentials: string; // encrypted password
  };
  action: 'approve' | 'reject';
  reason?: string;
  sameSession: true;
  initiatorJwtToken: string;
}
```

---

## ✅ Acceptance Criteria

### **Functional Requirements**

#### **Core Functionality**
- [ ] Users can access bulk same-session verification from bulk peer verification interface
- [ ] System extracts and displays only common verifiers across selected parameters
- [ ] Users can select a verifier and choose approve/reject action
- [ ] Password authentication works for selected verifier
- [ ] Bulk verification executes for all selected parameters simultaneously
- [ ] All parameter states update correctly after verification
- [ ] Audit logs are created for each parameter verification

#### **User Experience**
- [ ] Modal follows existing design patterns and styling
- [ ] Clear progress indication during processing
- [ ] Appropriate error messages for all failure scenarios
- [ ] Consistent button styling and alignment
- [ ] Responsive design works on different screen sizes

#### **Error Handling**
- [ ] Graceful handling when no common verifiers exist
- [ ] Clear error messages for authentication failures
- [ ] Proper rollback for partial verification failures
- [ ] Network error handling with retry options

### **Performance Requirements**
- [ ] Modal opens within 500ms of button click
- [ ] Verifier extraction completes within 1 second
- [ ] Bulk verification API call completes within 10 seconds
- [ ] UI remains responsive during processing

### **Security Requirements**
- [ ] Password encryption before transmission
- [ ] Proper token management for dual authentication
- [ ] Audit trail for all verification actions
- [ ] Session cleanup after verification completion

### **Accessibility Requirements**
- [ ] Keyboard navigation support
- [ ] Screen reader compatibility
- [ ] High contrast mode support
- [ ] Focus management in modal

---

## 📝 Implementation Notes

### **Development Phases**

#### **Phase 1: Core Infrastructure**
1. Redux actions and saga implementation
2. API integration and error handling
3. Common verifier extraction utility

#### **Phase 2: UI Components**
1. Enhanced AssignBulkPeerVerification component
2. BulkSameSessionVerificationModal component
3. Modal state management and transitions

#### **Phase 3: Integration & Testing**
1. End-to-end flow testing
2. Error scenario validation
3. Performance optimization
4. Accessibility compliance

### **Testing Strategy**

#### **Unit Tests**
- Common verifier extraction logic
- Modal state transitions
- Redux action creators and reducers

#### **Integration Tests**
- Complete bulk verification flow
- Error handling scenarios
- API integration

#### **E2E Tests**
- User journey from parameter selection to verification completion
- Cross-browser compatibility
- Performance benchmarks

---

## 🎯 Success Metrics

### **User Experience Metrics**
- Reduction in verification time for multiple parameters
- User adoption rate of bulk same-session verification
- Error rate and user recovery success

### **Technical Metrics**
- API response times for bulk operations
- Success rate of bulk verifications
- System performance impact

### **Business Metrics**
- Increased efficiency in verification workflows
- Reduced time-to-completion for jobs with multiple verifications
- User satisfaction scores

---

*This document serves as the comprehensive specification for implementing Bulk Same Session Verification feature. All mockups, user stories, and technical requirements should be validated with stakeholders before implementation begins.*
