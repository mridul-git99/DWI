import React, { FC, useState, useEffect } from 'react';
import { BaseModal, Button, TextInput, Select } from '#components';
import { SameSessionVerificationModalProps } from '#components/OverlayContainer/types';
import { CommonOverlayProps } from '#components/OverlayContainer/types';
import PasswordInputSection from '#components/shared/PasswordInputSection';
import styled from 'styled-components';
import { InputTypes } from '#utils/globalTypes';
import { useDispatch } from 'react-redux';
import { showNotification } from '#components/Notification/actions';
import { NotificationType } from '#components/Notification/types';
import { apiGetUser, apiAccountLookUp } from '#utils/apiUrls';
import { request } from '#utils/request';
import { setAuthHeader } from '#utils/axiosClient';
import { useForm } from 'react-hook-form';
import { navigate } from '@reach/router';
import { compressState, decompressUrl } from '#utils/decompressUrl';
import { verifierLoginUtils } from './verifierLoginUtils';
type Inputs = {
  password: string;
};

type SameSessionVerificationModalComponentProps =
  CommonOverlayProps<SameSessionVerificationModalProps>;

const Wrapper = styled.div.attrs({})`
  .correction-modal-body {
    padding: 8px;
    display: flex;
    flex-direction: column;
    gap: 24px;

    .form-group {
      padding: unset;
    }

    .verifier-section {
      margin-bottom: 0px;

      .verifier-label {
        display: block;
        margin-bottom: 8px;
        font-size: 14px;
        font-weight: 500;
        color: #525252;
      }

      .verifier-loading,
      .verifier-error,
      .verifier-empty,
      .verifier-display {
        padding: 8px 12px;
        border-radius: 4px;
        font-size: 14px;
      }

      .verifier-loading,
      .verifier-display {
        background-color: #f0f0f0;
        color: #666;
      }

      .verifier-error {
        background-color: #fee;
        color: #d00;
        border: 1px solid #fcc;
      }

      .verifier-empty {
        background-color: #fff3cd;
        color: #856404;
        border: 1px solid #ffeaa7;
      }
    }

    .parameter-verification {
      .verification-label {
        display: block;
        margin-bottom: 8px;
        font-size: 14px;
        font-weight: 500;
        color: #525252;
      }

      .verification-loading {
        padding: 8px 12px;
        background-color: #f0f0f0;
        border-radius: 4px;
        font-size: 14px;
        color: #666;
        text-align: center;
      }

      .verification-input {
        margin-bottom: 12px;
      }

      .verification-buttons {
        display: flex;
        gap: 12px;
      }
    }
  }

  .correction-modal-footer {
    border-top: 1px solid #f4f4f4 !important;
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 8px 0px;
    justify-content: flex-end;
  }

  .correction-modal-footer-password {
    display: flex;
    flex-direction: row;
    gap: 8px;
  }

  span {
    color: #c2c2c2;
  }
`;

const SameSessionVerificationModal: FC<SameSessionVerificationModalComponentProps> = ({
  props,
  closeOverlay,
  closeAllOverlays,
}) => {
  const { parameterResponseId, parameterId, verifications } = props || {};
  const dispatch = useDispatch();

  // State management following PeerVerificationAction pattern exactly
  const [selectedVerifier, setSelectedVerifier] = useState<any>(null);
  const [verifierUserType, setVerifierUserType] = useState<'LOCAL' | 'SSO' | null>(null);
  const [fetchingUserType, setFetchingUserType] = useState(false);
  const [showPasswordField, setShowPasswordField] = useState(false);
  const [showReasonField, setShowReasonField] = useState(false);
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [eligibleVerifiers, setEligibleVerifiers] = useState<any[]>([]);
  const [fetchingVerifiers, setFetchingVerifiers] = useState(true);
  const [verifierError, setVerifierError] = useState<string | null>(null);
  const [pendingAction, setPendingAction] = useState<'accept' | 'reject' | null>(null);
  const [rejectionReason, setRejectionReason] = useState<string>('');

  // Form management using react-hook-form exactly like PeerVerificationAction
  const { reset } = useForm<Inputs>({
    mode: 'onChange',
    criteriaMode: 'all',
  });

  // Extract assigned peer verifiers from existing verification data
  useEffect(() => {
    const extractAssignedVerifiers = () => {
      setFetchingVerifiers(true);
      setVerifierError(null);

      if (!verifications) {
        setVerifierError('No verification assignments found for this parameter.');
        setEligibleVerifiers([]);
        setFetchingVerifiers(false);
        return;
      }

      // Convert verifications to array if it's not already an array
      let verificationsArray: any[] = [];
      if (Array.isArray(verifications)) {
        verificationsArray = verifications;
      } else if (typeof verifications === 'object') {
        const values = Object.values(verifications);
        verificationsArray = values.reduce((acc: any[], val: any) => {
          if (Array.isArray(val)) {
            acc.push(...val);
          } else if (val !== null && val !== undefined) {
            acc.push(val);
          }
          return acc;
        }, []);
      } else {
        setVerifierError('Invalid verification data format.');
        setEligibleVerifiers([]);
        setFetchingVerifiers(false);
        return;
      }

      if (verificationsArray.length === 0) {
        setVerifierError('No verification assignments found for this parameter.');
        setEligibleVerifiers([]);
        setFetchingVerifiers(false);
        return;
      }

      // Filter for peer verifications that are in PENDING status
      const peerVerifications = verificationsArray.filter((v: any) => {
        if (!v || v === null || v === undefined) return false;

        const verificationStatus = v.verificationStatus || v.status;
        const isPending = verificationStatus === 'PENDING';

        const verificationType = v.verificationType || v.type;
        const hasRequestedTo = !!v.requestedTo;

        const isPeerVerification =
          verificationType === 'PEER' || (hasRequestedTo && !verificationType);

        return isPeerVerification && isPending;
      });

      if (peerVerifications.length === 0) {
        setVerifierError('No pending peer verifications found for this parameter.');
        setEligibleVerifiers([]);
        setFetchingVerifiers(false);
        return;
      }

      // Extract unique assigned verifiers - handle multiple requestedTo users properly
      const verifiersMap = new Map();

      peerVerifications.forEach((v: any) => {
        // Handle multiple requestedTo users in a single verification
        if (v.requestedTo) {
          // Check if requestedTo is an array (multiple users) or single user
          const requestedToUsers = Array.isArray(v.requestedTo) ? v.requestedTo : [v.requestedTo];

          requestedToUsers.forEach((user: any) => {
            if (user && user.id) {
              const verifierId = user.id;
              const verifierName =
                `${user.firstName || ''} ${user.lastName || ''}`.trim() || 'Unknown User';
              const verifierEmployeeId = user.employeeId || user.id;

              if (!verifiersMap.has(verifierId)) {
                verifiersMap.set(verifierId, {
                  id: verifierId,
                  name: verifierName,
                  employeeId: verifierEmployeeId,
                  verification: v,
                  requestedTo: user,
                });
              }
            }
          });
        } else {
          // Fallback for other field structures

          const verifierId = v.assigneeId || v.userId || v.reviewerId;
          const verifierName = v.assigneeName || v.userName || v.reviewerName || 'Unknown User';
          const verifierEmployeeId =
            v.assigneeEmployeeId || v.userEmployeeId || v.reviewerEmployeeId || verifierId;

          if (verifierId && !verifiersMap.has(verifierId)) {
            verifiersMap.set(verifierId, {
              id: verifierId,
              name: verifierName,
              employeeId: verifierEmployeeId,
              verification: v,
              requestedTo: null,
            });
          }
        }
      });

      const assignedVerifiers = Array.from(verifiersMap.values());

      if (assignedVerifiers.length === 0) {
        setVerifierError('No assigned verifiers found for peer verification.');
        setEligibleVerifiers([]);
      } else {
        setEligibleVerifiers(assignedVerifiers);
        setVerifierError(null);

        // Auto-select if only one verifier
        if (assignedVerifiers.length === 1) {
          setSelectedVerifier(assignedVerifiers[0]);
        }
      }

      setFetchingVerifiers(false);
    };

    extractAssignedVerifiers();
  }, [verifications]);

  const isGroupAssignment = eligibleVerifiers.length > 1;

  // Fetch verifier user type when verifier is selected
  useEffect(() => {
    const fetchVerifierUserType = async () => {
      if (selectedVerifier) {
        // Reset UI state when verifier changes
        setShowPasswordField(false);
        setShowReasonField(false);
        setPendingAction(null);
        setRejectionReason('');
        reset();

        setFetchingUserType(true);
        setVerifierUserType(null);

        try {
          const userDetailsResponse = await request('GET', apiGetUser(selectedVerifier.id));

          if (!userDetailsResponse.errors && userDetailsResponse.data) {
            const userData = userDetailsResponse.data;

            // Map API values to our enum values
            const rawUserType = userData.userType || 'LOCAL';
            let detectedUserType: 'LOCAL' | 'SSO';

            if (rawUserType === 'AZURE' || rawUserType === 'OKTA') {
              detectedUserType = 'SSO';
            } else {
              detectedUserType = 'LOCAL';
            }

            setVerifierUserType(detectedUserType);

            // Don't show password field immediately - wait for user to click Approve/Reject
          }
        } catch (error) {
          console.error('Failed to fetch verifier user type:', error);
          setVerifierUserType('LOCAL'); // Default to LOCAL
        } finally {
          setFetchingUserType(false);
        }
      } else {
        setVerifierUserType(null);
        setShowPasswordField(false);
        setShowReasonField(false);
        setPendingAction(null);
        setRejectionReason('');
        reset();
      }
    };

    fetchVerifierUserType();
  }, [selectedVerifier]);

  // Form submission handler following PeerVerificationAction pattern exactly
  const onSubmit = async (data: Inputs) => {
    if (!selectedVerifier && isGroupAssignment) return;

    setLoading(true);

    const {
      auth: { accessToken: initiatorToken },
    } = (window as any).store.getState();

    if (!initiatorToken) {
      console.error('No initiator token found in Redux store');
      setLoading(false);
      return;
    }

    try {
      const verifierToLogin = selectedVerifier || eligibleVerifiers[0];

      // Get user details
      const userDetailsResponse = await request('GET', apiGetUser(verifierToLogin?.id));
      if (userDetailsResponse.errors) {
        throw new Error('Failed to fetch verifier user details');
      }

      const username = userDetailsResponse.data.username;

      const lookupResponse = await request('GET', apiAccountLookUp(), {
        params: {
          username: username,
        },
      });

      if (lookupResponse.errors) {
        throw new Error('Account lookup failed');
      }

      if (lookupResponse.data.type !== 'LOCAL') {
        // SSO verifier - redirect to SSO with same session context
        const redirectUrl = new URL(lookupResponse.data.redirectUrl);
        const existingState = redirectUrl.searchParams.get('state')!;
        const formattedState = existingState.replace(/^["']|["']$/g, '').replace(/ /g, '+');

        try {
          const decompressedStateString = await decompressUrl(formattedState);
          if (!decompressedStateString || typeof decompressedStateString !== 'string') {
            throw new Error('Failed to decompress SSO state');
          }
          const parsedState = JSON.parse(decompressedStateString);

          // Add same session verification context
          parsedState.sameSession = 'true';
          parsedState.parameterExecutionId = parameterResponseId;
          parsedState.parameterId = parameterId;
          parsedState.initiatorJwtToken = initiatorToken;
          parsedState.pendingAction = pendingAction;
          parsedState.rejectionReason = rejectionReason;
          parsedState.verifierUserName = username;
          parsedState.returnTo = window.location.href; // Capture current page URL

          const compressedUpdatedState = await compressState(parsedState);
          redirectUrl.searchParams.set('state', compressedUpdatedState);
          navigate(redirectUrl.href);
        } catch (error) {
          console.error('Error processing SSO state:', error);
          throw new Error('Failed to process SSO redirect');
        }
      } else {
        // LOCAL verifier - use password authentication
        await verifierLoginUtils({
          verifierUserName: username,
          password: data.password,
          parameterResponseId: parameterResponseId!,
          initiatorToken,
          code: null,
          state: null,
          pendingAction,
          rejectionReason,
        });
      }

      closeOverlay();
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Unknown error occurred';
      dispatch(
        showNotification({
          type: NotificationType.ERROR,
          msg: `${
            pendingAction === 'reject' ? 'Rejection' : 'Verification'
          } failed: ${errorMessage}`,
          autoClose: 5000,
        }),
      );
    } finally {
      setLoading(false);
    }
  };

  // Handle reject with inline reason field
  const handleReject = () => {
    setPendingAction('reject');
    setShowReasonField(true);
  };

  // Handle reason submit - show password field (for LOCAL) or trigger SSO (for SSO)
  const handleReasonSubmit = () => {
    if (rejectionReason.trim()) {
      if (verifierUserType === 'LOCAL') {
        setShowPasswordField(true);
      } else {
        // For SSO users, trigger lookup directly
        handleSsoAction('reject');
      }
    }
  };

  // Handle SSO action (approve/reject) - call lookup directly
  const handleSsoAction = async (action: 'accept' | 'reject') => {
    if (!selectedVerifier && isGroupAssignment) return;

    setLoading(true);

    const {
      auth: { accessToken: initiatorToken },
    } = (window as any).store.getState();

    if (!initiatorToken) {
      console.error('❌ No initiator token found in Redux store');
      setLoading(false);
      return;
    }

    try {
      const verifierToLogin = selectedVerifier || eligibleVerifiers[0];

      // Get user details
      const userDetailsResponse = await request('GET', apiGetUser(verifierToLogin?.id));
      if (userDetailsResponse.errors) {
        throw new Error('Failed to fetch verifier user details');
      }

      const username = userDetailsResponse.data.username;

      const lookupResponse = await request('GET', apiAccountLookUp(), {
        params: {
          username: username,
        },
      });

      if (lookupResponse.errors) {
        throw new Error('Account lookup failed');
      }

      if (lookupResponse.data.type !== 'LOCAL') {
        // SSO verifier - redirect to SSO with same session context
        const redirectUrl = new URL(lookupResponse.data.redirectUrl);
        const existingState = redirectUrl.searchParams.get('state')!;
        const formattedState = existingState.replace(/^["']|["']$/g, '').replace(/ /g, '+');

        try {
          const decompressedStateString = await decompressUrl(formattedState);
          if (!decompressedStateString || typeof decompressedStateString !== 'string') {
            throw new Error('Failed to decompress SSO state');
          }
          const parsedState = JSON.parse(decompressedStateString);

          // Add same session verification context
          parsedState.sameSession = 'true';
          parsedState.parameterExecutionId = parameterResponseId;
          parsedState.parameterId = parameterId;
          parsedState.initiatorJwtToken = initiatorToken;
          parsedState.pendingAction = action;
          parsedState.rejectionReason = rejectionReason;
          parsedState.verifierUserName = username;
          parsedState.returnTo = window.location.href; // Capture current page URL

          const compressedUpdatedState = await compressState(parsedState);
          redirectUrl.searchParams.set('state', compressedUpdatedState);
          navigate(redirectUrl.href);
        } catch (error) {
          console.error('Error processing SSO state:', error);
          throw new Error('Failed to process SSO redirect');
        }
      } else {
        throw new Error('Expected SSO user but got LOCAL user');
      }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Unknown error occurred';
      dispatch(
        showNotification({
          type: NotificationType.ERROR,
          msg: `${action === 'reject' ? 'Rejection' : 'Verification'} failed: ${errorMessage}`,
          autoClose: 5000,
        }),
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <Wrapper>
      <BaseModal
        closeAllModals={closeAllOverlays}
        closeModal={closeOverlay}
        title="Same Session Verification"
        showFooter={false}
      >
        <div className="correction-modal-body">
          {/* Verifier Selection */}
          <div className="verifier-section">
            <label className="verifier-label">Verifier:</label>

            {fetchingVerifiers ? (
              <div className="verifier-loading">Loading verifiers...</div>
            ) : verifierError ? (
              <div className="verifier-error">{verifierError}</div>
            ) : eligibleVerifiers.length === 0 ? (
              <div className="verifier-empty">No eligible verifiers found for this parameter.</div>
            ) : (
              <Select
                value={
                  selectedVerifier
                    ? {
                        value: selectedVerifier.id,
                        label: `${selectedVerifier.name}`,
                        externalId: selectedVerifier.employeeId,
                      }
                    : null
                }
                onChange={(selectedOption) => {
                  if (selectedOption && isGroupAssignment) {
                    const verifier = eligibleVerifiers.find((v) => v.id === selectedOption.value);
                    setSelectedVerifier(verifier);
                  } else {
                    setSelectedVerifier(null);
                  }
                }}
                placeholder="Select a verifier"
                options={
                  eligibleVerifiers.map((verifier) => ({
                    ...verifier,
                    value: verifier.id,
                    label: verifier.name,
                    externalId: verifier.employeeId,
                  })) as any
                }
                isDisabled={!isGroupAssignment}
                isClearable={isGroupAssignment}
                menuPortalTarget={document.body}
              />
            )}
          </div>

          {/* Dynamic UI following PeerVerificationAction pattern exactly */}
          {showReasonField && (
            <div className="parameter-verification">
              <label className="verification-label">Reason for Rejection:</label>
              <TextInput
                className="verification-input"
                name="rejectionReason"
                placeholder="Provide reason for rejection"
                type={InputTypes.SINGLE_LINE}
                value={rejectionReason}
                onChange={({ value }) => setRejectionReason(value as string)}
                disabled={showPasswordField}
                style={showPasswordField ? { cursor: 'not-allowed' } : undefined}
              />
            </div>
          )}
          {!showPasswordField && fetchingUserType && (
            <div className="parameter-verification">
              <div className="verification-loading">Checking verifier authentication type...</div>
            </div>
          )}
        </div>
        <div className="correction-modal-footer">
          {showReasonField && !showPasswordField ? (
            <>
              <Button
                variant="secondary"
                onClick={() => {
                  setShowReasonField(false);
                  setPendingAction(null);
                  setRejectionReason('');
                }}
              >
                Cancel
              </Button>
              <Button
                variant="primary"
                onClick={handleReasonSubmit}
                disabled={!rejectionReason.trim()}
              >
                Submit
              </Button>
            </>
          ) : showPasswordField ? (
            <>
              <Button
                variant="secondary"
                onClick={() => {
                  setShowPasswordField(false);
                  setPendingAction(null);
                  setRejectionReason('');
                  reset();
                  if (pendingAction === 'reject') {
                    setShowReasonField(true);
                  }
                }}
              >
                Cancel
              </Button>
              <div className="correction-modal-footer-password">
                <PasswordInputSection handlePasswordChange={(value) => setPassword(value)} />
                <Button
                  variant="primary"
                  onClick={() => onSubmit({ password })}
                  disabled={!password || loading}
                >
                  Verify
                </Button>
              </div>
            </>
          ) : (
            !fetchingUserType && (
              <>
                <Button
                  variant="secondary"
                  color="red"
                  onClick={handleReject}
                  disabled={
                    loading ||
                    fetchingVerifiers ||
                    verifierError !== null ||
                    eligibleVerifiers.length === 0 ||
                    (isGroupAssignment && !selectedVerifier) ||
                    !verifierUserType
                  }
                >
                  Reject
                </Button>
                <Button
                  variant="primary"
                  onClick={() => {
                    setPendingAction('accept');
                    if (verifierUserType === 'LOCAL') {
                      // For LOCAL users, show password field
                      setShowPasswordField(true);
                    } else {
                      // For SSO users, trigger lookup directly
                      handleSsoAction('accept');
                    }
                  }}
                  disabled={
                    loading ||
                    fetchingVerifiers ||
                    verifierError !== null ||
                    eligibleVerifiers.length === 0 ||
                    (isGroupAssignment && !selectedVerifier) ||
                    !verifierUserType
                  }
                >
                  {loading ? 'Processing...' : 'Approve'}
                </Button>
              </>
            )
          )}
        </div>
      </BaseModal>
    </Wrapper>
  );
};

export default SameSessionVerificationModal;
