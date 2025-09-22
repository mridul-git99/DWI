# Peer Verification Code Flow Analysis

**Document Version:** 1.0  
**Date:** June 20, 2025  
**Analysis:** Complete Peer Verification System Understanding

---

## **Overview**

The peer verification system in Streem allows parameters to be verified by other users (peers) to ensure quality and accuracy. This analysis covers the complete flow from parameter execution to verification completion.

---

## **Key Components & Flow**

### **1. Parameter Execution & Verification States**

**Parameter States:**
- `BEING_EXECUTED` - Parameter is being filled out
- `VERIFICATION_PENDING` - Parameter awaits verification
- `APPROVAL_PENDING` - Parameter awaits supervisor approval

**Verification Status:**
- `PENDING` - Verification request sent, awaiting verifier action
- `ACCEPTED` - Verification approved by verifier
- `REJECTED` - Verification rejected by verifier

### **2. Core Components**

#### **A. Individual Peer Verification (`PeerVerification.tsx`)**
- **Location**: `src/views/Job/components/Task/Parameters/Verification/PeerVerification.tsx`
- **Purpose**: Handles individual parameter verification
- **Key Functions**:
  - Shows "Request Verification" button for parameter initiators
  - Shows verification actions for assigned verifiers
  - Shows "Same Session Verification" button for initiators
  - Displays verification status and history

#### **B. Bulk Peer Verification Assignment (`AssignBulkPeerVerification.tsx`)**
- **Location**: `src/views/Job/components/Task/Parameters/Verification/AssignBulkPeerVerification.tsx`
- **Purpose**: Assigns verifiers to multiple parameters at once
- **Flow**:
  1. Step 1: Select parameters (minimum 2 required)
  2. Step 2: Assign users/groups as verifiers
  3. Sends bulk peer verification requests

#### **C. Bulk Peer Verification Modal (`BulkVerificationModal.tsx`)**
- **Location**: `src/views/Job/components/Task/Header/BulkVerificationModal.tsx`
- **Purpose**: Allows verifiers to verify multiple parameters at once
- **Flow**:
  1. Select parameters to verify
  2. Enter password for authentication
  3. Submit bulk verification

---

## **Data Flow & State Management**

### **1. Parameter State Population (`saga.tsx` - `pollActiveTaskExecutionSaga`)**

The saga populates three key arrays in the job store:

```typescript
const pendingSelfVerificationParameters: any[] = [];
const pendingPeerVerificationParameters: any[] = [];
const executedParametersWithPeerVerification: any[] = [];
```

**Population Logic:**
- **`pendingPeerVerificationParameters`**: Parameters where current user is assigned as verifier and verification status is PENDING
- **`executedParametersWithPeerVerification`**: Parameters where current user is the initiator and can request peer verification
- **`pendingSelfVerificationParameters`**: Parameters where current user can perform self verification

### **2. Conditions for Bulk Verification Menu Visibility**

**In Task Header (`Header/index.tsx`):**
```typescript
{!isBlocked && hasBulkVerification && (
  // Bulk verification options appear here
)}
```

**Key Conditions:**
- `!isBlocked` - Task is not blocked
- `hasBulkVerification` - Task has bulk verification enabled
- `pendingPeerVerificationParameters.length > 0` - Has parameters awaiting verification

---

## **Verification Flow Types**

### **1. Individual Peer Verification Flow**
1. **Initiate**: User fills parameter → clicks "Request Verification"
2. **Assign**: Selects verifiers → sends verification request
3. **Verify**: Verifier receives notification → verifies parameter
4. **Complete**: Parameter state updates to verified/rejected

### **2. Bulk Peer Verification Assignment Flow**
1. **Select**: User selects multiple parameters (2+ required)
2. **Assign**: Assigns verifiers to all selected parameters
3. **Send**: Bulk verification requests sent to verifiers
4. **Result**: All parameters enter VERIFICATION_PENDING state

### **3. Bulk Peer Verification (Verifier Side)**
1. **Access**: Verifier sees "Bulk Peer Verification" in task menu
2. **Select**: Chooses parameters to verify from pending list
3. **Authenticate**: Enters password for verification
4. **Submit**: All selected parameters verified simultaneously

### **4. Same Session Verification Flow**
1. **Individual**: After requesting verification, initiator can immediately verify using verifier credentials
2. **Bulk**: After bulk assignment, initiator can verify multiple parameters using common verifier credentials

---

## **Key Conditions & Logic**

### **1. When Bulk Verification Menu Appears**

**Required Conditions:**
```typescript
!isBlocked && hasBulkVerification && pendingPeerVerificationParameters.length > 0
```

**Why Menu Might Not Appear:**
- `isBlocked = true` - Task is blocked (paused, completed, etc.)
- `hasBulkVerification = false` - Task doesn't have bulk verification enabled
- `pendingPeerVerificationParameters.length = 0` - No parameters awaiting verification

### **2. Parameter Verification Eligibility**

**For Peer Verification Request:**
```typescript
requestPeerVerification = 
  !(taskExecution.state in COMPLETED_STATES) &&
  userId === response?.audit?.modifiedBy?.id &&
  !taskExecution.correctionEnabled &&
  peerVerificationStatus !== PENDING &&
  [BEING_EXECUTED, APPROVAL_PENDING, VERIFICATION_PENDING].includes(state) &&
  verificationType includes PEER
```

**For Peer Verification Action:**
```typescript
showPeerVerification = 
  isUserReviewer &&
  peerVerificationStatus === PENDING &&
  verificationType includes PEER &&
  !(taskExecution.state in COMPLETED_STATES)
```

---

## **API Endpoints & Actions**

### **1. Individual Verification**
- `apiInitiatePeerVerification()` - Send verification request
- `apiAcceptVerification()` - Accept verification
- `apiRejectPeerVerification()` - Reject verification
- `apiRecallVerification()` - Recall verification request

### **2. Bulk Verification**
- `apiSendBulkPeerVerification()` - Send bulk verification requests
- `apiBulkAcceptVerification()` - Bulk accept verifications

### **3. Redux Actions**
- `sendPeerVerification` - Individual verification request
- `sendBulkPeerVerification` - Bulk verification assignment
- `completeBulkPeerVerification` - Bulk verification completion
- `acceptPeerVerification` - Accept individual verification
- `rejectPeerVerification` - Reject individual verification

---

## **Common Issues & Troubleshooting**

### **1. "Bulk Peer Verification" Menu Not Appearing**

**Check These Conditions:**
1. **Task State**: Ensure task is not completed/blocked
2. **Bulk Verification Flag**: Check if `hasBulkVerification` is true
3. **Pending Parameters**: Verify `pendingPeerVerificationParameters.length > 0`
4. **User Assignment**: Ensure user is assigned as verifier to parameters
5. **Parameter State**: Parameters must be in VERIFICATION_PENDING state

### **2. Parameters Not Appearing in Pending List**

**Verify:**
1. **Verification Type**: Parameter must have PEER verification type
2. **User Role**: Current user must be assigned as verifier
3. **Verification Status**: Must be PENDING status
4. **Task State**: Task must not be completed

### **3. Same Session Verification Not Working**

**Check:**
1. **Common Verifiers**: Parameters must have at least one common verifier
2. **Parameter Count**: Minimum 2 parameters required
3. **Verification Status**: Parameters must be in PENDING state
4. **User Permissions**: User must be the initiator of verification requests

---

## **Implementation Recommendations**

### **1. For Bulk Same Session Verification**

**Integration Point**: After bulk peer verification assignment
**Trigger**: When `pendingPeerVerificationParameters.length > 1`
**Location**: Task header menu (independent of `hasBulkVerification`)

### **2. User Experience Flow**

1. **Normal Flow**: User assigns bulk peer verification
2. **Enhanced Flow**: After assignment, option for immediate bulk verification appears
3. **Verification**: User can verify using common verifier credentials
4. **Result**: All parameters verified in single session

---

## **Code Locations Summary**

| Component | Location | Purpose |
|-----------|----------|---------|
| PeerVerification | `src/views/Job/components/Task/Parameters/Verification/PeerVerification.tsx` | Individual parameter verification |
| AssignBulkPeerVerification | `src/views/Job/components/Task/Parameters/Verification/AssignBulkPeerVerification.tsx` | Bulk verification assignment |
| BulkVerificationModal | `src/views/Job/components/Task/Header/BulkVerificationModal.tsx` | Bulk verification execution |
| Task Header | `src/views/Job/components/Task/Header/index.tsx` | Menu options for bulk operations |
| Job Saga | `src/views/Job/saga.tsx` | State management and API calls |
| Job Store | `src/views/Job/jobStore.ts` | Redux store for job state |

---

**Analysis Complete**: This document provides comprehensive understanding of the peer verification system architecture and flow.
