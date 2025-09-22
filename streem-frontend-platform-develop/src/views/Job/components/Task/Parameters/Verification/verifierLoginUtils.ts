import {
  apiAcceptVerification,
  apiLogin,
  apiLogOut,
  apiRejectPeerVerification,
  apiBulkAcceptVerification,
} from '#utils/apiUrls';
import { request } from '#utils/request';
import { encrypt } from '#utils/stringUtils';
import { setAuthHeader } from '#utils/axiosClient';

interface BulkParameter {
  parameterExecutionId: string;
  parameterId: string;
}

interface VerifierLoginParams {
  verifierUserName: string;
  password: string | null;
  // Single verification parameters
  parameterResponseId?: string;
  // Bulk verification parameters
  bulkParameters?: BulkParameter[];
  isBulk?: boolean;
  // Common parameters
  initiatorToken: string;
  code: string | null;
  state: string | null;
  pendingAction: 'accept' | 'reject' | null;
  rejectionReason?: string;
}

export const verifierLoginUtils = async ({
  verifierUserName,
  password,
  parameterResponseId,
  bulkParameters,
  isBulk = false,
  initiatorToken,
  code,
  state,
  pendingAction,
  rejectionReason = '',
}: VerifierLoginParams) => {
  try {
    // Validate initiator token is present
    if (!initiatorToken) {
      throw new Error('Session restoration failed: initiator token missing.');
    }

    // Login as verifier
    const loginResponse = await request('POST', apiLogin(), {
      data: {
        username: verifierUserName,
        password: password ? encrypt(password) : null,
        code,
        state,
      },
      preserveAuthHeader: true,
    });

    if (loginResponse.errors) {
      throw new Error('Invalid verifier credentials');
    }

    const verifierToken = `Bearer ${loginResponse.data.accessToken}`;

    // Accept or Reject verification based on pending action and bulk/single mode
    let verificationResponse;

    if (isBulk) {
      // Handle bulk verification
      if (pendingAction === 'reject') {
        // TODO: Implement bulk rejection API when available
        throw new Error('Bulk rejection is not yet implemented');
      } else {
        // Bulk acceptance
        verificationResponse = await request('PATCH', apiBulkAcceptVerification({ type: 'peer' }), {
          data: {
            peerVerify: bulkParameters,
            initiatorJwtToken: initiatorToken,
            sameSession: true,
          },
          headers: { Authorization: verifierToken },
        });
      }
    } else {
      // Handle single verification
      if (pendingAction === 'reject') {
        verificationResponse = await request(
          'PATCH',
          apiRejectPeerVerification({
            parameterResponseId: parameterResponseId!,
          }),
          {
            data: {
              initiatorJwtToken: initiatorToken,
              sameSession: true,
              comments: rejectionReason,
            },
            headers: { Authorization: verifierToken },
          },
        );
      } else {
        verificationResponse = await request(
          'PATCH',
          apiAcceptVerification({
            parameterResponseId: parameterResponseId!,
            type: 'peer',
          }),
          {
            data: {
              initiatorJwtToken: initiatorToken,
              sameSession: true,
            },
            headers: { Authorization: verifierToken },
          },
        );
      }
    }

    if (verificationResponse.errors) {
      throw new Error(`${pendingAction === 'reject' ? 'Rejection' : 'Verification'} failed`);
    }

    // Logout verifier
    try {
      await request('POST', apiLogOut(), {
        data: { idToken: '' },
        headers: { Authorization: verifierToken },
      });
    } catch (logoutError) {
      // Continue anyway if logout fails
    }

    // Restore initiator's session after verifier logout
    try {
      // Extract token without Bearer prefix if present
      const cleanToken = initiatorToken.replace(/^Bearer\s+/i, '');

      // 1. Update Redux store
      const store = (window as any).store;
      if (store) {
        store.dispatch({
          type: '@@auth/Login/REFRESH_TOKEN_SUCCESS',
          payload: { accessToken: cleanToken },
        });
      } else {
        throw new Error('Could not restore initiator session: Redux store not accessible');
      }

      // 2. Update Axios client authorization header
      setAuthHeader(cleanToken);
    } catch (sessionError) {
      throw new Error('Session restoration failed after verification');
    }

    return { success: true };
  } catch (error) {
    throw error;
  }
};
